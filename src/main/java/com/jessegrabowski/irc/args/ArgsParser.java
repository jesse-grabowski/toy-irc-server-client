/*
 * This project is licensed under the MIT License.
 *
 * In addition to the rights granted under the MIT License, explicit permission
 * is granted to the faculty, instructors, teaching assistants, and evaluators
 * of Ritsumeikan University for unrestricted educational evaluation and grading.
 *
 * ---------------------------------------------------------------------------
 *
 * MIT License
 *
 * Copyright (c) 2026 Jesse Grabowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jessegrabowski.irc.args;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
 * "Simple" command line argument parser. I'd love to pull in a library for this, but since we're
 * not using a proper build tool that'll probably be more painful than it's worth.
 *
 * @param <T> properties class type
 */
public class ArgsParser<T extends ArgsProperties> implements ArgsParserBuilder<T> {

    private static final Pattern TOKEN_FLAGS_END = Pattern.compile("^--$");
    private static final Pattern TOKEN_FLAGS = Pattern.compile("^-(?<flag>[a-zA-Z0-9]+)$");
    private static final Pattern TOKEN_FLAGS_LONG = Pattern.compile("^--(?<flag>[a-zA-Z-]+)$");

    private final List<FlagSpec<?>> flagSpecs = new ArrayList<>();
    private final List<PositionalSpec<?>> positionalSpecs = new ArrayList<>();
    private final List<String> usageExamples = new ArrayList<>();

    private final Supplier<T> propertiesFactory;
    private final boolean registerHelpFlag;
    private final String description;

    private boolean flagParsingEnabled = true;
    private boolean built = false;

    private ArgsParser(Supplier<T> propertiesFactory, boolean registerHelpFlag, String description) {
        this.propertiesFactory = Objects.requireNonNull(propertiesFactory, "propertiesFactory");
        this.registerHelpFlag = registerHelpFlag;
        this.description = Objects.requireNonNull(description);
    }

    public static <T extends ArgsProperties> ArgsParserBuilder<T> builder(
            Supplier<T> propertiesFactory, boolean registerHelpFlag, String description) {
        return new ArgsParser<>(propertiesFactory, registerHelpFlag, description);
    }

