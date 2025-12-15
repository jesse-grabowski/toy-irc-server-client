public class Colorizer {
  private static final String PREFIX = "\033[38;2;";
  private static final String SUFFIX = "\033[0m";

  public static String colorize(String s) {
    if (s == null) {
      return null;
    }

    int hash = s.hashCode();

    // Derive hue in a wide range (0–360)
    float hue = Math.floorMod(hash, 360);

    // Avoid dark bands:
    // If the hue falls in a known "dim" region (e.g., purples or grayish greens),
    // shift it a bit to a clearer color.
    if (hue >= 210 && hue <= 260) { // muddy blues/purples
      hue = (hue + 60) % 360;
    }
    if (hue >= 75 && hue <= 105) { // some yellow-green hues look brownish
      hue = (hue + 40) % 360;
    }

    // **Bright** profile (great on black)
    float saturation = 0.95f; // very colorful
    float value = 0.97f; // extremely bright

    int[] rgb = hsvToRgb(hue, saturation, value);
    int r = rgb[0], g = rgb[1], b = rgb[2];

    return PREFIX + r + ";" + g + ";" + b + "m" + s + SUFFIX;
  }

  // I outsourced this to ChatGPT, but it probably won't blow up
  private static int[] hsvToRgb(float h, float s, float v) {
    float c = v * s;
    float x = c * (1 - Math.abs(((h / 60) % 2) - 1));
    float m = v - c;

    float r1, g1, b1;

    if (h < 60) {
      r1 = c;
      g1 = x;
      b1 = 0;
    } else if (h < 120) {
      r1 = x;
      g1 = c;
      b1 = 0;
    } else if (h < 180) {
      r1 = 0;
      g1 = c;
      b1 = x;
    } else if (h < 240) {
      r1 = 0;
      g1 = x;
      b1 = c;
    } else if (h < 300) {
      r1 = x;
      g1 = 0;
      b1 = c;
    } else {
      r1 = c;
      g1 = 0;
      b1 = x;
    }

    int r = Math.round((r1 + m) * 255);
    int g = Math.round((g1 + m) * 255);
    int b = Math.round((b1 + m) * 255);

    return new int[] {r, g, b};
  }
}
