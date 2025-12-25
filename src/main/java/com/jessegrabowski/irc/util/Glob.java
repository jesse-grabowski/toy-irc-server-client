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

import com.jessegrabowski.irc.protocol.IRCCaseMapping;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/*
 * Java doesn't have a built-in glob matcher, so we roll our own.
 * Kinda nasty, but at least it's easy to test.
 */
public final class Glob {

    private final List<GlobPart> parts;

    private Glob(List<GlobPart> parts) {
        this.parts = parts;
    }

    public static Glob of(String value) {
        List<GlobPart> partList = new ArrayList<>();

        StringBuilder literalBuffer = new StringBuilder();
        int i = 0;

        while (i < value.length()) {
            char c = value.charAt(i);

            switch (c) {
                case '*' -> {
                    if (!literalBuffer.isEmpty()) {
                        partList.add(new Literal(literalBuffer.toString()));
                        literalBuffer.setLength(0);
                    }

                    while (i < value.length() && value.charAt(i) == '*') {
                        i++;
                    }

                    partList.add(new Star());
                }
                case '?' -> {
                    if (!literalBuffer.isEmpty()) {
                        partList.add(new Literal(literalBuffer.toString()));
                        literalBuffer.setLength(0);
                    }

                    partList.add(new QuestionMark());
                    i++;
                }

                case '[' -> {
                    final int bracketStart = i;
                    int j = i + 1;

                    boolean inverted = false;
                    if (j < value.length() && (value.charAt(j) == '!' || value.charAt(j) == '^')) {
                        inverted = true;
                        j++;
                    }

                    // find the end of the group
                    while (j < value.length() && value.charAt(j) != ']') {
                        j++;
                    }

                    // unclosed, so we treat literally
                    if (j == value.length()) {
                        literalBuffer.append('[');
                        i = bracketStart + 1;
                        continue;
                    }

                    if (!literalBuffer.isEmpty()) {
                        partList.add(new Literal(literalBuffer.toString()));
                        literalBuffer.setLength(0);
                    }

                    String inner = value.substring(bracketStart + 1 + (inverted ? 1 : 0), j);
                    // I would love to just use character arrays but that has some performance struggles
                    // with massive ranges so we're going to go with this instead
                    CharRanges ranges = CharRanges.parse(inner);
                    partList.add(inverted ? new InvertedCharacterSet(ranges) : new CharacterSet(ranges));
                    i = j + 1;
                }
                case '{' -> {
                    final int bracketStart = i;
                    int j = i + 1;

                    // find the end of the group
                    while (j < value.length() && value.charAt(j) != '}') {
                        j++;
                    }

                    // unclosed, so we treat literally
                    if (j == value.length()) {
                        literalBuffer.append('{');
                        i = bracketStart + 1;
                        continue;
                    }

                    if (!literalBuffer.isEmpty()) {
                        partList.add(new Literal(literalBuffer.toString()));
                        literalBuffer.setLength(0);
                    }

                    // -1 to keep trailing empties, which we consider an optional group
                    String[] array = value.substring(bracketStart + 1, j).split(",", -1);
                    partList.add(new StringSet(array));
                    i = j + 1;
                }
                default -> {
                    literalBuffer.append(c);
                    i++;
                }
            }
        }

        if (!literalBuffer.isEmpty()) {
            partList.add(new Literal(literalBuffer.toString()));
        }

        return new Glob(partList);
    }

    public Glob casefold(IRCCaseMapping caseMapping) {
        return new Glob(parts.stream().map(part -> part.casefold(caseMapping)).toList());
    }

    public boolean isLiteral() {
        return parts.stream().allMatch(part -> part instanceof Literal);
    }

    public boolean matches(String input) {
        return matches(input, 0, 0);
    }

