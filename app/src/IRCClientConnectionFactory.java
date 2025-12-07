import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

public class IRCClientConnectionFactory {

    public static IRCConnection create(InetAddress host, int port, Charset charset, int connectTimeout, int readTimeout) throws IOException {
        assert host != null;
        assert port > 0 && port < 65536;
        assert charset != null;
        assert connectTimeout > 0;
        assert readTimeout > 0;

        Socket socket = new Socket();
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSoTimeout(readTimeout);

        IRCConnection connection = null;
        try {
            socket.connect(new InetSocketAddress(host, port), connectTimeout);
            connection = new IRCConnection(socket, charset);
            return connection;
        } catch (IOException | RuntimeException e) {
            // probably an overly defensive null check but
            // for the sake of future-proofing we'll do it
            if (connection != null) {
                connection.close();
            } else {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // do nothing
                }
            }
            throw e;
        }
    }

}
