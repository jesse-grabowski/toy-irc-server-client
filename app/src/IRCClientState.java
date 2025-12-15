import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Predicate;

public class IRCClientState {

  private final Capabilities capabilities = new Capabilities();
  private final IRCServerParameters parameters = new IRCServerParameters();

  private final Map<String, User> users = new HashMap<>();
  private final Map<String, Channel> channels = new HashMap<>();

  private String me;
  private boolean operator;

  private String canonicalizeChannel(String channelName) {
    return parameters.getCaseMapping().normalizeChannel(channelName);
  }

  private String canonicalizeNickname(String nickname) {
    return parameters.getCaseMapping().normalizeNickname(nickname);
  }

  private Channel getOrCreateChannel(String rawChannelName) {
    return channels.computeIfAbsent(
        canonicalizeChannel(rawChannelName),
        unused -> {
          Channel channel = new Channel();
          channel.name = rawChannelName;
          return channel;
        });
  }

  private Channel findChannel(String rawChannelName) {
    return channels.get(canonicalizeChannel(rawChannelName));
  }

  private User getOrCreateUser(String rawNickname) {
    User user =
        users.computeIfAbsent(
            canonicalizeNickname(rawNickname),
            unused -> {
              User u = new User();
              u.nickname = rawNickname;
              return u;
            });
    user.lastTouched = System.currentTimeMillis();
    return user;
  }

  private User findUser(String rawNickname) {
    User user = users.get(canonicalizeNickname(rawNickname));
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

  public boolean isOperator() {
    return operator;
  }

  public void setOperator(boolean operator) {
    this.operator = operator;
  }

  public void addChannelMember(String channelName, String nickname, char... modes) {
    User user = getOrCreateUser(nickname);
    Channel channel = getOrCreateChannel(channelName);

    Membership membership = channel.getOrCreateMembership(user);
    for (char mode : modes) {
      membership.modes.add(mode);
    }

    user.channels.add(channel);
  }

  public void addChannelMemberModes(String channelName, String nickname, char... modes) {
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

    for (char mode : modes) {
      membership.modes.add(mode);
    }
  }

  public void deleteChannelMemberModes(String channelName, String nickname, char... modes) {
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

    for (Character mode : modes) {
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
      channels.remove(canonicalizeChannel(channelName));
    }
  }

  public List<String> getChannelList(String channelName, Character mode) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return List.of();
    }

