package com.jessegrabowski.irc.client.command.model;

import com.jessegrabowski.irc.args.ArgsProperties;

public sealed interface ClientCommand extends ArgsProperties
    permits ClientCommandConnect,
        ClientCommandExit,
        ClientCommandHelp,
        ClientCommandJoin,
        ClientCommandKick,
        ClientCommandMode,
        ClientCommandMsg,
        ClientCommandMsgCurrent,
        ClientCommandNick,
        ClientCommandNotice,
        ClientCommandOper,
        ClientCommandPart,
        ClientCommandQuit,
        ClientCommandTopic {}
