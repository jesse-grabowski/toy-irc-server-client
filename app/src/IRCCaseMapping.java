import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public enum IRCCaseMapping {
    ASCII("ascii", Function.identity()),
    RFC1459("rfc1459", Function.identity()),
    RFC1459_STRICT("rfc1459-strict", Function.identity()),
    RFC7613("rfc7613", Function.identity());

    private static final Map<String, IRCCaseMapping> CASE_MAPPINGS = new HashMap<>();

    static {
        for (IRCCaseMapping caseMapping : IRCCaseMapping.values()) {
            CASE_MAPPINGS.put(caseMapping.name, caseMapping);
        }
    }

    private final String name;
    private final Function<String, String> normalizer;

    IRCCaseMapping(String name, Function<String, String> normalizer) {
        this.name = name;
        this.normalizer = normalizer;
    }

    public static Optional<IRCCaseMapping> forName(String name) {
        return Optional.ofNullable(CASE_MAPPINGS.get(name));
    }

    public String normalize(String string) {
        return normalizer.apply(string);
    }
}
