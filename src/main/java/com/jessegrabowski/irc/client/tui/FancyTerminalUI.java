package com.jessegrabowski.irc.client.tui;

import java.awt.Color;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class FancyTerminalUI extends TerminalUI {

  private static final Logger LOG = Logger.getLogger(FancyTerminalUI.class.getName());

  private static final Map<Color, Integer> NAMED_ANSI_FOREGROUND =
      Map.ofEntries(
          Map.entry(Color.BLACK, 30),
          Map.entry(Color.RED, 31),
          Map.entry(Color.GREEN, 32),
          Map.entry(Color.YELLOW, 33),
          Map.entry(Color.BLUE, 34),
          // no java Color for Purple
          Map.entry(Color.CYAN, 36),
          Map.entry(Color.WHITE, 37));
  private static final Map<Color, Integer> NAMED_ANSI_BACKGROUND =
      Map.ofEntries(
          Map.entry(Color.BLACK, 40),
          Map.entry(Color.RED, 41),
          Map.entry(Color.GREEN, 42),
          Map.entry(Color.YELLOW, 43),
          Map.entry(Color.BLUE, 44),
          // no java Color for Purple
          Map.entry(Color.CYAN, 46),
          Map.entry(Color.WHITE, 47));

  private static final int FOOTER_ROWS = 3;
  private static final int MAX_MESSAGES = 200;
  private static final int METADATA_SENDER_WIDTH = 9;
  private static final int METADATA_RECEIVER_WIDTH = 9;
  private static final int METADATA_WIDTH = METADATA_SENDER_WIDTH + METADATA_RECEIVER_WIDTH + 12;
  private static final String BLANK_METADATA = " ".repeat(METADATA_WIDTH);
  private static final DateTimeFormatter METADATA_TIMESTAMP =
      DateTimeFormatter.ofPattern("HH:mm:ss");

  private static final String VERTICAL_RULE = "│";
  private static final String HORIZONTAL_RULE = "─";
  private static final String TEE_RULE = "┴";

  private static final String COLOR_CYAN = "\u001B[36m";
  private static final String COLOR_RESET = "\u001B[0m";

  private static final String COLOR_RULE = COLOR_CYAN;

  private final ScheduledExecutorService scheduler =
      Executors.newSingleThreadScheduledExecutor(
          r -> {
            Thread t = new Thread(r, "com.jessegrabowski.irc.client.tui.FancyTerminalUI-DimensionPoller");
            t.setDaemon(true);
            return t;
          });

  private final StringBuilder inputBuffer = new StringBuilder();
  private final Deque<TerminalMessage> messageBuffer = new ArrayDeque<>(MAX_MESSAGES);

  private STTY.TerminalDimensions dimensions = new STTY.UnknownTerminalDimensions();
  private RichString prompt = RichString.s("[]:");
  private RichString status = RichString.s("");

  @Override
  public synchronized void setPrompt(RichString prompt) {
    this.prompt = (prompt != null) ? prompt : RichString.s("");
    renderFooter();
  }

  @Override
  public synchronized void setStatus(RichString status) {
    this.status = (status != null) ? status : RichString.s("");
    renderFooter();
  }

  @Override
  public synchronized void println(TerminalMessage message) {
    if (message == null) {
      return;
    }

    messageBuffer.addLast(message);

    // Enforce max buffer size
    while (messageBuffer.size() > MAX_MESSAGES) {
      messageBuffer.removeFirst();
    }

    redraw();
  }

  @Override
  protected synchronized void initialize() {
    LOG.info("Initializing com.jessegrabowski.irc.client.tui.FancyTerminalUI");
    STTY.restoreTerminalStateOnExit();
    LOG.info("Registered com.jessegrabowski.irc.client.tui.STTY restoration shutdown hook");
    STTY.enableRawMode();
    LOG.info("Enabled Raw terminal mode");

    setDimensions(STTY.getTerminalSize());

    scheduler.scheduleAtFixedRate(
        () -> setDimensions(STTY.getTerminalSize()), 0, 500, TimeUnit.MILLISECONDS);
    LOG.info("Started terminal size monitor");

    redraw();
  }

  @Override
  protected void process() {
    int b;
    try {
      b = System.in.read();
    } catch (IOException e) {
      stop();
      return;
    }

    if (b == -1) {
      stop();
      return;
    }

    char c = (char) b;
    String completedLine = null;
    synchronized (this) {
      switch (c) {
        // Enter / Return
        case '\r', '\n' -> {
          if (!inputBuffer.isEmpty()) {
            completedLine = inputBuffer.toString();
            inputBuffer.setLength(0);
          }
          renderFooter();
        }
        // Backspace / Delete
        case '\b', 0x7F -> {
          if (!inputBuffer.isEmpty()) {
            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
            renderFooter();
          }
        }
        default -> {
          // Printable ASCII
          if (c >= 32 && c < 127) {
            inputBuffer.append(c);
            renderFooter();
          }
        }
      }
    }

    if (completedLine != null) {
      dispatchInput(completedLine);
    }
  }

  private synchronized void setDimensions(STTY.TerminalDimensions dimensions) {
    STTY.TerminalDimensions old = this.dimensions;
    this.dimensions = dimensions;
    if (old != null && !Objects.equals(old, dimensions)) {
      redraw();
    }
  }

  private synchronized int getRows() {
    return switch (dimensions) {
      case STTY.UnknownTerminalDimensions d -> 24;
      case STTY.KnownTerminalDimensions d -> d.rows();
    };
  }

  private synchronized int getCols() {
    return switch (dimensions) {
      case STTY.UnknownTerminalDimensions d -> 80;
      case STTY.KnownTerminalDimensions d -> d.cols();
    };
  }

  private synchronized void redraw() {
    LOG.fine("Redrawing com.jessegrabowski.irc.client.tui.TerminalUI");
    renderMessages();
    renderFooter();
  }

  // Layout: [metadata (METADATA_WIDTH)] [space] [VERTICAL_RULE] [space] [message...]
  private synchronized void renderMessages() {
    int rows = getRows();
    int cols = getCols();

    // figure out how many rows we need to display, terminate early if the terminal is tiny
    int maxMessageRows = rows - FOOTER_ROWS;
    if (maxMessageRows <= 0) {
      return;
    }

    // figure out how many columns we have to work with once we factor in metadata
    int messageWidth = Math.max(cols - METADATA_WIDTH - 3, 1);

    // This is a bit tricky. We want to build a list of rows to display on the screen, but for the
    // sake of
    // efficiency we don't want to iterate over messages that aren't going to be displayed. To
    // handle this,
    // we iterate backwards. For each message, we split it into lines and append them to the front
    // of the
    // list (while maintaining their order).
    List<String> visualRows = new ArrayList<>();
    Iterator<TerminalMessage> messageIterator = messageBuffer.descendingIterator();
    while (messageIterator.hasNext() && visualRows.size() < maxMessageRows) {
      TerminalMessage message = messageIterator.next();
      String meta = formatMetadata(message);

      RichString text = message.message() != null ? message.message() : RichString.s("");
      List<RichString> messageLines =
          Arrays.stream(text.split("\\R", -1))
              .map(l -> splitByLength(l, messageWidth))
              .flatMap(List::stream)
              .toList();

      boolean first = true;
      for (int i = 0; i < messageLines.size(); i++) {
        String metaPart = first ? meta : BLANK_METADATA;
        // we add at i and not 0 so that the lines maintain their order within the message
        visualRows.add(
            i,
            metaPart
                + " "
                + COLOR_RULE
                + VERTICAL_RULE
                + COLOR_RESET
                + " "
                + toString(messageLines.get(i)));
        first = false;
      }
    }

    // trim / pad to the height of the terminal
    while (visualRows.size() < maxMessageRows) {
      visualRows.addFirst("");
    }
    while (visualRows.size() > maxMessageRows) {
      visualRows.removeFirst();
    }

    // print in order from top to bottom in a single pass
    for (int row = 0; row < visualRows.size(); row++) {
      moveCursor(row + 1, 1);
      clearLine();
      System.out.print(visualRows.get(row));
    }

    System.out.flush();
  }

  private List<RichString> splitByLength(RichString line, int length) {
    List<RichString> results = new ArrayList<>();

    if (line.isEmpty()) {
      results.add(line);
      return results;
    }

    for (int start = 0; start < line.length(); start += length) {
      int end = Math.min(start + length, line.length());
      results.add(line.substring(start, end));
    }

    return results;
  }

  private String formatMetadata(TerminalMessage message) {
    StringBuilder metadataBuilder = new StringBuilder();
    metadataBuilder.append(
        toString(
            RichString.f(
                Color.GRAY,
                message.time() != null ? METADATA_TIMESTAMP.format(message.time()) : "XX:XX:XX")));
    metadataBuilder.append(' ');
    if (message.receiver() == null) {
      metadataBuilder.append(
          toString(
              formatExactLength(
                  message.sender(), METADATA_SENDER_WIDTH + 3 + METADATA_RECEIVER_WIDTH, false)));
    } else {
      metadataBuilder.append(
          toString(formatExactLength(message.sender(), METADATA_SENDER_WIDTH, false)));
      metadataBuilder.append(toString(RichString.f(Color.GRAY, " → ")));
      metadataBuilder.append(
          toString(formatExactLength(message.receiver(), METADATA_RECEIVER_WIDTH, false)));
    }
    return metadataBuilder.toString();
  }

  private RichString formatExactLength(RichString string, int length, boolean alignEnd) {
    if (string == null) {
      return RichString.s(" ".repeat(length));
    }

    if (string.length() > length) {
      return string.substring(0, length);
    } else {
      String padding = " ".repeat(length - string.length());
      if (alignEnd) {
        return RichString.s(padding, string);
      } else {
        return RichString.s(string, padding);
      }
    }
  }

  private synchronized void renderFooter() {
    int rows = getRows();
    int cols = getCols();

    // terminal too small
    if (rows < 3 || cols < METADATA_WIDTH + 3) {
      return;
    }

    // horizontal rule
    moveCursor(rows - 2, 1);
    clearLine();
    System.out.print(COLOR_RULE);
    System.out.print(HORIZONTAL_RULE.repeat(METADATA_WIDTH + 1));
    System.out.print(TEE_RULE);
    System.out.print(HORIZONTAL_RULE.repeat(cols - METADATA_WIDTH - 2));
    System.out.print(COLOR_RESET);

    // status line
    moveCursor(rows - 1, 1);
    clearLine();
    RichString statusText = (status != null) ? status : RichString.s("");
    if (statusText.length() > cols) {
      statusText = statusText.substring(0, cols);
    }
    System.out.print(toString(statusText));

    // user input
    moveCursor(rows, 1);
    clearLine();
    RichString prefix = RichString.s(prompt != null ? prompt : "[]:", " ");
    int inputColumns = cols - prefix.length();
    if (inputColumns < 0) {
      inputColumns = 0;
    }
    String inputText = inputBuffer.toString();
    if (inputText.length() > inputColumns) {
      inputText = inputText.substring(inputText.length() - inputColumns);
    }
    String full = toString(prefix) + inputText;
    System.out.print(full);

    System.out.flush();
  }

  private void moveCursor(int row, int col) {
    // ANSI escape: ESC[row;colH
    System.out.print("\u001B[" + row + ";" + col + "H");
  }

  private void clearLine() {
    // ANSI escape: ESC[2K clears entire current line
    System.out.print("\u001B[2K");
  }

  private String toString(RichString string) {
    return switch (string) {
      case RichString.TextRichString s -> s.getText();
      case RichString.BackgroundColorRichString s -> colorizeBackground(s);
      case RichString.ForegroundColorRichString s -> colorizeForeground(s);
      case RichString.CompositeRichString s -> {
        StringBuilder sb = new StringBuilder();
        for (RichString rs : s.getChildren()) {
          sb.append(toString(rs));
        }
        yield sb.toString();
      }
      case RichString.BoldRichString s -> bold(s);
    };
  }

  private String colorizeForeground(RichString.ForegroundColorRichString string) {
    String text = toString(string.getChild());
    Color color = string.isAuto() ? getColor(text) : string.getColor();

    if (NAMED_ANSI_FOREGROUND.containsKey(color)) {
      return "\u001b[" + NAMED_ANSI_FOREGROUND.get(color) + "m" + text + "\u001b[39m";
    }

    return "\u001b[38;5;" + toANSI256(color) + "m" + text + "\u001b[39m";
  }

  private String colorizeBackground(RichString.BackgroundColorRichString string) {
    String text = toString(string.getChild());
    Color color = string.isAuto() ? getColor(text) : string.getColor();

    if (NAMED_ANSI_BACKGROUND.containsKey(color)) {
      return "\u001b[" + NAMED_ANSI_BACKGROUND.get(color) + "m" + text + "\u001b[49m";
    }

    return "\u001b[48;5;" + toANSI256(color) + "m" + text + "\u001b[49m";
  }

  private String bold(RichString.BoldRichString string) {
    return "\u001b[1m" + toString(string.getChild()) + "\u001b[22m";
  }

  private Color getColor(String text) {
    // seeded random gives us a variety of colors w/
    // consistent results for the same input
    Random r = new Random(text.hashCode());
    // This looks weird, but just cuts out the blue
    // range of hues (hard to read on black terminal)
    float hue = r.nextFloat();
    if (hue >= 0.62f) {
      hue = 0.62f + (hue - 0.72f);
    }
    return Color.getHSBColor(hue, r.nextFloat(0.5f, 1f), r.nextFloat(0.5f, 0.85f));
  }

  // should work on most terminals
  private int toANSI256(Color color) {
    int r6 = Math.clamp(Math.round(color.getRed() / 51f), 0, 5);
    int g6 = Math.clamp(Math.round(color.getGreen() / 51f), 0, 5);
    int b6 = Math.clamp(Math.round(color.getBlue() / 51f), 0, 5);
    return 16 + 36 * r6 + 6 * g6 + b6;
  }

  @Override
  public synchronized void stop() {
    super.stop();
    scheduler.shutdownNow();
  }
}
