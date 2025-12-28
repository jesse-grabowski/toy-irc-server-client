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
package com.jessegrabowski.irc.server;

import com.jessegrabowski.irc.args.ArgsProperties;
import com.jessegrabowski.irc.network.Port;
import com.jessegrabowski.irc.util.Resource;
import java.nio.file.Paths;
import java.util.logging.Level;

public class IRCServerProperties implements ArgsProperties {
    private String host = "127.0.0.1";
    private String server = "ritsirc";
    private Port port = new Port.FixedPort(6667);
    private String password;
    private String logFile = "irc-server.log";
    private String logLevel = Level.INFO.getName();
    private Resource isupportProperties = Resource.of("classpath:/isupport.properties");
    private int pingFrequencyMilliseconds = 60000;
    private int maxIdleMilliseconds = 300000;
    private String operatorName = "admin";
    private String operatorPassword = "password";
    private int maxNicknameHistory = 200;
    private Resource motd = Resource.of("classpath:/motd.txt");
    // use the IANA dynamic/private port range by default
    private Port dccPortRange = new Port.PortRange(49152, 65535);

    @Override
    public void validate() {
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
        if (pingFrequencyMilliseconds < 0) {
            throw new IllegalArgumentException("pingFrequencyMilliseconds must be greater than or equal to 0");
        }
        if (maxIdleMilliseconds < 0) {
            throw new IllegalArgumentException("maxIdleMilliseconds must be greater than or equal to 0");
        }
        if (maxNicknameHistory < 1) {
            throw new IllegalArgumentException("maxNicknameHistory must be greater than 0");
        }
        if (dccPortRange instanceof Port.PortRange(int start, int end)) {
            if (end - start < 32) {
                throw new IllegalArgumentException("dccPortRange must contain at least 32 ports");
            }
        } else {
            throw new IllegalArgumentException("dccPortRange must be a range of ports");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public Port getPort() {
        return port;
    }

    public void setPort(Port port) {
        this.port = port;
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

    public Resource getIsupportProperties() {
        return isupportProperties;
    }

    public void setIsupportProperties(Resource isupportProperties) {
        this.isupportProperties = isupportProperties;
    }

    public int getPingFrequencyMilliseconds() {
        return pingFrequencyMilliseconds;
    }

    public void setPingFrequencyMilliseconds(int pingFrequencyMilliseconds) {
        this.pingFrequencyMilliseconds = pingFrequencyMilliseconds;
    }

    public int getMaxIdleMilliseconds() {
        return maxIdleMilliseconds;
    }

    public void setMaxIdleMilliseconds(int maxIdleMilliseconds) {
        this.maxIdleMilliseconds = maxIdleMilliseconds;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getOperatorPassword() {
        return operatorPassword;
    }

    public void setOperatorPassword(String operatorPassword) {
        this.operatorPassword = operatorPassword;
    }

    public int getMaxNicknameHistory() {
        return maxNicknameHistory;
    }

    public void setMaxNicknameHistory(int maxNicknameHistory) {
        this.maxNicknameHistory = maxNicknameHistory;
    }

    public Resource getMotd() {
        return motd;
    }

    public void setMotd(Resource motd) {
        this.motd = motd;
    }

    public Port getDccPortRange() {
        return dccPortRange;
    }

    public void setDccPortRange(Port dccPortRange) {
        this.dccPortRange = dccPortRange;
    }
}
