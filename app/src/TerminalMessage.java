import java.time.LocalTime;

public record TerminalMessage(LocalTime time, String sender, String message) {}
