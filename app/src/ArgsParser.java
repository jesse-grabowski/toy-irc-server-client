import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Simple" command line argument parser. I'd love to pull in a library for this, but since we're not using
 * a proper build tool that'll probably be more painful than it's worth.
 *
 * @param <T> properties class type
 */
public class ArgsParser<T extends ArgsProperties> {

    private static final Pattern TOKEN_FLAGS_END = Pattern.compile("^--$");
    private static final Pattern TOKEN_FLAGS = Pattern.compile("^-(?<flag>[a-zA-Z0-9]+)$");
    private static final Pattern TOKEN_FLAGS_LONG = Pattern.compile("^--(?<flag>[a-zA-Z-]+)$");

    private final List<FlagSpec<?>> flagSpecs;
    private final List<PositionalSpec<?>> positionalSpecs;
    private final List<String> usageExamples;
    private final Supplier<T> propertiesFactory;
    private final boolean registerHelpFlag;
    private final String description;

    public ArgsParser(Supplier<T> propertiesFactory, boolean registerHelpFlag, String description) {
        this.flagSpecs = new ArrayList<>();
        this.positionalSpecs = new ArrayList<>();
        this.usageExamples = new ArrayList<>();
        this.propertiesFactory = Objects.requireNonNull(propertiesFactory, "propertiesFactory");
        this.registerHelpFlag = registerHelpFlag;
        this.description = description;
    }

    public ArgsParser<T> addUsageExample(String usageExample) {
        usageExamples.add(usageExample);
        return this;
    }

    public ArgsParser<T> addBooleanFlag(char shortKey, String longKey, BiConsumer<T, Boolean> propertiesSetter, String description, boolean required) {
        addFlagSpec(new FlagSpec<>(shortKey, longKey, false, propertiesSetter, x -> true, description, required));
        return this;
    }

    public ArgsParser<T> addStringFlag(char shortKey, String longKey, BiConsumer<T, String> propertiesSetter, String description, boolean required) {
        addFlagSpec(new FlagSpec<>(shortKey, longKey, true, propertiesSetter, Function.identity(), description, required));
        return this;
    }

    public ArgsParser<T> addIntegerFlag(char shortKey, String longKey, BiConsumer<T, Integer> propertiesSetter, String description, boolean required) {
        addFlagSpec(new FlagSpec<>(shortKey, longKey, true, propertiesSetter, this::tryParseInt, description, required));
        return this;
    }

    public ArgsParser<T> addStringPositional(int position, BiConsumer<T, String> propertiesSetter, String description, boolean required) {
        addPositionalSpec(new PositionalSpec<>(position, propertiesSetter, Function.identity(), description, required));
        return this;
    }

    public ArgsParser<T> addInetAddressPositional(int position, BiConsumer<T, InetAddress> propertiesSetter, String description, boolean required) {
        addPositionalSpec(new PositionalSpec<>(position, propertiesSetter, this::tryParseInetAddress, description, required));
        return this;
    }

