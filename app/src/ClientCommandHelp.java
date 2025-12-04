public final class ClientCommandHelp implements ClientCommand {

    private String command;

    @Override
    public void validate() {}

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}
