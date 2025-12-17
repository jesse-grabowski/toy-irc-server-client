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

import com.jessegrabowski.irc.client.IRCClientState;
import com.jessegrabowski.irc.protocol.IRCCaseMapping;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IRCServerParametersUnmarshallerTest {

    private IRCServerParameters newParameters() {
        return new IRCClientState().getParameters();
    }

    @Test
    void awayLenEnableDisableAndError() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("AWAYLEN", "120", p);
        assertEquals(120, p.getAwayLength());
        IRCServerParametersUnmarshaller.unmarshal("-AWAYLEN", "0", p);
        assertEquals(Integer.MAX_VALUE, p.getAwayLength());
        IRCServerParametersUnmarshaller.unmarshal("AWAYLEN", "x", p);
        assertEquals(Integer.MAX_VALUE, p.getAwayLength());
    }

    @Test
    void caseMappingEnableDisableAndUnknown() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("CASEMAPPING", "rfc1459", p);
        Assertions.assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
        IRCServerParametersUnmarshaller.unmarshal("CASEMAPPING", "unknown", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
        IRCServerParametersUnmarshaller.unmarshal("-CASEMAPPING", "ascii", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
    }

    @Test
    void chanLimitEnableDisableWithAndWithoutValues() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("CHANLIMIT", "#:10,&", p);
        Map<Character, Integer> limits = p.getChannelLimits();
        assertEquals(10, limits.get('#'));
        assertEquals(Integer.MAX_VALUE, limits.get('&'));
        IRCServerParametersUnmarshaller.unmarshal("-CHANLIMIT", "", p);
        assertEquals(Map.of(), p.getChannelLimits());
    }

    @Test
    void chanModesEnableAllPartsAndDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("CHANMODES", "ab,c,def,gh", p);
        Set<Character> a = p.getTypeAChannelModes();
        Set<Character> b = p.getTypeBChannelModes();
        Set<Character> c = p.getTypeCChannelModes();
        Set<Character> d = p.getTypeDChannelModes();
        assertTrue(a.contains('a'));
        assertTrue(a.contains('b'));
        assertTrue(b.contains('c'));
        assertTrue(c.contains('d'));
        assertTrue(c.contains('e'));
        assertTrue(c.contains('f'));
        assertTrue(d.contains('g'));
        assertTrue(d.contains('h'));
        IRCServerParametersUnmarshaller.unmarshal("CHANMODES", "", p);
        assertEquals(Set.of(), p.getTypeAChannelModes());
        assertEquals(Set.of(), p.getTypeBChannelModes());
        assertEquals(Set.of(), p.getTypeCChannelModes());
        assertEquals(Set.of(), p.getTypeDChannelModes());
        IRCServerParametersUnmarshaller.unmarshal("-CHANMODES", "", p);
        assertEquals(Set.of(), p.getTypeAChannelModes());
        assertEquals(Set.of(), p.getTypeBChannelModes());
        assertEquals(Set.of(), p.getTypeCChannelModes());
        assertEquals(Set.of(), p.getTypeDChannelModes());
    }

    @Test
    void channelLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("CHANNELLEN", "50", p);
        assertEquals(50, p.getChannelLength());
        IRCServerParametersUnmarshaller.unmarshal("-CHANNELLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getChannelLength());
    }

    @Test
    void chanTypesEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("CHANTYPES", "#&+!", p);
        assertEquals(Set.of('#', '&', '+', '!'), p.getChannelTypes());
        IRCServerParametersUnmarshaller.unmarshal("-CHANTYPES", "", p);
        assertEquals(Set.of('#', '&'), p.getChannelTypes());
    }

    @Test
    void exceptsEnableDisableWithDefaultAndExplicit() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("EXCEPTS", "", p);
        assertEquals(Character.valueOf('e'), p.getExcepts());
        IRCServerParametersUnmarshaller.unmarshal("EXCEPTS", "x", p);
        assertEquals(Character.valueOf('x'), p.getExcepts());
        IRCServerParametersUnmarshaller.unmarshal("-EXCEPTS", "", p);
        assertNull(p.getExcepts());
    }

    @Test
    void extbanEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("EXTBAN", "~,cqnr", p);
        assertEquals(Character.valueOf('~'), p.getExtendedBanPrefix());
        assertTrue(p.getExtendedBanModes().contains('c'));
        assertTrue(p.getExtendedBanModes().contains('q'));
        assertTrue(p.getExtendedBanModes().contains('n'));
        assertTrue(p.getExtendedBanModes().contains('r'));
        IRCServerParametersUnmarshaller.unmarshal("-EXTBAN", "", p);
        assertEquals(Set.of(), p.getExtendedBanModes());
        assertNull(p.getExtendedBanPrefix());
    }

    @Test
    void extbanEmptyPrefix() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("EXTBAN", ",cqnr", p);
        assertNull(p.getExtendedBanPrefix());
        assertTrue(p.getExtendedBanModes().contains('c'));
        assertTrue(p.getExtendedBanModes().contains('q'));
        assertTrue(p.getExtendedBanModes().contains('n'));
        assertTrue(p.getExtendedBanModes().contains('r'));
    }

    @Test
    void hostLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("HOSTLEN", "64", p);
        assertEquals(64, p.getHostLength());
        IRCServerParametersUnmarshaller.unmarshal("-HOSTLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getHostLength());
    }

    @Test
    void invexEnableDisableWithDefaultAndExplicit() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("INVEX", "", p);
        assertEquals(Character.valueOf('I'), p.getInviteExceptions());
        IRCServerParametersUnmarshaller.unmarshal("INVEX", "x", p);
        assertEquals(Character.valueOf('x'), p.getInviteExceptions());
        IRCServerParametersUnmarshaller.unmarshal("-INVEX", "", p);
        assertNull(p.getInviteExceptions());
    }

    @Test
    void kickLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("KICKLEN", "120", p);
        assertEquals(120, p.getKickLength());
        IRCServerParametersUnmarshaller.unmarshal("-KICKLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getKickLength());
    }

    @Test
    void maxListEnableDisableWithAndWithoutValues() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("MAXLIST", "beI:60,q", p);
        Map<Character, Integer> max = p.getMaxList();
        assertEquals(60, max.get('b'));
        assertEquals(60, max.get('e'));
        assertEquals(60, max.get('I'));
        assertEquals(Integer.MAX_VALUE, max.get('q'));
        IRCServerParametersUnmarshaller.unmarshal("-MAXLIST", "", p);
        assertEquals(Map.of(), p.getMaxList());
    }

    @Test
    void maxTargetsEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("MAXTARGETS", "4", p);
        assertEquals(4, p.getMaxTargets());
        IRCServerParametersUnmarshaller.unmarshal("-MAXTARGETS", "", p);
        assertEquals(Integer.MAX_VALUE, p.getMaxTargets());
    }

    @Test
    void modesEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("MODES", "3", p);
        assertEquals(3, p.getModes());
        IRCServerParametersUnmarshaller.unmarshal("-MODES", "", p);
        assertEquals(Integer.MAX_VALUE, p.getModes());
    }

    @Test
    void networkEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("NETWORK", "TestNet", p);
        assertEquals("TestNet", p.getNetwork());
        IRCServerParametersUnmarshaller.unmarshal("-NETWORK", "", p);
        assertEquals("", p.getNetwork());
    }

    @Test
    void nickLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("NICKLEN", "16", p);
        assertEquals(16, p.getNickLength());
        IRCServerParametersUnmarshaller.unmarshal("-NICKLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getNickLength());
    }

    @Test
    void prefixDisableInvalidValidAndException() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("-PREFIX", "", p);
        assertEquals(Map.of(), p.getPrefixes());
        IRCServerParametersUnmarshaller.unmarshal("PREFIX", "invalid", p);
        assertEquals(Map.of(), p.getPrefixes());
        IRCServerParametersUnmarshaller.unmarshal("PREFIX", "(ov)!@", p);
        assertEquals(Map.of('o', '!', 'v', '@'), p.getPrefixes());
        IRCServerParametersUnmarshaller.unmarshal("PREFIX", "(ov)!@#", p);
        assertEquals(Map.of('o', '!', 'v', '@'), p.getPrefixes());
    }

    @Test
    void safeListEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("SAFELIST", "", p);
        assertTrue(p.isSafeList());
        IRCServerParametersUnmarshaller.unmarshal("-SAFELIST", "", p);
        assertFalse(p.isSafeList());
    }

    @Test
    void silenceEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("SILENCE", "5", p);
        assertEquals(5, p.getSilence());
        IRCServerParametersUnmarshaller.unmarshal("-SILENCE", "", p);
        assertEquals(Integer.MAX_VALUE, p.getSilence());
    }

    @Test
    void statusMsgEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("STATUSMSG", "@%+", p);
        assertEquals(Set.of('@', '%', '+'), p.getStatusMessage());
        IRCServerParametersUnmarshaller.unmarshal("-STATUSMSG", "", p);
        assertEquals(Set.of(), p.getStatusMessage());
    }

    @Test
    void targMaxEnableDisableEmptyAndWithValues() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("-TARGMAX", "", p);
        assertEquals(Map.of(), p.getTargetMax());
        IRCServerParametersUnmarshaller.unmarshal("TARGMAX", "", p);
        assertEquals(Map.of(), p.getTargetMax());
        IRCServerParametersUnmarshaller.unmarshal("TARGMAX", "PRIVMSG:4,NOTICE:3,JOIN", p);
        Map<String, Integer> max = p.getTargetMax();
        assertEquals(4, max.get("PRIVMSG"));
        assertEquals(3, max.get("NOTICE"));
        assertEquals(Integer.MAX_VALUE, max.get("JOIN"));
    }

    @Test
    void topicLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("TOPICLEN", "200", p);
        assertEquals(200, p.getTopicLength());
        IRCServerParametersUnmarshaller.unmarshal("-TOPICLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getTopicLength());
    }

    @Test
    void userLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("USERLEN", "10", p);
        assertEquals(10, p.getUserLength());
        IRCServerParametersUnmarshaller.unmarshal("-USERLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getUserLength());
    }

    @Test
    void unknownParameterIgnored() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.unmarshal("UNKNOWNPARAM", "value", p);
        assertNotNull(p);
    }
}