    private int tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("expected integer but got '%s'".formatted(value), e);
        }
    }

    private InetAddress tryParseInetAddress(String value) {
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("failed to resolve hostname '%s'".formatted(value), e);
        }
    }

    private void addFlagSpec(FlagSpec<?> newFlag) {
        for (FlagSpec<?> flag : flagSpecs) {
            if (flag.shortKey == newFlag.shortKey) {
                throw new IllegalArgumentException("Flag with key '-%c' already exists".formatted(flag.shortKey));
            }
            if (flag.longKey.equals(newFlag.longKey)) {
                throw new IllegalArgumentException("Flag with key '--%s' already exists".formatted(flag.longKey));
            }
        }
        flagSpecs.add(newFlag);
    }

    private void addPositionalSpec(PositionalSpec<?> newPositional) {
        for (PositionalSpec<?> positional : positionalSpecs) {
            if (positional.position == newPositional.position) {
                throw new IllegalArgumentException("Positional argument at %d already exists".formatted(positional.position));
            }
        }
        positionalSpecs.add(newPositional);
    }

    /**
     * Parse command line arguments into a properties object
     *
     * @param args raw command line input
     * @return a properties object with some or all fields populated
     * @throws IllegalArgumentException when an unknown property is supplied or a property is missing
     */
    public T parse(String[] args) throws ArgsParserHelpRequestedException {
        Set<ArgSpec<?>> usedSpecs = new HashSet<>();
        T properties = propertiesFactory.get();

        int position = 0;
        Iterator<String> argsIterator = List.of(args).iterator();
        while (argsIterator.hasNext()) {
            String token = argsIterator.next();

            if (registerHelpFlag && ("--help".equals(token) || "-h".equals(token))) {
                throw new ArgsParserHelpRequestedException();
            }

            Matcher flagMatcher = TOKEN_FLAGS.matcher(token);
            if (flagMatcher.matches()) {
                String flags = flagMatcher.group("flag");
                if (flags.length() == 1) {
                    FlagSpec<?> spec = findByShortKey(flags.charAt(0));
                    usedSpecs.add(spec);
                    parseFlagSpec(argsIterator, spec, properties, false);
                } else {
                    List<FlagSpec<?>> specs = new ArrayList<>();
                    for (char flag: flags.toCharArray()) {
                        FlagSpec<?> spec = findByShortKey(flag);
                        usedSpecs.add(spec);
                        specs.add(spec);
                    }
                    if (specs.stream().filter(s -> s.takesValue).count() > 1) {
                        throw new IllegalArgumentException("Flag group contains multiple value-taking flags: '%s'".formatted(flags));
                    }
                    specs.forEach(spec -> parseFlagSpec(argsIterator, spec, properties, false));
                }
                continue;
            }

            flagMatcher = TOKEN_FLAGS_LONG.matcher(token);
            if (flagMatcher.matches()) {
                String flag = flagMatcher.group("flag");
                FlagSpec<?> flagSpec = findByLongKey(flag);
                usedSpecs.add(flagSpec);
                parseFlagSpec(argsIterator, flagSpec, properties, true);
                continue;
            }

            if (TOKEN_FLAGS_END.matcher(token).matches()) {
                // all arguments from this point are positional regardless of shape
                while (argsIterator.hasNext()) {
                    token = argsIterator.next();
                    PositionalSpec<?> positionalSpec = findByPosition(position);
                    usedSpecs.add(positionalSpec);
                    parsePositionalSpec(token, positionalSpec, properties);
                    position++;
                }
                break;
            }

            if (token.startsWith("-")) {
                // catch accidental single-hyphen long names and similar
                throw new IllegalArgumentException("Unrecognized option: '%s'".formatted(token));
            }

            PositionalSpec<?> positionalSpec = findByPosition(position);
            usedSpecs.add(positionalSpec);
            parsePositionalSpec(token, positionalSpec, properties);
            position++;
        }

        for (FlagSpec<?> spec : flagSpecs) {
            if (spec.isRequired() && !usedSpecs.contains(spec)) {
                throw new IllegalArgumentException("Missing required flag: '--%s'".formatted(spec.longKey));
            }
        }

        for (PositionalSpec<?> spec : positionalSpecs) {
            if (spec.isRequired() && !usedSpecs.contains(spec)) {
                throw new IllegalArgumentException("Missing required positional argument at index %d".formatted(spec.position));
            }
        }

        properties.validate();

        return properties;
    }

    private void parseFlagSpec(Iterator<String> argsIterator, FlagSpec<?> spec, T properties, boolean useLongKey) {
        String value;
        if (spec.takesValue) {
            if (argsIterator.hasNext()) {
                value = argsIterator.next();
            } else if (useLongKey) {
                throw new IllegalArgumentException("Missing expected value for flag: '--%s <value>'".formatted(spec.longKey));
            } else {
                throw new IllegalArgumentException("Missing expected value for flag: '-%c <value>'".formatted(spec.shortKey));
            }
        } else {
            value = "";
        }

        try {
            spec.apply(value, properties);
        } catch (Exception e) {
            if (useLongKey) {
                throw new IllegalArgumentException("Illegal value for flag '--%s': %s".formatted(spec.longKey, e.getMessage()), e);
            } else {
                throw new IllegalArgumentException("Illegal value for flag '-%c': %s".formatted(spec.shortKey, e.getMessage()), e);
            }
        }
    }

    private void parsePositionalSpec(String token, PositionalSpec<?> spec, T properties) {
        try {
            spec.apply(token, properties);
        } catch (Exception e) {
            throw new IllegalArgumentException("Illegal value at index %d: %s".formatted(spec.position, e.getMessage()), e);
        }
    }

    public String getHelpText() {
        var sb = new StringBuilder(description);

        if (!usageExamples.isEmpty()) {
            sb.append("\n\nUsage:");
            for (String usageSample : usageExamples) {
                sb.append("\n\t").append(usageSample);
            }
        }

        if (!flagSpecs.isEmpty()) {
            sb.append("\n\nOptions:");
            for (FlagSpec<?> f : flagSpecs) {
                sb.append("\n\t-").append(f.shortKey)
                        .append(", --").append(f.longKey)
                        .append(f.takesValue ? " <value>" : "")
                        .append(f.isRequired() ? " (required)" : "")
                        .append(" : ").append(f.getDescription());
            }
        }

        if (!positionalSpecs.isEmpty()) {
            sb.append("\n\nPositionals:");
            for (PositionalSpec<?> p : positionalSpecs) {
                sb.append("\n\targ").append(p.position)
                        .append(p.isRequired() ? " (required)" : "")
                        .append(" : ").append(p.getDescription());
            }
        }

        return sb.toString();
    }

    private FlagSpec<?> findByShortKey(char key) {
        for (FlagSpec<?> flagSpec : flagSpecs) {
            if (key == flagSpec.shortKey) {
                return flagSpec;
            }
        }
        throw new IllegalArgumentException("Unrecognized flag: '-%c'".formatted(key));
    }

    private FlagSpec<?> findByLongKey(String key) {
        for (FlagSpec<?> flagSpec : flagSpecs) {
            if (flagSpec.longKey.equals(key)) {
                return flagSpec;
            }
        }
        throw new IllegalArgumentException("Unrecognized flag: '--%s'".formatted(key));
    }

    private PositionalSpec<?> findByPosition(int position) {
        for (PositionalSpec<?> positionalSpec : positionalSpecs) {
            if (position == positionalSpec.position) {
                return positionalSpec;
            }
        }
        throw new IllegalArgumentException("Unsupported positional argument at index %d".formatted(position));
    }

    private abstract class ArgSpec<F> {
        private final BiConsumer<T, F> propertiesSetter;
        private final Function<String, F> propertyMapper;
        private final String description;
        private final boolean required;

        protected ArgSpec(BiConsumer<T, F> propertiesSetter,
                          Function<String, F> propertyMapper,
                          String description,
                          boolean required) {
            this.propertiesSetter = Objects.requireNonNull(propertiesSetter, "propertiesSetter");
            this.propertyMapper = Objects.requireNonNull(propertyMapper, "propertyMapper");
            this.description = Objects.requireNonNull(description, "description");
            this.required = required;
        }

        protected void apply(String value, T properties) {
            propertiesSetter.accept(properties, propertyMapper.apply(value));
        }

        protected String getDescription() {
            return description;
        }

        protected boolean isRequired() {
            return required;
        }
    }

    private class FlagSpec<F> extends ArgSpec<F> {
        private final char shortKey;
        private final String longKey;
        private final boolean takesValue;

        public FlagSpec(char shortKey,
                        String longKey,
                        boolean takesValue,
                        BiConsumer<T, F> propertiesSetter,
                        Function<String, F> propertyMapper,
                        String description,
                        boolean required) {
            super(propertiesSetter, propertyMapper, description, required);

            this.shortKey = shortKey;
            this.longKey = Objects.requireNonNull(longKey, "longKey");
            this.takesValue = takesValue;
        }
    }

    private class PositionalSpec<F> extends ArgSpec<F> {
        private final int position;

        public PositionalSpec(int position,
                              BiConsumer<T, F> propertiesSetter,
                              Function<String, F> propertyMapper,
                              String description,
                              boolean required) {
            super(propertiesSetter, propertyMapper, description, required);
            this.position = position;
        }
    }
}
