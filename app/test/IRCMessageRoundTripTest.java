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
                // Basic messages
                Arguments.of("PING :12345", "PING :12345"),
                Arguments.of("PONG :12345", "PONG :12345"),
                Arguments.of("PONG irc.example.com :12345", "PONG irc.example.com :12345"),
                Arguments.of("JOIN 0", "JOIN 0"),
                Arguments.of("JOIN #chan", "JOIN #chan"),
                Arguments.of("JOIN #chan1,#chan2", "JOIN #chan1,#chan2"),
                Arguments.of("JOIN #chan1,#chan2 key1,key2", "JOIN #chan1,#chan2 key1,key2"),
                Arguments.of("PART #chan", "PART #chan"),
                Arguments.of("PART #chan1,#chan2", "PART #chan1,#chan2"),
                Arguments.of("PART #chan :bye", "PART #chan :bye"),
                Arguments.of("NICK newnick", "NICK newnick"),
                Arguments.of("PASS whatever", "PASS whatever"),
                Arguments.of("PRIVMSG #chan :hello world", "PRIVMSG #chan :hello world"),
                Arguments.of("USER myuser 0 * :Real Name", "USER myuser 0 * :Real Name"),
                Arguments.of("USER myuser x y :Real Name", "USER myuser 0 * :Real Name"),
                Arguments.of("001 mynick :Welcome to IRC", "001 mynick :Welcome to IRC"),
                Arguments.of("353 mynick = #chan :nick1", "353 mynick = #chan :nick1"),
                Arguments.of("353 mynick = #chan :nick1 nick2", "353 mynick = #chan :nick1 nick2"),
                Arguments.of("353 mynick = #chan :@nick1 +nick2 %nick3", "353 mynick = #chan :@nick1 +nick2 %nick3"),
                Arguments.of(
                        "@a=1;b=hello\\sworld :nick!user@host PRIVMSG #chan :hi there",
                        "@a=1;b=hello\\sworld :nick!user@host PRIVMSG #chan :hi there"
                ),
                Arguments.of("FOOCMD arg1 arg2", "FOOCMD arg1 arg2"),

                // --- CAP client commands ---
                // CAP LS (with version)
                Arguments.of("CAP LS 302", "CAP LS 302"),
                // CAP LIST (request)
                Arguments.of("CAP LIST", "CAP LIST"),
                // CAP REQ (enable / disable capabilities)
                Arguments.of("CAP REQ :multi-prefix sasl -batch", "CAP REQ :multi-prefix sasl -batch"),
                // CAP END
                Arguments.of("CAP END", "CAP END"),

                // --- CAP server replies / notifications ---
                // CAP <nick> ACK
                Arguments.of("CAP nick ACK :multi-prefix -batch", "CAP nick ACK :multi-prefix -batch"),
                // CAP <nick> NAK
                Arguments.of("CAP nick NAK :multi-prefix -batch", "CAP nick NAK :multi-prefix -batch"),
                // CAP <nick> NEW
                Arguments.of("CAP nick NEW :multi-prefix sasl", "CAP nick NEW :multi-prefix sasl"),
                // CAP <nick> DEL
                Arguments.of("CAP nick DEL :multi-prefix sasl", "CAP nick DEL :multi-prefix sasl"),

                // CAP <nick> LS (no more, capabilities in one line)
                Arguments.of("CAP nick LS :multi-prefix sasl", "CAP nick LS :multi-prefix sasl"),
                // CAP <nick> LS * (hasMore = true)
                Arguments.of("CAP nick LS * :multi-prefix sasl", "CAP nick LS * :multi-prefix sasl"),

                // CAP <nick> LIST (no more)
                Arguments.of("CAP nick LIST :multi-prefix sasl", "CAP nick LIST :multi-prefix sasl"),
                // CAP <nick> LIST * (hasMore = true)
                Arguments.of("CAP nick LIST * :multi-prefix sasl", "CAP nick LIST * :multi-prefix sasl")
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
                // Basic messages
                Arguments.of("PING :12345", IRCMessagePING.class),
                Arguments.of("PONG :12345", IRCMessagePONG.class),
                Arguments.of("JOIN 0", IRCMessageJOIN0.class),
                Arguments.of("JOIN #chan", IRCMessageJOINNormal.class),
                Arguments.of("JOIN #chan1,#chan2 key1,key2", IRCMessageJOINNormal.class),
                Arguments.of("PART #chan", IRCMessagePART.class),
                Arguments.of("PART #chan1,#chan2 :bye", IRCMessagePART.class),
                Arguments.of("NICK newnick", IRCMessageNICK.class),
                Arguments.of("PASS whatever", IRCMessagePASS.class),
                Arguments.of("PRIVMSG #chan :hi", IRCMessagePRIVMSG.class),
                Arguments.of("USER myuser 0 * :Real Name", IRCMessageUSER.class),
                Arguments.of("001 mynick :Welcome", IRCMessage001.class),
                Arguments.of("353 mynick = #chan :@nick1 +nick2 %nick3", IRCMessage353.class),
                Arguments.of("FOOCMD arg1 arg2", IRCMessageUnsupported.class),

                // --- CAP client commands ---
                Arguments.of("CAP LS 302", IRCMessageCAPLS.class),
                Arguments.of("CAP LIST", IRCMessageCAPLIST.class),
                Arguments.of("CAP REQ :multi-prefix sasl -batch", IRCMessageCAPREQ.class),
                Arguments.of("CAP END", IRCMessageCAPEND.class),

                // --- CAP server replies / notifications ---
                Arguments.of("CAP nick ACK :multi-prefix -batch", IRCMessageCAPACK.class),
                Arguments.of("CAP nick NAK :multi-prefix -batch", IRCMessageCAPNAK.class),
                Arguments.of("CAP nick NEW :multi-prefix sasl", IRCMessageCAPNEW.class),
                Arguments.of("CAP nick DEL :multi-prefix sasl", IRCMessageCAPDEL.class),

                Arguments.of("CAP nick LS :multi-prefix sasl", IRCMessageCAPLS.class),
                Arguments.of("CAP nick LS * :multi-prefix sasl", IRCMessageCAPLS.class),

                Arguments.of("CAP nick LIST :multi-prefix sasl", IRCMessageCAPLIST.class),
                Arguments.of("CAP nick LIST * :multi-prefix sasl", IRCMessageCAPLIST.class)
        );
    }
}
