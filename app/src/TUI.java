import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * "Simple" read-evaluate-print-loop handler, handles terminal interactions
 * to allow us to plug in better input handling than the default.
 *
 * Assumption: input handlers are all registered BEFORE start() is called
 * and are not modified afterward.
 */
public abstract class TUI {

    // Handlers invoked for each input line. Assumed immutable after start().
    private final List<Consumer<String>> inputHandlers = new ArrayList<>();

    /**
     * Add a handler that will be called for each completed input line.
     * This should be called before start() is invoked.
     */
    public void addInputHandler(Consumer<String> handler) {
        inputHandlers.add(handler);
    }

    /**
     * Dispatches a line of input to all registered handlers.
     */
    protected void dispatchInput(String line) {
        for (Consumer<String> handler : inputHandlers) {
            handler.accept(line);
        }
    }

    public abstract void setPrompt(String prompt);
    public abstract void setStatus(String status);
    public abstract void println(String text);
    public abstract void start();
    public abstract void stop();

    /**
     * Simple line-based REPL using BufferedReader.readLine().
     */
    public static class SimpleTUI extends TUI {

        private final BufferedReader in;
        private final PrintStream out;

        private volatile boolean running = false;

        public SimpleTUI(InputStream in, PrintStream out) {
            this.in = new BufferedReader(new InputStreamReader(in));
            this.out = out;
        }

        @Override
        public void setPrompt(String prompt) {
            // noop
        }

        @Override
        public void setStatus(String status) {
            // noop
        }

        @Override
        public void println(String text) {
            out.println(text);
        }

