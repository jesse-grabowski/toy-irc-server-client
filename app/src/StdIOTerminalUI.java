import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class StdIOTerminalUI extends TerminalUI {

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
        // No-op
    }

    @Override
    public void setPrompt(String prompt) {
        // No-op
    }

    @Override
    public void setStatus(String status) {
        // No-op
    }
}
