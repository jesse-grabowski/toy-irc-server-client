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
package com.jessegrabowski.irc.client.command.model;

import com.jessegrabowski.irc.client.IRCClientEngine;
import com.jessegrabowski.irc.client.command.ClientCommandParser;
import com.jessegrabowski.irc.client.tui.RichString;
import com.jessegrabowski.irc.client.tui.TerminalMessage;
import com.jessegrabowski.irc.client.tui.TerminalUI;
import java.awt.Color;
import java.time.LocalTime;

public class ClientCommandDispatcher {

    private final ClientCommandParser parser = new ClientCommandParser();

    private final TerminalUI terminalUI;
    private final IRCClientEngine ircClientEngine;

    public ClientCommandDispatcher(TerminalUI terminalUI, IRCClientEngine ircClientEngine) {
        this.terminalUI = terminalUI;
        this.ircClientEngine = ircClientEngine;
    }

    public void process(String line) {
        ClientCommand command;
        try {
            command = parser.parse(line);
        } catch (Exception e) {
            terminalUI.println(new TerminalMessage(
                    LocalTime.now(), RichString.f(Color.YELLOW, "SYSTEM"), null, RichString.s(e.getMessage())));
            return;
        }

        switch (command) {
            case ClientCommandHelp help
            when help.getCommand() != null -> {
                String helpText = parser.getHelpText(help.getCommand());
                terminalUI.println(new TerminalMessage(
                        LocalTime.now(), RichString.f(Color.YELLOW, "SYSTEM"), null, RichString.s(helpText)));
            }
            case ClientCommandHelp help -> {
                String helpText = parser.getHelpText();
                terminalUI.println(new TerminalMessage(
                        LocalTime.now(), RichString.f(Color.YELLOW, "SYSTEM"), null, RichString.s(helpText)));
            }
            default -> ircClientEngine.accept(command);
        }
    }
}
