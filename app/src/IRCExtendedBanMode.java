import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum IRCExtendedBanMode {
  ALLOW_INVITE('A'),
  BLOCK_CAPS('B'),
  CTCP('C'),
  CHANNEL('j'),
  NICKS('N'),
  OPERS('o'),
  OPER_TYPES('O'),
  PART('p'),
  QUIET('q'),
  BLOCK_KICKS('Q'),
  REAL_NAME('r'),
  SERVER('s'),
  BLOCK_NOTICE('T'),
  UNREGISTERED('U'),
  TLS_CERT('z'),
  AND('&'),
  OR('|');

  private static final Map<Character, IRCExtendedBanMode> EXTENDED_BAN_MODES = new HashMap<>();

  static {
    for (IRCExtendedBanMode extendedBanModes : IRCExtendedBanMode.values()) {
      EXTENDED_BAN_MODES.put(extendedBanModes.character, extendedBanModes);
    }
  }

  private final char character;

  IRCExtendedBanMode(char character) {
    this.character = character;
  }

  public static Optional<IRCExtendedBanMode> forName(char name) {
    return Optional.ofNullable(EXTENDED_BAN_MODES.get(name));
  }
}
