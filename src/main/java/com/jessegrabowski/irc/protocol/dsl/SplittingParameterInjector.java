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
import com.jessegrabowski.irc.util.ThrowingBiFunction;
import com.jessegrabowski.irc.util.ThrowingFunction;
import java.util.List;
import java.util.function.Supplier;

public class SplittingParameterInjector<L, R> {

    private final boolean required;
    private final ThrowingFunction<String, Pair<L, R>> splitter;
    private final String name;

    private Pair<L, R> result = null;
    private int extracted = -1;
    private boolean staked = false;

    public SplittingParameterInjector(boolean required, ThrowingFunction<String, Pair<L, R>> splitter, String name) {
        this.required = required;
        this.splitter = splitter;
        this.name = name;
    }

    private Pair<L, R> extract(int index, List<String> parameters) throws Exception {
        if (extracted == index) {
            return result;
        } else if (extracted != -1) {
            throw new IllegalStateException(
                    "SplittingParameterInjector already extracted %d and does not support reuse".formatted(extracted));
        }
        extracted = index;

        String rawValue = parameters.get(index);
        result = splitter.apply(rawValue);
        return result;
    }

    private Pair<Integer, Integer> claimStake() {
        if (staked) {
            return new Pair<>(0, 0);
        } else {
            staked = true;
            return new Pair<>(required ? 1 : 0, 1);
        }
    }

    public ParameterExtractor<L> left(L defaultValue) {
        return new SplitPart<>(
                (i, p) -> {
                    Pair<L, R> result = extract(i, p);
                    return result != null ? result.left() : defaultValue;
                },
                () -> result.left());
    }

    public ParameterExtractor<R> right(R defaultValue) {
        return new SplitPart<>(
                (i, p) -> {
                    Pair<L, R> result = extract(i, p);
                    return result != null ? result.right() : defaultValue;
                },
                () -> result.right());
    }

    private class SplitPart<T> implements ParameterExtractor<T> {

        private final ThrowingBiFunction<Integer, List<String>, T> resultExtractor;
        private final Supplier<T> defaultValueSupplier;

        private Pair<Integer, Integer> stake;

        public SplitPart(
                ThrowingBiFunction<Integer, List<String>, T> resultExtractor, Supplier<T> defaultValueSupplier) {
            this.resultExtractor = resultExtractor;
            this.defaultValueSupplier = defaultValueSupplier;
        }

        @Override
        public T extract(int start, int endInclusive, List<String> parameters) throws Exception {
            return resultExtractor.apply(start, parameters);
        }

        @Override
        public int consumeAtLeast() {
            if (stake == null) {
                stake = SplittingParameterInjector.this.claimStake();
            }
            return stake.left();
        }

        @Override
        public int consumeAtMost() {
            if (stake == null) {
                stake = SplittingParameterInjector.this.claimStake();
            }
            return stake.right();
        }

        @Override
        public T getDefaultValue() {
            return defaultValueSupplier.get();
        }

        @Override
        public String name() {
            return name;
        }
    }
}
