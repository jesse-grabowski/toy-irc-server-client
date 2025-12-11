public sealed interface ClientCommand extends ArgsProperties permits ClientCommandConnect,
        ClientCommandExit, ClientCommandHelp, ClientCommandJoin, ClientCommandKick, ClientCommandMsg, ClientCommandMsgCurrent,
        ClientCommandNick, ClientCommandPart, ClientCommandQuit {}
