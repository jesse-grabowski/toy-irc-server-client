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

import com.jessegrabowski.irc.util.ThrowingFunction;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class MultipleParameterExtractor<C extends Collection<T>, T, R> implements ParameterExtractor<R> {

    private final Supplier<C> factory;
    private final ThrowingFunction<String, T> mapper;
    private final BiConsumer<C, T> accumulator;
    private final ThrowingFunction<C, R> finisher;
    private final int consumeAtLeast;
    private final int consumeAtMost;
    private final R defaultValue;
    private final String name;

    public MultipleParameterExtractor(
            Supplier<C> factory,
            ThrowingFunction<String, T> mapper,
            BiConsumer<C, T> accumulator,
            ThrowingFunction<C, R> finisher,
            int consumeAtLeast,
            int consumeAtMost,
            R defaultValue,
            String name) {
        this.factory = factory;
        this.mapper = mapper;
        this.accumulator = accumulator;
        this.finisher = finisher;
        this.consumeAtLeast = consumeAtLeast;
        this.consumeAtMost = consumeAtMost;
        this.defaultValue = defaultValue;
        this.name = name;
    }

    @Override
    public R extract(int start, int endInclusive, List<String> parameters) throws Exception {
        C collection = factory.get();
        for (int i = start; i <= endInclusive; i++) {
            String rawValue = parameters.get(i);
            T mappedValue = mapper.apply(rawValue);
            accumulator.accept(collection, mappedValue);
        }
        return finisher.apply(collection);
    }

    @Override
    public int consumeAtLeast() {
        return consumeAtLeast;
    }

    @Override
    public int consumeAtMost() {
        return consumeAtMost;
    }

    @Override
    public R getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String name() {
        return name;
    }
}
