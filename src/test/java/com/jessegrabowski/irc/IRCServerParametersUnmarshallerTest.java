package com.jessegrabowski.irc;

import com.jessegrabowski.irc.client.IRCClientState;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class IRCServerParametersUnmarshallerTest {

    private IRCServerParameters newParameters() {
        return new IRCClientState().getParameters();
    }

    @Test
    void awayLenEnableDisableAndError() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("AWAYLEN", "120", p);
        assertEquals(120, p.getAwayLength());
        IRCServerParametersUnmarshaller.parse("-AWAYLEN", "0", p);
        assertEquals(Integer.MAX_VALUE, p.getAwayLength());
        IRCServerParametersUnmarshaller.parse("AWAYLEN", "x", p);
        assertEquals(Integer.MAX_VALUE, p.getAwayLength());
    }

    @Test
    void caseMappingEnableDisableAndUnknown() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("CASEMAPPING", "rfc1459", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
        IRCServerParametersUnmarshaller.parse("CASEMAPPING", "unknown", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
        IRCServerParametersUnmarshaller.parse("-CASEMAPPING", "ascii", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
    }

    @Test
    void chanLimitEnableDisableWithAndWithoutValues() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("CHANLIMIT", "#:10,&", p);
        Map<Character,Integer> limits = p.getChannelLimits();
        assertEquals(10, limits.get('#'));
        assertEquals(Integer.MAX_VALUE, limits.get('&'));
        IRCServerParametersUnmarshaller.parse("-CHANLIMIT", "", p);
        assertEquals(Map.of(), p.getChannelLimits());
    }

    @Test
    void chanModesEnableAllPartsAndDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("CHANMODES", "ab,c,def,gh", p);
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
        IRCServerParametersUnmarshaller.parse("CHANMODES", "", p);
        assertEquals(Set.of(), p.getTypeAChannelModes());
        assertEquals(Set.of(), p.getTypeBChannelModes());
        assertEquals(Set.of(), p.getTypeCChannelModes());
        assertEquals(Set.of(), p.getTypeDChannelModes());
        IRCServerParametersUnmarshaller.parse("-CHANMODES", "", p);
        assertEquals(Set.of(), p.getTypeAChannelModes());
        assertEquals(Set.of(), p.getTypeBChannelModes());
        assertEquals(Set.of(), p.getTypeCChannelModes());
        assertEquals(Set.of(), p.getTypeDChannelModes());
    }

    @Test
    void channelLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("CHANNELLEN", "50", p);
        assertEquals(50, p.getChannelLength());
        IRCServerParametersUnmarshaller.parse("-CHANNELLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getChannelLength());
    }

    @Test
    void chanTypesEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("CHANTYPES", "#&+!", p);
        assertEquals(Set.of('#', '&', '+', '!'), p.getChannelTypes());
        IRCServerParametersUnmarshaller.parse("-CHANTYPES", "", p);
        assertEquals(Set.of('#', '&'), p.getChannelTypes());
    }

    @Test
    void exceptsEnableDisableWithDefaultAndExplicit() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("EXCEPTS", "", p);
        assertEquals(Character.valueOf('e'), p.getExcepts());
        IRCServerParametersUnmarshaller.parse("EXCEPTS", "x", p);
        assertEquals(Character.valueOf('x'), p.getExcepts());
        IRCServerParametersUnmarshaller.parse("-EXCEPTS", "", p);
        assertNull(p.getExcepts());
    }

    @Test
    void extbanEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("EXTBAN", "~,cqnr", p);
        assertEquals(Character.valueOf('~'), p.getExtendedBanPrefix());
        assertTrue(p.getExtendedBanModes().contains('c'));
        assertTrue(p.getExtendedBanModes().contains('q'));
        assertTrue(p.getExtendedBanModes().contains('n'));
        assertTrue(p.getExtendedBanModes().contains('r'));
        IRCServerParametersUnmarshaller.parse("-EXTBAN", "", p);
        assertEquals(Set.of(), p.getExtendedBanModes());
        assertNull(p.getExtendedBanPrefix());
    }

    @Test
    void extbanEmptyPrefix() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("EXTBAN", ",cqnr", p);
        assertNull(p.getExtendedBanPrefix());
        assertTrue(p.getExtendedBanModes().contains('c'));
        assertTrue(p.getExtendedBanModes().contains('q'));
        assertTrue(p.getExtendedBanModes().contains('n'));
        assertTrue(p.getExtendedBanModes().contains('r'));
    }

    @Test
    void hostLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("HOSTLEN", "64", p);
        assertEquals(64, p.getHostLength());
        IRCServerParametersUnmarshaller.parse("-HOSTLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getHostLength());
    }

    @Test
    void invexEnableDisableWithDefaultAndExplicit() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("INVEX", "", p);
        assertEquals(Character.valueOf('I'), p.getInviteExceptions());
        IRCServerParametersUnmarshaller.parse("INVEX", "x", p);
        assertEquals(Character.valueOf('x'), p.getInviteExceptions());
        IRCServerParametersUnmarshaller.parse("-INVEX", "", p);
        assertNull(p.getInviteExceptions());
    }

    @Test
    void kickLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("KICKLEN", "120", p);
        assertEquals(120, p.getKickLength());
        IRCServerParametersUnmarshaller.parse("-KICKLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getKickLength());
    }

    @Test
    void maxListEnableDisableWithAndWithoutValues() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("MAXLIST", "beI:60,q", p);
        Map<Character,Integer> max = p.getMaxList();
        assertEquals(60, max.get('b'));
        assertEquals(60, max.get('e'));
        assertEquals(60, max.get('I'));
        assertEquals(Integer.MAX_VALUE, max.get('q'));
        IRCServerParametersUnmarshaller.parse("-MAXLIST", "", p);
        assertEquals(Map.of(), p.getMaxList());
    }

    @Test
    void maxTargetsEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("MAXTARGETS", "4", p);
        assertEquals(4, p.getMaxTargets());
        IRCServerParametersUnmarshaller.parse("-MAXTARGETS", "", p);
        assertEquals(Integer.MAX_VALUE, p.getMaxTargets());
    }

    @Test
    void modesEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("MODES", "3", p);
        assertEquals(3, p.getModes());
        IRCServerParametersUnmarshaller.parse("-MODES", "", p);
        assertEquals(Integer.MAX_VALUE, p.getModes());
    }

    @Test
    void networkEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("NETWORK", "TestNet", p);
        assertEquals("TestNet", p.getNetwork());
        IRCServerParametersUnmarshaller.parse("-NETWORK", "", p);
        assertEquals("", p.getNetwork());
    }

    @Test
    void nickLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("NICKLEN", "16", p);
        assertEquals(16, p.getNickLength());
        IRCServerParametersUnmarshaller.parse("-NICKLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getNickLength());
    }

    @Test
    void prefixDisableInvalidValidAndException() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("-PREFIX", "", p);
        assertEquals(Map.of(), p.getPrefixes());
        IRCServerParametersUnmarshaller.parse("PREFIX", "invalid", p);
        assertEquals(Map.of(), p.getPrefixes());
        IRCServerParametersUnmarshaller.parse("PREFIX", "(ov)!@", p);
        assertEquals(Map.of('o', '!', 'v', '@'), p.getPrefixes());
        IRCServerParametersUnmarshaller.parse("PREFIX", "(ov)!@#", p);
        assertEquals(Map.of('o', '!', 'v', '@'), p.getPrefixes());
    }

    @Test
    void safeListEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("SAFELIST", "", p);
        assertTrue(p.isSafeList());
        IRCServerParametersUnmarshaller.parse("-SAFELIST", "", p);
        assertFalse(p.isSafeList());
    }

    @Test
    void silenceEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("SILENCE", "5", p);
        assertEquals(5, p.getSilence());
        IRCServerParametersUnmarshaller.parse("-SILENCE", "", p);
        assertEquals(Integer.MAX_VALUE, p.getSilence());
    }

    @Test
    void statusMsgEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("STATUSMSG", "@%+", p);
        assertEquals(Set.of('@', '%', '+'), p.getStatusMessage());
        IRCServerParametersUnmarshaller.parse("-STATUSMSG", "", p);
        assertEquals(Set.of(), p.getStatusMessage());
    }

    @Test
    void targMaxEnableDisableEmptyAndWithValues() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("-TARGMAX", "", p);
        assertEquals(Map.of(), p.getTargetMax());
        IRCServerParametersUnmarshaller.parse("TARGMAX", "", p);
        assertEquals(Map.of(), p.getTargetMax());
        IRCServerParametersUnmarshaller.parse("TARGMAX", "PRIVMSG:4,NOTICE:3,JOIN", p);
        Map<String,Integer> max = p.getTargetMax();
        assertEquals(4, max.get("PRIVMSG"));
        assertEquals(3, max.get("NOTICE"));
        assertEquals(Integer.MAX_VALUE, max.get("JOIN"));
    }

    @Test
    void topicLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("TOPICLEN", "200", p);
        assertEquals(200, p.getTopicLength());
        IRCServerParametersUnmarshaller.parse("-TOPICLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getTopicLength());
    }

    @Test
    void userLenEnableDisable() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("USERLEN", "10", p);
        assertEquals(10, p.getUserLength());
        IRCServerParametersUnmarshaller.parse("-USERLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getUserLength());
    }

    @Test
    void unknownParameterIgnored() {
        IRCServerParameters p = newParameters();
        IRCServerParametersUnmarshaller.parse("UNKNOWNPARAM", "value", p);
        assertNotNull(p);
    }
}
