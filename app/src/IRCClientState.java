import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Predicate;

public class IRCClientState {

    private final Capabilities capabilities = new Capabilities();

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Channel> channels = new HashMap<>();

    private String me;

    private Channel getOrCreateChannel(String channelName) {
        return channels.computeIfAbsent(channelName, name -> {
            Channel channel = new Channel();
            channel.name = name;
            return channel;
        });
    }

    private Channel findChannel(String channelName) {
        return channels.get(channelName);
    }

    private User getOrCreateUser(String nickname) {
        User user = users.computeIfAbsent(nickname, nick -> {
            User u = new User();
            u.nickname = nick;
            return u;
        });
        user.lastTouched = System.currentTimeMillis();
        return user;
    }

    private User findUser(String nickname) {
        User user = users.get(nickname);
        if (user != null) {
            user.lastTouched = System.currentTimeMillis();
        }
        return user;
    }

    public String getMe() {
        return me;
    }

    public void setMe(String nickname) {
        me = nickname;
        getOrCreateUser(me);
    }

    public void addChannelMember(String channelName,
                                 String nickname,
                                 IRCChannelMode... modes) {
        User user = getOrCreateUser(nickname);
        Channel channel = getOrCreateChannel(channelName);

        Membership membership = channel.getOrCreateMembership(user);
        Collections.addAll(membership.modes, modes);

        user.channels.add(channel);
    }

    public void addChannelMemberModes(String channelName,
                                      String nickname,
                                      IRCChannelMode... modes) {
        Channel channel = findChannel(channelName);
        if (channel == null) {
            return;
        }

        User user = findUser(nickname);
        if (user == null) {
            return;
        }

        Membership membership = channel.findMembership(user);
        if (membership == null) {
            return;
        }

        Collections.addAll(membership.modes, modes);
    }

    public void deleteChannelMemberModes(String channelName,
                                         String nickname,
                                         IRCChannelMode... modes) {
        Channel channel = findChannel(channelName);
        if (channel == null) {
            return;
        }

        User user = findUser(nickname);
        if (user == null) {
            return;
        }

        Membership membership = channel.findMembership(user);
        if (membership == null) {
            return;
        }

        for (IRCChannelMode mode : modes) {
            membership.modes.remove(mode);
        }
    }

    public void deleteChannelMember(String channelName, String nickname) {
        Channel channel = findChannel(channelName);
        if (channel == null) {
            return;
        }

        User user = findUser(nickname);
        if (user == null) {
            return;
        }

        channel.removeMembership(user);
        user.channels.remove(channel);
        if (channel.members.isEmpty()) {
            channels.remove(channelName);
        }
    }

    public void changeNickname(String oldNickname, String newNickname) {
        if (Objects.equals(oldNickname, newNickname)) {
            return;
        }

        User user = users.remove(oldNickname);
        if (user == null) {
            return;
        }

        user.nickname = newNickname;
        user.lastTouched = System.currentTimeMillis();
        users.put(newNickname, user);

        if (Objects.equals(me, oldNickname)) {
            me = newNickname;
        }
    }

    public void gc(long cutoff) {
        users.values().removeIf(user ->
                !Objects.equals(me, user.nickname) &&
                user.channels.isEmpty() &&
                user.lastTouched < cutoff);
    }

    public void touch(String nickname) {
        getOrCreateUser(nickname);
    }

    public void quit(String nickname) {
        // if this is my own quit, I'll refresh
        // the state on reconnect so this shouldn't matter
        if (Objects.equals(me, nickname)) {
            return;
        }
        User user = findUser(nickname);
        if (user == null) {
            return;
        }
        for (Channel channel : user.channels) {
            channel.removeMembership(user);
            if (channel.members.isEmpty()) {
                channels.remove(channel.name);
            }
        }
        user.channels.clear();
        users.remove(nickname);
    }

    public Optional<Channel> getFocusedChannel() {
        if (me == null) {
            return Optional.empty();
        }
        User user = findUser(me);
        if (user == null || user.channels.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(user.channels.getLast());
    }

    public boolean focusChannel(String channelName) {
        if (me == null) {
            return false;
        }
        User user = findUser(me);
        if (user == null) {
            return false;
        }
        Channel channel = findChannel(channelName);
        if (channel == null || !user.channels.contains(channel)) {
            return false;
        }
        user.channels.remove(channel);
        user.channels.add(channel);
        return true;
    }

    public Capabilities getCapabilities() {
        return capabilities;
    }

    public static class Channel {
        private String name;
        private final Map<User, Membership> members = new HashMap<>();

        private Membership getOrCreateMembership(User user) {
            return members.computeIfAbsent(user, u -> new Membership());
        }

        private Membership findMembership(User user) {
            return members.get(user);
        }

        private void removeMembership(User user) {
            members.remove(user);
        }

        public String getName() {
            return name;
        }

        public Map<User, Membership> getMemberships() {
            return Collections.unmodifiableMap(members);
        }
    }

    public static class Membership {
        private final Set<IRCChannelMode> modes = EnumSet.noneOf(IRCChannelMode.class);

        public Set<IRCChannelMode> getModes() {
            return Collections.unmodifiableSet(modes);
        }
    }

    public static class User {
        private String nickname;
        private long lastTouched = System.currentTimeMillis();
        private final SequencedSet<Channel> channels = new LinkedHashSet<>();

        public String getNickname() {
            return nickname;
        }
    }

    public static class Capabilities {
        private final Map<IRCCapability, String> serverCapabilities = new HashMap<>();
        private final Map<IRCCapability, String> activeCapabilities = new HashMap<>();
        private final Set<IRCCapability> requestedCapabilities = new HashSet<>();
        private boolean receivingCapabilities = false; // in the middle of multiline response

        public void clearActiveCapabilities() {
            activeCapabilities.clear();
            requestedCapabilities.clear();
        }

        public void clearServerCapabilities() {
            serverCapabilities.clear();
            activeCapabilities.clear();
            requestedCapabilities.clear();
        }

        public boolean isReceivingCapabilities() {
            return receivingCapabilities;
        }

        public void startReceivingCapabilities() {
            this.receivingCapabilities = true;
        }

        public void stopReceivingCapabilities() {
            this.receivingCapabilities = false;
        }

        public void addServerCapability(IRCCapability capability, String value) {
            serverCapabilities.put(capability, value);
        }

        public void removeServerCapability(IRCCapability capability) {
            serverCapabilities.remove(capability);
            requestedCapabilities.remove(capability);
            activeCapabilities.remove(capability);
        }

        public Set<IRCCapability> getServerCapabilities() {
            return Collections.unmodifiableSet(serverCapabilities.keySet());
        }

        public void addRequestedCapability(IRCCapability capability) {
            requestedCapabilities.add(capability);
        }

        public void removeRequestedCapability(IRCCapability capability) {
            requestedCapabilities.remove(capability);
        }

        public Set<IRCCapability> getRequestedCapabilities() {
            return Collections.unmodifiableSet(requestedCapabilities);
        }

        public void enableCapability(IRCCapability capability) {
            if (serverCapabilities.containsKey(capability)) {
                activeCapabilities.put(capability, serverCapabilities.get(capability));
            }
        }

        public boolean isActive(IRCCapability capability) {
            return activeCapabilities.containsKey(capability);
        }

        public boolean isActive(IRCCapability capability, Predicate<String> valuePredicate) {
            return activeCapabilities.containsKey(capability) &&
                    valuePredicate.test(activeCapabilities.get(capability));
        }
    }
}
