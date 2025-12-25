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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jessegrabowski.irc.protocol.IRCCaseMapping;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GlobTest {

    @ParameterizedTest(name = "glob=\"{0}\" input=\"{1}\" -> {2}")
    @MethodSource("matchCases")
    void matches(String glob, String input, boolean expected) {
        Glob g = Glob.of(glob);
        assertEquals(expected, g.matches(input));
    }

    static Stream<Arguments> matchCases() {
        return Stream.of(
                // literals
                Arguments.of("nickname", "nickname", true),
                Arguments.of("nickname", "nicknam", false),
                Arguments.of("nicknam", "nickname", false),
                Arguments.of("{hello", "{hello", true),
                Arguments.of("[hello", "[hello", true),
                // question marks
                Arguments.of("nickn?me", "nickname", true),
                Arguments.of("nickn?me", "nicknme", false),
                Arguments.of("nickn?me", "nicknaame", false),
                Arguments.of("nickn?m", "nickname", false),
                Arguments.of("nickn?me", "nicknam", false),
                Arguments.of("nicknam?", "nicknam", false),
                // wildcards
                Arguments.of("nickn*me", "nickname", true),
                Arguments.of("nickn*me", "nicknme", true),
                Arguments.of("nickn*me", "nicknaame", true),
                Arguments.of("*me", "nickname", true),
                Arguments.of("nickn*", "nickname", true),
                Arguments.of("nickn*me", "nicknaam", false),
                Arguments.of("nickn*me", "nickn", false),
                Arguments.of("nickn*me", "ickname", false),
                Arguments.of("nickn**me", "nickname", true),
                // character groups
                Arguments.of("nickn[ae]me", "nickname", true),
                Arguments.of("nickn[ae]me", "nickneme", true),
                Arguments.of("nickn[ae]me", "nicknome", false),
                Arguments.of("a[a-z]b", "aab", true),
                Arguments.of("a[a-z]b", "azb", true),
                Arguments.of("a[a-z]b", "aAb", false),
                Arguments.of("x[0-9]y", "x0y", true),
                Arguments.of("x[0-9]y", "x9y", true),
                Arguments.of("x[0-9]y", "xay", false),
                Arguments.of("a[a-cx-z]b", "aab", true),
                Arguments.of("a[a-cx-z]b", "acb", true),
                Arguments.of("a[a-cx-z]b", "axb", true),
                Arguments.of("a[a-cx-z]b", "azb", true),
                Arguments.of("a[a-cx-z]b", "adb", false),
                Arguments.of("a[!a-z]b", "a-b", true),
                Arguments.of("a[!a-z]b", "aab", false),
                Arguments.of("a[^0-9]b", "a5b", false),
                Arguments.of("a[^0-9]b", "a_b", true),
                Arguments.of("a[-a]b", "a-b", true),
                Arguments.of("a[a-]b", "a-b", true),
                Arguments.of("a[-a]b", "aab", true),
                Arguments.of("a[z-a]b", "amb", true),
                Arguments.of("a[z-a]b", "a_b", false),
                Arguments.of("a[bcx-z]b", "abb", true),
                Arguments.of("a[bcx-z]b", "acb", true),
                Arguments.of("a[bcx-z]b", "ayb", true),
                // inverse character groups
                Arguments.of("nickn[!ae]me", "nickname", false),
                Arguments.of("nickn[!ae]me", "nickneme", false),
                Arguments.of("nickn[!ae]me", "nicknome", true),
                Arguments.of("nickn[^ae]me", "nickname", false),
                Arguments.of("nickn[^ae]me", "nickneme", false),
                Arguments.of("nickn[^ae]me", "nicknome", true),
                // string groups
                Arguments.of("nickn{am,em,}e", "nickname", true),
                Arguments.of("nickn{am,em,}e", "nickneme", true),
                Arguments.of("nickn{am,em,}e", "nickne", true),
                Arguments.of("nickn{am,em}e", "nicknome", false),
                // empty glob / empty input
                Arguments.of("", "", true),
                Arguments.of("", "a", false),
                // lone wildcard on empty input
                Arguments.of("*", "", true),
                Arguments.of("?", "", false),
                // multi-star backtracking / anchoring
                Arguments.of("a*b*c", "abc", true),
                Arguments.of("a*b*c", "axbyc", true),
                Arguments.of("a*b*c", "ac", false),
                // "contains" style wildcard
                Arguments.of("*name*", "nickname", true),
                Arguments.of("*name*", "nick", false),
                // unclosed '[' treated literally, but later wildcards still expand
                Arguments.of("[ab*cd", "[abZZZcd", true),
                // unclosed '{' treated literally, but later wildcards still expand
                Arguments.of("{ab*cd", "{abZZZcd", true),
                // the brain destroyer
                Arguments.of(
                        "*{nick,}n?c[k-m]a*[0-9]{,x,xx}b[^0-9]??*{foo,bar,baz,}*end*",
                        "zznicknXckaHELLO7xxb_12___bar___endZZ",
                        true),
                Arguments.of(
                        "*{nick,}n?c[k-m]a*[0-9]{,x,xx}b[^0-9]??*{foo,bar,baz,}*end*",
                        "zznicknXckaHELLO7xxb912___bar___endZZ",
                        false));
    }

    @ParameterizedTest(name = "casefold={0} glob=\"{1}\" input=\"{2}\" -> {3}")
    @MethodSource("casefoldCases")
    void matches_withCasefold(IRCCaseMapping mapping, String glob, String input, boolean expected) {
        Glob g = Glob.of(glob).casefold(mapping);
        String foldedInput = mapping.normalizeNickname(input);
        assertEquals(expected, g.matches(foldedInput));
    }

    static Stream<Arguments> casefoldCases() {
        return Stream.of(
                // ascii
                Arguments.of(IRCCaseMapping.ASCII, "Nick*", "nickname", true),
                Arguments.of(IRCCaseMapping.ASCII, "NICKNAME", "nickname", true),
                Arguments.of(IRCCaseMapping.ASCII, "nick[A-Z]me", "nickaMe", true),
                Arguments.of(IRCCaseMapping.ASCII, "nick[^A-Z]me", "nick1me", true),
                Arguments.of(IRCCaseMapping.RFC1459, "{Nick,nick,}", "NICK", true),
                Arguments.of(IRCCaseMapping.RFC1459, "pre{FOO,Bar}post", "prebarpost", true),
                Arguments.of(IRCCaseMapping.ASCII, "Nick*", "nickname", true),
                Arguments.of(IRCCaseMapping.ASCII, "N*E", "nickname", true),
                Arguments.of(IRCCaseMapping.ASCII, "*NAME", "nickname", true),
                Arguments.of(IRCCaseMapping.ASCII, "ni?kname", "nickname", true),
                Arguments.of(IRCCaseMapping.ASCII, "ni?kname", "niCkName", true),
                Arguments.of(IRCCaseMapping.ASCII, "*[A-Z]*", "fooBar", true),
                Arguments.of(IRCCaseMapping.ASCII, "*[A-Z]*", "foobar", true),
                Arguments.of(IRCCaseMapping.ASCII, "*[^A-Z]*", "foo123", true),
                Arguments.of(IRCCaseMapping.ASCII, "*[^A-Z]*", "foobar", false),
                // rfc1459 special character folding
                Arguments.of(IRCCaseMapping.RFC1459, "[", "{", true),
                Arguments.of(IRCCaseMapping.RFC1459, "Nick[[]", "nick{", true),
                Arguments.of(IRCCaseMapping.RFC1459, "]", "}", true),
                Arguments.of(IRCCaseMapping.RFC1459, "\\", "|", true),
                Arguments.of(IRCCaseMapping.RFC1459, "~", "^", true),
                Arguments.of(IRCCaseMapping.RFC1459, "[[]", "{", true),
                Arguments.of(IRCCaseMapping.RFC1459, "[\\|]", "\\", true),
                Arguments.of(IRCCaseMapping.RFC1459, "[\\|]", "|", true),
                Arguments.of(IRCCaseMapping.RFC1459, "[~]", "^", true),
                Arguments.of(IRCCaseMapping.RFC1459, "*[*", "foo{bar", true),
                Arguments.of(IRCCaseMapping.RFC1459, "*{*", "foo[bar", true),
                Arguments.of(IRCCaseMapping.RFC1459, "*\\*", "foo|bar", true),
                Arguments.of(IRCCaseMapping.RFC1459, "*|*", "foo\\bar", true),
                Arguments.of(IRCCaseMapping.RFC1459, "*~*", "foo^bar", true),
                Arguments.of(IRCCaseMapping.RFC1459, "n?ck", "n{ck", true),
                Arguments.of(IRCCaseMapping.RFC1459, "n?ck", "n[ck", true),
                Arguments.of(IRCCaseMapping.RFC1459, "n?ck", "nick", true),
                Arguments.of(IRCCaseMapping.RFC1459, "n?ck", "niick", false),
                // rfc1459-strict exceptions
                Arguments.of(IRCCaseMapping.RFC1459_STRICT, "~", "^", false),
                Arguments.of(IRCCaseMapping.RFC1459_STRICT, "[~]", "^", false),
                Arguments.of(IRCCaseMapping.RFC1459_STRICT, "[", "{", true),
                Arguments.of(IRCCaseMapping.RFC1459_STRICT, "\\", "|", true),
                Arguments.of(IRCCaseMapping.RFC1459_STRICT, "*~*", "foo^bar", false),
                // and another brain destroyer for funsies
                Arguments.of(IRCCaseMapping.RFC1459, "*Ni?k*{foo,}~*", "xxni{KyyFOO^bar", true),
                Arguments.of(IRCCaseMapping.RFC1459_STRICT, "*Ni?k*{foo,}~*", "xxni{KyyFOO^bar", false));
    }
}
