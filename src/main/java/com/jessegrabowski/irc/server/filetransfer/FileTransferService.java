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
import com.jessegrabowski.irc.protocol.model.FileTransferOperationHello;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationRole;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationVersion;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileTransferService implements Closeable {

    private static final Logger LOG = Logger.getLogger(FileTransferService.class.getName());
    private static final FileTransferOperationUnmarshaller UNMARSHALLER = new FileTransferOperationUnmarshaller();
    private static final FileTransferOperationMarshaller MARSHALLER = new FileTransferOperationMarshaller();

    //    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
    //            Thread.ofVirtual().name("IRCServer-FileTransfer-", 0).factory());

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Map<UUID, FileTransferPipe> pipes = new ConcurrentHashMap<>();

    public CompletableFuture<Void> accept(Socket socket) {
        return CompletableFuture.runAsync(() -> handle(socket), executor);
    }

    private void handle(Socket socket) {
        try (socket) {
            socket.setKeepAlive(true);
            socket.setSoTimeout(60000);

            DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            try {
                FileTransferOperation operation = UNMARSHALLER.unmarshal(dis);
                if (operation instanceof FileTransferOperationHello hello) {
                    if (hello.getVersion() != FileTransferOperationVersion.V1) {
                        write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.UNKNOWN_VERSION));
                        return;
                    }
                    UUID token = hello.getToken();
                    FileTransferOperationRole role = hello.getRole();
                    switch (role) {
                        case SENDER -> {
                            if (!pipes.computeIfAbsent(token, FileTransferPipe::new)
                                    .doSend(dis, dos)) {
                                write(
                                        dos,
                                        new FileTransferOperationError(
                                                FileTransferOperationErrorCode.MULTIPLE_SENDERS));
                            }
                        }
                        case RECEIVER -> {
                            if (!pipes.computeIfAbsent(token, FileTransferPipe::new)
                                    .doReceive(dis, dos)) {
                                write(
                                        dos,
                                        new FileTransferOperationError(
                                                FileTransferOperationErrorCode.MULTIPLE_RECEIVERS));
                            }
                        }
                        case UNKNOWN ->
                            write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.UNKNOWN_ROLE));
                    }
                } else {
                    write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.NO_HELLO));
                }
            } catch (FileTransferFramingException _) {
                write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.BAD_FRAMING));
            } catch (FileTransferUnknownOpCodeException _) {
                write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.UNKNOWN_OP));
            } catch (SocketTimeoutException | FileTransferTimeoutException _) {
                write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.TIMEOUT));
            } catch (IOException _) {
                write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.BROKEN_PIPE));
            } catch (InterruptedException _) {
                write(dos, new FileTransferOperationError(FileTransferOperationErrorCode.BROKEN_PIPE));
                Thread.currentThread().interrupt();
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Exception during file transfer", e);
        }
    }

    private void write(DataOutputStream os, FileTransferOperation op) throws IOException {
        synchronized (os) {
            MARSHALLER.marshal(os, op);
            os.flush();
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }
}
