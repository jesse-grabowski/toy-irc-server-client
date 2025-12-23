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
package com.jessegrabowski.irc.client;

import com.jessegrabowski.irc.args.ArgsProperties;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.logging.Level;

// oh lombok how my heart yearns for thee
public class IRCClientProperties implements ArgsProperties {

    private InetAddress host;
    private int port = 6667;
    private int connectTimeout = 10000;
    private int readTimeout = 10 * 60 * 1000;
    private Charset charset = StandardCharsets.UTF_8;
    private boolean useSimpleTerminal = false;
    private String nickname = "auto";
    private String realName = "Unknown";
    private String password;
    private String logFile = "irc-client.log";
    private String logLevel = Level.INFO.getName();
    private InetAddress myAddress;

    @Override
    public void validate() {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("connectTimeout must be greater than 0");
        }
        if (readTimeout < 0) {
            throw new IllegalArgumentException("readTimeout must be greater than 0");
        }
        try {
            Paths.get(logFile);
        } catch (Exception e) {
            throw new IllegalArgumentException("log file path must be a valid path");
        }
        try {
            Level.parse(logLevel);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "invalid log level '%s': expected integer or valid jul level name".formatted(logLevel));
        }
    }

    public InetAddress getHost() {
        return host;
    }

    public void setHost(InetAddress host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public boolean isUseSimpleTerminal() {
        return useSimpleTerminal;
    }

    public void setUseSimpleTerminal(boolean useSimpleTerminal) {
        this.useSimpleTerminal = useSimpleTerminal;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setLogFile(String logFile) {
        this.logFile = logFile;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public InetAddress getMyAddress() {
        return myAddress;
    }

    public void setMyAddress(InetAddress myAddress) {
        this.myAddress = myAddress;
    }
}
