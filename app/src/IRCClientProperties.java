import java.net.InetAddress;

// oh lombok how my heart yearns for thee
public class IRCClientProperties implements ArgsProperties {

    private InetAddress host;
    private int port = 6667;
    private boolean useSimpleTerminal = false;

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
}
