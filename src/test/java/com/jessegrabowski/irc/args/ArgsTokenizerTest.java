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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ArgsTokenizerTest {

    private final ArgsTokenizer tokenizer = new ArgsTokenizer();

    private void assertToken(ArgsToken tok, String text, int start, int end) {
        assertEquals(text, tok.token(), "token text");
        assertEquals(start, tok.startInclusive(), "start index");
        assertEquals(end, tok.endExclusive(), "end index");
    }

    @Test
    void emptyStringProducesNoTokens() {
        List<ArgsToken> tokens = tokenizer.tokenize("");
        assertTrue(tokens.isEmpty());
    }

    @Test
    void whitespaceOnlyProducesNoTokens() {
        List<ArgsToken> tokens = tokenizer.tokenize("   \t  \n  ");
        assertTrue(tokens.isEmpty());
    }

    @Test
    void singleWordProducesSingleToken() {
        List<ArgsToken> tokens = tokenizer.tokenize("foo");
        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "foo", 0, 3);
    }

    @Test
    void multipleWordsWithExtraSpaces() {
        List<ArgsToken> tokens = tokenizer.tokenize("  foo   bar   baz  ");
        assertEquals(3, tokens.size());

        assertToken(tokens.get(0), "foo", 2, 5);
        assertToken(tokens.get(1), "bar", 8, 11);
        assertToken(tokens.get(2), "baz", 14, 17);
    }

    @Test
    void quotedSingleToken() {
        String input = "\"hello world\"";
        List<ArgsToken> tokens = tokenizer.tokenize(input);
        assertEquals(1, tokens.size());

        ArgsToken t = tokens.get(0);
        assertEquals("hello world", t.token());
        assertEquals(0, t.startInclusive());
        assertEquals(input.length(), t.endExclusive());
        assertEquals(input, input.substring(t.startInclusive(), t.endExclusive()));
    }

    @Test
    void quotingGroupsWords() {
        String input = "foo \"bar baz\" qux";
        List<ArgsToken> tokens = tokenizer.tokenize(input);
        assertEquals(3, tokens.size());

        assertToken(tokens.get(0), "foo", 0, 3);
        assertToken(tokens.get(1), "bar baz", 4, 13);
        assertToken(tokens.get(2), "qux", 14, 17);
    }

    @Test
    void spacesInsideQuotesArePreserved() {
        String input = "\"a   b   c\"";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "a   b   c", 0, input.length());
    }

    @Test
    void unterminatedQuotedTokenConsumesRest() {
        String input = "\"hello   world";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "hello   world", 0, input.length());
    }

    @Test
    void escapedSpaceOutsideQuotesIsLiteral() {
        String input = "foo\\ bar";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "foo bar", 0, input.length());
    }

    @Test
    void escapedQuoteOutsideQuotesIsLiteralQuote() {
        String input = "foo \\\"bar\\\" baz";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(3, tokens.size());

        assertToken(tokens.get(0), "foo", 0, 3);
        assertToken(tokens.get(1), "\"bar\"", 4, 11);
        assertToken(tokens.get(2), "baz", 12, 15);
    }

    @Test
    void escapedQuoteInsideQuotesIsLiteralQuote() {
        String input = "\"say \\\"hi\\\"\"";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "say \"hi\"", 0, input.length());
    }

    @Test
    void doubleBackslashBecomesSingleLiteralBackslash() {
        String input = "foo\\\\bar";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "foo\\bar", 0, input.length());
    }

    @Test
    void trailingBackslashIsLiteral() {
        String input = "foo\\";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "foo\\", 0, input.length());
    }

    @Test
    void escapedSpaceInsideQuotesIsLiteral() {
        String input = "\"a\\ b\"";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(1, tokens.size());
        assertToken(tokens.get(0), "a b", 0, input.length());
    }

    @Test
    void spansIncludeQuotesAndBackslashes() {
        String input = "cmd \"a b\" c\\ d";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(3, tokens.size());

        ArgsToken t0 = tokens.get(0);
        ArgsToken t1 = tokens.get(1);
        ArgsToken t2 = tokens.get(2);

        assertEquals("cmd", t0.token());
        assertEquals("a b", t1.token());
        assertEquals("c d", t2.token());

        assertEquals("cmd", input.substring(t0.startInclusive(), t0.endExclusive()));
        assertEquals("\"a b\"", input.substring(t1.startInclusive(), t1.endExclusive()));
        assertEquals("c\\ d", input.substring(t2.startInclusive(), t2.endExclusive()));
    }

    @Test
    void complexMixedCase() {
        String input = " /msg  nick\\ name  \"hello  world\"  trailing\\ ";
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(4, tokens.size());

        assertEquals("/msg", tokens.get(0).token());
        assertEquals("nick name", tokens.get(1).token());
        assertEquals("hello  world", tokens.get(2).token());
        assertEquals("trailing ", tokens.get(3).token());
    }

    @Test
    void syntheticRawEmptyArrayIsEmptyString() {
        String[] input = {};
        String raw = tokenizer.toSyntheticRaw(input);
        assertEquals("", raw);
    }

    @Test
    void syntheticRawJoinsWithSingleSpaces() {
        String[] input = {"foo", "bar", "baz"};
        String raw = tokenizer.toSyntheticRaw(input);
        assertEquals("foo bar baz", raw);
    }

    @Test
    void tokenizeArrayProducesOneTokenPerElement() {
        String[] input = {"foo", "bar", "baz"};
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals(3, tokens.size());
        assertToken(tokens.get(0), "foo", 0, 3);
        assertToken(tokens.get(1), "bar", 4, 7);
        assertToken(tokens.get(2), "baz", 8, 11);
    }

    @Test
    void tokenizeArrayPreservesSpacesInsideElements() {
        String[] input = {"foo bar", "baz"};
        String raw = tokenizer.toSyntheticRaw(input);
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals("foo bar baz", raw);
        assertEquals(2, tokens.size());

        assertToken(tokens.get(0), "foo bar", 0, 7);
        assertToken(tokens.get(1), "baz", 8, 11);

        assertEquals(
                "foo bar",
                raw.substring(tokens.get(0).startInclusive(), tokens.get(0).endExclusive()));
        assertEquals(
                "baz",
                raw.substring(tokens.get(1).startInclusive(), tokens.get(1).endExclusive()));
    }

    @Test
    void arrayTokenSpansMatchSyntheticRawForMultipleArgs() {
        String[] input = {"arg1", "two words", "x"};
        String raw = tokenizer.toSyntheticRaw(input);
        List<ArgsToken> tokens = tokenizer.tokenize(input);

        assertEquals("arg1 two words x", raw);
        assertEquals(3, tokens.size());

        for (ArgsToken token : tokens) {
            String slice = raw.substring(token.startInclusive(), token.endExclusive());
            assertEquals(token.token(), slice, "slice of synthetic raw should equal token text");
        }
    }
}
