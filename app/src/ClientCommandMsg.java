import java.util.List;

public final class ClientCommandMsg implements ClientCommand {
  private List<String> targets;
  private String text;

  @Override
  public void validate() {}

  public List<String> getTargets() {
    return targets;
  }

  public void setTargets(List<String> targets) {
    this.targets = targets;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
