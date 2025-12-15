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
package com.jessegrabowski.irc.util;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// not a huge fan of jul but since we can't really use libraries this'll have to do
public final class LoggingConfigurer {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private LoggingConfigurer() {}

    public static void configure(String filename, Level level) throws IOException {
        LogManager.getLogManager().reset();

        // FileHandler supports %u (unique) and %g (rotation index)
        FileHandler fileHandler = new FileHandler(filename, true);
        fileHandler.setFormatter(new LogbackishFormatter());
        fileHandler.setLevel(Level.ALL);

        Logger.getLogger("java.lang").setLevel(Level.WARNING);

        Logger root = Logger.getLogger("");
        root.addHandler(fileHandler);
        root.setLevel(level);
    }

    // low quality replication of logback's default format
    private static class LogbackishFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            String timestamp = FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
            String level = padLevel(record.getLevel().getName());
            String thread = Thread.currentThread().getName();

            String logger = record.getLoggerName();
            if (logger == null) {
                logger = "unknown";
            }

            String msg = formatMessage(record).replace("\n", "\\n").replace("\r", "\\r");

            return String.format("%s %s [%s] %s - %s%n", timestamp, level, thread, logger, msg);
        }

        private String padLevel(String level) {
            if (level.length() >= 5) return level;
            return String.format("%-5s", level);
        }
    }
}
