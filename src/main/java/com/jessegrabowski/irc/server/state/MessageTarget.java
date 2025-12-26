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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MessageTarget {
    private final List<Predicate<ServerUser>> userFilters;
    private final List<Predicate<ServerChannel>> channelFilters;
    private final List<BiPredicate<ServerChannel, ServerUser>> channelUserFilters;
    ;

    private final String mask;
    private final boolean channel;
    private final boolean literal;

    private final Set<ServerUser> users;
    private final Set<ServerChannel> channels;

    MessageTarget(String mask, boolean channel, boolean literal, Set<ServerUser> users, Set<ServerChannel> channels) {
        this.mask = mask;
        this.channel = channel;
        this.literal = literal;
        this.users = users;
        this.channels = channels;
        userFilters = new ArrayList<>();
        channelFilters = new ArrayList<>();
        channelUserFilters = new ArrayList<>();
    }

    private MessageTarget(
            String mask,
            boolean channel,
            boolean literal,
            Set<ServerUser> users,
            Set<ServerChannel> channels,
            List<Predicate<ServerUser>> userFilters,
            List<Predicate<ServerChannel>> channelFilters,
            List<BiPredicate<ServerChannel, ServerUser>> channelUserFilters) {
        this.mask = mask;
        this.channel = channel;
        this.literal = literal;
        this.users = users;
        this.channels = channels;
        this.userFilters = userFilters;
        this.channelFilters = channelFilters;
        this.channelUserFilters = channelUserFilters;
    }

    public String getMask() {
        return mask;
    }

    public boolean isChannel() {
        return channel;
    }

    public boolean isLiteral() {
        return literal;
    }

    public boolean isEmpty() {
        return users.isEmpty() && channels.isEmpty();
    }

    public MessageTarget filterUsers(Predicate<ServerUser> filter) {
        List<Predicate<ServerUser>> newFilters = new ArrayList<>(userFilters);
        newFilters.add(filter);
        return new MessageTarget(
                mask, channel, literal, users, channels, newFilters, channelFilters, channelUserFilters);
    }

    public MessageTarget exclude(ServerUser user) {
        return filterUsers(u -> u != user);
    }

    public MessageTarget include(ServerUser user) {
        if (users.contains(user)) {
            return this;
        }
        Set<ServerUser> newUsers = new HashSet<>(users);
        newUsers.add(user);
        return new MessageTarget(
                mask, channel, literal, newUsers, channels, userFilters, channelFilters, channelUserFilters);
    }

    public MessageTarget filterChannels(Predicate<ServerChannel> filter) {
        List<Predicate<ServerChannel>> newFilters = new ArrayList<>(channelFilters);
        newFilters.add(filter);
        return new MessageTarget(mask, channel, literal, users, channels, userFilters, newFilters, channelUserFilters);
    }

    public MessageTarget filterChannelUsers(BiPredicate<ServerChannel, ServerUser> filter) {
        List<BiPredicate<ServerChannel, ServerUser>> newChannelUserFilters = new ArrayList<>(channelUserFilters);
        newChannelUserFilters.add(filter);
        return new MessageTarget(
                mask, channel, literal, users, channels, userFilters, channelFilters, newChannelUserFilters);
    }

    public Set<ServerChannel> findChannels(Predicate<ServerChannel> predicate) {
        return channels.stream().filter(predicate).collect(Collectors.toSet());
    }

    public Set<ServerUser> getAllMatchingUsers() {
        return Stream.concat(
                        users.stream(),
                        channels.stream()
                                .filter(c -> channelFilters.stream().allMatch(p -> p.test(c)))
                                .flatMap(c -> c.getMembers().stream()
                                        .filter(m -> channelUserFilters.stream().allMatch(f -> f.test(c, m)))))
                .filter(u -> userFilters.stream().allMatch(p -> p.test(u)))
                .collect(Collectors.toSet());
    }
}
