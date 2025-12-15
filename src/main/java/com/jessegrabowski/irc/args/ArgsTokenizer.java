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
package com.jessegrabowski.irc.args;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArgsTokenizer {

    /**
     * Create synthetic "raw" text for pre-tokenized input, suitable for use with {@link ArgsParser}.
     */
    public String toSyntheticRaw(String[] input) {
        Objects.requireNonNull(input);

        return String.join(" ", input);
    }

    /**
     * Wrap pre-tokenized input.
     *
     * <p>Token indexes point to the corresponding location in text generated using {@link
     * #toSyntheticRaw(String[])}
     */
    public List<ArgsToken> tokenize(String[] input) {
        Objects.requireNonNull(input);

        List<ArgsToken> tokens = new ArrayList<>();

        int index = 0;
        for (String token : input) {
            tokens.add(new ArgsToken(token, index, index + token.length()));
            index += token.length() + 1;
        }

        return tokens;
    }

    /**
     * Split an input line of text into a list of tokens, respecting quotes for grouping.
     *
     * <p>The token index ranges include the surrounding quote characters if a token was quoted, which
     * is useful for "raw" / greedy parsing. The token content itself omits quotes and applies
     * backslash escapes for simpler downstream handling.
     */
    public List<ArgsToken> tokenize(String input) {
        Objects.requireNonNull(input);

        char[] chars = input.toCharArray();
        List<ArgsToken> result = new ArrayList<>();

        int startIndex = -1;
        boolean quoted = false;
        boolean escaped = false;
        StringBuilder tokenBuilder = null;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (Character.isWhitespace(c)) {
                if (tokenBuilder == null) {
                    continue;
                }

                if (escaped) {
                    tokenBuilder.append(c);
                    escaped = false;
                } else if (quoted) {
                    tokenBuilder.append(c);
                } else {
                    result.add(new ArgsToken(tokenBuilder.toString(), startIndex, i));
                    tokenBuilder = null;
                }
                continue;
            }
            if (tokenBuilder == null) {
                tokenBuilder = new StringBuilder();
                startIndex = i;
            }
            if (escaped) {
                tokenBuilder.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                quoted = !quoted;
            } else {
                tokenBuilder.append(c);
            }
        }

        if (tokenBuilder != null) {
            if (escaped) {
                tokenBuilder.append('\\');
            }
            result.add(new ArgsToken(tokenBuilder.toString(), startIndex, chars.length));
        }

        return result;
    }
}
