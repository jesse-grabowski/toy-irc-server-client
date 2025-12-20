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

public final class StateGuard<T> {

    private volatile Thread thread;

    private T state;

    public StateGuard(T state) {
        this.state = state;
    }

    public StateGuard() {
        this(null);
    }

    public T getState() {
        assertBoundThread();
        return state;
    }

    public void doTransactionally(ThrowingConsumer<T> consumer) throws Exception {
        T state = getState();
        try {
            Transaction.start();
            consumer.apply(state);
            Transaction.commit();
        } catch (Exception e) {
            Transaction.rollback();
            throw e;
        }
    }

    public void setState(T state) {
        assertBoundThread();
        this.state = state;
    }

    public void bindToCurrentThread() {
        if (this.thread != null && this.thread != Thread.currentThread()) {
            throw new IllegalStateException("state has already been bound to a different thread");
        }
        this.thread = Thread.currentThread();
    }

    private void assertBoundThread() {
        if (thread != Thread.currentThread()) {
            throw new IllegalStateException(
                    "state accessed from %s (expected %s)".formatted(Thread.currentThread(), thread));
        }
    }
}
