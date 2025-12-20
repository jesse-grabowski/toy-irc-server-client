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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Transaction {

    private static final Logger LOG = Logger.getLogger(Transaction.class.getName());
    private static final ThreadLocal<Transaction> CURRENT = new ThreadLocal<>();

    private final List<Runnable> compensations = new ArrayList<>();

    private Transaction() {}

    public static void start() {
        if (CURRENT.get() != null) {
            throw new IllegalStateException("Transaction already started");
        }
        CURRENT.set(new Transaction());
    }

    public static void commit() {
        CURRENT.remove();
    }

    public static void rollback() {
        Transaction transaction = CURRENT.get();
        if (transaction == null) {
            return;
        }

        CURRENT.remove();
        for (Runnable compensation : transaction.compensations.reversed()) {
            try {
                compensation.run();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error rolling back transaction", e);
            }
        }
    }

    public static void addCompensation(Runnable compensation) {
        Transaction transaction = CURRENT.get();
        if (transaction == null) {
            return;
        }
        transaction.compensations.add(compensation);
    }

    public static <V> void addTransactionally(Set<V> set, V value) {
        if (set.add(value)) {
            addCompensation(() -> set.remove(value));
        }
    }

    public static <V> void removeTransactionally(Set<V> set, V value) {
        if (set.remove(value)) {
            addCompensation(() -> set.add(value));
        }
    }

    public static <V> void addTransactionally(SequencedSet<V> set, V value) {
        if (set.add(value)) {
            addCompensation(() -> set.remove(value));
        }
    }

    public static <V> void removeTransactionally(SequencedSet<V> set, V value) {
        if (!set.contains(value)) {
            return;
        }

        List<V> snapshot = new ArrayList<>(set);
        set.remove(value);
        addCompensation(() -> {
            set.clear();
            set.addAll(snapshot);
        });
    }

    public static <K, V> void putTransactionally(Map<K, V> map, K key, V value) {
        boolean hadKey = map.containsKey(key);
        V previous = map.put(key, value);

        if (hadKey) {
            addCompensation(() -> map.put(key, previous));
        } else {
            addCompensation(() -> map.remove(key));
        }
    }

    public static <K, V> V removeTransactionally(Map<K, V> map, K key) {
        if (map.containsKey(key)) {
            V previous = map.remove(key);
            addCompensation(() -> map.put(key, previous));
            return previous;
        }
        return null;
    }

    public static <K, V> V computeIfAbsentTransactionally(
            Map<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        if (map.containsKey(key)) {
            return map.get(key);
        }

        V value = mappingFunction.apply(key);
        map.put(key, value);
        addCompensation(() -> map.remove(key));
        return value;
    }

    public static <V> void addTransactionally(List<V> list, V value) {
        int index = list.size();
        list.add(value);
        addCompensation(() -> list.remove(index));
    }

    public static <V> void removeTransactionally(List<V> list, V value) {
        int index = list.indexOf(value);
        if (index >= 0) {
            V removed = list.remove(index);
            addCompensation(() -> list.add(index, removed));
        }
    }
}
