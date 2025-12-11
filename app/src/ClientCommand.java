public sealed interface ClientCommand extends ArgsProperties permits ClientCommandConnect,
        ClientCommandExit, ClientCommandHelp, ClientCommandJoin, ClientCommandKick, ClientCommandMode, ClientCommandMsg, ClientCommandMsgCurrent,
        ClientCommandNick, ClientCommandNotice, ClientCommandPart, ClientCommandQuit {}
