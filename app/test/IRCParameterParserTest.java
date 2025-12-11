import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class IRCParameterParserTest {

    private IRCClientState.Parameters newParameters() {
        return new IRCClientState.Parameters();
    }

    @Test
    void awayLenEnableDisableAndError() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("AWAYLEN", "120", p);
        assertEquals(120, p.getAwayLength());
        IRCParameterParser.parse("-AWAYLEN", "0", p);
        assertEquals(Integer.MAX_VALUE, p.getAwayLength());
        IRCParameterParser.parse("AWAYLEN", "x", p);
        assertEquals(Integer.MAX_VALUE, p.getAwayLength());
    }

    @Test
    void caseMappingEnableDisableAndUnknown() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("CASEMAPPING", "rfc1459", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
        IRCParameterParser.parse("CASEMAPPING", "unknown", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
        IRCParameterParser.parse("-CASEMAPPING", "ascii", p);
        assertEquals(IRCCaseMapping.RFC1459, p.getCaseMapping());
    }

    @Test
    void chanLimitEnableDisableWithAndWithoutValues() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("CHANLIMIT", "#:10,&", p);
        Map<Character,Integer> limits = p.getChannelLimits();
        assertEquals(10, limits.get('#'));
        assertEquals(Integer.MAX_VALUE, limits.get('&'));
        IRCParameterParser.parse("-CHANLIMIT", "", p);
        assertEquals(Map.of(), p.getChannelLimits());
    }

    @Test
    void chanModesEnableAllPartsAndDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("CHANMODES", "ab,c,def,gh", p);
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
        IRCParameterParser.parse("CHANMODES", "", p);
        assertEquals(Set.of(), p.getTypeAChannelModes());
        assertEquals(Set.of(), p.getTypeBChannelModes());
        assertEquals(Set.of(), p.getTypeCChannelModes());
        assertEquals(Set.of(), p.getTypeDChannelModes());
        IRCParameterParser.parse("-CHANMODES", "", p);
        assertEquals(Set.of(), p.getTypeAChannelModes());
        assertEquals(Set.of(), p.getTypeBChannelModes());
        assertEquals(Set.of(), p.getTypeCChannelModes());
        assertEquals(Set.of(), p.getTypeDChannelModes());
    }

    @Test
    void channelLenEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("CHANNELLEN", "50", p);
        assertEquals(50, p.getChannelLength());
        IRCParameterParser.parse("-CHANNELLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getChannelLength());
    }

    @Test
    void chanTypesEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("CHANTYPES", "#&+!", p);
        assertEquals(Set.of('#', '&', '+', '!'), p.getChannelTypes());
        IRCParameterParser.parse("-CHANTYPES", "", p);
        assertEquals(Set.of('#', '&'), p.getChannelTypes());
    }

    @Test
    void exceptsEnableDisableWithDefaultAndExplicit() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("EXCEPTS", "", p);
        assertEquals(Character.valueOf('e'), p.getExcepts());
        IRCParameterParser.parse("EXCEPTS", "x", p);
        assertEquals(Character.valueOf('x'), p.getExcepts());
        IRCParameterParser.parse("-EXCEPTS", "", p);
        assertNull(p.getExcepts());
    }

    @Test
    void extbanEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("EXTBAN", "~,cqnr", p);
        assertEquals(Character.valueOf('~'), p.getExtendedBanPrefix());
        assertTrue(p.getExtendedBanModes().contains('c'));
        assertTrue(p.getExtendedBanModes().contains('q'));
        assertTrue(p.getExtendedBanModes().contains('n'));
        assertTrue(p.getExtendedBanModes().contains('r'));
        IRCParameterParser.parse("-EXTBAN", "", p);
        assertEquals(Set.of(), p.getExtendedBanModes());
        assertNull(p.getExtendedBanPrefix());
    }

    @Test
    void extbanEmptyPrefix() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("EXTBAN", ",cqnr", p);
        assertNull(p.getExtendedBanPrefix());
        assertTrue(p.getExtendedBanModes().contains('c'));
        assertTrue(p.getExtendedBanModes().contains('q'));
        assertTrue(p.getExtendedBanModes().contains('n'));
        assertTrue(p.getExtendedBanModes().contains('r'));
    }

    @Test
    void hostLenEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("HOSTLEN", "64", p);
        assertEquals(64, p.getHostLength());
        IRCParameterParser.parse("-HOSTLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getHostLength());
    }

    @Test
    void invexEnableDisableWithDefaultAndExplicit() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("INVEX", "", p);
        assertEquals(Character.valueOf('I'), p.getInviteExceptions());
        IRCParameterParser.parse("INVEX", "x", p);
        assertEquals(Character.valueOf('x'), p.getInviteExceptions());
        IRCParameterParser.parse("-INVEX", "", p);
        assertNull(p.getInviteExceptions());
    }

    @Test
    void kickLenEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("KICKLEN", "120", p);
        assertEquals(120, p.getKickLength());
        IRCParameterParser.parse("-KICKLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getKickLength());
    }

    @Test
    void maxListEnableDisableWithAndWithoutValues() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("MAXLIST", "beI:60,q", p);
        Map<Character,Integer> max = p.getMaxList();
        assertEquals(60, max.get('b'));
        assertEquals(60, max.get('e'));
        assertEquals(60, max.get('I'));
        assertEquals(Integer.MAX_VALUE, max.get('q'));
        IRCParameterParser.parse("-MAXLIST", "", p);
        assertEquals(Map.of(), p.getMaxList());
    }

    @Test
    void maxTargetsEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("MAXTARGETS", "4", p);
        assertEquals(4, p.getMaxTargets());
        IRCParameterParser.parse("-MAXTARGETS", "", p);
        assertEquals(Integer.MAX_VALUE, p.getMaxTargets());
    }

    @Test
    void modesEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("MODES", "3", p);
        assertEquals(3, p.getModes());
        IRCParameterParser.parse("-MODES", "", p);
        assertEquals(Integer.MAX_VALUE, p.getModes());
    }

    @Test
    void networkEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("NETWORK", "TestNet", p);
        assertEquals("TestNet", p.getNetwork());
        IRCParameterParser.parse("-NETWORK", "", p);
        assertEquals("", p.getNetwork());
    }

    @Test
    void nickLenEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("NICKLEN", "16", p);
        assertEquals(16, p.getNickLength());
        IRCParameterParser.parse("-NICKLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getNickLength());
    }

    @Test
    void prefixDisableInvalidValidAndException() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("-PREFIX", "", p);
        assertEquals(Map.of(), p.getPrefixes());
        IRCParameterParser.parse("PREFIX", "invalid", p);
        assertEquals(Map.of(), p.getPrefixes());
        IRCParameterParser.parse("PREFIX", "(ov)!@", p);
        assertEquals(Map.of('o', '!', 'v', '@'), p.getPrefixes());
        IRCParameterParser.parse("PREFIX", "(ov)!@#", p);
        assertEquals(Map.of('o', '!', 'v', '@'), p.getPrefixes());
    }

    @Test
    void safeListEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("SAFELIST", "", p);
        assertTrue(p.isSafeList());
        IRCParameterParser.parse("-SAFELIST", "", p);
        assertFalse(p.isSafeList());
    }

    @Test
    void silenceEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("SILENCE", "5", p);
        assertEquals(5, p.getSilence());
        IRCParameterParser.parse("-SILENCE", "", p);
        assertEquals(Integer.MAX_VALUE, p.getSilence());
    }

    @Test
    void statusMsgEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("STATUSMSG", "@%+", p);
        assertEquals(Set.of('@', '%', '+'), p.getStatusMessage());
        IRCParameterParser.parse("-STATUSMSG", "", p);
        assertEquals(Set.of(), p.getStatusMessage());
    }

    @Test
    void targMaxEnableDisableEmptyAndWithValues() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("-TARGMAX", "", p);
        assertEquals(Map.of(), p.getTargetMax());
        IRCParameterParser.parse("TARGMAX", "", p);
        assertEquals(Map.of(), p.getTargetMax());
        IRCParameterParser.parse("TARGMAX", "PRIVMSG:4,NOTICE:3,JOIN", p);
        Map<String,Integer> max = p.getTargetMax();
        assertEquals(4, max.get("PRIVMSG"));
        assertEquals(3, max.get("NOTICE"));
        assertEquals(Integer.MAX_VALUE, max.get("JOIN"));
    }

    @Test
    void topicLenEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("TOPICLEN", "200", p);
        assertEquals(200, p.getTopicLength());
        IRCParameterParser.parse("-TOPICLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getTopicLength());
    }

    @Test
    void userLenEnableDisable() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("USERLEN", "10", p);
        assertEquals(10, p.getUserLength());
        IRCParameterParser.parse("-USERLEN", "", p);
        assertEquals(Integer.MAX_VALUE, p.getUserLength());
    }

    @Test
    void unknownParameterIgnored() {
        IRCClientState.Parameters p = newParameters();
        IRCParameterParser.parse("UNKNOWNPARAM", "value", p);
        assertNotNull(p);
    }
}
