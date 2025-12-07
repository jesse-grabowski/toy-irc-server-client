import java.util.Random;

public final class NicknameGenerator {

    private static final String[] PARTS = {
            "red", "blue", "sky", "wolf", "fox", "oak", "keen", "soft", "jade"
    };
    private static final Random RAND = new Random();

    private NicknameGenerator() {}

    public static String generate(String input) {
        if (!"auto".equals(input)) {
            return input;
        }

        String w1 = PARTS[RAND.nextInt(PARTS.length)];
        String w2 = PARTS[RAND.nextInt(PARTS.length)];

        return w1 + w2;
    }
}
