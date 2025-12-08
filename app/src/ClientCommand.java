public sealed interface ClientCommand extends ArgsProperties permits ClientCommandConnect,
        ClientCommandExit, ClientCommandHelp, ClientCommandJoin, ClientCommandMsg, ClientCommandMsgCurrent,
        ClientCommandNick, ClientCommandQuit {}
