package com.jessegrabowski.irc;

import java.util.Optional;

public enum IRCChannelMode {
  BAN,
  EXCEPTION,
  CLIENT_LIMIT,
  INVITE_ONLY,
  INVITE_EXCEPTION,
  KEY,
  MODERATED,
  SECRET,
  PROTECTED,
  NO_EXTERNAL_MESSAGES;

  public static Optional<IRCChannelMode> forName(IRCServerParameters parameters, char name) {
    return switch (name) {
      case 'b' -> Optional.of(BAN);
      case 'l' -> Optional.of(CLIENT_LIMIT);
      case 'i' -> Optional.of(INVITE_ONLY);
      case 'k' -> Optional.of(KEY);
      case 'm' -> Optional.of(MODERATED);
      case 's' -> Optional.of(SECRET);
      case 't' -> Optional.of(PROTECTED);
      case 'n' -> Optional.of(NO_EXTERNAL_MESSAGES);
      default -> {
        if (parameters.getExcepts() == name) {
          yield Optional.of(EXCEPTION);
        } else if (parameters.getInviteExceptions() == name) {
          yield Optional.of(INVITE_EXCEPTION);
        } else {
          yield Optional.empty();
        }
      }
    };
  }
}
