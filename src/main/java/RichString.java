import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** String with additional formatting information for use in terminal messages */
public abstract sealed class RichString
    permits RichString.TextRichString,
        RichString.CompositeRichString,
        RichString.BackgroundColorRichString,
        RichString.ForegroundColorRichString,
        RichString.BoldRichString {

  public static RichString s(Object arg0, Object... args) {
    RichString r0 = arg0 instanceof RichString r ? r : new TextRichString(arg0.toString());
    if (args.length == 0) {
      return r0;
    }

    RichString[] children = new RichString[args.length + 1];
    children[0] = r0;
    for (int i = 0; i < args.length; i++) {
      if (args[i] instanceof RichString r2) {
        children[i + 1] = r2;
      } else {
        children[i + 1] = new TextRichString(args[i].toString());
      }
    }
    return new CompositeRichString(children);
  }

  public static RichString j(Object del, RichString... args) {
    if (args.length == 0) {
      return new TextRichString("");
    }
    RichString delimiter = del instanceof RichString r ? r : new TextRichString(del.toString());
    RichString[] parts = new RichString[args.length + args.length - 1];
    for (int i = 0; i < args.length; i++) {
      parts[i * 2] = args[i];
    }
    for (int i = 1; i < parts.length; i += 2) {
      parts[i] = delimiter;
    }
    return new CompositeRichString(parts);
  }

  public static RichString b(Object arg0) {
    RichString child = arg0 instanceof RichString r ? r : new TextRichString(arg0.toString());
    return new BackgroundColorRichString(null, true, child);
  }

  public static RichString b(Color color, Object arg0) {
    RichString child = arg0 instanceof RichString r ? r : new TextRichString(arg0.toString());
    return new BackgroundColorRichString(color, false, child);
  }

  public static RichString f(Object arg0) {
    RichString child = arg0 instanceof RichString r ? r : new TextRichString(arg0.toString());
    return new ForegroundColorRichString(null, true, child);
  }

  public static RichString f(Color color, Object arg0) {
    RichString child = arg0 instanceof RichString r ? r : new TextRichString(arg0.toString());
    return new ForegroundColorRichString(color, false, child);
  }

  public static RichString B(Object arg0) {
    RichString child = arg0 instanceof RichString r ? r : new TextRichString(arg0.toString());
    return new BoldRichString(child);
  }

  public abstract int length();

  public abstract boolean isEmpty();

  public abstract RichString[] split(String regex, int limit);

  public abstract RichString substring(int start, int end);

  public static final class TextRichString extends RichString {
    private final String text;

    TextRichString(String text) {
      this.text = text;
    }

    public String getText() {
      return text;
    }

    @Override
    public int length() {
      return text.length();
    }

    @Override
    public boolean isEmpty() {
      return length() == 0;
    }

    @Override
    public RichString[] split(String regex, int limit) {
      String[] parts = text.split(regex, limit);
      if (parts.length == 1) {
        return new RichString[] {this};
      } else {
        return Arrays.stream(parts).map(TextRichString::new).toArray(RichString[]::new);
      }
    }

    @Override
    public RichString substring(int start, int end) {
      return new TextRichString(text.substring(start, end));
    }

    @Override
    public String toString() {
      return text;
    }
  }

  public static final class BackgroundColorRichString extends RichString {
    private final Color color;
    private final boolean auto;
    private final RichString child;

    BackgroundColorRichString(Color color, boolean auto, RichString child) {
      this.color = color;
      this.auto = auto;
      this.child = child;
    }

    public Color getColor() {
      return color;
    }

    public boolean isAuto() {
      return auto;
    }

    public RichString getChild() {
      return child;
    }

    @Override
    public int length() {
      return child.length();
    }

    @Override
    public boolean isEmpty() {
      return length() == 0;
    }

    @Override
    public RichString[] split(String regex, int limit) {
      return Arrays.stream(child.split(regex, limit))
          .map(c -> new BackgroundColorRichString(color, auto, c))
          .toArray(RichString[]::new);
    }

    @Override
    public RichString substring(int start, int end) {
      return new BackgroundColorRichString(color, auto, child.substring(start, end));
    }

    @Override
    public String toString() {
      return child.toString();
    }
  }

  public static final class ForegroundColorRichString extends RichString {
    private final Color color;
    private final boolean auto;
    private final RichString child;

    ForegroundColorRichString(Color color, boolean auto, RichString child) {
      this.color = color;
      this.auto = auto;
      this.child = child;
    }

    public Color getColor() {
      return color;
    }

    public boolean isAuto() {
      return auto;
    }

    public RichString getChild() {
      return child;
    }

    @Override
    public int length() {
      return child.length();
    }

    @Override
    public boolean isEmpty() {
      return length() == 0;
    }

    @Override
    public RichString[] split(String regex, int limit) {
      return Arrays.stream(child.split(regex, limit))
          .map(c -> new ForegroundColorRichString(color, auto, c))
          .toArray(RichString[]::new);
    }

    @Override
    public RichString substring(int start, int end) {
      return new ForegroundColorRichString(color, auto, child.substring(start, end));
    }

    @Override
    public String toString() {
      return child.toString();
    }
  }

  public static final class BoldRichString extends RichString {

    private final RichString child;

    BoldRichString(RichString child) {
      this.child = child;
    }

    public RichString getChild() {
      return child;
    }

    @Override
    public int length() {
      return child.length();
    }

    @Override
    public boolean isEmpty() {
      return length() == 0;
    }

    @Override
    public RichString[] split(String regex, int limit) {
      return Arrays.stream(child.split(regex, limit))
          .map(BoldRichString::new)
          .toArray(RichString[]::new);
    }

    @Override
    public RichString substring(int start, int end) {
      return new BoldRichString(child.substring(start, end));
    }

    @Override
    public String toString() {
      return child.toString();
    }
  }

  public static final class CompositeRichString extends RichString {

    private final RichString[] children;

    CompositeRichString(RichString... children) {
      this.children = children;
    }

    public RichString[] getChildren() {
      return children;
    }

    @Override
    public int length() {
      return Arrays.stream(children).mapToInt(RichString::length).sum();
    }

    @Override
    public boolean isEmpty() {
      return length() == 0;
    }

    @Override
    public RichString[] split(String regex, int limit) {
      // this feels like it should be simple, but since we need to combine children
      // across child boundaries, this winds up being a fair bit more involved
      String rawText = toString();
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(rawText);

      List<RichString> segments = new ArrayList<>();
      int position = 0;
      while (matcher.find()) {
        if (limit > 0 && segments.size() == limit - 1) {
          break;
        }

        int start = matcher.start();
        segments.add(substring(position, start));
        position = matcher.end();
      }
      segments.add(substring(position, length()));

      // replicate String.split behavior & remove trailing empties
      if (limit == 0) {
        for (int i = segments.size() - 1; i >= 0; i--) {
          if (segments.get(i).length() == 0) {
            segments.remove(i);
          } else {
            break;
          }
        }
      }

      return segments.toArray(RichString[]::new);
    }

    @Override
    public RichString substring(int start, int end) {
      if (start == 0 && end == length()) {
        return this;
      }
      if (start == end) {
        return new TextRichString("");
      }

      int position = 0;
      List<RichString> results = new ArrayList<>();
      for (RichString child : children) {
        if (position >= end) {
          break;
        }
        int childLen = child.length();
        int childEnd = position + childLen;
        if (childEnd <= start) {
          position = childEnd;
          continue;
        }

        results.add(
            child.substring(Math.max(0, start - position), Math.min(childLen, end - position)));
      }

      if (results.size() == 1) {
        return results.getFirst();
      }

      return new CompositeRichString(results.toArray(RichString[]::new));
    }

    @Override
    public String toString() {
      return Arrays.stream(children).map(RichString::toString).collect(Collectors.joining());
    }
  }
}
