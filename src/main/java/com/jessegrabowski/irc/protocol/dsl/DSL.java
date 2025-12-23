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
package com.jessegrabowski.irc.protocol.dsl;

import com.jessegrabowski.irc.util.Pair;
import com.jessegrabowski.irc.util.ThrowingFunction;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.stream.Collectors;

public final class DSL {
    private DSL() {}

    public static ParameterExtractor<String> required(String name) {
        return new SingleParameterExtractor<>(x -> x, true, false, null, name);
    }

    public static ParameterExtractor<String> requiredAllowEmpty(String name) {
        return new SingleParameterExtractor<>(x -> x, true, true, null, name);
    }

    public static <T> ParameterExtractor<T> required(String name, ThrowingFunction<String, T> mapper) {
        return new SingleParameterExtractor<>(mapper, true, false, null, name);
    }

    public static ParameterExtractor<String> optional(String name) {
        return new SingleParameterExtractor<>(x -> x, false, false, null, name);
    }

    public static ParameterExtractor<String> optionalAllowEmpty(String name) {
        return new SingleParameterExtractor<>(x -> x, false, true, null, name);
    }

    public static <T> ParameterExtractor<T> optional(String name, ThrowingFunction<String, T> mapper) {
        return new SingleParameterExtractor<>(mapper, false, false, null, name);
    }

    public static <T> ParameterExtractor<T> optional(String name, ThrowingFunction<String, T> mapper, T defaultValue) {
        return new SingleParameterExtractor<>(mapper, false, false, defaultValue, name);
    }

    public static ParameterExtractor<List<String>> greedyRequired(String name) {
        return new MultipleParameterExtractor<List<String>, String, List<String>>(
                ArrayList::new, x -> x, List::add, x -> x, 1, Integer.MAX_VALUE, List.of(), name);
    }

    public static ParameterExtractor<List<String>> greedyOptional(String name) {
        return new MultipleParameterExtractor<List<String>, String, List<String>>(
                ArrayList::new, x -> x, List::add, x -> x, 0, Integer.MAX_VALUE, List.of(), name);
    }

    public static <K, V> ParameterExtractor<SequencedMap<K, V>> greedyRequiredMap(
            String name, ThrowingFunction<String, Map.Entry<K, V>> mapper) {
        return new MultipleParameterExtractor<List<Map.Entry<K, V>>, Map.Entry<K, V>, SequencedMap<K, V>>(
                ArrayList::new,
                mapper,
                List::add,
                entries -> entries.stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new)),
                1,
                Integer.MAX_VALUE,
                new LinkedHashMap<>(),
                name);
    }

    public static <L, R> SplittingParameterInjector<L, R> splitRequired(
            String name, ThrowingFunction<String, Pair<L, R>> splitter) {
        return new SplittingParameterInjector<>(true, splitter, name);
    }

    public static SplittingParameterInjector<String, String> splitString(String name, String regex) {
        return new SplittingParameterInjector<>(
                true,
                s -> {
                    String[] parts = s.split(regex, 2);
                    if (parts.length == 2) {
                        return new Pair<>(parts[0], parts[1]);
                    } else {
                        return new Pair<>(parts[0], null);
                    }
                },
                name);
    }
}
