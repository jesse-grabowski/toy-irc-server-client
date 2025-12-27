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
package com.jessegrabowski.irc.server.filetransfer;

import com.jessegrabowski.irc.protocol.FileTransferFramingException;
import com.jessegrabowski.irc.protocol.FileTransferOperationMarshaller;
import com.jessegrabowski.irc.protocol.FileTransferOperationUnmarshaller;
import com.jessegrabowski.irc.protocol.FileTransferUnknownOpCodeException;
import com.jessegrabowski.irc.protocol.model.FileTransferOperation;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationError;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationErrorCode;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationGoodbye;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationISend;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationReady;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FileTransferPipe {

    private static final long MAX_FILE_SIZE = 10000000; // 10 MB

    private static final FileTransferOperationMarshaller MARSHALLER = new FileTransferOperationMarshaller();
    private static final FileTransferOperationUnmarshaller UNMARSHALLER = new FileTransferOperationUnmarshaller();

    private final AtomicReference<Endpoint> sender = new AtomicReference<>();
    private final AtomicReference<Endpoint> receiver = new AtomicReference<>();
    private final CountDownLatch latch = new CountDownLatch(2);

    private final UUID token;

    public FileTransferPipe(UUID token) {
        this.token = token;
    }

    public UUID getToken() {
        return token;
    }

    public boolean doSend(DataInputStream is, DataOutputStream os)
            throws IOException, InterruptedException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        Endpoint me = new Endpoint(is, os);
        if (!sender.compareAndSet(null, me)) {
            return false;
        }

        latch.countDown();
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new FileTransferTimeoutException("Timed out waiting for receiver");
        }

        write(os, new FileTransferOperationReady());

        relay(me, receiver.get());

        return true;
    }

    public boolean doReceive(DataInputStream is, DataOutputStream os)
            throws IOException, InterruptedException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        Endpoint me = new Endpoint(is, os);
        if (!receiver.compareAndSet(null, me)) {
            return false;
        }

        latch.countDown();
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new FileTransferTimeoutException("Timed out waiting for sender");
        }

        write(os, new FileTransferOperationReady());

        relay(me, sender.get());

        return true;
    }

    private void relay(Endpoint source, Endpoint destination)
            throws IOException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        while (true) {
            try {
                FileTransferOperation operation = UNMARSHALLER.unmarshal(source.is());
                switch (operation) {
                    case FileTransferOperationGoodbye op -> {
                        try {
                            write(destination.os(), op);
                        } catch (IOException _) {
                        }
                        return;
                    }
                    case FileTransferOperationISend op -> {
                        if (op.getSize() < 0) {
                            write(
                                    source.os(),
                                    new FileTransferOperationError(FileTransferOperationErrorCode.FILE_TOO_SMALL));
                        } else if (op.getSize() > MAX_FILE_SIZE) {
                            write(
                                    source.os(),
                                    new FileTransferOperationError(FileTransferOperationErrorCode.FILE_TOO_LARGE));
                        } else {
                            write(destination.os(), op);
                            transferExactly(source.is(), destination.os(), op.getSize());
                        }
                    }
                    default -> write(destination.os(), operation);
                }
            } catch (IOException | FileTransferFramingException | FileTransferUnknownOpCodeException e) {
                try {
                    destination.is().close();
                } catch (IOException _) {
                }
                throw e;
            }
        }
    }

    private void write(DataOutputStream os, FileTransferOperation op) throws IOException {
        synchronized (os) {
            MARSHALLER.marshal(os, op);
            os.flush();
        }
    }

    private void transferExactly(InputStream in, OutputStream out, long bytes) throws IOException {
        byte[] buffer = new byte[16 * 1024];
        long remaining = bytes;

        synchronized (out) {
            while (remaining > 0) {
                int toRead = (int) Math.min(buffer.length, remaining);
                int read = in.read(buffer, 0, toRead);

                if (read == -1) {
                    throw new IOException("Unexpected EOF: needed " + remaining + " more bytes");
                }

                out.write(buffer, 0, read);
                remaining -= read;
            }
            out.flush();
        }
    }

    private record Endpoint(DataInputStream is, DataOutputStream os) {}
}
