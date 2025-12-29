/*
 * This project is licensed under the MIT License.
 *
 * In addition to the rights granted under the MIT License, explicit permission
 * is granted to the faculty, instructors, teaching assistants, and evaluators
 * of Ritsumeikan University for unrestricted educational evaluation and grading.
 *
 * ---------------------------------------------------------------------------
 *
 * MIT License
 *
 * Copyright (c) 2026 Jesse Grabowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jessegrabowski.irc.server.dcc;

import static org.junit.jupiter.api.Assertions.*;

import com.jessegrabowski.irc.server.IRCServerProperties;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DCCRelayEngineTest {

    DCCRelayEngine engine;
    BlockingQueue<DCCServerEvent> events;
    DCCServerEventListener eventSink;

    @BeforeEach
    void setUp() {
        engine = new DCCRelayEngine(new IRCServerProperties());
        events = new LinkedBlockingQueue<>();
        eventSink = events::add;
        engine.addListener(eventSink);
    }

    @AfterEach
    void tearDown() throws IOException {
        engine.close();
    }

    @Test
    void simpleByteExchange() throws Exception {
        UUID token = UUID.randomUUID();
        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        try (Socket receiver = new Socket(InetAddress.getLoopbackAddress(), receiverPort);
                Socket sender = new Socket(InetAddress.getLoopbackAddress(), senderPort)) {

            receiver.setSoTimeout(5000);
            sender.setSoTimeout(5000);

            byte[] senderToReceiver = "hello-from-sender".getBytes();
            byte[] receiverToSender = "ack-from-receiver".getBytes();

            CompletableFuture<byte[]> receiverReads =
                    CompletableFuture.supplyAsync(() -> readNBytes(receiver, senderToReceiver.length));
            CompletableFuture<byte[]> senderReads =
                    CompletableFuture.supplyAsync(() -> readNBytes(sender, receiverToSender.length));
            CompletableFuture<Void> senderWrites =
                    CompletableFuture.runAsync(() -> write(sender, senderToReceiver, true));
            CompletableFuture<Void> receiverWrites =
                    CompletableFuture.runAsync(() -> write(receiver, receiverToSender, true));

            senderWrites.get(2, TimeUnit.SECONDS);
            receiverWrites.get(2, TimeUnit.SECONDS);

            assertArrayEquals(senderToReceiver, receiverReads.get(2, TimeUnit.SECONDS));
            assertArrayEquals(receiverToSender, senderReads.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void simulatedDCCSend() throws Exception {
        UUID token = UUID.randomUUID();
        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        try (Socket receiver = new Socket(InetAddress.getLoopbackAddress(), receiverPort);
                Socket sender = new Socket(InetAddress.getLoopbackAddress(), senderPort)) {
            receiver.setSoTimeout(5000);
            sender.setSoTimeout(5000);

            List<byte[]> chunks = List.of(
                    "chunk-0-".getBytes(),
                    "chunk-1-".getBytes(),
                    "chunk-2-".getBytes(),
                    "chunk-3-".getBytes(),
                    "chunk-4".getBytes());
            int totalLen = chunks.stream().mapToInt(b -> b.length).sum();

            CompletableFuture<byte[]> receiverReads =
                    CompletableFuture.supplyAsync(() -> readNBytes(receiver, totalLen));

            CompletableFuture<Void> senderWrites = CompletableFuture.runAsync(() -> write(sender, chunks));

            CompletableFuture<Void> receiverAcks = CompletableFuture.runAsync(() -> sendDccAcks(receiver, chunks));

            CompletableFuture.allOf(senderWrites, receiverAcks).get(5, TimeUnit.SECONDS);

            byte[] result = receiverReads.get(5, TimeUnit.SECONDS);

            assertArrayEquals(concatenate(chunks), result);
        }
    }

    @Test
    void openSameSideTwice() throws Exception {
        UUID token = UUID.randomUUID();

        int receiverPort1 = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        assertTrue(receiverPort1 > 0);

        int receiverPort2 = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        assertEquals(-1, receiverPort2);

        int senderPort1 = engine.openForSender(token).get(5, TimeUnit.SECONDS);
        assertTrue(senderPort1 > 0);

        int senderPort2 = engine.openForSender(token).get(5, TimeUnit.SECONDS);
        assertEquals(-1, senderPort2);
    }

    @Test
    void cancelBeforeConnect() throws Exception {
        UUID token = UUID.randomUUID();

        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        engine.cancel(token);

        awaitEvent(DCCServerEventTransferClosed.class, token, Duration.ofSeconds(2));

        assertConnectFails(receiverPort);
        assertConnectFails(senderPort);
    }

    @Test
    void cancelMidTransfer() throws Exception {
        UUID token = UUID.randomUUID();
        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        try (Socket receiver = connectLoopback(receiverPort);
                Socket sender = connectLoopback(senderPort)) {

            receiver.setSoTimeout(2000);
            sender.setSoTimeout(2000);

            awaitEvent(DCCServerEventReceiverConnected.class, token, Duration.ofSeconds(2));
            awaitEvent(DCCServerEventSenderConnected.class, token, Duration.ofSeconds(2));

            byte[] payload = new byte[256 * 1024];
            Arrays.fill(payload, (byte) 0x5A);

            CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
                try {
                    OutputStream os = sender.getOutputStream();
                    for (int i = 0; i < 10000; i++) {
                        os.write(payload);
                        os.flush();
                    }
                } catch (IOException ignored) {
                }
            });

            readExactly(receiver.getInputStream(), 4096);

            engine.cancel(token);

            awaitEvent(DCCServerEventTransferClosed.class, token, Duration.ofSeconds(2));

            assertEventuallySocketCloses(receiver, Duration.ofSeconds(2));
            assertEventuallySocketCloses(sender, Duration.ofSeconds(2));

            writer.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void closeDuringTransfer() throws Exception {
        UUID token = UUID.randomUUID();
        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        Socket receiver = connectLoopback(receiverPort);
        Socket sender = connectLoopback(senderPort);
        receiver.setSoTimeout(2000);
        sender.setSoTimeout(2000);

        try (receiver;
                sender) {
            awaitEvent(DCCServerEventReceiverConnected.class, token, Duration.ofSeconds(2));
            awaitEvent(DCCServerEventSenderConnected.class, token, Duration.ofSeconds(2));

            CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
                try {
                    OutputStream os = sender.getOutputStream();
                    byte[] buf = new byte[64 * 1024];
                    while (true) {
                        os.write(buf);
                        os.flush();
                    }
                } catch (IOException ignored) {
                }
            });

            readExactly(receiver.getInputStream(), 1024);

            engine.close();

            assertEventuallySocketCloses(receiver, Duration.ofSeconds(5));
            assertEventuallySocketCloses(sender, Duration.ofSeconds(5));

            assertThrows(IllegalStateException.class, () -> engine.openForReceiver(UUID.randomUUID()));
            assertThrows(IllegalStateException.class, () -> engine.openForSender(UUID.randomUUID()));

            writer.get(2, TimeUnit.SECONDS);
        }
    }

    @Test
    void twoTokens_parallelTransfers() throws Exception {
        UUID tokenA = UUID.randomUUID();
        UUID tokenB = UUID.randomUUID();

        int recvA = engine.openForReceiver(tokenA).get(5, TimeUnit.SECONDS);
        int sendA = engine.openForSender(tokenA).get(5, TimeUnit.SECONDS);
        int recvB = engine.openForReceiver(tokenB).get(5, TimeUnit.SECONDS);
        int sendB = engine.openForSender(tokenB).get(5, TimeUnit.SECONDS);

        try (Socket receiverA = connectLoopback(recvA);
                Socket senderA = connectLoopback(sendA);
                Socket receiverB = connectLoopback(recvB);
                Socket senderB = connectLoopback(sendB)) {

            receiverA.setSoTimeout(2000);
            senderA.setSoTimeout(2000);
            receiverB.setSoTimeout(2000);
            senderB.setSoTimeout(2000);

            awaitEvent(DCCServerEventReceiverConnected.class, tokenA, Duration.ofSeconds(2));
            awaitEvent(DCCServerEventSenderConnected.class, tokenA, Duration.ofSeconds(2));
            awaitEvent(DCCServerEventReceiverConnected.class, tokenB, Duration.ofSeconds(2));
            awaitEvent(DCCServerEventSenderConnected.class, tokenB, Duration.ofSeconds(2));

            byte[] msgA = "tokenA-HELLO".getBytes();
            byte[] msgB = "tokenB-WORLD".getBytes();

            CompletableFuture<byte[]> readA = CompletableFuture.supplyAsync(() -> readNBytes(receiverA, msgA.length));
            CompletableFuture<byte[]> readB = CompletableFuture.supplyAsync(() -> readNBytes(receiverB, msgB.length));

            CompletableFuture<Void> writeA = CompletableFuture.runAsync(() -> writeAndHalfClose(senderA, msgA));
            CompletableFuture<Void> writeB = CompletableFuture.runAsync(() -> writeAndHalfClose(senderB, msgB));

            CompletableFuture.allOf(writeA, writeB).get(2, TimeUnit.SECONDS);

            assertArrayEquals(msgA, readA.get(2, TimeUnit.SECONDS));
            assertArrayEquals(msgB, readB.get(2, TimeUnit.SECONDS));
        }
    }

    @Test
    void events_areEmittedWithCorrectTokens_andTransferClosedIsExactlyOnce() throws Exception {
        UUID token = UUID.randomUUID();

        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        DCCServerEventReceiverOpened ro = awaitEvent(DCCServerEventReceiverOpened.class, token, Duration.ofSeconds(2));
        assertEquals(receiverPort, ro.getPort());

        DCCServerEventSenderOpened so = awaitEvent(DCCServerEventSenderOpened.class, token, Duration.ofSeconds(2));
        assertEquals(senderPort, so.getPort());

        try (Socket receiver = connectLoopback(receiverPort);
                Socket sender = connectLoopback(senderPort)) {

            awaitEvent(DCCServerEventReceiverConnected.class, token, Duration.ofSeconds(2));
            awaitEvent(DCCServerEventSenderConnected.class, token, Duration.ofSeconds(2));

            engine.cancel(token);
            engine.cancel(token);

            awaitEvent(DCCServerEventTransferClosed.class, token, Duration.ofSeconds(2));

            assertFalse(containsAnother(DCCServerEventTransferClosed.class, token, Duration.ofMillis(250)));
        }
    }

    @Test
    void cancelWithOnlySenderConnected() throws Exception {
        UUID token = UUID.randomUUID();
        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        try (Socket sender = connectLoopback(senderPort)) {
            sender.setSoTimeout(2000);

            awaitEvent(DCCServerEventSenderConnected.class, token, Duration.ofSeconds(2));

            engine.cancel(token);

            awaitEvent(DCCServerEventTransferClosed.class, token, Duration.ofSeconds(2));

            assertEventuallySocketCloses(sender, Duration.ofSeconds(2));
        }

        assertConnectFails(receiverPort);
        assertConnectFails(senderPort);
    }

    @Test
    void rstMidTransfer_finalizesPipe() throws Exception {
        UUID token = UUID.randomUUID();
        int receiverPort = engine.openForReceiver(token).get(5, TimeUnit.SECONDS);
        int senderPort = engine.openForSender(token).get(5, TimeUnit.SECONDS);

        Socket receiver = connectLoopback(receiverPort);
        Socket sender = connectLoopback(senderPort);
        receiver.setSoTimeout(2000);
        sender.setSoTimeout(2000);

        try (receiver;
                sender) {
            awaitEvent(DCCServerEventReceiverConnected.class, token, Duration.ofSeconds(2));
            awaitEvent(DCCServerEventSenderConnected.class, token, Duration.ofSeconds(2));

            byte[] payload = new byte[256 * 1024];
            Arrays.fill(payload, (byte) 0x33);

            CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
                try {
                    OutputStream os = sender.getOutputStream();
                    for (int i = 0; i < 128; i++) {
                        os.write(payload);
                        os.flush();
                    }
                } catch (IOException ignored) {
                }
            });

            readExactly(receiver.getInputStream(), 4096);

            // this should trigger a RST instead of a FIN
            sender.setSoLinger(true, 0);
            sender.close();

            // we need to drain the queue so that the receiver notices the reset
            try {
                receiver.getInputStream().readAllBytes();
            } catch (IOException _) {
            }

            awaitEvent(DCCServerEventTransferClosed.class, token, Duration.ofSeconds(5));

            assertEventuallySocketCloses(receiver, Duration.ofSeconds(5));

            writer.get(2, TimeUnit.SECONDS);
        }
    }

    private void sendDccAcks(Socket receiver, List<byte[]> chunks) {
        try {
            DataOutputStream dos = new DataOutputStream(receiver.getOutputStream());
            int total = 0;

            for (byte[] c : chunks) {
                total += c.length;
                dos.writeInt(total);
                dos.flush();
            }

            receiver.shutdownOutput();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void write(Socket socket, List<byte[]> data) {
        Iterator<byte[]> iter = data.iterator();
        while (iter.hasNext()) {
            write(socket, iter.next(), !iter.hasNext());
        }
    }

    private void write(Socket socket, byte[] data, boolean close) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(data);
            os.flush();
            if (close) {
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeAndHalfClose(Socket socket, byte[] data) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write(data);
            os.flush();
            socket.shutdownOutput();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readNBytes(Socket socket, int n) {
        try {
            return readExactly(socket.getInputStream(), n);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readExactly(InputStream is, int n) throws IOException {
        byte[] out = new byte[n];
        int off = 0;

        while (off < n) {
            int r = is.read(out, off, n - off);
            if (r < 0) {
                throw new EOFException("Expected " + n + " bytes, got " + off);
            }
            off += r;
        }

        return out;
    }

    private byte[] concatenate(List<byte[]> chunks) {
        int totalLength = chunks.stream().mapToInt(b -> b.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }
        return result;
    }

    private Socket connectLoopback(int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 750);
        return s;
    }

    private void assertConnectFails(int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 500);
            fail("Expected connection failure, but connect() succeeded to port " + port);
        } catch (IOException expected) {
        }
    }

    private void assertEventuallySocketCloses(Socket socket, Duration within) throws Exception {
        long deadline = System.nanoTime() + within.toNanos();
        byte[] drainBuf = new byte[64 * 1024];

        while (System.nanoTime() < deadline) {
            if (socket.isClosed()) {
                return;
            }

            try {
                InputStream is = socket.getInputStream();

                int available = is.available();
                if (available > 0) {
                    int toRead = Math.min(available, drainBuf.length);
                    int r = is.read(drainBuf, 0, toRead);
                    if (r < 0) {
                        return;
                    }
                    continue;
                }

                int r = is.read();
                if (r < 0) {
                    return;
                }
            } catch (IOException e) {
                return;
            }

            Thread.sleep(10);
        }

        fail("Socket did not close within " + within);
    }

    private <T extends DCCServerEvent> T awaitEvent(Class<T> type, UUID token, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                fail("Timed out waiting for " + type.getSimpleName() + " token=" + token);
            }

            long waitMs = Math.min(TimeUnit.NANOSECONDS.toMillis(remaining), 250);
            DCCServerEvent e = events.poll(waitMs, TimeUnit.MILLISECONDS);
            if (e == null) {
                continue;
            }

            if (type.isInstance(e) && token.equals(e.getToken())) {
                return type.cast(e);
            }

            events.add(e);
        }
    }

    private boolean containsAnother(Class<? extends DCCServerEvent> type, UUID token, Duration within)
            throws InterruptedException {
        long deadline = System.nanoTime() + within.toNanos();
        while (System.nanoTime() < deadline) {
            DCCServerEvent e = events.poll(25, TimeUnit.MILLISECONDS);
            if (e == null) {
                continue;
            }
            if (type.isInstance(e) && token.equals(e.getToken())) {
                return true;
            }
            events.add(e);
        }
        return false;
    }
}
