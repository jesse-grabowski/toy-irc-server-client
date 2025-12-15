import java.time.LocalTime;

public record TerminalMessage(
    LocalTime time, RichString sender, RichString receiver, RichString message) {}
