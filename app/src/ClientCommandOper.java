public final class ClientCommandOper implements ClientCommand {

  private String name;
  private String password;

  @Override
  public void validate() {}

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
}
