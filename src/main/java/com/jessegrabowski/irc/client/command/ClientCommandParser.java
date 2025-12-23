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
package com.jessegrabowski.irc.client.command;

import com.jessegrabowski.irc.args.ArgsParser;
import com.jessegrabowski.irc.args.ArgsParserHelpRequestedException;
import com.jessegrabowski.irc.args.ArgsTokenizer;
import com.jessegrabowski.irc.client.command.model.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.stream.Collectors;

public class ClientCommandParser {

    private final ArgsTokenizer tokenizer = new ArgsTokenizer();

    private final SequencedMap<String, ArgsParser<? extends ClientCommand>> parsers;
    private final ArgsParser<? extends ClientCommand> defaultParser;
    private final List<String> sortedCommands;

    public ClientCommandParser() {
        defaultParser = ArgsParser.builder(ClientCommandMsgCurrent::new, false, "message the current channel")
                .addGreedyStringPositional(0, ClientCommandMsgCurrent::setText, "message text", true)
                .build();

        parsers = new LinkedHashMap<>();
        parsers.put(
                "/afk",
                ArgsParser.builder(ClientCommandAfk::new, false, "mark self away")
                        .addUsageExample("/afk [<message>]")
                        .addUsageExample("/afk")
                        .addUsageExample("/afk I'll be right back")
                        .addGreedyStringPositional(
                                0, ClientCommandAfk::setText, "message shown as reason for being away", false)
                        .build());
        parsers.put(
                "/back",
                ArgsParser.builder(ClientCommandBack::new, false, "mark self back (not away)")
                        .addUsageExample("/back")
                        .build());
        parsers.put(
                "/connect",
                ArgsParser.builder(ClientCommandConnect::new, false, "reconnect to the server")
                        .addUsageExample("/connect")
                        .build());
        parsers.put(
                "/exit",
                ArgsParser.builder(ClientCommandExit::new, false, "quit RitsIRC")
                        .addUsageExample("/exit")
                        .build());
        parsers.put(
                "/help",
                ArgsParser.builder(ClientCommandHelp::new, false, "view information about commands")
                        .addUsageExample("/help [<command>]")
                        .addUsageExample("/help")
                        .addUsageExample("/help /msg")
                        .addUsageExample("/help msg")
                        .addGreedyStringPositional(0, ClientCommandHelp::setCommand, "command to look up", false)
                        .build());
        parsers.put(
                "/join",
                ArgsParser.builder(ClientCommandJoin::new, false, "join a channel")
                        .addUsageExample("/join [-n] <channel1>[,<channel2>...] [<key1[,key2...]]")
                        .addUsageExample("/join #channel")
                        .addUsageExample("/join #protectedchan,#channel key")
                        .addUsageExample("/join -n #channel")
                        .addBooleanFlag(
                                'n', "no-switch", ClientCommandJoin::setNoSwitch, "do not switch to new channel", false)
                        .addCommaSeparatedListPositional(0, ClientCommandJoin::setChannels, "channel name(s)", true)
                        .addCommaSeparatedListPositional(
                                1,
                                ClientCommandJoin::setKeys,
                                "key to join channels (channels with a key must be the first in list)",
                                false)
                        .build());
        parsers.put(
                "/kick",
                ArgsParser.builder(ClientCommandKick::new, false, "kick a user from a channel")
                        .addUsageExample("/kick <channel> <nick> [<reason>]")
                        .addUsageExample("/kick #channel taro")
                        .addUsageExample("/kick #channel taro Kicked for speaking english")
                        .addStringPositional(0, ClientCommandKick::setChannel, "channel name", true)
                        .addStringPositional(1, ClientCommandKick::setNick, "nick to kick", true)
                        .addGreedyStringPositional(2, ClientCommandKick::setReason, "reason for kick", false)
                        .build());
        parsers.put(
                "/kill",
                ArgsParser.builder(ClientCommandKill::new, false, "disconnect a user from the server (requires OPER)")
                        .addUsageExample("/kill <nickname> <reason>")
                        .addUsageExample("/kill taro you shouldn't have done that")
                        .addStringPositional(0, ClientCommandKill::setNick, "nickname", true)
                        .addGreedyStringPositional(1, ClientCommandKill::setReason, "reason", true)
                        .build());
        parsers.put(
                "/list",
                ArgsParser.builder(ClientCommandList::new, false, "list available channels and their topics")
                        .addUsageExample("/list [<channels>]")
                        .addUsageExample("/list")
                        .addUsageExample("/list #channel1 #channel2")
                        .addGreedyListPositional(
                                0, ClientCommandList::getChannels, ClientCommandList::setChannels, "channels", false)
                        .build());
        parsers.put(
                "/me",
                ArgsParser.builder(ClientCommandAction::new, false, "send a CTCP action to the current channel")
                        .addUsageExample("/me <action>")
                        .addUsageExample("/me waves")
                        .addGreedyStringPositional(0, ClientCommandAction::setText, "message to send", true)
                        .build());
        parsers.put(
                "/mode",
                ArgsParser.builder(ClientCommandMode::new, false, "update modes on a user or channel")
                        .setFlagParsingEnabled(false)
                        .addUsageExample("/mode <target> [<modestring> [<modeargs>...]]")
                        .addUsageExample("/mode #channel")
                        .addUsageExample("/mode #channel +o nick")
                        .addUsageExample("/mode #channel +o-v nick1 nick2")
                        .addStringPositional(0, ClientCommandMode::setTarget, "target user or channel", true)
                        .addStringPositional(
                                1, ClientCommandMode::setModeString, "+<modes to add>-<modes to remove>", false)
                        .addGreedyListPositional(
                                2,
                                ClientCommandMode::getModeArguments,
                                ClientCommandMode::setModeArguments,
                                "arguments for certain channel modes",
                                false)
                        .build());
        parsers.put(
                "/msg",
                ArgsParser.builder(ClientCommandMsg::new, false, "send message to a nick or channel")
                        .addUsageExample("/msg <target>[,<target>...] <text>")
                        .addUsageExample("/msg #channel hello there")
                        .addCommaSeparatedListPositional(
                                0,
                                ClientCommandMsg::setTargets,
                                "nick or channel (may be mask, \"*\" = current channel)",
                                true)
                        .addGreedyStringPositional(1, ClientCommandMsg::setText, "text to send", true)
                        .build());
        parsers.put(
                "/nick",
                ArgsParser.builder(ClientCommandNick::new, false, "change your nickname")
                        .addUsageExample("/nick <nickname>")
                        .addUsageExample("/nick taro")
                        .addStringPositional(
                                0, ClientCommandNick::setNick, "nickname to use (rules vary by server)", true)
                        .build());
        parsers.put(
                "/notice",
                ArgsParser.builder(ClientCommandNotice::new, false, "send a notice to a nick or channel")
                        .addUsageExample("/notice <target>[,<target>...] <text>")
                        .addUsageExample("/notice #channel sink's on fire")
                        .addCommaSeparatedListPositional(
                                0,
                                ClientCommandNotice::setTargets,
                                "nick or channel (may be mask, \"*\" = current channel)",
                                true)
                        .addGreedyStringPositional(1, ClientCommandNotice::setText, "text to send", true)
                        .build());
        parsers.put(
                "/oper",
                ArgsParser.builder(ClientCommandOper::new, false, "become a server operator")
                        .addUsageExample("/oper <name> <password>")
                        .addStringPositional(0, ClientCommandOper::setName, "operator name", true)
                        .addStringPositional(1, ClientCommandOper::setPassword, "operator password", true)
                        .build());
        parsers.put(
                "/part",
                ArgsParser.builder(ClientCommandPart::new, false, "leave a channel")
                        .addUsageExample("/part <channel>[,<channel>...] [<reason>]")
                        .addUsageExample("/part #channel")
                        .addUsageExample("/part #channel1,#channel2")
                        .addUsageExample("/part #channel dinner time")
                        .addCommaSeparatedListPositional(0, ClientCommandPart::setChannels, "channels to leave", true)
                        .addGreedyStringPositional(1, ClientCommandPart::setReason, "reason for leaving", false)
                        .build());
        parsers.put(
                "/quit",
                ArgsParser.builder(ClientCommandQuit::new, false, "disconnect from the server")
                        .addUsageExample("/quit [<reason>]")
                        .addUsageExample("/quit")
                        .addUsageExample("/quit jobs done")
                        .addGreedyStringPositional(0, ClientCommandQuit::setReason, "reason for disconnection", false)
                        .build());
        parsers.put(
                "/topic",
                ArgsParser.builder(ClientCommandTopic::new, false, "view or set a channel's topic")
                        .addUsageExample("/topic <channel> [<topic>]")
                        .addUsageExample("/topic")
                        .addUsageExample("/topic This is my new topic")
                        .addUsageExample("/topic -c #chan")
                        .addUsageExample("/topic -c #chan This is my new topic")
                        .addStringFlag('c', "channel", ClientCommandTopic::setChannel, "channel to view/modify", false)
                        .addGreedyStringPositional(0, ClientCommandTopic::setTopic, "topic to set", false)
                        .build());

        List<String> commands = new ArrayList<>(parsers.keySet());
        commands.sort((a, b) -> Integer.compare(b.length(), a.length()));
        this.sortedCommands = commands;
    }

    public ClientCommand parse(String command) throws ArgsParserHelpRequestedException {
        for (String prefix : sortedCommands) {
            if (command.startsWith(prefix)) {
                String suffix = command.substring(prefix.length());
                return parsers.get(prefix).parse(suffix, tokenizer.tokenize(suffix));
            }
        }
        return defaultParser.parse(command, tokenizer.tokenize(command));
    }

    public String getHelpText() {
        String commandHelps = parsers.entrySet().stream()
                .map(e -> "\t%s: %s".formatted(e.getKey(), e.getValue().getDescription()))
                .collect(Collectors.joining("\n"));
        return """
                RitsIRC client commands. You may view more information about a given command using '/help <command>'

                Commands:
                %s""".formatted(commandHelps);
    }

    public String getHelpText(String command) {
        String cleanCommand = command.trim();
        if (!cleanCommand.startsWith("/")) {
            cleanCommand = "/" + cleanCommand;
        }
        if (parsers.containsKey(cleanCommand)) {
            return parsers.get(cleanCommand).getHelpText();
        } else {
            return """
                    Could not find command '%s'

                    %s""".formatted(cleanCommand, parsers.get(cleanCommand).getHelpText());
        }
    }
}
