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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DCCRelayPipe {

    private final Exchanger<Socket> exchanger = new Exchanger<>();
    private final CountDownLatch latch = new CountDownLatch(2);

    DCCRelayPipe() {}

    public void join(Socket socket, long timeout, TimeUnit unit) throws IOException {
        Socket other;
        try {
            other = exchanger.exchange(socket, timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } catch (TimeoutException e) {
            return;
        }

        try {
            transmit(socket, other);
        } finally {
            latch.countDown();
        }

        try {
            latch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void transmit(Socket src, Socket dest) throws IOException {
        InputStream is = src.getInputStream();
        OutputStream os = dest.getOutputStream();
        is.transferTo(os);
        os.flush();

        // these are best effort, if they fail they'll get cleaned up
        // properly when the socket is closed
        try {
            src.shutdownInput();
        } catch (IOException _) {
        }
        try {
            dest.shutdownOutput();
        } catch (IOException _) {
        }
    }
}
