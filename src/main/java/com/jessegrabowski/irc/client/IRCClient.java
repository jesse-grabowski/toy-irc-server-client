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

import com.jessegrabowski.irc.args.ArgsParser;
import com.jessegrabowski.irc.args.ArgsParserHelpRequestedException;
import com.jessegrabowski.irc.args.ArgsToken;
import com.jessegrabowski.irc.args.ArgsTokenizer;
import com.jessegrabowski.irc.client.command.model.ClientCommandDispatcher;
import com.jessegrabowski.irc.client.tui.FancyTerminalUI;
import com.jessegrabowski.irc.client.tui.STTY;
import com.jessegrabowski.irc.client.tui.StdIOTerminalUI;
import com.jessegrabowski.irc.client.tui.TerminalUI;
import com.jessegrabowski.irc.util.LoggingConfigurer;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class IRCClient {

    public static void main(String[] args) throws IOException {
        IRCClientProperties properties = parseArgs(args);

        LoggingConfigurer.configure(properties.getLogFile(), Level.parse(properties.getLogLevel()));

        TerminalUI terminalUI =
                STTY.isAvailable() && !properties.isUseSimpleTerminal() ? new FancyTerminalUI() : new StdIOTerminalUI();
        terminalUI.start();

        IRCClientEngine engine = new IRCClientEngine(properties, terminalUI);
        ClientCommandDispatcher parser = new ClientCommandDispatcher(terminalUI, engine);
        terminalUI.addInputHandler(parser::process);
        engine.start();
    }

    private static IRCClientProperties parseArgs(String[] args) {
        ArgsParser<IRCClientProperties> argsParser = ArgsParser.builder(
                        IRCClientProperties::new, true, "Pure-Java IRC Client")
                .addUsageExample("java -cp [jarfile] com.jessegrabowski.irc.client.IRCClient [options] [args]")
                .addInetAddressPositional(0, IRCClientProperties::setHost, "hostname of the IRC server", true)
                .addIntegerFlag(
                        'p', "port", IRCClientProperties::setPort, "port of the IRC server (default 6667)", false)
                .addIntegerFlag(
                        'r',
                        "read-timeout",
                        IRCClientProperties::setReadTimeout,
                        "idle timeout before closing connection (default 600000)",
                        false)
                .addIntegerFlag(
                        'c',
                        "connect-timeout",
                        IRCClientProperties::setConnectTimeout,
                        "timeout for establishing server connection (default 10000)",
                        false)
                .addCharsetFlag(
                        'C',
                        "charset",
                        IRCClientProperties::setCharset,
                        "charset used for communication with the server (default UTF-8)",
                        false)
                .addBooleanFlag(
                        's',
                        "simple-ui",
                        IRCClientProperties::setUseSimpleTerminal,
                        "use non-interactive mode (no cursor repositioning or dynamic updates; required on some terminals)",
                        false)
                .addStringFlag('n', "nickname", IRCClientProperties::setNickname, "nickname of the IRC user", false)
                .addStringFlag('R', "real-name", IRCClientProperties::setRealName, "real name of the IRC user", false)
                .addStringFlag('P', "password", IRCClientProperties::setPassword, "password for the IRC server", false)
                .addStringFlag(
                        'l',
                        "log-file",
                        IRCClientProperties::setLogFile,
                        "log file pattern, supports %u and %g formats for rotation",
                        false)
                .addStringFlag(
                        'L',
                        "log-level",
                        IRCClientProperties::setLogLevel,
                        "log level, integer or j.u.l.Level well-known name",
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
