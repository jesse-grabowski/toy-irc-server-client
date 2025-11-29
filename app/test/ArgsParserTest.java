import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgsParserTest {

    private static class TestProps implements ArgsProperties {
        boolean verbose;
        String output;
        Integer count;
        String input;

        @Override
        public void validate() {
            // no interesting behaviour here
        }
    }

    private ArgsParser<TestProps> newParser() {
        return new ArgsParser<>(TestProps.class, TestProps::new);
    }

    @Test
    void parsesBooleanShortFlag() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", (p, v) -> p.verbose = v, "verbose mode", false);

        TestProps props = parser.parse(new String[]{"-v"});

        assertTrue(props.verbose);
    }

    @Test
    void parsesBooleanLongFlag() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", (p, v) -> p.verbose = v, "verbose mode", false);

        TestProps props = parser.parse(new String[]{"--verbose"});

        assertTrue(props.verbose);
    }

    @Test
    void parsesStringFlagWithShortName() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addStringFlag('o', "output", (p, v) -> p.output = v, "output file", false);

        TestProps props = parser.parse(new String[]{"-o", "out.txt"});

        assertEquals("out.txt", props.output);
    }

    @Test
    void parsesStringFlagWithLongName() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addStringFlag('o', "output", (p, v) -> p.output = v, "output file", false);

        TestProps props = parser.parse(new String[]{"--output", "out.txt"});

        assertEquals("out.txt", props.output);
    }

    @Test
    void parsesIntegerFlag() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addIntegerFlag('n', "count", (p, v) -> p.count = v, "count", false);

        TestProps props = parser.parse(new String[]{"--count", "42"});

        assertEquals(42, props.count);
    }

    @Test
    void parsesGroupedBooleanFlags() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('a', "flag-a", (p, v) -> {}, "a", false)
                .addBooleanFlag('b', "flag-b", (p, v) -> {}, "b", false)
                .addBooleanFlag('c', "flag-c", (p, v) -> p.verbose = v, "c", false);

        TestProps props = parser.parse(new String[]{"-abc"});

        assertTrue(props.verbose); // last one sets verbose, mainly checking it doesn't throw
    }

    @Test
    void groupedFlagsAllowSingleValueTakingFlag() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", (p, v) -> p.verbose = v, "verbose", false)
                .addIntegerFlag('n', "count", (p, v) -> p.count = v, "count", false);

        TestProps props = parser.parse(new String[]{"-vn", "10"});

        assertTrue(props.verbose);
        assertEquals(10, props.count);
    }

    @Test
    void groupedFlagsWithMultipleValueTakingFlagsThrow() {
        ArgsParser<TestProps> parser = newParser()
                .addStringFlag('o', "output", (p, v) -> p.output = v, "output", false)
                .addIntegerFlag('n', "count", (p, v) -> p.count = v, "count", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"-on", "value"})
        );

        assertTrue(ex.getMessage().contains("multiple value-taking flags"));
    }

    @Test
    void missingRequiredFlagThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addStringFlag('o', "output", (p, v) -> p.output = v, "output", true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{})
        );

        assertTrue(ex.getMessage().contains("Missing required flag: '--output'"));
    }

    @Test
    void missingRequiredPositionalThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addStringPositional(0, (p, v) -> p.input = v, "input file", true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{})
        );

        assertTrue(ex.getMessage().contains("Missing required positional argument"));
    }

    @Test
    void parsesPositionalArgument() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addStringPositional(0, (p, v) -> p.input = v, "input file", true);

        TestProps props = parser.parse(new String[]{"input.txt"});

        assertEquals("input.txt", props.input);
    }

    @Test
    void unsupportedExtraPositionalThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addStringPositional(0, (p, v) -> p.input = v, "input file", true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"file1", "file2"})
        );

        assertTrue(ex.getMessage().contains("Unsupported positional argument"));
    }

    @Test
    void helpFlagThrowsHelpRequestedException_long() {
        ArgsParser<TestProps> parser = newParser();

        assertThrows(
                ArgsParserHelpRequestedException.class,
                () -> parser.parse(new String[]{"--help"})
        );
    }

    @Test
    void helpFlagThrowsHelpRequestedException_short() {
        ArgsParser<TestProps> parser = newParser();

        assertThrows(
                ArgsParserHelpRequestedException.class,
                () -> parser.parse(new String[]{"-h"})
        );
    }

    @Test
    void helpTextForComplexConfiguration_isExactlyAsExpected() {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", (p, v) -> p.verbose = v,
                        "enable verbose mode", false)
                .addBooleanFlag('q', "quiet", (p, v) -> { /* ignore for test */ },
                        "suppress all output", false)
                .addStringFlag('o', "output", (p, v) -> p.output = v,
                        "path to output file", true)
                .addIntegerFlag('n', "count", (p, v) -> p.count = v,
                        "number of items to process", false)
                .addIntegerFlag('t', "timeout", (p, v) -> { /* ignore */ },
                        "timeout in seconds", true)
                .addStringPositional(0, (p, v) -> p.input = v,
                        "primary input file", true)
                .addStringPositional(1, (p, v) -> { /* ignore */ },
                        "secondary input file", false)
                .addStringPositional(2, (p, v) -> { /* ignore */ },
                        "log directory", false);

        String help = parser.getHelpText();

        String expected = """
            Usage:
            \tjava TestProps [options] [args]

            Options:
            \t-v, --verbose : enable verbose mode
            \t-q, --quiet : suppress all output
            \t-o, --output <value> (required) : path to output file
            \t-n, --count <value> : number of items to process
            \t-t, --timeout <value> (required) : timeout in seconds

            Positionals:
            \targ0 (required) : primary input file
            \targ1 : secondary input file
            \targ2 : log directory
            """;

        assertEquals(expected, help);
    }

    @Test
    void doubleDashStopsFlagParsing() throws ArgsParserHelpRequestedException {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", (p, v) -> p.verbose = v, "verbose", false)
                .addStringPositional(0, (p, v) -> p.input = v, "input file", true);

        TestProps props = parser.parse(new String[]{"-v", "--", "-notAFlag"});

        assertTrue(props.verbose);
        assertEquals("-notAFlag", props.input); // treated as positional, not a flag
    }

    @Test
    void unknownShortFlagThrows() {
        ArgsParser<TestProps> parser = newParser();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"-x"})
        );

        assertTrue(ex.getMessage().contains("Unrecognized flag: '-x'"));
    }

    @Test
    void unknownLongFlagThrows() {
        ArgsParser<TestProps> parser = newParser();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--unknown"})
        );

        assertTrue(ex.getMessage().contains("Unrecognized flag: '--unknown'"));
    }

    @Test
    void missingValueForShortFlagThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addStringFlag('o', "output", (p, v) -> p.output = v, "output", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"-o"})
        );

        assertTrue(ex.getMessage().contains("Missing expected value for flag: '-o"));
    }

    @Test
    void missingValueForLongFlagThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addStringFlag('o', "output", (p, v) -> p.output = v, "output", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--output"})
        );

        assertTrue(ex.getMessage().contains("Missing expected value for flag: '--output"));
    }

    @Test
    void badIntegerValueIsWrappedWithContext() {
        ArgsParser<TestProps> parser = newParser()
                .addIntegerFlag('n', "count", (p, v) -> p.count = v, "count", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"--count", "not-a-number"})
        );

        assertTrue(ex.getMessage().contains("Illegal value for flag '--count'"));
        assertTrue(ex.getMessage().contains("expected integer but got 'not-a-number'"));
        assertNotNull(ex.getCause());
    }

    @Test
    void badPositionalValueIsWrappedWithContext() {
        ArgsParser<TestProps> parser = newParser()
                .addInetAddressPositional(0, (p, v) -> p.input = v.toString(), "input file", true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(new String[]{"|"})
        );

        assertTrue(ex.getMessage().contains("Illegal value at index 0"));
        assertTrue(ex.getMessage().contains("failed to resolve hostname"));
        assertNotNull(ex.getCause());
    }

    @Test
    void duplicateShortFlagRegistrationThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", (p, v) -> {}, "verbose", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.addBooleanFlag('v', "version", (p, v) -> {}, "version", false)
        );

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void duplicateLongFlagRegistrationThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", (p, v) -> {}, "verbose", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.addBooleanFlag('q', "verbose", (p, v) -> {}, "another verbose", false)
        );

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void duplicatePositionalRegistrationThrows() {
        ArgsParser<TestProps> parser = newParser()
                .addStringPositional(0, (p, v) -> p.input = v, "input", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.addStringPositional(0, (p, v) -> {}, "another", false)
        );

        assertTrue(ex.getMessage().contains("already exists"));
    }
}
