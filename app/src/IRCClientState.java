import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IRCClientState {
    private String nick;
    private Set<IRCCapability> claimedCapabilities = new HashSet<>();
    private Set<IRCCapability> serverCapabilities = new HashSet<>();
    private Deque<String> joinedChannels = new ArrayDeque<>();
    private Map<String, IRCClientChannelState> channels = new HashMap<>();
    private String currentChannel;

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Set<IRCCapability> getClaimedCapabilities() {
        return claimedCapabilities;
    }

    public void setClaimedCapabilities(Set<IRCCapability> claimedCapabilities) {
        this.claimedCapabilities = claimedCapabilities;
    }

    public Set<IRCCapability> getServerCapabilities() {
        return serverCapabilities;
    }

    public void setServerCapabilities(Set<IRCCapability> serverCapabilities) {
        this.serverCapabilities = serverCapabilities;
    }

    public Deque<String> getJoinedChannels() {
        return joinedChannels;
    }

    public void setJoinedChannels(Deque<String> joinedChannels) {
        this.joinedChannels = joinedChannels;
    }

    public Map<String, IRCClientChannelState> getChannels() {
        return channels;
    }

    public void setChannels(Map<String, IRCClientChannelState> channels) {
        this.channels = channels;
    }

    public String getCurrentChannel() {
        return currentChannel;
    }

    public void setCurrentChannel(String currentChannel) {
        this.currentChannel = currentChannel;
    }

    public static class IRCClientChannelState {
        private Set<String> members = new HashSet<>();

        public Set<String> getMembers() {
            return members;
        }

        public void setMembers(Set<String> members) {
            this.members = members;
        }
    }
}
