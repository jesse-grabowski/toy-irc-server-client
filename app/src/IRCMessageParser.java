import java.util.ArrayList;
import java.util.List;

public class IRCMessageParser {

    public static IRCMessage parse(String line) {
        IRCMessage msg = new IRCMessage();

        if (line == null) {
            throw new IllegalArgumentException("line cannot be null");
        }

        line = line.trim();

        int idx = 0;
        int len = line.length();

        // prefix
        if (line.startsWith(":")) {
            int space = line.indexOf(' ');
            if (space == -1) {
                IRCMessage m = new IRCMessage();
                m.setPrefix(line.substring(1));
                m.setCommand("");
                m.setParams(List.of());
                return m;
            }
            msg.setPrefix(line.substring(1, space));
            idx = space + 1;
        }

        // command
        int space = line.indexOf(' ', idx);
        String command;

        if (space == -1) {
            // No params at all
            command = line.substring(idx);
            msg.setCommand(command);
            msg.setParams(List.of());
            return msg;
        } else {
            command = line.substring(idx, space);
            msg.setCommand(command);
            idx = space + 1;
        }

        // params
        List<String> params = new ArrayList<>();

        while (idx < len) {
            // Trailing param?
            if (line.charAt(idx) == ':') {
                // Everything after ':' is one single trailing param
                params.add(line.substring(idx + 1));
                break;
            }

            // Standard middle parameter
            int nextSpace = line.indexOf(' ', idx);
            if (nextSpace == -1) {
                // Last middle param
                params.add(line.substring(idx));
                break;
            } else {
                params.add(line.substring(idx, nextSpace));
                idx = nextSpace + 1;
            }
        }

        msg.setParams(params);
        return msg;
    }
}
