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
     *  - Puts the terminal in "raw" mode (Unix-like only).
     *  - Reads input character-by-character.
     *  - Maintains its own input buffer.
     *  - When println() is called from another thread, it:
     *      * clears the current line,
     *      * prints the message,
     *      * redraws the prompt + the partially typed input.
     *
     * NOTE: Uses "stty" and ANSI escape sequences, so this is intended
     * for Unix-like systems (Linux/macOS) in a real terminal.
     */
    public static class InteractiveREPL extends REPL {

        private static final String ESC = "\u001B";

        private final Object lock = new Object();
        private final StringBuilder currentLine = new StringBuilder();
        private volatile boolean running = false;

        // Optional prompt (can be empty)
        private final String prompt;

        // Store original terminal settings to restore later
        private String originalSttyConfig;
        private volatile boolean rawModeEnabled = false;

        public InteractiveREPL(InputStream in, PrintStream out) {
            this(in, out, "> ");
        }

        public InteractiveREPL(InputStream in, PrintStream out, String prompt) {
            super(in, out);
            this.prompt = prompt != null ? prompt : "";
        }

        @Override
        public void println(String text) {
            synchronized (lock) {
                // 1. Clear the current input line
                out.print("\r");           // go to start of line
                out.print(ESC + "[2K");    // clear entire line

                // 2. Print the message on its own line
                out.println(text);

                // 3. Redraw prompt + whatever the user has typed so far
                out.print(prompt);
                out.print(currentLine);
                out.flush();
            }
        }

        @Override
        public void start() {
            running = true;

            try {
                enableRawMode();
            } catch (Exception e) {
                System.err.println(
                        "Failed to enable raw mode; InteractiveREPL may not behave as expected: "
                                + e.getMessage()
                );
            }

            synchronized (lock) {
                out.print(prompt);
                out.flush();
            }

            try {
                while (running) {
                    int ch = in.read(); // read raw byte/char
                    if (ch == -1) {
                        break; // EOF
                    }

                    char c = (char) ch;

                    if (c == '\r' || c == '\n') {
                        // End of line
                        String lineToHandle;
                        synchronized (lock) {
                            // Clear current line
                            out.print("\r");
                            out.print(ESC + "[2K");
                            // Print the completed line as normal output
                            out.println(prompt + currentLine);
                            out.flush();

                            lineToHandle = currentLine.toString();
                            currentLine.setLength(0);

                            // New prompt on the next line
                            out.print(prompt);
                            out.flush();
                        }
                        // Call handlers outside of lock
                        dispatchInput(lineToHandle);
                    } else if (c == 8 || c == 127) {
                        // Backspace/Delete
                        synchronized (lock) {
                            if (currentLine.length() > 0) {
                                currentLine.setLength(currentLine.length() - 1);
                                // Move cursor back, overwrite char, move back again
                                out.print("\b \b");
                                out.flush();
                            }
                        }
                    } else if (c == 3) {
                        // Ctrl-C: stop REPL
                        stop();
                    } else {
                        // Regular character
                        synchronized (lock) {
                            currentLine.append(c);
                            out.print(c);
                            out.flush();
                        }
                    }
                }
            } catch (IOException e) {
                // Likely caused by stop() or process exit
            } finally {
                running = false;
                try {
                    disableRawMode();
                } catch (Exception e) {
                    System.err.println("Failed to restore terminal mode: " + e.getMessage());
                }
            }
        }

        @Override
        public void stop() {
            running = false;
            // We don't close System.in here; loop will exit when running == false
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
    }
}
