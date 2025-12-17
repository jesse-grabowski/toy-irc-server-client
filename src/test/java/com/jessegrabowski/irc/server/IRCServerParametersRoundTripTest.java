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
package com.jessegrabowski.irc.server;

import static org.junit.jupiter.api.Assertions.*;

import com.jessegrabowski.irc.protocol.IRCCaseMapping;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class IRCServerParametersRoundTripTest {

    private final IRCServerParametersMarshaller marshaller = new IRCServerParametersMarshaller();

    @Test
    void roundTrip_typicalValues() {
        IRCServerParameters original = new IRCServerParameters();
        original.setAwayLength(160);
        original.setCaseMapping(IRCCaseMapping.RFC1459);
        original.setChannelLimits(new HashMap<>(Map.of('#', 50, '&', 20)));
        original.setTypeAChannelModes(charSet("beI"));
        original.setTypeBChannelModes(charSet("k"));
        original.setTypeCChannelModes(charSet("l"));
        original.setTypeDChannelModes(charSet("imnpst"));
        original.setChannelLength(200);
        original.setChannelTypes(charSet("#&"));
        original.setExcepts('e');
        original.setInviteExceptions('I');
        original.setExtendedBanPrefix('$');
        original.setExtendedBanModes(charSet("acjr"));
        original.setHostLength(64);
        original.setKickLength(255);
        original.setMaxList(new HashMap<>(Map.of('b', 60, 'e', 60, 'I', 60)));
        original.setMaxTargets(4);
        original.setModes(6);
        original.setNetwork("ExampleNet");
        original.setNickLength(30);
        original.setPrefixes(prefixMap("ov", "@+"));
        original.setSafeList(true);
        original.setSilence(32);
        original.setStatusMessage(charSet("@+"));
        original.setTargetMax(new HashMap<>(Map.of("PRIVMSG", 4, "NOTICE", 2)));
        original.setTopicLength(390);
        original.setUserLength(12);

        assertRoundTrip(original);
    }

    @Test
    void roundTrip_sharedChanlimitGroupsAndMultipleTargmax() {
        IRCServerParameters original = new IRCServerParameters();
        original.setAwayLength(300);
        original.setCaseMapping(IRCCaseMapping.forCasemapping("ascii").orElse(IRCCaseMapping.RFC1459));
        original.setChannelLimits(new HashMap<>(Map.of('#', 25, '&', 25, '!', 10)));
        original.setTypeAChannelModes(charSet("bI"));
        original.setTypeBChannelModes(charSet("k"));
        original.setTypeCChannelModes(charSet("l"));
        original.setTypeDChannelModes(charSet("imnpstz"));
        original.setChannelLength(120);
        original.setChannelTypes(charSet("#&!"));
        original.setExcepts('e');
        original.setInviteExceptions('I');
        original.setExtendedBanPrefix('~');
        original.setExtendedBanModes(charSet("q"));
        original.setHostLength(80);
        original.setKickLength(180);
        original.setMaxList(new HashMap<>(Map.of('b', 100, 'I', 50)));
        original.setMaxTargets(8);
        original.setModes(4);
        original.setNetwork("SharedLimitsNet");
        original.setNickLength(15);
        original.setPrefixes(prefixMap("qaohv", "~&@%+"));
        original.setSafeList(true);
        original.setSilence(16);
        original.setStatusMessage(charSet("@+%"));
        original.setTargetMax(new HashMap<>(Map.of("PRIVMSG", 8, "NOTICE", 4, "TAGMSG", 2)));
        original.setTopicLength(250);
        original.setUserLength(10);

        assertRoundTrip(original);
    }

    @Test
    void roundTrip_emptyChannelModesAndLargeNumericLimits() {
        IRCServerParameters original = new IRCServerParameters();
        original.setAwayLength(1);
        original.setCaseMapping(IRCCaseMapping.RFC1459);
        original.setChannelLimits(new HashMap<>(Map.of('#', 9999)));
        original.setTypeAChannelModes(Set.of());
        original.setTypeBChannelModes(Set.of());
        original.setTypeCChannelModes(Set.of());
        original.setTypeDChannelModes(Set.of());
        original.setChannelLength(Integer.MAX_VALUE);
        original.setChannelTypes(charSet("#"));
        original.setExcepts('e');
        original.setInviteExceptions('I');
        original.setExtendedBanPrefix('$');
        original.setExtendedBanModes(Set.of());
        original.setHostLength(Integer.MAX_VALUE);
        original.setKickLength(Integer.MAX_VALUE);
        original.setMaxList(new HashMap<>(Map.of('b', 1)));
        original.setMaxTargets(Integer.MAX_VALUE);
        original.setModes(Integer.MAX_VALUE);
        original.setNetwork("UnlimitedNet");
        original.setNickLength(Integer.MAX_VALUE);
        original.setPrefixes(prefixMap("o", "@"));
        original.setSafeList(true);
        original.setSilence(Integer.MAX_VALUE);
        original.setStatusMessage(Set.of());
        original.setTargetMax(new HashMap<>(Map.of("PRIVMSG", Integer.MAX_VALUE)));
        original.setTopicLength(Integer.MAX_VALUE);
        original.setUserLength(Integer.MAX_VALUE);

        assertRoundTrip(original);
    }

    @Test
    void roundTrip_nonStandardChannelTypesAndStatusMsg() {
        IRCServerParameters original = new IRCServerParameters();
        original.setAwayLength(120);
        original.setCaseMapping(IRCCaseMapping.RFC1459);
        original.setChannelLimits(new HashMap<>(Map.of('#', 10, '&', 10, '+', 5)));
        original.setTypeAChannelModes(charSet("b"));
        original.setTypeBChannelModes(charSet("k"));
        original.setTypeCChannelModes(charSet("l"));
        original.setTypeDChannelModes(charSet("imnt"));
        original.setChannelLength(80);
        original.setChannelTypes(charSet("#&+"));
        original.setExcepts('e');
        original.setInviteExceptions('I');
        original.setExtendedBanPrefix('$');
        original.setExtendedBanModes(charSet("a"));
        original.setHostLength(63);
        original.setKickLength(200);
        original.setMaxList(new HashMap<>(Map.of('b', 25, 'e', 25)));
        original.setMaxTargets(1);
        original.setModes(3);
        original.setNetwork("ChanTypesPlusNet");
        original.setNickLength(9);
        original.setPrefixes(prefixMap("ohv", "@%+"));
        original.setSafeList(true);
        original.setSilence(5);
        original.setStatusMessage(charSet("@"));
        original.setTargetMax(new HashMap<>(Map.of("NOTICE", 1, "PRIVMSG", 1)));
        original.setTopicLength(120);
        original.setUserLength(8);

        assertRoundTrip(original);
    }

    @Test
    void roundTrip_prefixOrderingAndMaxValuesInMaps() {
        IRCServerParameters original = new IRCServerParameters();
        original.setAwayLength(512);
        original.setCaseMapping(IRCCaseMapping.forCasemapping("strict-rfc1459").orElse(IRCCaseMapping.RFC1459));

        Map<Character, Integer> chanlimits = new HashMap<>();
        chanlimits.put('#', 30);
        chanlimits.put('&', Integer.MAX_VALUE);
        chanlimits.put('!', 30);
        original.setChannelLimits(chanlimits);

        original.setTypeAChannelModes(charSet("beI"));
        original.setTypeBChannelModes(charSet("k"));
        original.setTypeCChannelModes(charSet("l"));
        original.setTypeDChannelModes(charSet("imnpst"));
        original.setChannelLength(500);
        original.setChannelTypes(charSet("#&!"));
        original.setExcepts('e');
        original.setInviteExceptions('I');
        original.setExtendedBanPrefix('~');
        original.setExtendedBanModes(charSet("Zx"));
        original.setHostLength(100);
        original.setKickLength(400);

        Map<Character, Integer> maxlist = new HashMap<>();
        maxlist.put('b', 60);
        maxlist.put('e', Integer.MAX_VALUE);
        maxlist.put('I', 60);
        original.setMaxList(maxlist);

        original.setMaxTargets(20);
        original.setModes(10);
        original.setNetwork("PrefixOrderNet");
        original.setNickLength(25);
        original.setPrefixes(prefixMap("yov", "!@+"));
        original.setSafeList(true);
        original.setSilence(64);
        original.setStatusMessage(charSet("!@+"));

        Map<String, Integer> targmax = new HashMap<>();
        targmax.put("PRIVMSG", 3);
        targmax.put("NOTICE", Integer.MAX_VALUE);
        targmax.put("WHO", 1);
        original.setTargetMax(targmax);

        original.setTopicLength(600);
        original.setUserLength(16);

        assertRoundTrip(original);
    }

    private void assertRoundTrip(IRCServerParameters original) {
        SequencedMap<String, String> wire = marshaller.marshal(original);

        IRCServerParameters parsed = new IRCServerParameters();
        for (Map.Entry<String, String> e : wire.entrySet()) {
            IRCServerParametersUnmarshaller.unmarshal(e.getKey(), e.getValue(), parsed);
        }

        assertParametersEqual(original, parsed);
    }

    private static void assertParametersEqual(IRCServerParameters a, IRCServerParameters b) {
        assertEquals(a.getAwayLength(), b.getAwayLength(), "awayLength");
        assertEquals(a.getCaseMapping(), b.getCaseMapping(), "caseMapping");

        assertEquals(a.getChannelLimits(), b.getChannelLimits(), "channelLimits");
        assertEquals(a.getTypeAChannelModes(), b.getTypeAChannelModes(), "typeAChannelModes");
        assertEquals(a.getTypeBChannelModes(), b.getTypeBChannelModes(), "typeBChannelModes");
        assertEquals(a.getTypeCChannelModes(), b.getTypeCChannelModes(), "typeCChannelModes");
        assertEquals(a.getTypeDChannelModes(), b.getTypeDChannelModes(), "typeDChannelModes");

        assertEquals(a.getChannelLength(), b.getChannelLength(), "channelLength");
        assertEquals(a.getChannelTypes(), b.getChannelTypes(), "channelTypes");

        assertEquals(a.getExcepts(), b.getExcepts(), "excepts");
        assertEquals(a.getExtendedBanPrefix(), b.getExtendedBanPrefix(), "extendedBanPrefix");
        assertEquals(a.getExtendedBanModes(), b.getExtendedBanModes(), "extendedBanModes");

        assertEquals(a.getHostLength(), b.getHostLength(), "hostLength");
        assertEquals(a.getInviteExceptions(), b.getInviteExceptions(), "inviteExceptions");
        assertEquals(a.getKickLength(), b.getKickLength(), "kickLength");

        assertEquals(a.getMaxList(), b.getMaxList(), "maxList");
        assertEquals(a.getMaxTargets(), b.getMaxTargets(), "maxTargets");
        assertEquals(a.getModes(), b.getModes(), "modes");

        assertEquals(a.getNetwork(), b.getNetwork(), "network");
        assertEquals(a.getNickLength(), b.getNickLength(), "nickLength");

        assertEquals(a.getPrefixes(), b.getPrefixes(), "prefixes");
        assertEquals(a.isSafeList(), b.isSafeList(), "safeList");

        assertEquals(a.getSilence(), b.getSilence(), "silence");
        assertEquals(a.getStatusMessage(), b.getStatusMessage(), "statusMessage");

        assertEquals(a.getTargetMax(), b.getTargetMax(), "targetMax");

        assertEquals(a.getTopicLength(), b.getTopicLength(), "topicLength");
        assertEquals(a.getUserLength(), b.getUserLength(), "userLength");
    }

    private static Set<Character> charSet(String s) {
        Set<Character> out = new HashSet<>();
        for (char c : s.toCharArray()) out.add(c);
        return out;
    }

    private static SequencedMap<Character, Character> prefixMap(String modes, String prefixes) {
        assertEquals(modes.length(), prefixes.length(), "prefixMap inputs must have same length");
        SequencedMap<Character, Character> m = new LinkedHashMap<>();
        for (int i = 0; i < modes.length(); i++) {
            m.put(modes.charAt(i), prefixes.charAt(i));
        }
        return m;
    }
}
