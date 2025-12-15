package com.jessegrabowski.irc.client.command.model;

import com.jessegrabowski.irc.client.IRCClientEngine;
import com.jessegrabowski.irc.client.tui.RichString;
import com.jessegrabowski.irc.client.tui.TerminalMessage;
import com.jessegrabowski.irc.client.tui.TerminalUI;
import com.jessegrabowski.irc.client.command.ClientCommandParser;

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
      terminalUI.println(
          new TerminalMessage(
              LocalTime.now(),
              RichString.f(Color.YELLOW, "SYSTEM"),
              null,
              RichString.s(e.getMessage())));
      return;
    }

    switch (command) {
      case ClientCommandHelp help when help.getCommand() != null -> {
        String helpText = parser.getHelpText(help.getCommand());
        terminalUI.println(
            new TerminalMessage(
                LocalTime.now(),
                RichString.f(Color.YELLOW, "SYSTEM"),
                null,
                RichString.s(helpText)));
      }
      case ClientCommandHelp help -> {
        String helpText = parser.getHelpText();
        terminalUI.println(
            new TerminalMessage(
                LocalTime.now(),
                RichString.f(Color.YELLOW, "SYSTEM"),
                null,
                RichString.s(helpText)));
      }
      default -> ircClientEngine.accept(command);
    }
  }
}
