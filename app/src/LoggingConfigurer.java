import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

// not a huge fan of jul but since we can't really use libraries this'll have to do
public final class LoggingConfigurer {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    .withZone(ZoneId.systemDefault());

    private LoggingConfigurer() {}

    public static void configure(String filename, Level level) throws IOException {
        LogManager.getLogManager().reset();

        // FileHandler supports %u (unique) and %g (rotation index)
        FileHandler fileHandler = new FileHandler(filename, true);
        fileHandler.setFormatter(new LogbackishFormatter());
        fileHandler.setLevel(Level.ALL);

        Logger root = Logger.getLogger("");
        root.addHandler(fileHandler);
        root.setLevel(level);
    }

    // low quality replication of logback's default format
    private static class LogbackishFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            String timestamp = FORMATTER.format(Instant.ofEpochMilli(record.getMillis()));
            String level = padLevel(record.getLevel().getName());
            String thread = Thread.currentThread().getName();

            String logger = record.getLoggerName();
            if (logger == null) {
                logger = "unknown";
            }

            String msg = formatMessage(record)
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            return String.format(
                    "%s %s [%s] %s - %s%n",
                    timestamp,
                    level,
                    thread,
                    logger,
                    msg
            );
        }

        private String padLevel(String level) {
            if (level.length() >= 5) return level;
            return String.format("%-5s", level);
        }
    }

}
