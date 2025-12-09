import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.UUID;

/*
 * I usually err more towards anemic domain models since they're easier
 * to serialize, but since this is only ever going to be in-memory, might
 * as well make it as easy to work with as possible
 */
public class IRCClientState {
    private final IRCUserState me = new IRCUserState();
    private final Map<UUID, IRCUserState> serverMembers = new HashMap<>();
    private final Map<String, UUID> membersNickIndex = new HashMap<>();
    private Map<String, IRCClientChannelState> channels = new HashMap<>();

    private SequencedMap<IRCCapability, String> claimedCapabilities = new LinkedHashMap<>();
    private SequencedMap<IRCCapability, String> serverCapabilities = new LinkedHashMap<>();
    private Deque<String> joinedChannels = new ArrayDeque<>();
    private String currentChannel;

    public IRCClientState() {
        me.setId(UUID.randomUUID());
        serverMembers.put(me.getId(), me);
    }

    public IRCUserState getMe() {
        return me;
    }

    public IRCUserState addMember(String nick) {
        if (membersNickIndex.containsKey(nick)) {
            return serverMembers.get(membersNickIndex.get(nick));
        }

        IRCUserState user = new IRCUserState();
        user.setId(UUID.randomUUID());
        serverMembers.put(user.getId(), user);
        user.setNick(nick);
        return user;
    }

    public void removeMember(String nick) {
        if (membersNickIndex.containsKey(nick)) {
            serverMembers.remove(membersNickIndex.get(nick));
            membersNickIndex.remove(nick);
        }
    }

    public IRCUserState renameMember(String oldNick, String newNick) {
        if (membersNickIndex.containsKey(oldNick)) {
            IRCUserState user = serverMembers.get(membersNickIndex.get(oldNick));
            user.setNick(newNick);
            return user;
        }
        return null;
    }

    public IRCClientChannelState getChannel(String channel) {
        return channels.computeIfAbsent(channel, c -> new IRCClientChannelState());
    }


    public SequencedMap<IRCCapability, String> getClaimedCapabilities() {
        return claimedCapabilities;
    }


    public SequencedMap<IRCCapability, String> getServerCapabilities() {
        return serverCapabilities;
    }


    public Deque<String> getJoinedChannels() {
        return joinedChannels;
    }


    public String getCurrentChannel() {
        return currentChannel;
    }

    public void setCurrentChannel(String currentChannel) {
        this.currentChannel = currentChannel;
    }

    public class IRCClientChannelState {
        private Set<UUID> channelMembers = new HashSet<>();
        private Map<UUID, String> memberModes = new HashMap<>();

        public void addMember(UUID member, String mode) {
            channelMembers.add(member);
            memberModes.put(member, mode);
        }

        public String getMemberMode(UUID member) {
            return memberModes.get(member);
        }

        public List<IRCUserState> getUsers() {
            return channelMembers.stream()
                    .map(serverMembers::get)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    public class IRCUserState {
        private UUID id;
        private String nick;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getNick() {
            return nick;
        }

        public void setNick(String nick) {
            String previous = this.nick;
            this.nick = nick;
            if (previous != null) {
                membersNickIndex.remove(previous);
            }
            membersNickIndex.put(nick, this.id);
        }
    }
}
