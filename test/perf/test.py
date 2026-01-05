#!/usr/bin/env python3

from __future__ import annotations

import asyncio
import ssl
import time
import statistics
from dataclasses import dataclass, field
from typing import Dict, Set, Optional, Tuple, List

# CONFIG — EDIT THESE VALUES
CONFIG = {
    # IRC connection
    "host": "localhost",
    "port": 6667,
    "use_tls": False,
    "password": None,
    "channel": "#test",
    "enable_echo_message": True,
    "clients": 20,
    "messages_per_client": 6000,
    "rate": 5,
    "start_stagger_s": 0.02,
    "verbose": False,
    "connect_timeout_s": 30.0,
    "join_timeout_s": 30.0,
    "nicklen": 9,
}

def now_s() -> float:
    return time.perf_counter()

def irc_escape(s: str) -> str:
    return s.replace("\r", "").replace("\n", "")

def parse_prefix_nick(line: str) -> Optional[str]:
    if not line.startswith(":"):
        return None
    prefix = line[1:].split(" ", 1)[0]
    if "!" in prefix:
        return prefix.split("!", 1)[0]
    return prefix or None

@dataclass
class FanoutTracker:
    n_clients: int
    sent: Dict[str, Tuple[float, Set[int], float]] = field(default_factory=dict)
    completion_latencies: List[float] = field(default_factory=list)
    recv_counts: List[int] = field(default_factory=list)

    def __post_init__(self):
        self.recv_counts = [0] * self.n_clients

    def record_send(self, msg_id: str, t_send: float) -> None:
        self.sent[msg_id] = (t_send, set(), t_send)

    def record_receive(self, msg_id: str, client_idx: int, t_recv: float) -> None:
        self.recv_counts[client_idx] += 1
        item = self.sent.get(msg_id)
        if not item:
            return

        t_send, got, t_last = item
        if client_idx in got:
            return

        got.add(client_idx)
        t_last = max(t_last, t_recv)
        self.sent[msg_id] = (t_send, got, t_last)

        if len(got) == self.n_clients:
            self.completion_latencies.append(t_last - t_send)
            del self.sent[msg_id]


