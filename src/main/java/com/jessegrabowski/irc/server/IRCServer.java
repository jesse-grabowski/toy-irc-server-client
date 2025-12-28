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

import com.jessegrabowski.irc.args.ArgsParser;
import com.jessegrabowski.irc.args.ArgsParserHelpRequestedException;
import com.jessegrabowski.irc.args.ArgsToken;
import com.jessegrabowski.irc.args.ArgsTokenizer;
import com.jessegrabowski.irc.network.Acceptor;
import com.jessegrabowski.irc.util.LoggingConfigurer;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class IRCServer {
    static void main(String[] args) throws IOException {
        IRCServerProperties properties = parseArgs(args);

        LoggingConfigurer.configure(properties.getLogFile(), Level.parse(properties.getLogLevel()), true);

        IRCServerEngine engine = new IRCServerEngine(properties);
        Acceptor acceptor = new Acceptor(null, properties.getPort(), engine::accept);
        acceptor.start();
    }

    private static IRCServerProperties parseArgs(String[] args) {
        ArgsParser<IRCServerProperties> argsParser = ArgsParser.builder(
                        IRCServerProperties::new, true, "Pure-Java IRC Server")
                .addUsageExample("java -cp [jarfile] com.jessegrabowski.irc.server.IRCServer [options] [args]")
                .addPortFlag('p', "port", IRCServerProperties::setPort, "port of the IRC server (default 6667)", false)
                .addStringFlag(
                        'H',
                        "host",
                        IRCServerProperties::setHost,
                        "host name of the IRC server (default: auto-detected; not reliable behind NAT — configure explicitly for production)",
                        false)
                .addStringFlag(
                        'l',
                        "log-file",
                        IRCServerProperties::setLogFile,
                        "log file pattern, supports %u and %g formats for rotation",
                        false)
                .addStringFlag(
                        'L',
                        "log-level",
                        IRCServerProperties::setLogLevel,
                        "log level, integer or j.u.l.Level well-known name",
                        false)
                .addStringFlag('P', "password", IRCServerProperties::setPassword, "password for the IRC server", false)
                .addResourceFlag(
                        'I',
                        "isupport-properties",
                        IRCServerProperties::setIsupportProperties,
                        "location of RPL_ISUPPORT definitions (default classpath:/isupport.properties)",
                        false)
                .addStringFlag('S', "server-name", IRCServerProperties::setServer, "name of the irc server", false)
                .addStringFlag(
                        'o', "operator-name", IRCServerProperties::setOperatorName, "operator account name", false)
                .addStringFlag(
                        'O',
                        "operator-password",
                        IRCServerProperties::setOperatorPassword,
                        "operator account password",
                        false)
                .addIntegerFlag(
                        'N',
                        "nickname-history",
                        IRCServerProperties::setMaxNicknameHistory,
                        "maximum depth of nickname history (default 200)",
                        false)
                .addResourceFlag('M', "motd", IRCServerProperties::setMotd, "location of MOTD file (.txt)", false)
                .addPortFlag(
                        'D',
                        "dcc-port",
                        IRCServerProperties::setDccPort,
                        "port for DCC connections (default 49152-65535)",
                        false)
                .build();

        ArgsTokenizer tokenizer = new ArgsTokenizer();

        try {
            String syntheticRaw = tokenizer.toSyntheticRaw(args);
            List<ArgsToken> tokens = tokenizer.tokenize(args);
            return argsParser.parse(syntheticRaw, tokens);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.out.println(argsParser.getHelpText());
            System.exit(2);
        } catch (ArgsParserHelpRequestedException e) {
            System.out.println(argsParser.getHelpText());
            System.exit(0);
        }

        // this should never happen
        throw new AssertionError("Application failed to terminate on System.exit");
    }
}
