import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArgsTokenizer {

  /**
   * Create synthetic "raw" text for pre-tokenized input, suitable for use with {@link ArgsParser}.
   */
  public String toSyntheticRaw(String[] input) {
    Objects.requireNonNull(input);

    return String.join(" ", input);
  }

  /**
   * Wrap pre-tokenized input.
   *
   * <p>Token indexes point to the corresponding location in text generated using {@link
   * #toSyntheticRaw(String[])}
   */
  public List<ArgsToken> tokenize(String[] input) {
    Objects.requireNonNull(input);

    List<ArgsToken> tokens = new ArrayList<>();

    int index = 0;
    for (String token : input) {
      tokens.add(new ArgsToken(token, index, index + token.length()));
      index += token.length() + 1;
    }

    return tokens;
  }

  /**
   * Split an input line of text into a list of tokens, respecting quotes for grouping.
   *
   * <p>The token index ranges include the surrounding quote characters if a token was quoted, which
   * is useful for "raw" / greedy parsing. The token content itself omits quotes and applies
   * backslash escapes for simpler downstream handling.
   */
  public List<ArgsToken> tokenize(String input) {
    Objects.requireNonNull(input);

    char[] chars = input.toCharArray();
    List<ArgsToken> result = new ArrayList<>();

    int startIndex = -1;
    boolean quoted = false;
    boolean escaped = false;
    StringBuilder tokenBuilder = null;
    for (int i = 0; i < chars.length; i++) {
      char c = chars[i];
      if (Character.isWhitespace(c)) {
        if (tokenBuilder == null) {
          continue;
        }

        if (escaped) {
          tokenBuilder.append(c);
          escaped = false;
        } else if (quoted) {
          tokenBuilder.append(c);
        } else {
          result.add(new ArgsToken(tokenBuilder.toString(), startIndex, i));
          tokenBuilder = null;
        }
        continue;
      }
      if (tokenBuilder == null) {
        tokenBuilder = new StringBuilder();
        startIndex = i;
      }
      if (escaped) {
        tokenBuilder.append(c);
        escaped = false;
      } else if (c == '\\') {
        escaped = true;
      } else if (c == '"') {
        quoted = !quoted;
      } else {
        tokenBuilder.append(c);
      }
    }

    if (tokenBuilder != null) {
      if (escaped) {
        tokenBuilder.append('\\');
      }
      result.add(new ArgsToken(tokenBuilder.toString(), startIndex, chars.length));
    }

    return result;
  }
}
