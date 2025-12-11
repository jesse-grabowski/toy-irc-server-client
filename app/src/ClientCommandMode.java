import java.util.ArrayList;
import java.util.List;

public final class ClientCommandMode implements ClientCommand {

    private String target;
    private String modeString;
    private List<String> modeArguments = new ArrayList<>();

    @Override
    public void validate() {}

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getModeString() {
        return modeString;
    }

    public void setModeString(String modeString) {
        this.modeString = modeString;
    }

    public List<String> getModeArguments() {
        return modeArguments;
    }

    public void setModeArguments(List<String> modeArguments) {
        this.modeArguments = modeArguments;
    }
}
