public final class ClientCommandQuit implements ClientCommand {

    private String reason;

    @Override
    public void validate() {}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