        @Override
        public void start() {
            running = true;
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    dispatchInput(line);
                }
            } catch (IOException e) {
                // Likely caused by stop() closing the reader
            } finally {
                running = false;
            }
        }

        @Override
        public void stop() {
            running = false;
            try {
                in.close(); // this will unblock readLine() in start()
            } catch (IOException e) {
                System.err.println("Failed to close input reader: " + e.getMessage());
            }
        }
    }

    /**
     * Interactive REPL that:
     *  - On a real Unix-like TTY:
     *      * Uses STTY utility to enter raw mode.
     *      * Reads input a byte at a time.
     *      * Maintains its own text buffer, prompt, and redraw logic.
     *      * Responds to terminal resizes.
     *  - On non-TTY:
     *      * Falls back to a line-based REPL.
     */
    public static class InteractiveTUI extends TUI {

        private static final String ESC = "\u001B";

        private final InputStream in;
        private final PrintStream out;
        private final Object lock = new Object();
        private final StringBuilder currentLine = new StringBuilder();
        private volatile boolean running = false;

        private volatile String prompt;

        private boolean fancyMode;
        private int termRows = -1;
        private int termCols = -1;

        // Ring buffer max messages
        private static final int MAX_MESSAGES = 50;
        private final List<String> messages = new ArrayList<>();

        public InteractiveTUI(InputStream in, PrintStream out) {
            this(in, out, "> ");
        }

        public InteractiveTUI(InputStream in, PrintStream out, String prompt) {
            this.in = in;
            this.out = out;
            this.prompt = prompt != null ? prompt : "";
            this.fancyMode = STTY.isAvailable() && System.console() != null;
        }

        @Override
        public void setPrompt(String newPrompt) {
            String p = newPrompt != null ? newPrompt : "";
            synchronized (lock) {
                this.prompt = p;
                if (fancyMode) {
                    renderScreenLockedBottomOnly();
                } else {
                    out.println();
                    out.print(this.prompt);
                    out.flush();
                }
            }
        }

        @Override
        public void setStatus(String status) {
            // Not currently implemented
        }

        @Override
        public void println(String text) {
            if (!fancyMode) {
                synchronized (lock) {
                    addMessage(text);
                    out.println(text);
                    out.flush();
                }
                return;
            }

            synchronized (lock) {
                addMessage(text);
                renderScreenLocked();
            }
        }

        @Override
        public void start() {
            running = true;

            if (fancyMode) {
                try {
                    // Snapshot + restore-on-exit is handled by STTY
                    STTY.restoreTerminalStateOnExit();
                    if (!STTY.enableRawMode()) {
                        throw new IOException("failed to enable stty raw mode");
                    }
                    initTerminalSize();
                } catch (Exception e) {
                    System.err.println("Failed to enable raw mode or read size; falling back: " + e.getMessage());
                    fancyMode = false;
                }
            }

            synchronized (lock) {
                if (fancyMode) {
                    renderScreenLocked();
                } else {
                    out.print(prompt);
                    out.flush();
                }
            }

            try {
                if (!fancyMode) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        addMessage(prompt + line);
                        dispatchInput(line);
                        synchronized (lock) {
                            out.print(prompt);
                            out.flush();
                        }
                    }
                    return;
                }

                // Fancy raw-mode loop
                while (running) {
                    int ch = in.read();
                    if (ch == -1) break;
                    char c = (char) ch;

                    synchronized (lock) {
                        if (refreshTerminalSizeIfNeeded()) {
                            renderScreenLocked();
                        }
                    }

                    if (c == '\r' || c == '\n') {
                        String lineToHandle;
                        synchronized (lock) {
                            lineToHandle = currentLine.toString();
                            currentLine.setLength(0);
                            renderScreenLocked();
                        }
                        dispatchInput(lineToHandle);

                    } else if (c == 8 || c == 127) {
                        synchronized (lock) {
                            if (currentLine.length() > 0) {
                                currentLine.setLength(currentLine.length() - 1);
                            }
                            renderScreenLockedBottomOnly();
                        }

                    } else if (c == 3) {
                        stop(); // Ctrl-C

                    } else {
                        synchronized (lock) {
                            currentLine.append(c);
                            renderScreenLockedBottomOnly();
                        }
                    }
                }
            } catch (IOException ignored) {
            } finally {
                running = false;
                if (fancyMode) {
                    try {
                        clearScreen();
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        @Override
        public void stop() {
            running = false;
        }

        // ------------------------------------------------------------
        // Rendering + terminal size helpers
        // ------------------------------------------------------------

        private void addMessage(String text) {
            messages.add(text);
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }

        private void renderScreenLocked() {
            try {
                initTerminalSize();
            } catch (Exception ignored) {
            }

            clearScreen();

            if (termRows <= 0) {
                for (String msg : messages) out.println(msg);
                out.print(prompt);
                out.print(currentLine);
                out.flush();
                return;
            }

            int linesForMessages = Math.max(0, termRows - 1);
            int msgCount = messages.size();
            int start = Math.max(0, msgCount - linesForMessages);

            out.print(ESC + "[1;1H");
            for (int i = start; i < msgCount; i++) {
                out.print(ESC + "[2K");
                out.println(messages.get(i));
            }

            drawBottomInputLineLocked();
        }

        private void renderScreenLockedBottomOnly() {
            if (termRows <= 0) {
                out.print("\r");
                out.print(ESC + "[2K");
                out.print(prompt);
                out.print(currentLine);
                out.flush();
                return;
            }
            drawBottomInputLineLocked();
        }

        private void drawBottomInputLineLocked() {
            int row = termRows > 0 ? termRows : 1;
            out.print(ESC + "[" + row + ";1H");
            out.print(ESC + "[2K");
            out.print(prompt);
            out.print(currentLine);
            out.flush();
        }

        private void clearScreen() {
            out.print(ESC + "[2J");
            out.print(ESC + "[H");
            out.flush();
        }

        private void initTerminalSize() {
            STTY.TerminalDimensions dims = STTY.getTerminalSize();
            if (dims instanceof STTY.KnownTerminalDimensions known) {
                termRows = known.rows();
                termCols = known.cols();
            }
        }

        private boolean refreshTerminalSizeIfNeeded() {
            STTY.TerminalDimensions dims = STTY.getTerminalSize();
            if (!(dims instanceof STTY.KnownTerminalDimensions known)) return false;

            if (known.rows() != termRows || known.cols() != termCols) {
                termRows = known.rows();
                termCols = known.cols();
                return true;
            }
            return false;
        }
    }

}
