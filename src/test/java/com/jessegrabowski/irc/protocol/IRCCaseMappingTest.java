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
package com.jessegrabowski.irc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IRCCaseMappingTest {

    @Test
    void asciiNicknameLowercasesAsciiOnly() {
        Assertions.assertEquals("foo", IRCCaseMapping.ASCII.normalizeNickname("Foo"));
        assertEquals("f", IRCCaseMapping.ASCII.normalizeNickname("F"));
        assertEquals("föÖ", IRCCaseMapping.ASCII.normalizeNickname("FöÖ"));
        assertNull(IRCCaseMapping.ASCII.normalizeNickname(null));
    }

    @Test
    void asciiChannelPreservesPrefixAndLowercasesRest() {
        assertEquals("#foo", IRCCaseMapping.ASCII.normalizeChannel("#Foo"));
        assertEquals("&foo", IRCCaseMapping.ASCII.normalizeChannel("&FOO"));
        assertEquals("+", IRCCaseMapping.ASCII.normalizeChannel("+"));
        assertEquals("", IRCCaseMapping.ASCII.normalizeChannel(""));
        assertNull(IRCCaseMapping.ASCII.normalizeChannel(null));
    }

    @Test
    void rfc1459NicknameDoesAsciiAndBracketMapping() {
        assertEquals("foo", IRCCaseMapping.RFC1459.normalizeNickname("Foo"));
        assertEquals("{foo}", IRCCaseMapping.RFC1459.normalizeNickname("[FOO]"));
        assertEquals("{foo|bar^", IRCCaseMapping.RFC1459.normalizeNickname("[FOO\\BAR~"));
        assertEquals("{foo|bar^", IRCCaseMapping.RFC1459.normalizeNickname("{foo|bar^"));
    }

    @Test
    void rfc1459ChannelPreservesPrefixAndMapsBody() {
        assertEquals("#foo", IRCCaseMapping.RFC1459.normalizeChannel("#Foo"));
        assertEquals("#{foo}", IRCCaseMapping.RFC1459.normalizeChannel("#[FOO]"));
        assertEquals("!{foo|bar^", IRCCaseMapping.RFC1459.normalizeChannel("![FOO\\BAR~"));
    }

    @Test
    void rfc1459StrictNicknameDoesNotMapTildeCaret() {
        assertEquals("foo", IRCCaseMapping.RFC1459_STRICT.normalizeNickname("Foo"));
        assertEquals("{foo}", IRCCaseMapping.RFC1459_STRICT.normalizeNickname("[FOO]"));
        assertEquals("{foo|bar~", IRCCaseMapping.RFC1459_STRICT.normalizeNickname("[FOO\\BAR~"));
        assertEquals("{foo|bar~", IRCCaseMapping.RFC1459_STRICT.normalizeNickname("{foo|bar~"));
    }

    @Test
    void rfc1459StrictChannelPreservesPrefixAndDoesNotMapTildeCaret() {
        assertEquals("#foo", IRCCaseMapping.RFC1459_STRICT.normalizeChannel("#Foo"));
        assertEquals("#{foo}", IRCCaseMapping.RFC1459_STRICT.normalizeChannel("#[FOO]"));
        assertEquals("!{foo|bar~", IRCCaseMapping.RFC1459_STRICT.normalizeChannel("![FOO\\BAR~"));
    }

    @Test
    void nonAsciiCharactersAreLeftUntouched() {
        String unicodeNick = "ÄΩ漢字";
        assertEquals("ÄΩ漢字", IRCCaseMapping.ASCII.normalizeNickname(unicodeNick));
        assertEquals("ÄΩ漢字", IRCCaseMapping.RFC1459.normalizeNickname(unicodeNick));
        assertEquals("ÄΩ漢字", IRCCaseMapping.RFC1459_STRICT.normalizeNickname(unicodeNick));
    }

    @Test
    void forNameResolvesKnownMappings() {
        assertEquals(Optional.of(IRCCaseMapping.ASCII), IRCCaseMapping.forName("ascii"));
        assertEquals(Optional.of(IRCCaseMapping.RFC1459), IRCCaseMapping.forName("rfc1459"));
        assertEquals(Optional.of(IRCCaseMapping.RFC1459_STRICT), IRCCaseMapping.forName("rfc1459-strict"));
    }

    @Test
    void forNameHandlesUnknownAndNull() {
        assertEquals(Optional.empty(), IRCCaseMapping.forName("nope"));
        assertEquals(Optional.empty(), IRCCaseMapping.forName(null));
    }

    @Test
    void channelLengthOneAndZeroAreReturnedAsIs() {
        assertEquals("", IRCCaseMapping.RFC1459.normalizeChannel(""));
        assertEquals("#", IRCCaseMapping.RFC1459.normalizeChannel("#"));
        assertEquals("&", IRCCaseMapping.ASCII.normalizeChannel("&"));
    }
}
