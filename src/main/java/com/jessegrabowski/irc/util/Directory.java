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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Directory {

    private final Path path;

    public Directory(String pathString) {
        this.path = Paths.get(pathString);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("unable to create directory '" + path + "'", e);
        }
        if (!Files.isDirectory(this.path)) {
            throw new IllegalArgumentException("'" + path + "' is not a directory");
        }
        if (!Files.isWritable(path)) {
            throw new IllegalArgumentException("'" + path + "' is not writeable");
        }
    }

    public Path getPath() {
        return path;
    }

    public Path createFile(String filename) throws IOException {
        Files.createDirectories(path);

        String base = baseName(filename);
        String ext = extension(filename);

        int i = 0;
        while (i < 999) {
            String candidateName = (i == 0) ? filename : base + "-" + i + ext;

            Path candidate = path.resolve(candidateName);

            try {
                return Files.createFile(candidate);
            } catch (FileAlreadyExistsException ignored) {
                // try next suffix
            }

            i++;
        }

        throw new IOException("Unable to create file, could not claim unique filename");
    }

    private static String baseName(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return filename;
        }
        return filename.substring(0, dot);
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0) {
            return "";
        }
        return filename.substring(dot);
    }
}