    @Override
    public ArgsParserBuilder<T> addUsageExample(String usageExample) {
        if (built) {
            throw new IllegalStateException("com.jessegrabowski.irc.args.ArgsParser already built");
        }
        usageExamples.add(usageExample);
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addBooleanFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, Boolean> propertiesSetter,
            String description,
            boolean required) {
        addFlagSpec(new FlagSpec<>(shortKey, longKey, false, propertiesSetter, x -> true, description, required));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addStringFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, String> propertiesSetter,
            String description,
            boolean required) {
        addFlagSpec(
                new FlagSpec<>(shortKey, longKey, true, propertiesSetter, Function.identity(), description, required));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addIntegerFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, Integer> propertiesSetter,
            String description,
            boolean required) {
        addFlagSpec(
                new FlagSpec<>(shortKey, longKey, true, propertiesSetter, this::tryParseInt, description, required));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addCharsetFlag(
            char shortKey,
            String longKey,
            BiConsumer<T, Charset> propertiesSetter,
            String description,
            boolean required) {
        addFlagSpec(new FlagSpec<>(
                shortKey, longKey, true, propertiesSetter, this::tryParseCharset, description, required));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addStringPositional(
            int position, BiConsumer<T, String> propertiesSetter, String description, boolean required) {
        addPositionalSpec(new PositionalSpec<>(
                position, propertiesSetter, Function.identity(), description, required, TokenConsumption.SELF));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addInetAddressPositional(
            int position, BiConsumer<T, InetAddress> propertiesSetter, String description, boolean required) {
        addPositionalSpec(new PositionalSpec<>(
                position, propertiesSetter, this::tryParseInetAddress, description, required, TokenConsumption.SELF));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addCommaSeparatedListPositional(
            int position, BiConsumer<T, List<String>> propertiesSetter, String description, boolean required) {
        addPositionalSpec(new PositionalSpec<>(
                position,
                propertiesSetter,
                this::tryParseCommaSeparatedList,
                description,
                required,
                TokenConsumption.SELF));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addGreedyStringPositional(
            int position, BiConsumer<T, String> propertiesSetter, String description, boolean required) {
        addPositionalSpec(new PositionalSpec<>(
                position, propertiesSetter, Function.identity(), description, required, TokenConsumption.DRAIN_RAW));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> addGreedyListPositional(
            int position,
            Function<T, List<String>> propertiesGetter,
            BiConsumer<T, List<String>> propertiesSetter,
            String description,
            boolean required) {
        addPositionalSpec(new GreedyCollectionPositionalSpec<>(
                position, ArrayList::new, propertiesGetter, propertiesSetter, description, required));
        return this;
    }

    @Override
    public ArgsParserBuilder<T> setFlagParsingEnabled(boolean value) {
        this.flagParsingEnabled = value;
        return this;
    }

    @Override
    public ArgsParser<T> build() {
        if (built) {
            throw new IllegalStateException("com.jessegrabowski.irc.args.ArgsParser already built");
        }
        if (positionalSpecs.size()
                != positionalSpecs.stream()
                                .mapToInt(PositionalSpec::getPosition)
                                .max()
                                .orElse(-1)
                        + 1) {
            throw new IllegalStateException("Positional arguments do not span all positions");
        }
        for (PositionalSpec<?> spec : positionalSpecs) {
            if (spec.getTokenConsumption() != TokenConsumption.SELF
                    && spec.getPosition() != positionalSpecs.size() - 1) {
                throw new IllegalStateException("Greedy positional arguments may only be at the final position");
            }
        }
        if (!flagParsingEnabled && !flagSpecs.isEmpty()) {
            throw new IllegalStateException("Flag parsing is disabled but flag specs are defined");
        }
        built = true;
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

    private Charset tryParseCharset(String value) {
        try {
            return Charset.forName(value);
        } catch (UnsupportedCharsetException e) {
            throw new IllegalArgumentException("failed to resolve charset '%s'".formatted(value), e);
        }
    }

    private List<String> tryParseCommaSeparatedList(String value) {
        return Arrays.asList(value.split(","));
    }

    private void addFlagSpec(FlagSpec<?> newFlag) {
        if (built) {
            throw new IllegalStateException("com.jessegrabowski.irc.args.ArgsParser already built");
        }
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
        if (built) {
            throw new IllegalStateException("com.jessegrabowski.irc.args.ArgsParser already built");
        }
        for (PositionalSpec<?> positional : positionalSpecs) {
            if (positional.getPosition() == newPositional.getPosition()) {
                throw new IllegalArgumentException(
                        "Positional argument at %d already exists".formatted(positional.getPosition()));
            }
        }
        positionalSpecs.add(newPositional);
    }

    /**
     * Parse a command line represented by a raw input string and its tokenized form.
     *
     * <p>The {@code raw} string preserves the original text exactly as written, while {@code tokens}
     * contains the already–tokenized arguments with span information (as produced by {@link
     * ArgsTokenizer}).
     *
     * @param raw the original unmodified command line text
     * @param tokens the list of parsed {@link ArgsToken} objects derived from {@code raw}
     * @return a populated properties object
     * @throws ArgsParserHelpRequestedException if the {@code -h} or {@code --help} flag was
     *     encountered
     * @throws IllegalArgumentException if an unknown option is supplied, a required argument is
     *     missing, or a value fails to parse
     */
    public T parse(String raw, List<ArgsToken> tokens) throws ArgsParserHelpRequestedException {
        if (!built) {
            throw new IllegalStateException("com.jessegrabowski.irc.args.ArgsParser not yet built");
        }

        Set<ArgSpec> usedSpecs = new HashSet<>();
        T properties = propertiesFactory.get();

        int position = 0;
        Iterator<ArgsToken> argsIterator = tokens.iterator();
        while (argsIterator.hasNext()) {
            ArgsToken token = argsIterator.next();

            if (registerHelpFlag && ("--help".equals(token.token()) || "-h".equals(token.token()))) {
                throw new ArgsParserHelpRequestedException();
            }

            Matcher flagMatcher = TOKEN_FLAGS.matcher(token.token());
            if (flagParsingEnabled && flagMatcher.matches()) {
                String flags = flagMatcher.group("flag");
                if (flags.length() == 1) {
                    FlagSpec<?> spec = findByShortKey(flags.charAt(0));
                    usedSpecs.add(spec);
                    parseFlagSpec(argsIterator, spec, properties, false);
                } else {
                    List<FlagSpec<?>> specs = new ArrayList<>();
                    for (char flag : flags.toCharArray()) {
                        FlagSpec<?> spec = findByShortKey(flag);
                        usedSpecs.add(spec);
                        specs.add(spec);
                    }
                    if (specs.stream()
                                    .filter(s -> s.getTokenConsumption() != TokenConsumption.SELF)
                                    .count()
                            > 1) {
                        throw new IllegalArgumentException(
                                "Flag group contains multiple value-taking flags: '%s'".formatted(flags));
                    }
                    specs.forEach(spec -> parseFlagSpec(argsIterator, spec, properties, false));
                }
                continue;
            }

            flagMatcher = TOKEN_FLAGS_LONG.matcher(token.token());
            if (flagParsingEnabled && flagMatcher.matches()) {
                String flag = flagMatcher.group("flag");
                FlagSpec<?> flagSpec = findByLongKey(flag);
                usedSpecs.add(flagSpec);
                parseFlagSpec(argsIterator, flagSpec, properties, true);
                continue;
            }

            if (TOKEN_FLAGS_END.matcher(token.token()).matches()) {
                // all arguments from this point are positional regardless of shape
                while (argsIterator.hasNext()) {
                    token = argsIterator.next();
                    PositionalSpec<?> positionalSpec = findByPosition(position);
                    usedSpecs.add(positionalSpec);
                    parsePositionalSpec(raw, argsIterator, token, positionalSpec, properties);
                    position++;
                }
                break;
            }

            if (flagParsingEnabled && token.token().startsWith("-")) {
                // catch accidental single-hyphen long names and similar
                throw new IllegalArgumentException("Unrecognized option: '%s'".formatted(token.token()));
            }

            PositionalSpec<?> positionalSpec = findByPosition(position);
            usedSpecs.add(positionalSpec);
            parsePositionalSpec(raw, argsIterator, token, positionalSpec, properties);
            position++;
        }

        for (FlagSpec<?> spec : flagSpecs) {
            if (spec.isRequired() && !usedSpecs.contains(spec)) {
                throw new IllegalArgumentException("Missing required flag: '--%s'".formatted(spec.longKey));
            }
        }

        for (PositionalSpec<?> spec : positionalSpecs) {
            if (spec.isRequired() && !usedSpecs.contains(spec)) {
                throw new IllegalArgumentException(
                        "Missing required positional argument at index %d".formatted(spec.getPosition()));
            }
        }

        properties.validate();

        return properties;
    }

    private void parseFlagSpec(Iterator<ArgsToken> argsIterator, FlagSpec<?> spec, T properties, boolean useLongKey) {
        try {
            switch (spec.getTokenConsumption()) {
                case SELF -> spec.apply("", properties);
                case NEXT -> {
                    if (argsIterator.hasNext()) {
                        spec.apply(argsIterator.next().token(), properties);
                    } else {
                        throw new IllegalArgumentException("no value provided");
                    }
                }
                default ->
                    throw new UnsupportedOperationException(
                            "invalid token consumption mode %s".formatted(spec.getTokenConsumption()));
            }
        } catch (Exception e) {
            if (useLongKey) {
                throw new IllegalArgumentException(
                        "Illegal value for flag '--%s': %s".formatted(spec.longKey, e.getMessage()), e);
            } else {
                throw new IllegalArgumentException(
                        "Illegal value for flag '-%c': %s".formatted(spec.shortKey, e.getMessage()), e);
            }
        }
    }

    private void parsePositionalSpec(
            String raw, Iterator<ArgsToken> argsIterator, ArgsToken token, PositionalSpec<?> spec, T properties) {
        try {
            switch (spec.getTokenConsumption()) {
                case SELF -> spec.apply(token.token(), properties);
                case DRAIN_CONSUME -> {
                    spec.apply(token.token(), properties);
                    while (argsIterator.hasNext()) {
                        ArgsToken next = argsIterator.next();
                        spec.apply(next.token(), properties);
                    }
                }
                case DRAIN_RAW -> {
                    spec.apply(raw.substring(token.startInclusive()), properties);
                    argsIterator.forEachRemaining(t -> {
                        /* do nothing */
                    });
                }
                default ->
                    throw new UnsupportedOperationException(
                            "invalid token consumption mode %s".formatted(spec.getTokenConsumption()));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Illegal value at index %d: %s".formatted(spec.getPosition(), e.getMessage()), e);
        }
    }

    public String getDescription() {
        return description;
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
                sb.append("\n\t-")
                        .append(f.shortKey)
                        .append(", --")
                        .append(f.longKey)
                        .append(
                                switch (f.getTokenConsumption()) {
                                    case SELF -> "";
                                    case NEXT -> " <value>";
                                    case DRAIN_RAW -> " <remaining text>";
                                    case DRAIN_CONSUME -> " <value> <value> ...";
                                })
                        .append(f.isRequired() ? " (required)" : "")
                        .append(" : ")
                        .append(f.getDescription());
            }
        }

        if (!positionalSpecs.isEmpty()) {
            sb.append("\n\nPositionals:");
            for (PositionalSpec<?> p : positionalSpecs) {
                sb.append("\n\t")
                        .append(
                                switch (p.getTokenConsumption()) {
                                    case SELF -> "arg" + p.getPosition();
                                    case NEXT -> "arg" + p.getPosition() + " arg" + (p.getPosition() + 1);
                                    case DRAIN_RAW -> "arg" + p.getPosition() + "...";
                                    case DRAIN_CONSUME -> "arg" + p.getPosition() + " ... argN";
                                })
                        .append(p.isRequired() ? " (required)" : "")
                        .append(" : ")
                        .append(p.getDescription());
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
            if (position == positionalSpec.getPosition()) {
                return positionalSpec;
            }
        }
        throw new IllegalArgumentException("Unsupported positional argument at index %d".formatted(position));
    }

    private abstract static class ArgSpec {
        private final String description;
        private final boolean required;
        private final TokenConsumption tokenConsumption;

        protected ArgSpec(String description, boolean required, TokenConsumption tokenConsumption) {
            this.description = Objects.requireNonNull(description, "description");
            this.required = required;
            this.tokenConsumption = Objects.requireNonNull(tokenConsumption, "tokenConsumption");
        }

        public String getDescription() {
            return description;
        }

        public boolean isRequired() {
            return required;
        }

        public TokenConsumption getTokenConsumption() {
            return tokenConsumption;
        }
    }

    private class FlagSpec<F> extends ArgSpec {
        private final BiConsumer<T, F> propertiesSetter;
        private final Function<String, F> propertyMapper;
        private final char shortKey;
        private final String longKey;

        public FlagSpec(
                char shortKey,
                String longKey,
                boolean takesValue,
                BiConsumer<T, F> propertiesSetter,
                Function<String, F> propertyMapper,
                String description,
                boolean required) {
            super(description, required, takesValue ? TokenConsumption.NEXT : TokenConsumption.SELF);
            this.propertiesSetter = Objects.requireNonNull(propertiesSetter, "propertiesSetter");
            this.propertyMapper = Objects.requireNonNull(propertyMapper, "propertyMapper");
            this.shortKey = shortKey;
            this.longKey = Objects.requireNonNull(longKey, "longKey");
        }

        public void apply(String value, T properties) {
            propertiesSetter.accept(properties, propertyMapper.apply(value));
        }
    }

    private class PositionalSpec<F> extends ArgSpec {
        private final BiConsumer<T, F> propertiesSetter;
        private final Function<String, F> propertyMapper;
        private final int position;

        public PositionalSpec(
                int position,
                BiConsumer<T, F> propertiesSetter,
                Function<String, F> propertyMapper,
                String description,
                boolean required,
                TokenConsumption tokenConsumption) {
            super(description, required, tokenConsumption);
            this.propertiesSetter = Objects.requireNonNull(propertiesSetter, "propertiesSetter");
            this.propertyMapper = Objects.requireNonNull(propertyMapper, "propertyMapper");
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

        public void apply(String value, T properties) {
            propertiesSetter.accept(properties, propertyMapper.apply(value));
        }
    }

    private class GreedyCollectionPositionalSpec<C extends Collection<String>> extends PositionalSpec<C> {
        private final Supplier<C> collectionSupplier;
        private final Function<T, C> propertiesGetter;
        private final BiConsumer<T, C> propertiesSetter;

        public GreedyCollectionPositionalSpec(
                int position,
                Supplier<C> collectionSupplier,
                Function<T, C> propertiesGetter,
                BiConsumer<T, C> propertiesSetter,
                String description,
                boolean required) {
            // pass a fake mapper to the parent to satisfy the constructor, we override apply so it
            // doesn't matter
            super(position, propertiesSetter, unused -> null, description, required, TokenConsumption.DRAIN_CONSUME);
            this.collectionSupplier = Objects.requireNonNull(collectionSupplier, "collectionSupplier");
            this.propertiesGetter = Objects.requireNonNull(propertiesGetter, "propertiesGetter");
            this.propertiesSetter = Objects.requireNonNull(propertiesSetter, "propertiesSetter");
        }

        @Override
        public void apply(String value, T properties) {
            C collection = propertiesGetter.apply(properties);
            if (collection == null) {
                collection = collectionSupplier.get();
            }
            collection.add(value);
            propertiesSetter.accept(properties, collection);
        }
    }

    private enum TokenConsumption {
        SELF,
        NEXT,
        DRAIN_RAW,
        DRAIN_CONSUME
    }
}