    private boolean matches(String input, int startIndex, int partIndex) {
        if (partIndex >= parts.size()) {
            return startIndex == input.length();
        }

        GlobPart part = parts.get(partIndex);
        GlobPart nextPart = partIndex + 1 < parts.size() ? parts.get(partIndex + 1) : null;
        int[] candidates = part.consume(input, startIndex, nextPart);
        if (candidates == null) {
            return false;
        }
        for (int candidate : candidates) {
            if (matches(input, startIndex + candidate, partIndex + 1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Glob glob = (Glob) o;
        return Objects.equals(parts, glob.parts);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parts);
    }

    @Override
    public String toString() {
        return parts.stream().map(GlobPart::toString).collect(Collectors.joining());
    }

    private interface GlobPart {
        int[] consume(String input, int startIndex, GlobPart nextGlobPart);

        GlobPart casefold(IRCCaseMapping caseMapping);
    }

    private static class Literal implements GlobPart {

        private final String literal;

        private Literal(String literal) {
            this.literal = literal;
        }

        @Override
        public int[] consume(String input, int startIndex, GlobPart nextGlobPart) {
            if (input.length() - startIndex < literal.length()) {
                return null;
            }

            for (int i = 0; i < literal.length(); i++) {
                if (input.charAt(startIndex + i) != literal.charAt(i)) {
                    return null;
                }
            }

            return new int[] {literal.length()};
        }

        @Override
        public GlobPart casefold(IRCCaseMapping caseMapping) {
            return new Literal(caseMapping.normalizeNickname(literal));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Literal literal1 = (Literal) o;
            return Objects.equals(literal, literal1.literal);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(literal);
        }

        @Override
        public String toString() {
            return literal;
        }
    }

    private static class QuestionMark implements GlobPart {
        @Override
        public int[] consume(String input, int startIndex, GlobPart nextGlobPart) {
            if (startIndex == input.length()) {
                return null;
            }
            return new int[] {1};
        }

        @Override
        public GlobPart casefold(IRCCaseMapping caseMapping) {
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof QuestionMark;
        }

        @Override
        public int hashCode() {
            return QuestionMark.class.hashCode();
        }

        @Override
        public String toString() {
            return "?";
        }
    }

    private static class Star implements GlobPart {
        @Override
        public int[] consume(String input, int startIndex, GlobPart nextGlobPart) {
            if (nextGlobPart == null) {
                return new int[] {input.length() - startIndex};
            }
            List<Integer> candidates = new ArrayList<>();
            for (int i = startIndex; i <= input.length(); i++) {
                if (nextGlobPart.consume(input, i, null) != null) {
                    candidates.add(i - startIndex);
                }
            }
            return candidates.isEmpty()
                    ? null
                    : candidates.stream().mapToInt(i -> i).toArray();
        }

        @Override
        public GlobPart casefold(IRCCaseMapping caseMapping) {
            return this;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Star;
        }

        @Override
        public int hashCode() {
            return Star.class.hashCode();
        }

        @Override
        public String toString() {
            return "*";
        }
    }

    private static final class CharRanges {
        private final String spec;
        private final char[] start;
        private final char[] end;

        private CharRanges(String spec, char[] start, char[] end) {
            this.spec = spec;
            this.start = start;
            this.end = end;
        }

        boolean contains(char c) {
            for (int i = 0; i < start.length; i++) {
                if (c >= start[i] && c <= end[i]) {
                    return true;
                }
            }
            return false;
        }

        // this is a bit gnarly so let's give it its own method
        private static CharRanges parse(String spec) {
            List<Character> starts = new ArrayList<>();
            List<Character> ends = new ArrayList<>();

            int i = 0;
            while (i < spec.length()) {
                char a = spec.charAt(i);

                if (i + 2 < spec.length() && spec.charAt(i + 1) == '-') {
                    char b = spec.charAt(i + 2);
                    char lo = a;
                    char hi = b;
                    // can't miss an opportunity to bust out the xor swap
                    if (lo > hi) {
                        lo ^= hi;
                        hi ^= lo;
                        lo ^= hi;
                    }
                    starts.add(lo);
                    ends.add(hi);
                    i += 3;
                    continue;
                }

                starts.add(a);
                ends.add(a);
                i++;
            }

            char[] s = new char[starts.size()];
            char[] e = new char[ends.size()];
            for (int x = 0; x < starts.size(); x++) {
                s[x] = starts.get(x);
                e[x] = ends.get(x);
            }
            return new CharRanges(spec, s, e);
        }

        private CharRanges casefold(IRCCaseMapping caseMapping) {
            return CharRanges.parse(caseMapping.normalizeNickname(spec));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            CharRanges that = (CharRanges) o;
            return Objects.equals(spec, that.spec) && Arrays.equals(start, that.start) && Arrays.equals(end, that.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(spec, Arrays.hashCode(start), Arrays.hashCode(end));
        }

        @Override
        public String toString() {
            return spec;
        }
    }

    private static class CharacterSet implements GlobPart {
        private final CharRanges ranges;

        private CharacterSet(CharRanges ranges) {
            this.ranges = ranges;
        }

        @Override
        public int[] consume(String input, int startIndex, GlobPart nextGlobPart) {
            if (startIndex == input.length()) {
                return null;
            }
            return ranges.contains(input.charAt(startIndex)) ? new int[] {1} : null;
        }

        @Override
        public GlobPart casefold(IRCCaseMapping caseMapping) {
            return new CharacterSet(ranges.casefold(caseMapping));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            CharacterSet that = (CharacterSet) o;
            return Objects.equals(ranges, that.ranges);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(ranges);
        }

        @Override
        public String toString() {
            return "[" + ranges + "]";
        }
    }

    private static class InvertedCharacterSet implements GlobPart {
        private final CharRanges ranges;

        private InvertedCharacterSet(CharRanges ranges) {
            this.ranges = ranges;
        }

        @Override
        public int[] consume(String input, int startIndex, GlobPart nextGlobPart) {
            if (startIndex == input.length()) {
                return null;
            }
            return ranges.contains(input.charAt(startIndex)) ? null : new int[] {1};
        }

        @Override
        public GlobPart casefold(IRCCaseMapping caseMapping) {
            return new InvertedCharacterSet(ranges.casefold(caseMapping));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            InvertedCharacterSet that = (InvertedCharacterSet) o;
            return Objects.equals(ranges, that.ranges);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(ranges);
        }

        @Override
        public String toString() {
            return "[!" + ranges + "]";
        }
    }

    private static class StringSet implements GlobPart {
        private final String[] strings;

        private StringSet(String[] strings) {
            this.strings = strings;
        }

        @Override
        public int[] consume(String input, int startIndex, GlobPart nextGlobPart) {
            List<Integer> candidates = new ArrayList<>();
            for (String s : strings) {
                if (input.startsWith(s, startIndex)) {
                    candidates.add(s.length());
                }
            }
            return candidates.isEmpty()
                    ? null
                    : candidates.stream().mapToInt(i -> i).toArray();
        }

        @Override
        public GlobPart casefold(IRCCaseMapping caseMapping) {
            return new StringSet(
                    Arrays.stream(strings).map(caseMapping::normalizeNickname).toArray(String[]::new));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            StringSet stringSet = (StringSet) o;
            return Objects.deepEquals(strings, stringSet.strings);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(strings);
        }

        @Override
        public String toString() {
            return "{" + String.join(",", strings) + "}";
        }
    }
}
