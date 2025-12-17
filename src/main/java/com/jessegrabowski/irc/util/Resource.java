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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.regex.Pattern;

// moderately borrowed the design from spring
public sealed interface Resource permits Resource.ClasspathResource, Resource.FileSystemResource {

    Pattern SCHEME_LIKE = Pattern.compile("^[A-Za-z][A-Za-z0-9+.-]*:.*");
    Pattern WINDOWS_FILE_LIKE = Pattern.compile("^[A-Za-z]:[\\\\/].*");

    InputStream getInputStream() throws IOException;

    static Resource of(String path) {
        if (path.startsWith("classpath:")) {
            return new ClasspathResource(path.substring("classpath:".length()));
        }

        if (path.startsWith("file:")) {
            return new FileSystemResource(Paths.get(URI.create(path)));
        }

        // I genuinely do not know why I am supporting absolute paths
        // on Windows but you're welcome
        boolean isSchemeLike = SCHEME_LIKE.matcher(path).matches();
        boolean isWindowsLike = WINDOWS_FILE_LIKE.matcher(path).matches();

        if (isSchemeLike && !isWindowsLike) {
            throw new IllegalArgumentException("Unsupported scheme: " + path.substring(0, path.indexOf(':')));
        }

        return new FileSystemResource(Paths.get(path));
    }

    final class ClasspathResource implements Resource {

        private final URL resource;

        private ClasspathResource(String path) {
            String p = path.startsWith("/") ? path.substring(1) : path;
            this.resource = Resource.class.getClassLoader().getResource(p);
            if (this.resource == null) {
                throw new IllegalArgumentException("'" + path + "' could not be found on the classpath");
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return resource.openStream();
        }
    }

    final class FileSystemResource implements Resource {

        private final Path path;

        private FileSystemResource(Path path) {
            this.path = path;
            if (!Files.exists(this.path)) {
                throw new IllegalArgumentException("'" + path + "' does not exist");
            }
            if (!Files.isRegularFile(this.path)) {
                throw new IllegalArgumentException("'" + path + "' is not a regular file");
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path, StandardOpenOption.READ);
        }
    }
}
