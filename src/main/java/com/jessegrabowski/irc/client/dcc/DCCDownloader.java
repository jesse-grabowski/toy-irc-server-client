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
package com.jessegrabowski.irc.client.dcc;

import com.jessegrabowski.irc.util.Directory;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DCCDownloader {

    private static final Logger LOG = Logger.getLogger(DCCDownloader.class.getName());
    private static final Executor EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final Directory downloadDirectory;
    private final String filename;
    private final String host;
    private final int port;
    private final Long length;

    public DCCDownloader(Directory downloadDirectory, String filename, String host, int port, Long length) {
        this.downloadDirectory = downloadDirectory;
        this.filename = filename;
        this.host = host;
        this.port = port;
        this.length = length;
    }

    public void fetchAsync() {
        EXECUTOR.execute(this::download);
    }

    private void download() {
        Path file = null;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 60000);
            socket.setSoTimeout(60000);

            file = downloadDirectory.createFile(filename);
            Files.createDirectories(file.getParent());

            try (InputStream socketIn = new BufferedInputStream(socket.getInputStream());
                    DataOutputStream socketOut =
                            new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
                    OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(
                            file,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE))) {

                byte[] buf = new byte[64 * 1024];
                long received = 0L;
                int ackCounter = 0;

                while (true) {
                    int n = socketIn.read(buf);
                    if (n == -1) break;

                    fileOut.write(buf, 0, n);
                    received += n;

                    int ack = (int) (received & 0xFFFF_FFFFL);
                    socketOut.writeInt(ack);

                    if ((++ackCounter & 0x0F) == 0) { // every 16 chunks
                        socketOut.flush();
                    }

                    if (length != null && length > 0 && received >= length) break;
                }

                fileOut.flush();
                socketOut.flush();

                if (length != null && length > 0 && received < length) {
                    throw new EOFException("Connection closed early: received " + received + " / expected " + length);
                }

                LOG.info("Successfully downloaded " + filename + " (" + received + " bytes) to " + file);
            }
        } catch (SocketTimeoutException e) {
            LOG.log(Level.WARNING, "Download timed out" + (file != null ? " (" + file + ")" : ""), e);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Error downloading file" + (file != null ? " (" + file + ")" : ""), e);
        }
    }
}
