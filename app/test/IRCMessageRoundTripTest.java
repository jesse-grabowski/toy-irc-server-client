import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class IRCMessageRoundTripTest {

    private final IRCMessageMarshaller marshaller = new IRCMessageMarshaller();
    private final IRCMessageUnmarshaller unmarshaller = new IRCMessageUnmarshaller();

    @ParameterizedTest(name = "{index}: round-trip \"{0}\" -> \"{1}\"")
    @MethodSource("rawMessages")
    void roundTripRawMessages(String input, String expected) {
        IRCMessage parsed = unmarshaller.unmarshal(input);
        String marshalled = marshaller.marshal(parsed);

        assertEquals(expected, marshalled);
    }

    static Stream<Arguments> rawMessages() {
        return Stream.of(
                Arguments.of("PING :12345", "PING :12345"),
                Arguments.of("PONG :12345", "PONG :12345"),
                Arguments.of("PONG irc.example.com :12345", "PONG irc.example.com :12345"),
                Arguments.of("JOIN 0", "JOIN 0"),
                Arguments.of("JOIN #chan", "JOIN #chan"),
                Arguments.of("JOIN #chan1,#chan2", "JOIN #chan1,#chan2"),
                Arguments.of("JOIN #chan1,#chan2 key1,key2", "JOIN #chan1,#chan2 key1,key2"),
                Arguments.of("NICK newnick", "NICK newnick"),
                Arguments.of("PRIVMSG #chan :hello world", "PRIVMSG #chan :hello world"),
                Arguments.of("USER myuser 0 * :Real Name", "USER myuser 0 * :Real Name"),
                Arguments.of("USER myuser x y :Real Name", "USER myuser 0 * :Real Name"),
                Arguments.of("001 mynick :Welcome to IRC", "001 mynick :Welcome to IRC"),
                Arguments.of(
                        "@a=1;b=hello\\sworld :nick!user@host PRIVMSG #chan :hi there",
                        "@a=1;b=hello\\sworld :nick!user@host PRIVMSG #chan :hi there"
                ),
                Arguments.of("FOOCMD arg1 arg2", "FOOCMD arg1 arg2")
        );
    }

    @ParameterizedTest(name = "{index}: type for \"{0}\" is {1}")
    @MethodSource("messageTypes")
    void unmarshalProducesExpectedType(String input, Class<? extends IRCMessage> expectedType) {
        IRCMessage parsed = unmarshaller.unmarshal(input);
        assertInstanceOf(expectedType, parsed);
    }

    static Stream<Arguments> messageTypes() {
        return Stream.of(
                Arguments.of("PING :12345", IRCMessagePING.class),
                Arguments.of("PONG :12345", IRCMessagePONG.class),
                Arguments.of("JOIN 0", IRCMessageJOIN0.class),
                Arguments.of("JOIN #chan", IRCMessageJOINNormal.class),
                Arguments.of("JOIN #chan1,#chan2 key1,key2", IRCMessageJOINNormal.class),
                Arguments.of("NICK newnick", IRCMessageNICK.class),
                Arguments.of("PRIVMSG #chan :hi", IRCMessagePRIVMSG.class),
                Arguments.of("USER myuser 0 * :Real Name", IRCMessageUSER.class),
                Arguments.of("001 mynick :Welcome", IRCMessage001.class),
                Arguments.of("FOOCMD arg1 arg2", IRCMessageUnsupported.class)
        );
    }
}
