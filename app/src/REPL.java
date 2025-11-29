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
public abstract class REPL {

    protected final InputStream in;
    protected final PrintStream out;

    // Handlers invoked for each input line. Assumed immutable after start().
    private final List<Consumer<String>> inputHandlers = new ArrayList<>();

    public REPL(InputStream in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

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
    public abstract void println(String text);
    public abstract void start();
    public abstract void stop();

    /**
     * Simple line-based REPL using BufferedReader.readLine().
     */
    public static class SimpleREPL extends REPL {

        private final BufferedReader inReader;
        private volatile boolean running = false;

        public SimpleREPL(InputStream in, PrintStream out) {
            super(in, out);
            this.inReader = new BufferedReader(new InputStreamReader(in));
        }

        @Override
        public void setPrompt(String prompt) {
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
                while (running && (line = inReader.readLine()) != null) {
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
                inReader.close(); // this will unblock readLine() in start()
            } catch (IOException e) {
                System.err.println("Failed to close input reader: " + e.getMessage());
            }
        }
    }

    /**
     * Interactive REPL that:
     *  - On a real Unix-like TTY:
     *      * Puts the terminal in raw mode.
     *      * Reads input character-by-character.
     *      * Maintains its own input buffer.
     *      * Keeps the prompt & input "stuck" to the bottom row of the terminal.
     *      * Buffers the last N messages and redraws the whole console on resize.
     *      * Allows dynamically changing the prompt and refreshes the input line.
     *  - On non-TTY / IDE consoles:
     *      * Falls back to a simple line-based REPL.
     */
    public static class InteractiveREPL extends REPL {

        private static final String ESC = "\u001B";

        private final Object lock = new Object();
        private final StringBuilder currentLine = new StringBuilder();
        private volatile boolean running = false;

        // Prompt is now mutable
        private volatile String prompt;

        // Terminal control state
        private String originalSttyConfig;
        private volatile boolean rawModeEnabled = false;
        private boolean fancyMode; // true when we can do TTY tricks
        private int termRows = -1;
        private int termCols = -1;

        // Message buffer for redraws (ring buffer)
        private static final int MAX_MESSAGES = 50;
        private final List<String> messages = new ArrayList<>();

        public InteractiveREPL(InputStream in, PrintStream out) {
            this(in, out, "> ");
        }

        public InteractiveREPL(InputStream in, PrintStream out, String prompt) {
            super(in, out);
            this.prompt = prompt != null ? prompt : "";
            // We only attempt fancy behavior on Unix-like systems with a real console.
            this.fancyMode = isUnixLike() && System.console() != null;
        }

        /**
         * Dynamically change the prompt.
         * Safe to call from any thread.
         */
        public void setPrompt(String newPrompt) {
            String p = newPrompt != null ? newPrompt : "";
            synchronized (lock) {
                this.prompt = p;
                if (fancyMode) {
                    // Redraw just the bottom line with the new prompt + current input
                    renderScreenLockedBottomOnly();
                } else {
                    // Fallback: print a fresh prompt on a new line
                    out.println();
                    out.print(this.prompt);
                    out.flush();
                }
            }
        }

        @Override
        public void println(String text) {
            if (!fancyMode) {
                // Fallback behavior for IDEA / non-TTY: just print and re-prompt
                synchronized (lock) {
                    addMessage(text);
                    out.println(text);
                    out.flush();
                }
                return;
            }

            synchronized (lock) {
                addMessage(text);
                // On each message, redraw the visible area
                renderScreenLocked();
            }
        }

        @Override
        public void start() {
            running = true;

            if (fancyMode) {
                try {
                    enableRawMode();
                    initTerminalSize(); // sets termRows, termCols
                } catch (Exception e) {
                    System.err.println(
                            "Failed to enable raw mode / read size; falling back to simple mode: " + e.getMessage()
                    );
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
                    // Fallback: behave like SimpleREPL in IDEA / non-TTY
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

                // Fancy raw-mode behavior (real terminal)
                while (running) {
                    int ch = in.read();
                    if (ch == -1) break;
                    char c = (char) ch;

                    synchronized (lock) {
                        // Check for resize on every key; cheap enough here
                        if (refreshTerminalSizeIfNeeded()) {
                            // Size changed: full redraw
                            renderScreenLocked();
                        }
                    }

                    if (c == '\r' || c == '\n') {
                        String lineToHandle;
                        synchronized (lock) {
                            lineToHandle = currentLine.toString();
                            currentLine.setLength(0);
                            // Redraw full screen to include the new message
                            renderScreenLocked();
                        }
                        dispatchInput(lineToHandle);
                    } else if (c == 8 || c == 127) {
                        // Backspace/Delete
                        synchronized (lock) {
                            if (currentLine.length() > 0) {
                                currentLine.setLength(currentLine.length() - 1);
                            }
                            renderScreenLockedBottomOnly();
                        }
                    } else if (c == 3) {
                        // Ctrl-C: stop REPL
                        stop();
                    } else {
                        // Regular character
                        synchronized (lock) {
                            currentLine.append(c);
                            renderScreenLockedBottomOnly();
                        }
                    }
                }
            } catch (IOException e) {
                // Likely caused by stop() or process exit
            } finally {
                running = false;
                if (fancyMode) {
                    try {
                        clearScreen();
                        disableRawMode();
                    } catch (Exception e) {
                        System.err.println("Failed to restore terminal mode: " + e.getMessage());
                    }
                }
            }
        }

        @Override
        public void stop() {
            running = false;
        }

        /**
         * Add a message to the ring buffer (bounded).
         */
        private void addMessage(String text) {
            messages.add(text);
            if (messages.size() > MAX_MESSAGES) {
                messages.remove(0);
            }
        }

        /**
         * Render the entire screen from the message buffer + current input,
         * assuming we hold the lock.
         */
        private void renderScreenLocked() {
            // Update size each time we do a full render
            try {
                initTerminalSize();
            } catch (Exception ignored) {
                // If size fetch fails, we just do a naive render below
            }

            clearScreen();

            if (termRows <= 0) {
                // Unknown size, just dump messages and prompt+line
                for (String msg : messages) {
                    out.println(msg);
                }
                out.print(prompt);
                out.print(currentLine);
                out.flush();
                return;
            }

            // Reserve the last row for input
            int linesForMessages = Math.max(0, termRows - 1);

            int msgCount = messages.size();
            int start = Math.max(0, msgCount - linesForMessages);

            // Print messages starting at top-left
            out.print(ESC + "[1;1H");
            for (int i = start; i < msgCount; i++) {
                out.print(ESC + "[2K"); // clear line
                out.println(messages.get(i));
            }

            // Draw the input line at the bottom
            drawBottomInputLineLocked();
        }

        /**
         * Only redraw the bottom input line (for simple edits), assuming we hold the lock.
         */
        private void renderScreenLockedBottomOnly() {
            if (termRows <= 0) {
                // Fallback
                out.print("\r");
                out.print(ESC + "[2K");
                out.print(prompt);
                out.print(currentLine);
                out.flush();
                return;
            }
            drawBottomInputLineLocked();
        }

        /**
         * Draw the prompt + current line on the last row.
         * Assumes we hold the lock and termRows is known.
         */
        private void drawBottomInputLineLocked() {
            int bottomRow = (termRows > 0) ? termRows : 1;
            out.print(ESC + "[" + bottomRow + ";1H");
            out.print(ESC + "[2K"); // clear bottom line
            out.print(prompt);
            out.print(currentLine);
            out.flush();
        }

        /**
         * Initialize terminal size (rows, cols) once, or refresh if needed.
         */
        private void initTerminalSize() throws IOException, InterruptedException {
            int[] size = getTerminalSize();
            if (size == null || size[0] <= 0) {
                return;
            }
            termRows = size[0];
            termCols = size[1];
        }

        /**
         * Check if terminal size changed. If so, update termRows/termCols and return true.
         */
        private boolean refreshTerminalSizeIfNeeded() {
            try {
                int[] size = getTerminalSize();
                if (size == null || size[0] <= 0) {
                    return false;
                }
                int rows = size[0];
                int cols = size[1];
                if (rows != termRows || cols != termCols) {
                    termRows = rows;
                    termCols = cols;
                    return true;
                }
            } catch (Exception ignored) {
                // If size detection fails, just keep old values
            }
            return false;
        }

        /**
         * Clear entire screen and move cursor to home.
         */
        private void clearScreen() {
            out.print(ESC + "[2J"); // clear screen
            out.print(ESC + "[H");  // move cursor to home
            out.flush();
        }

        /**
         * Enable raw mode on a Unix-like terminal by using 'stty'.
         *
         * Also registers a shutdown hook to restore the original mode when
         * the JVM exits normally (System.exit, uncaught exception, etc.).
         */
        private void enableRawMode() throws IOException, InterruptedException {
            if (!isUnixLike() || rawModeEnabled) {
                return;
            }

            // Save current settings
            originalSttyConfig = execSttyCommand("-g").trim();

            // Disable canonical mode (line buffering) and echo
            execSttyCommand("-icanon -echo min 1 time 0");
            rawModeEnabled = true;

            // Register shutdown hook to restore original terminal settings
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    disableRawMode();
                } catch (Exception e) {
                    System.err.println(
                            "Shutdown hook failed to restore terminal mode: " + e.getMessage()
                    );
                }
            }));
        }

        /**
         * Restore the original terminal mode.
         */
        private void disableRawMode() throws IOException, InterruptedException {
            if (!isUnixLike() || !rawModeEnabled) {
                return;
            }
            if (originalSttyConfig != null && !originalSttyConfig.isEmpty()) {
                execSttyCommand(originalSttyConfig);
            }
            rawModeEnabled = false;
        }

        private boolean isUnixLike() {
            String os = System.getProperty("os.name", "").toLowerCase();
            return os.contains("nix") || os.contains("nux") || os.contains("mac");
        }

        private String execSttyCommand(String args) throws IOException, InterruptedException {
            // Use /dev/tty so we manipulate the real terminal, not just stdin
            String[] cmd = {"sh", "-c", "stty " + args + " < /dev/tty"};
            Process p = Runtime.getRuntime().exec(cmd);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()))) {
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    result.append(line).append('\n');
                }
                p.waitFor();
                return result.toString();
            }
        }

        /**
         * Returns terminal size as [rows, cols], or null if it can't be determined.
         */
        private int[] getTerminalSize() throws IOException, InterruptedException {
            String out = execSttyCommand("size").trim();
            if (out.isEmpty()) {
                return null;
            }
            String[] parts = out.split("\\s+");
            if (parts.length < 2) {
                return null;
            }
            try {
                int rows = Integer.parseInt(parts[0]);
                int cols = Integer.parseInt(parts[1]);
                return new int[]{rows, cols};
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
}