class IRCClient:
    def __init__(self, idx: int, tracker: FanoutTracker):
        self.idx = idx
        self.tracker = tracker
        self.requested_nick = self._make_nick(idx, CONFIG.get("nicklen", 9))
        self.nick = self.requested_nick
        self.reader: asyncio.StreamReader
        self.writer: asyncio.StreamWriter
        self._ready = asyncio.Event()
        self._joined = asyncio.Event()
        self._cap_done = asyncio.Event()
        self._want_echo = bool(CONFIG.get("enable_echo_message", False))
        self._echo_enabled = False
        self._cap_ls_in_progress = False
        self._seen_echo_in_ls = False
        self._read_task: Optional[asyncio.Task] = None

    @staticmethod
    def _make_nick(idx: int, nicklen: int) -> str:
        base = f"l{idx:08d}"
        return base[:nicklen]

    async def connect(self) -> None:
        ssl_ctx = ssl.create_default_context() if CONFIG["use_tls"] else None
        self.reader, self.writer = await asyncio.open_connection(
            CONFIG["host"], CONFIG["port"], ssl=ssl_ctx
        )
        self._read_task = asyncio.create_task(self._read_loop())
        if self._want_echo:
            await self.send_line("CAP LS 302")
        else:
            self._cap_done.set()
        if CONFIG["password"]:
            await self.send_line(f"PASS {CONFIG['password']}")
        await self.send_line(f"NICK {self.requested_nick}")
        await self.send_line(f"USER {self.requested_nick} 0 * :latency bot")

        if self._want_echo:
            try:
                await asyncio.wait_for(self._cap_done.wait(), timeout=CONFIG["connect_timeout_s"])
            except asyncio.TimeoutError:
                await self._finish_cap()

        await asyncio.wait_for(self._ready.wait(), timeout=CONFIG["connect_timeout_s"])
        await self.send_line(f"JOIN {CONFIG['channel']}")
        await asyncio.wait_for(self._joined.wait(), timeout=CONFIG["join_timeout_s"])

    async def close(self) -> None:
        try:
            await self.send_line("QUIT :bye")
        except Exception:
            pass
        try:
            self.writer.close()
            await self.writer.wait_closed()
        except Exception:
            pass
        if self._read_task:
            self._read_task.cancel()

    async def send_line(self, line: str) -> None:
        if CONFIG["verbose"]:
            print(f"[{self.nick} >>>] {line}")
        self.writer.write((line + "\r\n").encode())
        await self.writer.drain()

    async def privmsg(self, text: str) -> None:
        await self.send_line(f"PRIVMSG {CONFIG['channel']} :{irc_escape(text)}")

    async def _finish_cap(self) -> None:
        if not self._cap_done.is_set():
            await self.send_line("CAP END")
            self._cap_done.set()

    async def _read_loop(self) -> None:
        try:
            while not self.reader.at_eof():
                raw = await self.reader.readline()
                if not raw:
                    break

                line = raw.decode(errors="ignore").strip()
                if CONFIG["verbose"]:
                    print(f"[{self.nick} <<<] {line}")

                if line.startswith("PING"):
                    token = line.split(" ", 1)[1] if " " in line else ""
                    await self.send_line(f"PONG {token}")
                    continue

                parts = line.split()

                if len(parts) >= 4 and parts[1] == "CAP":
                    subcmd = parts[3].upper()

                    if subcmd == "LS":
                        self._cap_ls_in_progress = True
                        has_continuation = (len(parts) >= 5 and parts[4] == "*")
                        caps_str = line.split(" :", 1)[1] if " :" in line else ""
                        caps = set(caps_str.split())

                        if "echo-message" in caps:
                            self._seen_echo_in_ls = True

                        if self._want_echo and self._seen_echo_in_ls and not self._echo_enabled:
                            await self.send_line("CAP REQ :echo-message")

                        if not has_continuation and self._want_echo and not self._seen_echo_in_ls:
                            await self._finish_cap()

                    elif subcmd == "ACK":
                        acked = set(line.split(" :", 1)[1].split()) if " :" in line else set()
                        if "echo-message" in acked:
                            self._echo_enabled = True
                        await self._finish_cap()

                    elif subcmd == "NAK":
                        await self._finish_cap()

                    continue

                if len(parts) >= 3 and parts[1] == "001":
                    self.nick = parts[2]
                    self._ready.set()
                    continue

                if len(parts) >= 3 and parts[1].upper() == "JOIN":
                    nick = parse_prefix_nick(line)
                    if nick == self.nick:
                        chan = parts[-1].lstrip(":")
                        if chan == CONFIG["channel"]:
                            self._joined.set()
                    continue

                if " PRIVMSG " in line and "LAT|" in line:
                    try:
                        payload = line.split(" :", 1)[1]
                        if payload.startswith("LAT|"):
                            _, msg_id, _ = payload.split("|", 2)
                            self.tracker.record_receive(msg_id, self.idx, now_s())
                    except Exception:
                        pass

        except asyncio.CancelledError:
            return
        except Exception:
            return


async def run_test():
    tracker = FanoutTracker(CONFIG["clients"])
    clients = [IRCClient(i, tracker) for i in range(CONFIG["clients"])]

    print(f"Connecting {CONFIG['clients']} clients…")
    await asyncio.gather(*(c.connect() for c in clients))
    print("All clients joined.")

    interval = 1.0 / CONFIG["rate"]

    async def sender(c: IRCClient):
        await asyncio.sleep(c.idx * CONFIG["start_stagger_s"])
        for i in range(CONFIG["messages_per_client"]):
            msg_id = f"{c.idx}-{i}-{time.time_ns()}"
            t_send = now_s()
            tracker.record_send(msg_id, t_send)
            await c.privmsg(f"LAT|{msg_id}|{t_send}")
            await asyncio.sleep(interval)

    await asyncio.gather(*(sender(c) for c in clients))

    expected = CONFIG["clients"] * CONFIG["messages_per_client"]
    while len(tracker.completion_latencies) < expected and tracker.sent:
        await asyncio.sleep(0.05)

    await asyncio.gather(*(c.close() for c in clients))

    lats = [x * 1000 for x in tracker.completion_latencies]
    print("\n=== Results ===")
    print(f"Messages completed: {len(lats)} / {expected}")
    if lats:
        print(f"Avg latency: {statistics.mean(lats):.2f} ms")
        print(f"P50: {statistics.median(lats):.2f} ms")
        print(f"P95: {sorted(lats)[int(0.95 * len(lats))]:.2f} ms")
        print(f"Max: {max(lats):.2f} ms")


def main():
    asyncio.run(run_test())


if __name__ == "__main__":
    main()
