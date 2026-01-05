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

import java.util.List;

public class ParameterPlanner {

    private final ParameterPlan[] plans;
    private final ParameterExtractor<?>[] extractors;

    public ParameterPlanner(int paramCount, ParameterExtractor<?>... extractors) throws NotEnoughParametersException {
        this.extractors = extractors;
        this.plans = new ParameterPlan[extractors.length];

        int remaining = paramCount;
        int[] stakes = new int[extractors.length];
        for (int i = 0; i < extractors.length; i++) {
            ParameterExtractor<?> extractor = extractors[i];
            stakes[i] += extractor.consumeAtLeast();
            remaining -= extractor.consumeAtLeast();
        }
        for (int i = 0; i < extractors.length && remaining > 0; i++) {
            ParameterExtractor<?> extractor = extractors[i];
            int extra = Math.min(remaining, extractor.consumeAtMost() - extractor.consumeAtLeast());
            stakes[i] += extra;
            remaining -= extra;
        }
        if (remaining < 0) {
            throw new NotEnoughParametersException(
                    "expected at least %d parameters but got %d".formatted(paramCount - remaining, paramCount));
        }

        int position = 0;
        for (int i = 0; i < plans.length; i++) {
            if (stakes[i] == 0) {
                plans[i] = new DefaultParameterPlan();
            } else {
                plans[i] = new RangeParameterPlan(position, position + stakes[i] - 1);
                position += stakes[i];
            }
        }
    }

    public <T> T get(int index, List<String> parameters, ParameterExtractor<T> extractor) throws Exception {
        if (index < 0 || index >= plans.length) {
            throw new IndexOutOfBoundsException(index);
        }
        if (extractor != extractors[index]) {
            throw new IllegalArgumentException("Extractor must match value passed in constructor");
        }
        ParameterPlan plan = plans[index];
        return switch (plan) {
            case DefaultParameterPlan _ -> extractor.getDefaultValue();
            case RangeParameterPlan p -> extractor.extract(p.start(), p.end(), parameters);
        };
    }
}
