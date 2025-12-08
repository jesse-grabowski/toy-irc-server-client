public final class ClientCommandNick implements ClientCommand {

    private String nick;

    @Override
    public void validate() {}

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }
}
