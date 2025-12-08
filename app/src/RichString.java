import java.awt.Color;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * String with additional formatting information for use in terminal messages
 */
public sealed abstract class RichString permits RichString.TextRichString,
        RichString.CompositeRichString, RichString.BackgroundColorRichString,
        RichString.ForegroundColorRichString {

    public static RichString s(Object arg0, Object ... args) {
        RichString r0 = arg0 instanceof RichString r
                ? r
                : new TextRichString(arg0.toString());
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

    public abstract int length();
    public abstract RichString[] split(String regex, int limit);

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
        public RichString[] split(String regex, int limit) {
            String[] parts = text.split(regex, limit);
            if (parts.length == 1) {
                return new RichString[] { this };
            } else {
                return Arrays.stream(parts).map(TextRichString::new).toArray(RichString[]::new);
            }
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
        public RichString[] split(String regex, int limit) {
            return Arrays.stream(child.split(regex, limit))
                    .map(c -> new BackgroundColorRichString(color, auto, c))
                    .toArray(RichString[]::new);
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
        public RichString[] split(String regex, int limit) {
            return Arrays.stream(child.split(regex, limit))
                    .map(c -> new ForegroundColorRichString(color, auto, c))
                    .toArray(RichString[]::new);
        }

        @Override
        public String toString() {
            return child.toString();
        }
    }

    public static final class CompositeRichString extends RichString {

        private final RichString[] children;

        CompositeRichString(RichString ... children) {
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
        public RichString[] split(String regex, int limit) {
            return Arrays.stream(children)
                    .map(c -> c.split(regex, limit))
                    .flatMap(Arrays::stream)
                    .toArray(RichString[]::new);
        }

        @Override
        public String toString() {
            return Arrays.stream(children).map(RichString::toString).collect(Collectors.joining());
        }
    }

}
