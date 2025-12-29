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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StdIOTerminalUI extends TerminalUI {

    private static final Logger LOG = Logger.getLogger(StdIOTerminalUI.class.getName());

    private final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    private final AtomicBoolean statusChanged = new AtomicBoolean(false);

    private volatile String prompt = "[Type Something]:";
    private volatile String status;

    @Override
    protected void process() {
        try {
            if (statusChanged.compareAndSet(true, false) && status != null && !status.isBlank()) {
                System.out.println(status);
            }
            System.out.print(prompt + " ");
            String line = reader.readLine();
            if (line == null) {
                stop();
                return;
            }
            dispatchInput(line).get();
            if (System.console() != null) {
                for (char c : "|/-\\".repeat(2).toCharArray()) {
                    System.out.print("\r\033[2K");
                    System.out.print("\rSending " + c);
                    System.out.flush();
                    Thread.sleep(125);
                }
                System.out.print("\r\033[2K");
            }
        } catch (IOException e) {
            stop();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error processing input", e);
            if (Thread.currentThread().isInterrupted()) {
                LOG.log(Level.SEVERE, "IO thread interrupted, terminating...", e);
                stop();
            }
        }
    }

    @Override
    public synchronized void println(TerminalMessage message) {
        System.out.printf(
                "\r%s: (%s) -> (%s): %s%n", message.time(), message.sender(), message.receiver(), message.message());
        System.out.print(prompt + " ");
    }

    @Override
    protected void initialize() {
        LOG.info("Initializing StdIOTerminalUI");
    }

    @Override
    public synchronized void setPrompt(RichString prompt) {
        this.prompt = prompt.toString();
    }

    @Override
    public synchronized void setStatus(RichString status) {
        if (Objects.equals(status, this.status)) {
            return;
        }
        this.status = status.toString();
        statusChanged.set(true);
    }
}
