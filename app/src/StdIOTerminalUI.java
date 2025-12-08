import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class StdIOTerminalUI extends TerminalUI {

    private static final Logger LOG = Logger.getLogger(StdIOTerminalUI.class.getName());

    private final BufferedReader reader =
            new BufferedReader(new InputStreamReader(System.in));

    @Override
    protected void process() {
        try {
            // Blocking read. Returns null on EOF.
            String line = reader.readLine();
            if (line == null) {
                // EOF: stop the UI loop gracefully
                stop();
                return;
            }
            dispatchInput(line);
        } catch (IOException e) {
            // Treat IO errors as termination
            stop();
        } catch (Exception e) {
            // If interrupted during blocking read, exit the loop
            if (Thread.currentThread().isInterrupted()) {
                stop();
            }
        }
    }

    @Override
    public void println(TerminalMessage message) {
        System.out.println(message.message());
    }

    @Override
    protected void initialize() {
        LOG.info("Initializing StdIOTerminalUI");
    }

    @Override
    public void setPrompt(RichString prompt) {
        // No-op
    }

    @Override
    public void setStatus(RichString status) {
        // No-op
    }
}
