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
package com.jessegrabowski.irc.network;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class IRCConnectionTest {

    @Test
    void callsShutdownHandlersOnClose() throws IOException {
        AtomicBoolean handlerANotified = new AtomicBoolean();
        AtomicBoolean handlerBNotified = new AtomicBoolean();

        PipedInputStream is = new PipedInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.addShutdownHandler(() -> handlerANotified.set(true));
        connection.addShutdownHandler(() -> handlerBNotified.set(true));
        connection.start();
        connection.close();

        assertTrue(handlerANotified.get());
        assertTrue(handlerBNotified.get());
    }

    @Test
    void callsIngressHandlersOnRead() throws IOException, InterruptedException {
        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(2);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        inWriter.print("line1\r\n");
        inWriter.print("line2\r\n");
        inWriter.flush();
        assertTrue(accumulator.await(5, TimeUnit.SECONDS));

        connection.close();

        assertEquals(List.of("line1", "line2"), accumulator.getLines());
    }

    @Test
    void printsToSocketOnWrite() throws IOException {
        PipedInputStream is = new PipedInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(2);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        connection.offer("line1");
        connection.offer("line2");
        connection.close();

        assertEquals("line1\r\nline2\r\n", os.toString(StandardCharsets.UTF_8));
    }

    @Test
    void doesNotDeliverPartialLineOnEof() throws IOException, InterruptedException {
        PipedInputStream is = new PipedInputStream();
        PipedOutputStream pos = new PipedOutputStream(is);
        PrintWriter inWriter = new PrintWriter(pos);
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(1);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        inWriter.print("partial");
        inWriter.flush();
        pos.close();

        assertFalse(accumulator.await(250, TimeUnit.MILLISECONDS));
        connection.close();

        assertEquals(List.of(), accumulator.getLines());
    }

    @Test
    void deliversEmptyLineOnCrlf() throws IOException, InterruptedException {
        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(1);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        inWriter.print("\r\n");
        inWriter.flush();

        assertTrue(accumulator.await(5, TimeUnit.SECONDS));
        connection.close();

        assertEquals(List.of(""), accumulator.getLines());
    }

    @Test
    void treatsSoloLfAsCharacterNotDelimiter() throws IOException, InterruptedException {
        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(1);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        inWriter.print("a\nb\r\n");
        inWriter.flush();

        assertTrue(accumulator.await(5, TimeUnit.SECONDS));
        connection.close();

        assertEquals(List.of("a\nb"), accumulator.getLines());
    }

    @Test
    void treatsSoloCrAsCharacterNotDelimiter() throws IOException, InterruptedException {
        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(1);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        inWriter.print("a\rb\r\r\n");
        inWriter.flush();

        assertTrue(accumulator.await(5, TimeUnit.SECONDS));
        connection.close();

        assertEquals(List.of("a\rb\r"), accumulator.getLines());
    }

    @Test
    void deliversMultipleLinesIncludingEmpty() throws IOException, InterruptedException {
        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(3);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        inWriter.print("a\r\n\r\nb\r\n");
        inWriter.flush();

        assertTrue(accumulator.await(5, TimeUnit.SECONDS));
        connection.close();

        assertEquals(List.of("a", "", "b"), accumulator.getLines());
    }

    @Test
    void truncatesOverlongLine() throws IOException, InterruptedException {
        int max = 10000;
        String over = "x".repeat(max + 250);

        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        LineAccumulator accumulator = new LineAccumulator(1);
        connection.addIngressHandler(accumulator::add);
        connection.start();

        inWriter.print(over);
        inWriter.print("\r\n");
        inWriter.flush();

        assertTrue(accumulator.await(5, TimeUnit.SECONDS));
        connection.close();

        assertEquals(1, accumulator.getLines().size());
        assertEquals(max, accumulator.getLines().getFirst().length());
    }

    @Test
    void closesAndCallsShutdownHandlersIfIngressHandlerThrows() throws IOException, InterruptedException {
        AtomicBoolean shutdownNotified = new AtomicBoolean();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.addIngressHandler(line -> {
            throw new RuntimeException();
        });
        connection.addShutdownHandler(() -> {
            shutdownNotified.set(true);
            shutdownLatch.countDown();
        });
        connection.start();

        inWriter.print("line\r\n");
        inWriter.flush();

        assertTrue(shutdownLatch.await(5, TimeUnit.SECONDS));
        connection.close();

        assertTrue(shutdownNotified.get());
    }

    @Test
    void doesNotCallLaterIngressHandlersIfEarlierThrows() throws IOException, InterruptedException {
        AtomicBoolean laterCalled = new AtomicBoolean();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        PipedInputStream is = new PipedInputStream();
        PrintWriter inWriter = new PrintWriter(new PipedOutputStream(is));
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.addIngressHandler(line -> {
            throw new RuntimeException("boom");
        });
        connection.addIngressHandler(line -> laterCalled.set(true));
        connection.addShutdownHandler(shutdownLatch::countDown);
        connection.start();

        inWriter.print("line\r\n");
        inWriter.flush();

        assertTrue(shutdownLatch.await(5, TimeUnit.SECONDS));
        connection.close();

        assertFalse(laterCalled.get());
    }

    @Test
    void offerReturnsFalseBeforeStart() throws IOException {
        PipedInputStream is = new PipedInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));

        assertFalse(connection.offer("line"));
        connection.close();
    }

    @Test
    void offerReturnsFalseAfterClose() throws IOException {
        PipedInputStream is = new PipedInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.start();
        connection.close();

        assertFalse(connection.offer("line"));
    }

    /*
    @Test
    void doesNotAllowMoreThanTwentyQueuedMessages() throws IOException, InterruptedException {
        PipedInputStream is = new PipedInputStream();
        BlockingOutputStream os = new BlockingOutputStream();
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.start();

        os.blockWrites();

        int ok = 0;
        for (int i = 0; i < 21; i++) {
            if (connection.offer("line" + i)) {
                ok++;
            }
        }
        boolean overflow = connection.offer("overflow");

        os.unblockWrites();
        connection.close();

        // we don't have a way to block queue consumption
        // so we may enqueue up to 21 before it gets stuck
        // on the blocking stream
        assertTrue(ok == 20 || ok == 21);
        assertFalse(overflow);
    }

    @Test
    void drainsEgressQueueOnClose() throws IOException {
        PipedInputStream is = new PipedInputStream();
        BlockingOutputStream os = new BlockingOutputStream();
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.start();

        os.blockWrites();

        for (int i = 0; i < 10; i++) {
            assertTrue(connection.offer("line" + i));
        }

        CompletableFuture<Void> future = connection.closeDeferred();
        os.unblockWrites();
        future.join();

        String out = os.toString();
        for (int i = 0; i < 10; i++) {
            assertTrue(out.contains("line" + i + "\r\n"));
        }
    }
     */

    @Test
    void closeDeferredCompletes() throws IOException {
        PipedInputStream is = new PipedInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.start();

        assertDoesNotThrow(() -> connection.closeDeferred().get(5, TimeUnit.SECONDS));
    }

    @Test
    void callingCloseTwiceRunsShutdownHandlersOnce() throws IOException {
        AtomicInteger calls = new AtomicInteger();

        PipedInputStream is = new PipedInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.addShutdownHandler(calls::incrementAndGet);
        connection.start();

        connection.close();
        connection.close();

        assertEquals(1, calls.get());
    }

    @Test
    void startThrowsIfCalledTwice() throws IOException {
        PipedInputStream is = new PipedInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.start();

        assertThrows(IllegalStateException.class, connection::start);
        connection.close();
    }

    @Test
    void socketTimeoutOnIngressClosesConnection() throws IOException, InterruptedException {
        AtomicBoolean shutdownNotified = new AtomicBoolean();
        CountDownLatch shutdownLatch = new CountDownLatch(1);

        InputStream is = new TimeoutInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream(8192);
        IRCConnection connection = new IRCConnection(new Mocket(is, os));
        connection.addShutdownHandler(() -> {
            shutdownNotified.set(true);
            shutdownLatch.countDown();
        });
        connection.start();

        assertTrue(shutdownLatch.await(5, TimeUnit.SECONDS));
        connection.close();

        assertTrue(shutdownNotified.get());
    }

    private static class LineAccumulator {
        private final List<String> lines = new ArrayList<>();

        private final CountDownLatch latch;

        public LineAccumulator(int expectedLines) {
            this.latch = new CountDownLatch(expectedLines);
        }

        public synchronized void add(String line) {
            lines.add(line);
            latch.countDown();
        }

        public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public List<String> getLines() {
            return lines;
        }
    }

    private static class BlockingOutputStream extends OutputStream {
        private final ByteArrayOutputStream delegate = new ByteArrayOutputStream(8192);
        private final CountDownLatch block = new CountDownLatch(1);
        private final AtomicBoolean shouldBlock = new AtomicBoolean(false);

        public void blockWrites() {
            shouldBlock.set(true);
        }

        public void unblockWrites() {
            shouldBlock.set(false);
            block.countDown();
        }

        @Override
        public void write(int b) throws IOException {
            if (shouldBlock.get()) {
                try {
                    block.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            delegate.write(b);
        }

        @Override
        public String toString() {
            return delegate.toString(StandardCharsets.UTF_8);
        }
    }

    private static class TimeoutInputStream extends InputStream {
        private final AtomicBoolean thrown = new AtomicBoolean(false);

        @Override
        public int read() throws IOException {
            if (thrown.compareAndSet(false, true)) {
                throw new SocketTimeoutException("timeout");
            }
            return -1;
        }
    }

    private static class Mocket extends Socket {

        private final AtomicBoolean closed = new AtomicBoolean(false);

        private final InputStream is;
        private final OutputStream os;

        public Mocket(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
        }

        @Override
        public boolean isClosed() {
            return closed.get();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return is;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return os;
        }

        @Override
        public void shutdownInput() throws IOException {
            is.close();
        }

        @Override
        public void close() throws IOException {
            if (closed.compareAndSet(false, true)) {
                is.close();
                os.close();
            }
        }
    }
}
