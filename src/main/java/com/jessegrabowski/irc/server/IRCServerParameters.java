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

import com.jessegrabowski.irc.protocol.IRCCaseMapping;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;

public final class IRCServerParameters {

    private int awayLength = Integer.MAX_VALUE;
    private IRCCaseMapping caseMapping = null; // start out undefined
    private Map<Character, Integer> channelLimits = Map.of();
    private Set<Character> typeAChannelModes = new HashSet<>();
    private Set<Character> typeBChannelModes = new HashSet<>();
    private Set<Character> typeCChannelModes = new HashSet<>();
    private Set<Character> typeDChannelModes = new HashSet<>();
    private int channelLength = Integer.MAX_VALUE;
    private Set<Character> channelTypes = Set.of('#', '&');
    private Character excepts;
    private Character extendedBanPrefix;
    private Set<Character> extendedBanModes = Set.of();
    private int hostLength = Integer.MAX_VALUE;
    private Character inviteExceptions;
    private int kickLength = Integer.MAX_VALUE;
    private Map<Character, Integer> maxList = Map.of();
    private int maxTargets = Integer.MAX_VALUE;
    private int modes = Integer.MAX_VALUE;
    private String network;
    private int nickLength = Integer.MAX_VALUE;
    private SequencedMap<Character, Character> prefixes = new LinkedHashMap<>();
    private boolean safeList;
    private Integer silence = null;
    private Set<Character> statusMessage = Set.of();
    private Map<String, Integer> targetMax = Map.of();
    private int topicLength = Integer.MAX_VALUE;
    private int userLength = Integer.MAX_VALUE;

    public IRCServerParameters() {}

    public int getAwayLength() {
        return awayLength;
    }

    public void setAwayLength(int awayLength) {
        this.awayLength = awayLength;
    }

    // casemapping is a bit tricky, we need to assume RFC1459 until the server
    // tells us otherwise, then refuse any new values even if the server changes
    // mid-session to avoid unstable casefolding errors
    public IRCCaseMapping getCaseMapping() {
        return Objects.requireNonNullElse(caseMapping, IRCCaseMapping.RFC1459);
    }

    public void setCaseMapping(IRCCaseMapping caseMapping) {
        if (this.caseMapping == null) {
            this.caseMapping = caseMapping;
        } else {
            throw new IllegalStateException("Case mapping can only be set once");
        }
    }

    public Map<Character, Integer> getChannelLimits() {
        return Collections.unmodifiableMap(channelLimits);
    }

    public void setChannelLimits(Map<Character, Integer> channelLimits) {
        this.channelLimits = new HashMap<>(channelLimits);
    }

    public Set<Character> getTypeAChannelModes() {
        return Collections.unmodifiableSet(typeAChannelModes);
    }

    public void setTypeAChannelModes(Set<Character> typeAChannelModes) {
        this.typeAChannelModes = new HashSet<>(typeAChannelModes);
    }

    public Set<Character> getTypeBChannelModes() {
        return Collections.unmodifiableSet(typeBChannelModes);
    }

    public void setTypeBChannelModes(Set<Character> typeBChannelModes) {
        this.typeBChannelModes = new HashSet<>(typeBChannelModes);
    }

    public Set<Character> getTypeCChannelModes() {
        return Collections.unmodifiableSet(typeCChannelModes);
    }

    public void setTypeCChannelModes(Set<Character> typeCChannelModes) {
        this.typeCChannelModes = new HashSet<>(typeCChannelModes);
    }

    public Set<Character> getTypeDChannelModes() {
        return Collections.unmodifiableSet(typeDChannelModes);
    }

    public void setTypeDChannelModes(Set<Character> typeDChannelModes) {
        this.typeDChannelModes = new HashSet<>(typeDChannelModes);
    }

    public int getChannelLength() {
        return channelLength;
    }

    public void setChannelLength(int channelLength) {
        this.channelLength = channelLength;
    }

    public Set<Character> getChannelTypes() {
        return Collections.unmodifiableSet(channelTypes);
    }

    public void setChannelTypes(Set<Character> channelTypes) {
        this.channelTypes = new HashSet<>(channelTypes);
    }

    public Character getExcepts() {
        return excepts;
    }

    public void setExcepts(Character excepts) {
        this.excepts = excepts;
    }

    public Character getExtendedBanPrefix() {
        return extendedBanPrefix;
    }

    public void setExtendedBanPrefix(Character extendedBanPrefix) {
        this.extendedBanPrefix = extendedBanPrefix;
    }

    public Set<Character> getExtendedBanModes() {
        return Collections.unmodifiableSet(extendedBanModes);
    }

    public void setExtendedBanModes(Set<Character> extendedBanModes) {
        this.extendedBanModes = new HashSet<>(extendedBanModes);
    }

    public int getHostLength() {
        return hostLength;
    }

    public void setHostLength(int hostLength) {
        this.hostLength = hostLength;
    }

    public Character getInviteExceptions() {
        return inviteExceptions;
    }

    public void setInviteExceptions(Character inviteExceptions) {
        this.inviteExceptions = inviteExceptions;
    }

    public int getKickLength() {
        return kickLength;
    }

    public void setKickLength(int kickLength) {
        this.kickLength = kickLength;
    }

    public Map<Character, Integer> getMaxList() {
        return Collections.unmodifiableMap(maxList);
    }

    public void setMaxList(Map<Character, Integer> maxList) {
        this.maxList = new HashMap<>(maxList);
    }

    public int getMaxTargets() {
        return maxTargets;
    }

    public void setMaxTargets(int maxTargets) {
        this.maxTargets = maxTargets;
    }

    public int getModes() {
        return modes;
    }

    public void setModes(int modes) {
        this.modes = modes;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public int getNickLength() {
        return nickLength;
    }

    public void setNickLength(int nickLength) {
        this.nickLength = nickLength;
    }

    public Map<Character, Character> getPrefixes() {
        return Collections.unmodifiableMap(prefixes);
    }

    public void setPrefixes(SequencedMap<Character, Character> prefixes) {
        this.prefixes = new LinkedHashMap<>(prefixes);
    }

    public boolean isSafeList() {
        return safeList;
    }

    public void setSafeList(boolean safeList) {
        this.safeList = safeList;
    }

    public Integer getSilence() {
        return silence;
    }

    public void setSilence(Integer silence) {
        this.silence = silence;
    }

    public Set<Character> getStatusMessage() {
        return Collections.unmodifiableSet(statusMessage);
    }

    public void setStatusMessage(Set<Character> statusMessage) {
        this.statusMessage = new HashSet<>(statusMessage);
    }

    public Map<String, Integer> getTargetMax() {
        return Collections.unmodifiableMap(targetMax);
    }

    public void setTargetMax(Map<String, Integer> targetMax) {
        this.targetMax = new HashMap<>(targetMax);
    }

    public int getTopicLength() {
        return topicLength;
    }

    public void setTopicLength(int topicLength) {
        this.topicLength = topicLength;
    }

    public int getUserLength() {
        return userLength;
    }

    public void setUserLength(int userLength) {
        this.userLength = userLength;
    }
}
