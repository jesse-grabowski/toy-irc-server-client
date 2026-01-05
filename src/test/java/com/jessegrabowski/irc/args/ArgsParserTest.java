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

import static org.junit.jupiter.api.Assertions.*;

import com.jessegrabowski.irc.network.Port;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ArgsParserTest {

    private final ArgsTokenizer tokenizer = new ArgsTokenizer();

    private ArgsParserBuilder<TestProps> newParser() {
        return ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]");
    }

    private TestProps parse(ArgsParserBuilder<TestProps> builder, String... args)
            throws ArgsParserHelpRequestedException {
        String raw = tokenizer.toSyntheticRaw(args);
        List<ArgsToken> tokens = tokenizer.tokenize(args);
        ArgsParser<TestProps> parser = builder.build();
        return parser.parse(raw, tokens);
    }

    @Test
    void parsesBooleanShortFlag() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser =
                newParser().addBooleanFlag('v', "verbose", TestProps::setVerbose, "verbose mode", false);

        TestProps props = parse(parser, "-v");

        assertTrue(props.verbose);
    }

    @Test
    void parsesBooleanLongFlag() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser =
                newParser().addBooleanFlag('v', "verbose", TestProps::setVerbose, "verbose mode", false);

        TestProps props = parse(parser, "--verbose");

        assertTrue(props.verbose);
    }

    @Test
    void parsesStringFlagWithShortName() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringFlag('o', "output", TestProps::setOutput, "output file", false);

        TestProps props = parse(parser, "-o", "out.txt");

        assertEquals("out.txt", props.output);
    }

    @Test
    void parsesStringFlagWithLongName() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringFlag('o', "output", TestProps::setOutput, "output file", false);

        TestProps props = parse(parser, "--output", "out.txt");

        assertEquals("out.txt", props.output);
    }

    @Test
    void parsesIntegerFlag() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser =
                newParser().addIntegerFlag('n', "count", TestProps::setCount, "count", false);

        TestProps props = parse(parser, "--count", "42");

        assertEquals(42, props.count);
    }

    @Test
    void parsesPortFlag() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = newParser().addPortFlag('p', "port", TestProps::setPort, "port", false);

        TestProps props = parse(parser, "--port", "42");

        assertEquals(new Port.FixedPort(42), props.port);
    }

    @Test
    void parsesCharsetFlag() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser =
                newParser().addCharsetFlag('c', "charset", TestProps::setCharset, "charset", false);

        TestProps props = parse(parser, "--charset", "UTF-8");

        assertEquals(StandardCharsets.UTF_8, props.charset);
    }

    @Test
    void parsesGroupedBooleanFlags() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = newParser()
                .addBooleanFlag('a', "flag-a", ArgsParserTest::noop, "a", false)
                .addBooleanFlag('b', "flag-b", ArgsParserTest::noop, "b", false)
                .addBooleanFlag('c', "flag-c", TestProps::setVerbose, "c", false);

        TestProps props = parse(parser, "-abc");

        assertTrue(props.verbose); // last one sets verbose, mainly checking it doesn't throw
    }

    @Test
    void groupedFlagsAllowSingleValueTakingFlag() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", TestProps::setVerbose, "verbose", false)
                .addIntegerFlag('n', "count", TestProps::setCount, "count", false);

        TestProps props = parse(parser, "-vn", "10");

        assertTrue(props.verbose);
        assertEquals(10, props.count);
    }

    @Test
    void groupedFlagsWithMultipleValueTakingFlagsThrow() {
        ArgsParserBuilder<TestProps> parser = newParser()
                .addStringFlag('o', "output", TestProps::setOutput, "output", false)
                .addIntegerFlag('n', "count", TestProps::setCount, "count", false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser, "-on", "value"));

        assertTrue(ex.getMessage().contains("multiple value-taking flags"));
    }

    @Test
    void missingRequiredFlagThrows() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringFlag('o', "output", TestProps::setOutput, "output", true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser));

        assertTrue(ex.getMessage().contains("Missing required flag: '--output'"));
    }

    @Test
    void missingRequiredPositionalThrows() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringPositional(0, TestProps::setInput, "input file", true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser));

        assertTrue(ex.getMessage().contains("Missing required positional argument"));
    }

    @Test
    void parsesPositionalArgument() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringPositional(0, TestProps::setInput, "input file", true);

        TestProps props = parse(parser, "input.txt");

        assertEquals("input.txt", props.input);
    }

    @Test
    void unsupportedExtraPositionalThrows() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringPositional(0, TestProps::setInput, "input file", true);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> parse(parser, "file1", "file2"));

        assertTrue(ex.getMessage().contains("Unsupported positional argument"));
    }

    @Test
    void helpFlagThrowsHelpRequestedException_long() {
        ArgsParserBuilder<TestProps> parser = newParser();

        assertThrows(ArgsParserHelpRequestedException.class, () -> parse(parser, "--help"));
    }

    @Test
    void helpFlagThrowsHelpRequestedException_short() {
        ArgsParserBuilder<TestProps> parser = newParser();

        assertThrows(ArgsParserHelpRequestedException.class, () -> parse(parser, "-h"));
    }

    @Test
    void helpFlagThrowsIllegalArgumentExceptionWhenDisabled_long() {
        ArgsParserBuilder<TestProps> parser = ArgsParser.builder(TestProps::new, false, "test parser");

        assertThrows(IllegalArgumentException.class, () -> parse(parser, "--help"));
    }

    @Test
    void helpFlagThrowsIllegalArgumentExceptionWhenDisabled_short() {
        ArgsParserBuilder<TestProps> parser = ArgsParser.builder(TestProps::new, false, "test parser");

        assertThrows(IllegalArgumentException.class, () -> parse(parser, "-h"));
    }

    @Test
    void helpTextForComplexConfiguration_isExactlyAsExpected() {
        ArgsParser<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", TestProps::setVerbose, "enable verbose mode", false)
                .addBooleanFlag('q', "quiet", ArgsParserTest::noop, "suppress all output", false)
                .addStringFlag('o', "output", TestProps::setOutput, "path to output file", true)
                .addIntegerFlag('n', "count", TestProps::setCount, "number of items to process", false)
                .addIntegerFlag('t', "timeout", ArgsParserTest::noop, "timeout in seconds", true)
                .addStringPositional(0, TestProps::setInput, "primary input file", true)
                .addStringPositional(1, ArgsParserTest::noop, "secondary input file", false)
                .addStringPositional(2, ArgsParserTest::noop, "log directory", false)
                .build();

        String help = parser.getHelpText();

        String expected = """
            test parser

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
            \targ2 : log directory""";

        assertEquals(expected, help);
    }

    @Test
    void helpTextForMsgCommandWithGreedyMessage_isExactlyAsExpected() {
        ArgsParser<TestProps> parser = ArgsParser.builder(TestProps::new, true, "/msg: send a message")
                .addUsageExample("/msg [-server <name>] <target> <message>")
                .addStringFlag('s', "server", ArgsParserTest::noop, "server name", false)
                .addStringPositional(0, (p, v) -> p.input = v, "target nick or channel", true)
                .addGreedyStringPositional(1, ArgsParserTest::noop, "message text", true)
                .build();

        String help = parser.getHelpText();

        String expected = """
            /msg: send a message

            Usage:
            \t/msg [-server <name>] <target> <message>

            Options:
            \t-s, --server <value> : server name

            Positionals:
            \targ0 (required) : target nick or channel
            \targ1... (required) : message text""";

        assertEquals(expected, help);
    }

    @Test
    void helpTextForPartCommandWithGreedyChannelList_isExactlyAsExpected() {
        ArgsParser<TestProps> parser = ArgsParser.builder(TestProps::new, true, "/part: leave one or more channels")
                .addUsageExample("/part [-quiet] <server> <channel> [<channel> ...]")
                .addBooleanFlag('q', "quiet", TestProps::setVerbose, "do not send PART messages", false)
                .addStringPositional(0, TestProps::setInput, "server name", true)
                .addGreedyListPositional(1, p -> p.extraArgs, TestProps::setExtraArgs, "channel names", false)
                .build();

        String help = parser.getHelpText();

        String expected = """
            /part: leave one or more channels

            Usage:
            \t/part [-quiet] <server> <channel> [<channel> ...]

            Options:
            \t-q, --quiet : do not send PART messages

            Positionals:
            \targ0 (required) : server name
            \targ1 ... argN : channel names""";

        assertEquals(expected, help);
    }

    @Test
    void doubleDashStopsFlagParsing() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = newParser()
                .addBooleanFlag('v', "verbose", TestProps::setVerbose, "verbose", false)
                .addStringPositional(0, TestProps::setInput, "input file", true);

        TestProps props = parse(parser, "-v", "--", "-notAFlag");

        assertTrue(props.verbose);
        assertEquals("-notAFlag", props.input);
    }

    @Test
    void unknownShortFlagThrows() {
        ArgsParserBuilder<TestProps> parser = newParser();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser, "-x"));

        assertTrue(ex.getMessage().contains("Unrecognized flag: '-x'"));
    }

    @Test
    void unknownLongFlagThrows() {
        ArgsParserBuilder<TestProps> parser = newParser();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser, "--unknown"));

        assertTrue(ex.getMessage().contains("Unrecognized flag: '--unknown'"));
    }

    @Test
    void missingValueForShortFlagThrows() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringFlag('o', "output", TestProps::setOutput, "output", false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser, "-o"));

        assertTrue(ex.getMessage().contains("Illegal value for flag '-o'"));
    }

    @Test
    void missingValueForLongFlagThrows() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addStringFlag('o', "output", TestProps::setOutput, "output", false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser, "--output"));

        assertTrue(ex.getMessage().contains("Illegal value for flag '--output'"));
    }

    @Test
    void badIntegerValueIsWrappedWithContext() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addIntegerFlag('n', "count", TestProps::setCount, "count", false);

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> parse(parser, "--count", "not-a-number"));

        assertTrue(ex.getMessage().contains("Illegal value for flag '--count'"));
        assertTrue(ex.getMessage().contains("expected integer but got 'not-a-number'"));
        assertNotNull(ex.getCause());
    }

    @Test
    void badPositionalValueIsWrappedWithContext() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addInetAddressPositional(0, ArgsParserTest::noop, "input file", true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> parse(parser, "|"));

        assertTrue(ex.getMessage().contains("Illegal value at index 0"));
        assertTrue(ex.getMessage().contains("failed to resolve hostname"));
        assertNotNull(ex.getCause());
    }

    @Test
    void duplicateShortFlagRegistrationThrows() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addBooleanFlag('v', "verbose", ArgsParserTest::noop, "verbose", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.addBooleanFlag('v', "version", ArgsParserTest::noop, "version", false));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void duplicateLongFlagRegistrationThrows() {
        ArgsParserBuilder<TestProps> parser =
                newParser().addBooleanFlag('v', "verbose", ArgsParserTest::noop, "verbose", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.addBooleanFlag('q', "verbose", ArgsParserTest::noop, "another verbose", false));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void duplicatePositionalRegistrationThrows() {
        ArgsParserBuilder<TestProps> parser = newParser().addStringPositional(0, TestProps::setInput, "input", false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.addStringPositional(0, ArgsParserTest::noop, "another", false));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void buildFailsWhenPositionalIndicesAreNotContiguous_singleGap() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]")
                .addStringPositional(1, TestProps::setInput, "input file", true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(ex.getMessage().contains("Positional arguments do not span all positions"));
    }

    @Test
    void buildFailsWhenPositionalIndicesAreNotContiguous_multipleGaps() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]")
                .addStringPositional(0, ArgsParserTest::noop, "arg0", false)
                .addStringPositional(2, ArgsParserTest::noop, "arg2", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(ex.getMessage().contains("Positional arguments do not span all positions"));
    }

    @Test
    void buildSucceedsWhenPositionalIndicesAreContiguous() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]")
                .addStringPositional(0, ArgsParserTest::noop, "arg0", false)
                .addStringPositional(1, ArgsParserTest::noop, "arg1", false)
                .addStringPositional(2, ArgsParserTest::noop, "arg2", false);

        // Should not throw
        ArgsParser<TestProps> parser = builder.build();
        assertNotNull(parser);
    }

    @Test
    void buildFailsWhenGreedyStringPositionalIsNotLast() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]")
                .addGreedyStringPositional(0, TestProps::setInput, "greedy", false)
                .addStringPositional(1, ArgsParserTest::noop, "non-greedy", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(ex.getMessage().contains("Greedy positional arguments may only be at the final position"));
    }

    @Test
    void buildFailsWhenGreedyListPositionalIsNotLast() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]")
                .addStringPositional(0, ArgsParserTest::noop, "arg0", false)
                .addGreedyListPositional(1, p -> p.extraArgs, TestProps::setExtraArgs, "greedy list", false)
                .addStringPositional(2, ArgsParserTest::noop, "arg2", false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(ex.getMessage().contains("Greedy positional arguments may only be at the final position"));
    }

    @Test
    void buildSucceedsWhenGreedyStringPositionalIsLast() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]")
                .addStringPositional(0, ArgsParserTest::noop, "arg0", false)
                .addGreedyStringPositional(1, TestProps::setInput, "greedy", false);

        ArgsParser<TestProps> parser = builder.build();
        assertNotNull(parser);
    }

    @Test
    void buildSucceedsWhenGreedyListPositionalIsLast() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("java TestProps [options] [args]")
                .addStringPositional(0, ArgsParserTest::noop, "arg0", false)
                .addGreedyListPositional(1, p -> p.extraArgs, TestProps::setExtraArgs, "greedy list", false);

        ArgsParser<TestProps> parser = builder.build();
        assertNotNull(parser);
    }

    @Test
    void parsesMsgCommandWithGreedyMessage() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = ArgsParser.builder(TestProps::new, true, "/msg: send a message")
                .addUsageExample("/msg [-server <name>] <target> <message>")
                .addStringFlag('s', "server", TestProps::setOutput, "server name", false)
                .addStringPositional(0, TestProps::setInput, "target nick or channel", true)
                .addGreedyStringPositional(1, TestProps::setMessage, "message text", true);

        TestProps props = parse(parser, "-s", "irc.example.org", "nick", "hello", "there", "world");

        assertEquals("irc.example.org", props.output, "server should come from -s/--server");
        assertEquals("nick", props.input, "target should be first positional");
        assertEquals("hello there world", props.message, "greedy message should capture the rest of the line");
    }

    @Test
    void parsesMsgCommandWithGreedyQuotedMessage() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = ArgsParser.builder(TestProps::new, true, "/msg: send a message")
                .addUsageExample("/msg [-server <name>] <target> <message>")
                .addStringFlag('s', "server", TestProps::setOutput, "server name", false)
                .addStringPositional(0, TestProps::setInput, "target nick or channel", true)
                .addGreedyStringPositional(1, TestProps::setMessage, "message text", true);

        TestProps props = parse(parser, "-s", "irc.example.org", "nick", "\"hello there world\"");

        assertEquals("irc.example.org", props.output, "server should come from -s/--server");
        assertEquals("nick", props.input, "target should be first positional");
        assertEquals("\"hello there world\"", props.message, "greedy message should capture the rest of the line");
    }

    @Test
    void parsesPartCommandWithGreedyChannelList() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = ArgsParser.builder(
                        TestProps::new, true, "/part: leave one or more channels")
                .addUsageExample("/part [-quiet] <server> <channel> [<channel> ...]")
                .addBooleanFlag('q', "quiet", TestProps::setVerbose, "do not send PART messages", false)
                .addStringPositional(0, TestProps::setInput, "server name", true)
                .addGreedyListPositional(1, p -> p.extraArgs, TestProps::setExtraArgs, "channel names", true);

        TestProps props = parse(parser, "-q", "irc.example.org", "#weechat", "#random", "#offtopic");

        assertTrue(props.verbose, "quiet flag should flip verbose field for this test");
        assertEquals("irc.example.org", props.input, "server should be first positional");
        assertNotNull(props.extraArgs, "extraArgs should be initialized");
        assertEquals(
                List.of("#weechat", "#random", "#offtopic"),
                props.extraArgs,
                "greedy list should contain all channels in order");
    }

    @Test
    void parsesCommaSeparatedListPositional() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = ArgsParser.builder(TestProps::new, true, "/join: join channels")
                .addUsageExample("/join <channel>[,<channel>...]")
                .addCommaSeparatedListPositional(0, TestProps::setExtraArgs, "comma-separated channel list", true);

        TestProps props = parse(parser, "#weechat,#random,#offtopic");

        assertNotNull(props.extraArgs, "extraArgs should be initialized");
        assertEquals(List.of("#weechat", "#random", "#offtopic"), props.extraArgs);
    }

    @Test
    void flagParsingDisabledTreatsHyphenTokensAsPositionals() throws ArgsParserHelpRequestedException {
        ArgsParserBuilder<TestProps> parser = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("MODE <target> <modes> [args...]")
                .setFlagParsingEnabled(false)
                .addStringPositional(0, TestProps::setInput, "command name", true)
                .addGreedyListPositional(1, p -> p.extraArgs, TestProps::setExtraArgs, "command arguments", false);

        TestProps props = parse(parser, "MODE", "-ov", "nick1", "nick2");

        assertEquals("MODE", props.input, "first positional should be the command name");
        assertNotNull(props.extraArgs, "extraArgs should be initialized");
        assertEquals(
                List.of("-ov", "nick1", "nick2"),
                props.extraArgs,
                "all remaining tokens, including those starting with '-', should be positional");
    }

    @Test
    void buildFailsWhenFlagParsingDisabledButFlagsAreDefined() {
        ArgsParserBuilder<TestProps> builder = newParser()
                .addBooleanFlag('v', "verbose", TestProps::setVerbose, "verbose", false)
                .setFlagParsingEnabled(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, builder::build);

        assertTrue(ex.getMessage().contains("Flag parsing is disabled but flag specs are defined"));
    }

    @Test
    void helpFlagStillWorksWhenFlagParsingDisabled_long() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("cmd [args]")
                .setFlagParsingEnabled(false)
                .addGreedyStringPositional(0, TestProps::setInput, "rest", false);

        assertThrows(ArgsParserHelpRequestedException.class, () -> parse(builder, "--help"));
    }

    @Test
    void helpFlagStillWorksWhenFlagParsingDisabled_short() {
        ArgsParserBuilder<TestProps> builder = ArgsParser.builder(TestProps::new, true, "test parser")
                .addUsageExample("cmd [args]")
                .setFlagParsingEnabled(false)
                .addGreedyStringPositional(0, TestProps::setInput, "rest", false);

        assertThrows(ArgsParserHelpRequestedException.class, () -> parse(builder, "-h"));
    }

    @ParameterizedTest(name = "port={0}")
    @CsvSource({"-1-200", "1-200-", "-50", "70000"})
    void badPortRangeThrowsCorrectException(String value) {
        ArgsParserBuilder<TestProps> parser = newParser().addPortFlag('p', "port", TestProps::setPort, "port", false);

        assertThrows(IllegalArgumentException.class, () -> parse(parser, "--port", value));
    }

    private static class TestProps implements ArgsProperties {
        boolean verbose;
        String output;
        Integer count;
        String input;
        String message;
        List<String> extraArgs;
        Charset charset;
        Port port;

        @Override
        public void validate() {}

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setExtraArgs(List<String> extraArgs) {
            this.extraArgs = extraArgs;
        }

        public void setCharset(Charset charset) {
            this.charset = charset;
        }

        public void setPort(Port port) {
            this.port = port;
        }
    }

    private static void noop(Object p, Object v) {}
}