    return channel.getList(mode);
  }

  public void addToChannelList(String channelName, Character mode, String entry) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.addToList(mode, entry);
  }

  public void removeFromChannelList(String channelName, Character mode, String entry) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.removeFromList(mode, entry);
  }

  public String getChannelSetting(String channelName, Character mode) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return null;
    }

    return channel.getSetting(mode);
  }

  public void setChannelSetting(String channelName, Character mode, String setting) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.setSetting(mode, setting);
  }

  public void removeChannelSetting(String channelName, Character mode) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.removeSetting(mode);
  }

  public boolean checkChannelFlag(String channelName, Character mode) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return false;
    }

    return channel.checkFlag(mode);
  }

  public void setChannelFlag(String channelName, Character mode) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.setFlag(mode);
  }

  public void clearChannelFlag(String channelName, Character mode) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.clearFlag(mode);
  }

  public String getChannelTopic(String channelName) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return null;
    }

    return channel.getTopic();
  }

  public void setChannelTopic(String channelName, String topic) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.setTopic(topic);
  }

  public void setChannelTopicUpdater(String channelName, String setBy, long setAt) {
    Channel channel = findChannel(channelName);
    if (channel == null) {
      return;
    }

    channel.setTopicSetBy(setBy);
    channel.setTopicSetAt(setAt);
  }

  public void setUserFlag(String nick, Character mode) {
    User user = findUser(nick);
    if (user == null) {
      return;
    }

    user.setFlag(mode);
  }

  public void clearUserFlag(String nick, Character mode) {
    User user = findUser(nick);
    if (user == null) {
      return;
    }

    user.clearFlag(mode);
  }

  public boolean checkUserFlag(String nick, Character mode) {
    User user = findUser(nick);
    if (user == null) {
      return false;
    }

    return user.checkFlag(mode);
  }

  public void changeNickname(String oldNickname, String newNickname) {
    String oldKey = canonicalizeNickname(oldNickname);
    String newKey = canonicalizeNickname(newNickname);
    // check non-canonicalized nicknames, since we want to update
    // the display value if they're different
    if (Objects.equals(oldNickname, newNickname)) {
      return;
    }

    User user = users.remove(oldKey);
    if (user == null) {
      return;
    }

    user.nickname = newNickname;
    user.lastTouched = System.currentTimeMillis();
    users.put(newKey, user);

    if (me != null && Objects.equals(canonicalizeNickname(me), oldKey)) {
      me = newNickname;
    }
  }

  public void gc(long cutoff) {
    users
        .values()
        .removeIf(
            user ->
                !Objects.equals(me, user.nickname)
                    && user.channels.isEmpty()
                    && user.lastTouched < cutoff);
  }

  public void touch(String nickname) {
    getOrCreateUser(nickname);
  }

  public void quit(String nickname) {
    String canonicalNickname = canonicalizeNickname(nickname);
    // if this is my own quit, I'll refresh
    // the state on reconnect so this shouldn't matter
    if (Objects.equals(canonicalizeNickname(me), canonicalNickname)) {
      return;
    }
    User user = findUser(nickname);
    if (user == null) {
      return;
    }
    for (Channel channel : user.channels) {
      channel.removeMembership(user);
      if (channel.members.isEmpty()) {
        channels.remove(canonicalizeChannel(channel.name));
      }
    }
    user.channels.clear();
    users.remove(canonicalNickname);
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

  public IRCServerParameters getParameters() {
    return parameters;
  }

  public static final class Channel {
    private String name;
    private String topic;
    private String topicSetBy;
    private long topicSetAt;
    private final Map<User, Membership> members = new HashMap<>();
    private final Map<Character, List<String>> lists = new HashMap<>();
    private final Map<Character, String> settings = new HashMap<>();
    private final Set<Character> flags = new HashSet<>();

    private Channel() {}

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

    private List<String> getList(Character mode) {
      return Collections.unmodifiableList(lists.getOrDefault(mode, List.of()));
    }

    private void addToList(Character mode, String entry) {
      lists.computeIfAbsent(mode, k -> new ArrayList<>()).add(entry);
    }

    private void removeFromList(Character mode, String entry) {
      List<String> list = lists.get(mode);
      if (list != null) {
        list.remove(entry);
        if (list.isEmpty()) {
          lists.remove(mode);
        }
      }
    }

    private String getSetting(Character mode) {
      return settings.get(mode);
    }

    private void setSetting(Character mode, String setting) {
      settings.put(mode, setting);
    }

    private void removeSetting(Character mode) {
      settings.remove(mode);
    }

    private boolean checkFlag(Character mode) {
      return flags.contains(mode);
    }

    private void setFlag(Character mode) {
      flags.add(mode);
    }

    private void clearFlag(Character mode) {
      flags.remove(mode);
    }

    private String getTopic() {
      return topic;
    }

    private void setTopic(String topic) {
      this.topic = topic;
    }

    private String getTopicSetBy() {
      return topicSetBy;
    }

    private void setTopicSetBy(String topicSetBy) {
      this.topicSetBy = topicSetBy;
    }

    private long getTopicSetAt() {
      return topicSetAt;
    }

    private void setTopicSetAt(long topicSetAt) {
      this.topicSetAt = topicSetAt;
    }
  }

  public static final class Membership {
    private final Set<Character> modes = new HashSet<>();

    private Membership() {}

    public Set<Character> getModes() {
      return Collections.unmodifiableSet(modes);
    }
  }

  public static final class User {

    private String nickname;
    private long lastTouched = System.currentTimeMillis();
    private final SequencedSet<Channel> channels = new LinkedHashSet<>();
    private final Set<Character> flags = new HashSet<>();

    private User() {}

    public String getNickname() {
      return nickname;
    }

    private void setFlag(Character mode) {
      flags.add(mode);
    }

    private void clearFlag(Character mode) {
      flags.remove(mode);
    }

    private boolean checkFlag(Character mode) {
      return flags.contains(mode);
    }
  }

  public static final class Capabilities {
    private final Map<IRCCapability, String> serverCapabilities = new HashMap<>();
    private final Map<IRCCapability, String> activeCapabilities = new HashMap<>();
    private final Set<IRCCapability> requestedCapabilities = new HashSet<>();

    private boolean receivingCapabilities = false; // in the middle of multiline response

    private Capabilities() {}

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
      return activeCapabilities.containsKey(capability)
          && valuePredicate.test(activeCapabilities.get(capability));
    }
  }
}
