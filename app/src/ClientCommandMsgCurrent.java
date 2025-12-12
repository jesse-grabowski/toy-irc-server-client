public final class ClientCommandMsgCurrent implements ClientCommand {

  private String text;

  @Override
  public void validate() {}

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
