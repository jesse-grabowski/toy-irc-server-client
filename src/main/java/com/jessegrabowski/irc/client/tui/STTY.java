/*
 * This project is licensed under the MIT License.
 *
 * In addition to the rights granted under the MIT License, explicit permission
 * is granted to the faculty, instructors, teaching assistants, and evaluators
 * of Ritsumeikan University for unrestricted educational evaluation and grading.
 *
 * ---------------------------------------------------------------------------
 *
 * MIT License
 *
 * Copyright (c) 2026 Jesse Grabowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jessegrabowski.irc.client.tui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Normally I would pull in a library for this, but given the circumstances I'm going to do it
 * myself. This only works on UNIX-like systems.
 *
 * <p>Wrapper library to dispatch calls to stty. This allows us to access properties about the
 * terminal running the application, as well as to manipulate certain settings (mainly cooked vs raw
 * mode)
 */
public final class STTY {

    private static final Logger LOG = Logger.getLogger(STTY.class.getName());

    private static final Pattern SIZE_PATTERN = Pattern.compile("^(?<rows>\\d+)\\s+(?<cols>\\d+)$");
    private static final TerminalDimensions UNKNOWN_TERMINAL_DIMENSIONS = new UnknownTerminalDimensions();

    // mutable state in a static helper class is a bit gross, but in this case it's probably fine
    // since it's
    // guarding the jvm's mutable shutdown hook global state
    private static final AtomicBoolean RESTORATION_HOOK_REGISTERED = new AtomicBoolean(false);

    // non-instantiable
    private STTY() {}

    /**
     * Verify that stty is available and can be used the way this code expects
     *
     * @return true if we're good to go, false otherwise
     */
    public static boolean isAvailable() {
        try {
            stty("-g");
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static boolean enableRawMode() {
        try {
            stty("-icanon", "-echo", "min", "1", "time", "0");
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public static boolean restoreTerminalStateOnExit() {
        String originalState;
        try {
            originalState = stty("-g").trim();
        } catch (IOException e) {
            return false;
        }

        if (originalState.isEmpty()) {
            return false;
        }

        if (RESTORATION_HOOK_REGISTERED.compareAndSet(false, true)) {
            Runtime.getRuntime()
                    .addShutdownHook(new Thread(
                            () -> {
                                try {
                                    stty(originalState);
                                } catch (Exception e) {
                                    System.err.println(
                                            "Shutdown hook failed to restore terminal mode: " + e.getMessage());
                                }
                            },
                            "stty-restore-hook"));
        }

        return true;
    }

    public static TerminalDimensions getTerminalSize() {
        try {
            String size = stty("size").trim();
            Matcher matcher = SIZE_PATTERN.matcher(size);
            if (!matcher.matches()) {
                // we got something weird back, return unknown dimensions
                return UNKNOWN_TERMINAL_DIMENSIONS;
            }
            return new KnownTerminalDimensions(
                    Integer.parseInt(matcher.group("rows")), Integer.parseInt(matcher.group("cols")));
        } catch (IOException | NumberFormatException e) {
            // this shouldn't be fatal, return unknown dimensions
            return UNKNOWN_TERMINAL_DIMENSIONS;
        }
    }

    private static String stty(String... args) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("stty");
        Collections.addAll(command, args);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectInput(new File("/dev/tty"));
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String output;
        try (BufferedReader br =
                new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            output = br.lines().collect(Collectors.joining("\n"));
        }

        int exitCode;
        try {
            exitCode = p.waitFor();
        } catch (InterruptedException e) {
            LOG.log(Level.WARNING, "Interrupted while waiting for stty", e);
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for stty", e);
        }

        LOG.log(Level.FINEST, "com.jessegrabowski.irc.client.tui.STTY: {0} -> {1}", new Object[] {command, output});

        if (exitCode != 0) {
            throw new IOException("stty exited with code %d: %s".formatted(exitCode, output));
        }
        return output;
    }

    public sealed interface TerminalDimensions permits KnownTerminalDimensions, UnknownTerminalDimensions {}

    public record KnownTerminalDimensions(int rows, int cols) implements TerminalDimensions {}

    public record UnknownTerminalDimensions() implements TerminalDimensions {}
}
