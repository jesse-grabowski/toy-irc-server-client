package com.jessegrabowski.irc;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class IRCCaseMappingTest {

    @Test
    void asciiNicknameLowercasesAsciiOnly() {
        assertEquals("foo", IRCCaseMapping.ASCII.normalizeNickname("Foo"));
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
