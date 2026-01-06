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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IRCClientConnectionFactory {

    private static final Logger LOG = Logger.getLogger(IRCClientConnectionFactory.class.getName());

    public static IRCConnection create(InetAddress host, int port, Charset charset, int connectTimeout, int readTimeout)
            throws IOException {
        if (host == null) {
            throw new IllegalArgumentException("host cannot be null");
        }
        if (port <= 0 || port >= 65536) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (charset == null) {
            throw new IllegalArgumentException("charset cannot be null");
        }
        if (connectTimeout <= 0) {
            throw new IllegalArgumentException("connectTimeout must be greater than 0");
        }
        if (readTimeout <= 0) {
            throw new IllegalArgumentException("readTimeout must be greater than 0");
        }

        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSoTimeout(readTimeout);

        IRCConnection connection = null;
        try {
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            connection = new IRCConnection(socket, charset);
            return connection;
        } catch (IOException | RuntimeException e) {
            // probably an overly defensive null check but
            // for the sake of future-proofing we'll do it
            if (connection != null) {
                connection.close();
            } else {
                try {
                    socket.close();
                } catch (IOException ex) {
                    LOG.log(Level.WARNING, "Error closing IRCConnection socket", ex);
                }
            }
            throw e;
        }
    }
}
