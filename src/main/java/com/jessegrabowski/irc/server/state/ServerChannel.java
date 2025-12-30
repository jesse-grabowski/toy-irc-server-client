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
package com.jessegrabowski.irc.server.state;

import com.jessegrabowski.irc.protocol.IRCChannelFlag;
import com.jessegrabowski.irc.protocol.IRCChannelList;
import com.jessegrabowski.irc.protocol.IRCChannelMembershipMode;
import com.jessegrabowski.irc.protocol.IRCChannelSetting;
import com.jessegrabowski.irc.protocol.IRCServerParameters;
import com.jessegrabowski.irc.protocol.IRCUserMode;
import com.jessegrabowski.irc.protocol.model.IRCMessage443;
import com.jessegrabowski.irc.protocol.model.IRCMessage482;
import com.jessegrabowski.irc.util.Glob;
import com.jessegrabowski.irc.util.Transaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ServerChannel {
    private final String name;
    private String topic;
    private ServerSetBy topicSetBy;
    private long topicSetAt;
    private final long creationTime = System.currentTimeMillis();
    private final Map<ServerUser, ServerChannelMembership> members = new HashMap<>();
    private final Map<IRCChannelList, List<Glob>> lists = new HashMap<>();
    private final Map<IRCChannelSetting, String> settings = new HashMap<>();
    private final Set<IRCChannelFlag> flags = new HashSet<>();
    private final Set<ServerUser> invited = new HashSet<>();

    ServerChannel(String name) {
        this.name = name;
    }

    Set<ServerUser> getMembers() {
        return Set.copyOf(members.keySet());
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    void join(ServerUser user, String key) {
        if (members.containsKey(user)) {
            return;
        }
        Transaction.putTransactionally(members, user, new ServerChannelMembership());
        if (topicSetBy != null && topicSetBy.isUser(user)) {
            ServerSetBy oldSetBy = topicSetBy;
            Transaction.addCompensation(() -> topicSetBy = oldSetBy);
            topicSetBy = new ServerSetBy.SetByUser(user);
        }
    }

    void part(ServerUser user) {
        Transaction.removeTransactionally(members, user);
        if (topicSetBy != null && topicSetBy.isUser(user)) {
            ServerSetBy oldSetBy = topicSetBy;
            Transaction.addCompensation(() -> topicSetBy = oldSetBy);
            topicSetBy = new ServerSetBy.SetByNickname(user.getNickname());
        }
    }

    public ServerChannelMembership getMembership(ServerUser user) {
        return members.get(user);
    }

    void addMemberMode(ServerUser caller, ServerUser target, IRCChannelMembershipMode memberMode)
            throws StateInvariantException {
        ServerChannelMembership callerMembership = members.get(caller);
        if (callerMembership == null || !callerMembership.canGrant(memberMode)) {
            throw new StateInvariantException(
                    "User %s cannot grant mode %s".formatted(caller.getNickname(), memberMode),
                    caller.getNickname(),
                    name,
                    "Insufficient permission to set mode '%s'".formatted(memberMode.getLetter()),
                    IRCMessage482::new);
        }

        ServerChannelMembership targetMembership = members.get(target);
        if (targetMembership == null) {
            throw new StateInvariantException(
                    "User %s not in channel %s".formatted(target.getNickname(), name),
                    caller.getNickname(),
                    target.getNickname(),
                    name,
                    IRCMessage443::new);
        }

        targetMembership.addMode(memberMode);
    }

    void addMemberModeAutomatic(ServerUser target, IRCChannelMembershipMode memberMode) throws StateInvariantException {
        ServerChannelMembership targetMembership = members.get(target);
        if (targetMembership == null) {
            throw new StateInvariantException(
                    "User %s not in channel %s".formatted(target.getNickname(), name),
                    target.getNickname(),
                    target.getNickname(),
                    name,
                    IRCMessage443::new);
        }

        targetMembership.addMode(memberMode);
    }

    void removeMemberMode(ServerUser caller, ServerUser target, IRCChannelMembershipMode memberMode)
            throws StateInvariantException {
        ServerChannelMembership callerMembership = members.get(caller);
        if (callerMembership == null || !callerMembership.canGrant(memberMode)) {
            throw new StateInvariantException(
                    "User %s cannot remove mode %s".formatted(caller.getNickname(), memberMode),
                    caller.getNickname(),
                    name,
                    "Insufficient permission to unset mode '%s'".formatted(memberMode.getLetter()),
                    IRCMessage482::new);
        }

        ServerChannelMembership targetMembership = members.get(target);
        if (targetMembership == null) {
            throw new StateInvariantException(
                    "User %s not in channel %s".formatted(target.getNickname(), name),
                    caller.getNickname(),
                    target.getNickname(),
                    name,
                    IRCMessage443::new);
        }

        targetMembership.removeMode(memberMode);
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    void setTopic(String topic) {
        String previous = this.topic;
        this.topic = topic;
        Transaction.addCompensation(() -> this.topic = previous);
    }

    public ServerSetBy getTopicSetBy() {
        return topicSetBy;
    }

    void setTopicSetBy(ServerSetBy topicSetBy) {
        ServerSetBy oldTopicSetBy = this.topicSetBy;
        this.topicSetBy = topicSetBy;
        Transaction.addCompensation(() -> this.topicSetBy = oldTopicSetBy);
    }

    public long getTopicSetAt() {
        return topicSetAt;
    }

    void setTopicSetAt(long topicSetAt) {
        long oldTopicSetAt = this.topicSetAt;
        this.topicSetAt = topicSetAt;
        Transaction.addCompensation(() -> this.topicSetAt = oldTopicSetAt);
    }

    public long getCreationTime() {
        return creationTime;
    }

    public List<Glob> getList(IRCChannelList mode) {
        return Collections.unmodifiableList(lists.getOrDefault(mode, List.of()));
    }

    void addToList(IRCChannelList mode, Glob entry) {
        List<Glob> list = Transaction.computeIfAbsentTransactionally(lists, mode, k -> new ArrayList<>());
        if (list.contains(entry)) {
            return;
        }
        Transaction.addTransactionally(list, entry);
    }

    void removeFromList(IRCChannelList mode, Glob entry) {
        List<Glob> list = lists.get(mode);
        if (list != null) {
            Transaction.removeTransactionally(list, entry);
            if (list.isEmpty()) {
                Transaction.removeTransactionally(lists, mode);
            }
        }
    }

    public String getSetting(IRCChannelSetting mode) {
        return settings.get(mode);
    }

    void setSetting(IRCChannelSetting mode, String setting) {
        Transaction.putTransactionally(settings, mode, setting);
    }

    void removeSetting(IRCChannelSetting mode) {
        Transaction.removeTransactionally(settings, mode);
    }

    public boolean checkFlag(IRCChannelFlag mode) {
        return flags.contains(mode);
    }

    void setFlag(IRCChannelFlag mode) {
        Transaction.addTransactionally(flags, mode);
    }

    void clearFlag(IRCChannelFlag mode) {
        Transaction.removeTransactionally(flags, mode);
    }

    public boolean checkModerationAllows(ServerUser user) {
        if (!flags.contains(IRCChannelFlag.MODERATED)) {
            return true;
        }

        ServerChannelMembership membership = members.get(user);
        if (membership == null) {
            return false;
        }

        return membership.hasAtLeast(IRCChannelMembershipMode.VOICE);
    }

    public boolean checkExternalMessagingAllows(ServerUser user) {
        if (!flags.contains(IRCChannelFlag.NO_EXTERNAL_MESSAGES)) {
            return true;
        }

        return members.get(user) != null;
    }

    public boolean checkVisible(ServerUser user) {
        if (!flags.contains(IRCChannelFlag.SECRET)) {
            return true;
        }

        return members.get(user) != null || user.getModes().contains(IRCUserMode.OPERATOR);
    }

    void setInvited(ServerUser user) {
        Transaction.addTransactionally(invited, user);
    }

    void clearInvited(ServerUser user) {
        Transaction.removeTransactionally(invited, user);
    }

    boolean isFull() {
        try {
            String clientLimit = settings.get(IRCChannelSetting.CLIENT_LIMIT);
            return clientLimit != null && Integer.parseInt(clientLimit) <= members.size();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    boolean isBanned(IRCServerParameters parameters, ServerUser user) {
        if (!lists.containsKey(IRCChannelList.BAN)) {
            return false;
        }

        String nickmask = user.getNickmask(parameters);
        boolean banned = lists.get(IRCChannelList.BAN).stream().anyMatch(glob -> glob.matches(nickmask));
        if (!banned || !lists.containsKey(IRCChannelList.EXCEPTS)) {
            return banned;
        }

        return lists.get(IRCChannelList.EXCEPTS).stream().noneMatch(glob -> glob.matches(nickmask));
    }

    boolean isKeyValid(String key) {
        if (!settings.containsKey(IRCChannelSetting.KEY)) {
            return true;
        }

        return settings.get(IRCChannelSetting.KEY).equals(key);
    }

    boolean isInvited(IRCServerParameters parameters, ServerUser user) {
        if (!flags.contains(IRCChannelFlag.INVITE_ONLY)) {
            return true;
        }

        if (invited.contains(user)) {
            return true;
        }

        if (lists.containsKey(IRCChannelList.INVEX)) {
            String nickmask = user.getNickmask(parameters);
            return lists.get(IRCChannelList.INVEX).stream().anyMatch(glob -> glob.matches(nickmask));
        }

        return false;
    }
}
