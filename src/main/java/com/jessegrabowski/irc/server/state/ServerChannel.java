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

import com.jessegrabowski.irc.protocol.IRCChannelMembershipMode;
import com.jessegrabowski.irc.protocol.model.IRCMessage443;
import com.jessegrabowski.irc.protocol.model.IRCMessage482;
import com.jessegrabowski.irc.util.Transaction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ServerChannel {
    private final String name;
    private String topic;
    private ServerSetBy topicSetBy;
    private long topicSetAt;
    private final Map<ServerUser, ServerChannelMembership> members = new HashMap<>();
    private final Map<Character, List<String>> lists = new HashMap<>();
    private final Map<Character, String> settings = new HashMap<>();
    private final Set<Character> flags = new HashSet<>();

    ServerChannel(String name) {
        this.name = name;
    }

    public Set<ServerUser> getMembers() {
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

    public long getTopicSetAt() {
        return topicSetAt;
    }

    void setTopicSetAt(long topicSetAt) {
        long oldTopicSetAt = this.topicSetAt;
        this.topicSetAt = topicSetAt;
        Transaction.addCompensation(() -> this.topicSetAt = oldTopicSetAt);
    }

    public List<String> getList(Character mode) {
        return Collections.unmodifiableList(lists.getOrDefault(mode, List.of()));
    }

    void addToList(Character mode, String entry) {
        List<String> list = Transaction.computeIfAbsentTransactionally(lists, mode, k -> new ArrayList<>());
        if (list.contains(entry)) {
            return;
        }
        Transaction.addTransactionally(list, entry);
    }

    void removeFromList(Character mode, String entry) {
        List<String> list = lists.get(mode);
        if (list != null) {
            Transaction.removeTransactionally(list, entry);
            if (list.isEmpty()) {
                Transaction.removeTransactionally(lists, mode);
            }
        }
    }

    public String getSetting(Character mode) {
        return settings.get(mode);
    }

    void setSetting(Character mode, String setting) {
        Transaction.putTransactionally(settings, mode, setting);
    }

    void removeSetting(Character mode) {
        Transaction.removeTransactionally(settings, mode);
    }

    public boolean checkFlag(Character mode) {
        return flags.contains(mode);
    }

    void setFlag(Character mode) {
        Transaction.addTransactionally(flags, mode);
    }

    void clearFlag(Character mode) {
        Transaction.removeTransactionally(flags, mode);
    }
}
