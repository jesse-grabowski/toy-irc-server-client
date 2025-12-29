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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class TerminalUI {

    private final List<TerminalInputHandler> inputHandlers = new ArrayList<>();

    private volatile boolean running = false;
    private volatile Thread thread;

    public void addInputHandler(TerminalInputHandler handler) {
        inputHandlers.add(handler);
    }

    protected CompletableFuture<Void> dispatchInput(String line) {
        return CompletableFuture.allOf(
                inputHandlers.stream().map(handler -> handler.handle(line)).toArray(CompletableFuture[]::new));
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;

        initialize();

        thread = new Thread(
                () -> {
                    try {
                        while (running) {
                            process();
                        }
                    } finally {
                        running = false;
                    }
                },
                "TerminalUI-Loop");

        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
        }
    }

    public abstract void setPrompt(RichString prompt);

    public abstract void setStatus(RichString status);

    public abstract void println(TerminalMessage message);

    protected abstract void initialize();

    protected abstract void process();
}
