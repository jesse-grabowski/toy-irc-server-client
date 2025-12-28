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

import com.jessegrabowski.irc.network.Port;
import com.jessegrabowski.irc.util.Resource;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface ArgsParserBuilder<T extends ArgsProperties> {
    ArgsParserBuilder<T> addUsageExample(String usageExample);

    ArgsParserBuilder<T> addBooleanFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, Boolean> propertiesSetter,
            String description,
            boolean required);

    ArgsParserBuilder<T> addStringFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, String> propertiesSetter,
            String description,
            boolean required);

    ArgsParserBuilder<T> addInetAddressFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, InetAddress> propertiesSetter,
            String description,
            boolean required);

    ArgsParserBuilder<T> addIntegerFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, Integer> propertiesSetter,
            String description,
            boolean required);

    ArgsParserBuilder<T> addPortFlag(
            char shortKey, String longKey, BiConsumer<T, Port> propertiesSetter, String description, boolean required);

    ArgsParserBuilder<T> addCharsetFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, Charset> propertiesSetter,
            String description,
            boolean required);

    ArgsParserBuilder<T> addResourceFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, Resource> propertiesSetter,
            String description,
            boolean required);

    ArgsParserBuilder<T> addStringPositional(
            int position, BiConsumer<T, String> propertiesSetter, String description, boolean required);

    ArgsParserBuilder<T> addInetAddressPositional(
            int position, BiConsumer<T, InetAddress> propertiesSetter, String description, boolean required);

    ArgsParserBuilder<T> addGreedyResourcePositional(
            int position, BiConsumer<T, Resource> propertiesSetter, String description, boolean required);

    ArgsParserBuilder<T> addCommaSeparatedListPositional(
            int position, BiConsumer<T, List<String>> propertiesSetter, String description, boolean required);

    ArgsParserBuilder<T> addGreedyStringPositional(
            int position, BiConsumer<T, String> propertiesSetter, String description, boolean required);

    ArgsParserBuilder<T> addGreedyListPositional(
            int position,
            Function<T, List<String>> propertiesGetter,
            BiConsumer<T, List<String>> propertiesSetter,
            String description,
            boolean required);

    ArgsParserBuilder<T> setFlagParsingEnabled(boolean value);

    ArgsParser<T> build();
}
