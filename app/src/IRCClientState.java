import java.util.Set;

public class IRCClientState {
    private String nick;
    private Set<IRCCapability> capabilities = Set.of();

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Set<IRCCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<IRCCapability> capabilities) {
        this.capabilities = capabilities;
    }
}
