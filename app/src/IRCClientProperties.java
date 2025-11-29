import java.net.InetAddress;

// oh lombok how my heart yearns for thee
public class IRCClientProperties implements ArgsProperties {

    private InetAddress host;
    private int port = 6667;
    private boolean useSimpleTerminal = false;
    private String nickname = "auto";
    private String realName = "Unknown";

    @Override
    public void validate() {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }
    }

    public InetAddress getHost() {
        return host;
    }

    public void setHost(InetAddress host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isUseSimpleTerminal() {
        return useSimpleTerminal;
    }

    public void setUseSimpleTerminal(boolean useSimpleTerminal) {
        this.useSimpleTerminal = useSimpleTerminal;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
