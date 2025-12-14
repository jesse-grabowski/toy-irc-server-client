import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class IRCMessageSerializationTest {

    private final IRCMessageMarshaller marshaller = new IRCMessageMarshaller();
    private final IRCMessageUnmarshaller unmarshaller = new IRCMessageUnmarshaller();

    @ParameterizedTest(name = "{index}: round-trip \"{0}\" -> \"{1}\"")
    @MethodSource("rawMessages")
    void roundTripRawMessages(String input, String expected) {
        IRCMessage parsed = unmarshaller.unmarshal(new IRCServerParameters(), StandardCharsets.UTF_8, input);
        assertFalse(parsed instanceof IRCMessageParseError);
        assertFalse(parsed instanceof IRCMessageUnsupported);
        String marshalled = marshaller.marshal(parsed);

        assertEquals(expected, marshalled);
    }

    static Stream<Arguments> rawMessages() {
        return Stream.of(
                Arguments.of("PING :12345", "PING :12345"),
                Arguments.of("PONG :12345", "PONG :12345"),
                Arguments.of("PONG irc.example.com :12345", "PONG irc.example.com :12345"),
                Arguments.of("ERROR :Closing Link: 10.0.0.1 (Ping timeout)", "ERROR :Closing Link: 10.0.0.1 (Ping timeout)"),
                Arguments.of("JOIN 0", "JOIN 0"),
                Arguments.of("JOIN #chan", "JOIN #chan"),
                Arguments.of("JOIN #chan1,#chan2", "JOIN #chan1,#chan2"),
                Arguments.of("JOIN #chan1,#chan2 key1,key2", "JOIN #chan1,#chan2 key1,key2"),
                Arguments.of("KICK #chan nick", "KICK #chan nick"),
                Arguments.of("KICK #chan nick :I don't like you", "KICK #chan nick :I don't like you"),
                Arguments.of("MODE #chan", "MODE #chan"),
                Arguments.of("MODE #chan +ov nick1 nick2", "MODE #chan +ov nick1 nick2"),
                Arguments.of("PART #chan", "PART #chan"),
                Arguments.of("PART #chan1,#chan2", "PART #chan1,#chan2"),
                Arguments.of("PART #chan :bye", "PART #chan :bye"),
                Arguments.of("NICK newnick", "NICK newnick"),
                Arguments.of("NOTICE #chan :hello world", "NOTICE #chan :hello world"),
                Arguments.of("PASS whatever", "PASS whatever"),
                Arguments.of("PRIVMSG #chan :hello world", "PRIVMSG #chan :hello world"),
                Arguments.of("USER myuser 0 * :Real Name", "USER myuser 0 * :Real Name"),
                Arguments.of("USER myuser x y :Real Name", "USER myuser 0 * :Real Name"),
                Arguments.of(":irc.example.net 001 alice :Welcome to the Example IRC Network alice!alice@host", ":irc.example.net 001 alice :Welcome to the Example IRC Network alice!alice@host"),
                Arguments.of(":irc.example.net 002 alice :Your host is irc.example.net, running version example-1.0", ":irc.example.net 002 alice :Your host is irc.example.net, running version example-1.0"),
                Arguments.of(":irc.example.net 003 alice :This server was created Mon Dec 1 2025", ":irc.example.net 003 alice :This server was created Mon Dec 1 2025"),
                Arguments.of(":irc.example.net 004 alice irc.example.net example-1.0 iowghraAsORTVSxNCWqBzvdHtGp lvhopsmntikrRcaqOALQbSeIKVf", ":irc.example.net 004 alice irc.example.net example-1.0 iowghraAsORTVSxNCWqBzvdHtGp lvhopsmntikrRcaqOALQbSeIKVf"),
                Arguments.of(":irc.example.net 005 alice CHANTYPES=# PREFIX=(ov)@+ NETWORK=ExampleNet :are supported by this server", ":irc.example.net 005 alice CHANTYPES=# PREFIX=(ov)@+ NETWORK=ExampleNet :are supported by this server"),
                Arguments.of(":irc.example.net 010 alice irc.backup.example.net 6667 :Please connect to this server", ":irc.example.net 010 alice irc.backup.example.net 6667 :Please connect to this server"),
                Arguments.of(":irc.example.net 212 alice JOIN 12345", ":irc.example.net 212 alice JOIN 12345"),
                Arguments.of(":irc.example.net 219 alice STATS :End of STATS report", ":irc.example.net 219 alice STATS :End of /STATS report"),
                Arguments.of(":irc.example.net 221 alice +iw", ":irc.example.net 221 alice +iw"),
                Arguments.of(":irc.example.net 242 alice :Server up 5 days, 3:42:10", ":irc.example.net 242 alice :Server up 5 days, 3:42:10"),
                Arguments.of(":irc.example.net 251 alice :There are 42 users and 10 services on 3 servers", ":irc.example.net 251 alice :There are 42 users and 10 services on 3 servers"),
                Arguments.of(":irc.example.net 252 alice 5 :operator(s) online", ":irc.example.net 252 alice 5 :operator(s) online"),
                Arguments.of(":irc.example.net 253 alice 1 :unknown connection(s)", ":irc.example.net 253 alice 1 :unknown connection(s)"),
                Arguments.of(":irc.example.net 254 alice 20 :channels formed", ":irc.example.net 254 alice 20 :channels formed"),
                Arguments.of(":irc.example.net 255 alice :I have 42 clients and 3 servers", ":irc.example.net 255 alice :I have 42 clients and 3 servers"),
                Arguments.of(":irc.example.net 256 alice :Administrative info for irc.example.net", ":irc.example.net 256 alice :Administrative info"),
                Arguments.of(":irc.example.net 257 alice :Example IRC Network", ":irc.example.net 257 alice :Example IRC Network"),
                Arguments.of(":irc.example.net 258 alice :Somewhere on the Internet", ":irc.example.net 258 alice :Somewhere on the Internet"),
                Arguments.of(":irc.example.net 259 alice :admin@example.net", ":irc.example.net 259 alice :admin@example.net"),
                Arguments.of(":irc.example.net 263 alice WHO :Please wait a while and try again", ":irc.example.net 263 alice WHO :Please wait a while and try again"),
                Arguments.of(":irc.example.net 265 alice 40 50 :Current local users 40, max 50", ":irc.example.net 265 alice 40 50 :Current local users 40, max 50"),
                Arguments.of(":irc.example.net 266 alice 42 100 :Current global users 42, max 100", ":irc.example.net 266 alice 42 100 :Current global users 42, max 100"),
                Arguments.of(":irc.example.net 276 alice bob :has client certificate fingerprint ABCDEF123456", ":irc.example.net 276 alice bob :has client certificate fingerprint ABCDEF123456"),
                Arguments.of(":irc.example.net 301 alice bob :Gone to lunch", ":irc.example.net 301 alice bob :Gone to lunch"),
                Arguments.of(":irc.example.net 302 alice bob=+bob!bob@host", ":irc.example.net 302 alice :bob=+bob!bob@host"),
                Arguments.of(":irc.example.net 305 alice :You are no longer marked as away", ":irc.example.net 305 alice :You are no longer marked as away"),
                Arguments.of(":irc.example.net 306 alice :You have been marked as away", ":irc.example.net 306 alice :You have been marked as away"),
                Arguments.of(":irc.example.net 307 alice bob :is a registered nickname", ":irc.example.net 307 alice bob :has identified for this nick"),
                Arguments.of(":irc.example.net 311 alice bob bob host.example.com * :Bob Example", ":irc.example.net 311 alice bob bob host.example.com * :Bob Example"),
                Arguments.of(":irc.example.net 312 alice bob irc.example.net :Example Server", ":irc.example.net 312 alice bob irc.example.net :Example Server"),
                Arguments.of(":irc.example.net 313 alice bob :is an IRC operator", ":irc.example.net 313 alice bob :is an IRC operator"),
                Arguments.of(":irc.example.net 314 alice oldnick user oldhost * :Old Realname", ":irc.example.net 314 alice oldnick user oldhost * :Old Realname"),
                Arguments.of(":irc.example.net 315 alice bob :End of WHO list", ":irc.example.net 315 alice bob :End of WHO list"),
                Arguments.of(":irc.example.net 317 alice bob 120 1700000000 :seconds idle, signon time", ":irc.example.net 317 alice bob 120 1700000000 :120, 1970-01-20T16:13:20Z"),
                Arguments.of(":irc.example.net 318 alice bob :End of WHOIS", ":irc.example.net 318 alice bob :End of /WHOIS list"),
                Arguments.of(":irc.example.net 319 alice bob :@#chat #help", ":irc.example.net 319 alice bob :@#chat #help"),
                Arguments.of(":irc.example.net 320 alice bob :is identified to services", ":irc.example.net 320 alice bob :is identified to services"),
                Arguments.of(":irc.example.net 321 alice Channel Users Name", ":irc.example.net 321 alice Channel :Users  Name"),
                Arguments.of(":irc.example.net 322 alice #chat 12 :General discussion", ":irc.example.net 322 alice #chat 12 :General discussion"),
                Arguments.of(":irc.example.net 323 alice :End of /LIST", ":irc.example.net 323 alice :End of /LIST"),
                Arguments.of(":irc.example.net 324 alice #chat +nt", ":irc.example.net 324 alice #chat +nt"),
                Arguments.of(":irc.example.net 329 alice #chat 1700000000", ":irc.example.net 329 alice #chat 1700000000"),
                Arguments.of(":irc.example.net 330 alice bob bob_account :is logged in as", ":irc.example.net 330 alice bob bob_account :is logged in as"),
                Arguments.of(":irc.example.net 331 alice #chat :No topic is set", ":irc.example.net 331 alice #chat :No topic is set"),
                Arguments.of(":irc.example.net 332 alice #chat :Welcome to #chat", ":irc.example.net 332 alice #chat :Welcome to #chat"),
                Arguments.of(":irc.example.net 333 alice #chat bob 1700001000", ":irc.example.net 333 alice #chat bob 1700001000"),
                Arguments.of(":irc.example.net 336 alice #chat bob", ":irc.example.net 336 alice #chat"),
                Arguments.of(":irc.example.net 337 alice #chat :End of invite list", ":irc.example.net 337 alice :End of /INVITE list"),
                Arguments.of(":irc.example.net 338 alice bob bob@real.host 127.0.0.1 :actually using host", ":irc.example.net 338 alice bob bob@real.host 127.0.0.1 :is actually using host"),
                Arguments.of(":irc.example.net 341 alice bob #chat", ":irc.example.net 341 alice bob #chat"),
                Arguments.of(":irc.example.net 346 alice #chat *!*@trusted.host", ":irc.example.net 346 alice #chat *!*@trusted.host"),
                Arguments.of(":irc.example.net 347 alice #chat :End of invite exception list", ":irc.example.net 347 alice #chat :End of Channel Invite Exception List"),
                Arguments.of(":irc.example.net 348 alice #chat *!*@except.host", ":irc.example.net 348 alice #chat *!*@except.host"),
                Arguments.of(":irc.example.net 349 alice #chat :End of exception list", ":irc.example.net 349 alice #chat :End of channel exception list"),
                Arguments.of(":irc.example.net 351 alice example-1.0 irc.example.net :Modern IRCd", ":irc.example.net 351 alice example-1.0 irc.example.net :Modern IRCd"),
                Arguments.of(":irc.example.net 352 alice #chat bob host irc.example.net bob H@ :0 Bob Example", ":irc.example.net 352 alice #chat bob host irc.example.net bob H@ :0 Bob Example"),
                Arguments.of("353 mynick = #chan :nick1", "353 mynick = #chan :nick1"),
                Arguments.of("353 mynick = #chan :nick1 nick2", "353 mynick = #chan :nick1 nick2"),
                Arguments.of("353 mynick = #chan :@nick1 +nick2 %nick3", "353 mynick = #chan :@nick1 +nick2 %nick3"),
                Arguments.of(":irc.example.net 364 alice irc.example.net irc.example.net :0 Example server", ":irc.example.net 364 alice irc.example.net irc.example.net :0 Example server"),
                Arguments.of(":irc.example.net 365 alice :End of LINKS list", ":irc.example.net 365 alice * :End of /LINKS list"),
                Arguments.of(":irc.example.net 366 alice #chat :End of /NAMES list", ":irc.example.net 366 alice #chat :End of /NAMES list"),
                Arguments.of(":irc.example.net 367 alice #chat *!*@bad.host", ":irc.example.net 367 alice #chat *!*@bad.host"),
                Arguments.of(":irc.example.net 368 alice #chat :End of ban list", ":irc.example.net 368 alice #chat :End of channel ban list"),
                Arguments.of(":irc.example.net 369 alice oldnick :End of WHOWAS", ":irc.example.net 369 alice oldnick :End of WHOWAS"),
                Arguments.of(":irc.example.net 371 alice :This server follows modern IRC specs", ":irc.example.net 371 alice :This server follows modern IRC specs"),
                Arguments.of(":irc.example.net 372 alice :- Welcome to ExampleNet", ":irc.example.net 372 alice :- Welcome to ExampleNet"),
                Arguments.of(":irc.example.net 374 alice :End of INFO", ":irc.example.net 374 alice :End of INFO list"),
                Arguments.of(":irc.example.net 375 alice :- Message of the Day -", ":irc.example.net 375 alice :- Message of the Day -"),
                Arguments.of(":irc.example.net 376 alice :End of MOTD", ":irc.example.net 376 alice :End of /MOTD command"),
                Arguments.of(":irc.example.net 378 alice bob :is connecting from host.example.com", ":irc.example.net 378 alice bob :is connecting from host.example.com"),
                Arguments.of(":irc.example.net 379 alice bob :is using modes +iw", ":irc.example.net 379 alice bob :is using modes +iw"),
                Arguments.of(":irc.example.net 381 alice :You are now an IRC operator", ":irc.example.net 381 alice :You are now an IRC operator"),
                Arguments.of(":irc.example.net 382 alice file.txt :Rehashing server config", ":irc.example.net 382 alice file.txt :Rehashing"),
                Arguments.of(":irc.example.net 391 alice irc.example.net :2025-12-13 12:00:00 UTC", ":irc.example.net 391 alice irc.example.net :2025-12-13 12:00:00 UTC"),
                Arguments.of(":irc.example.net 400 alice KICK :Unknown error", ":irc.example.net 400 alice KICK :Unknown error"),
                Arguments.of(":irc.example.net 401 alice badnick :No such nick", ":irc.example.net 401 alice badnick :No such nick"),
                Arguments.of(":irc.example.net 402 alice bad.server :No such server", ":irc.example.net 402 alice bad.server :No such server"),
                Arguments.of(":irc.example.net 403 alice #missing :No such channel", ":irc.example.net 403 alice #missing :No such channel"),
                Arguments.of(":irc.example.net 404 alice #chat :Cannot send to channel", ":irc.example.net 404 alice #chat :Cannot send to channel"),
                Arguments.of(":irc.example.net 405 alice #chat :You have joined too many channels", ":irc.example.net 405 alice #chat :You have joined too many channels"),
                Arguments.of(":irc.example.net 406 alice oldnick :No such nick", ":irc.example.net 406 alice oldnick :No such nick"),
                Arguments.of(":irc.example.net 409 alice :No origin specified", ":irc.example.net 409 alice :No origin specified"),
                Arguments.of(":irc.example.net 411 alice :No recipient given", ":irc.example.net 411 alice :No recipient given"),
                Arguments.of(":irc.example.net 412 alice :No text to send", ":irc.example.net 412 alice :No text to send"),
                Arguments.of(":irc.example.net 417 alice :Input line too long", ":irc.example.net 417 alice :Input line too long"),
                Arguments.of(":irc.example.net 421 alice FOO :Unknown command", ":irc.example.net 421 alice FOO :Unknown command"),
                Arguments.of(":irc.example.net 422 alice :MOTD File is missing", ":irc.example.net 422 alice :MOTD File is missing"),
                Arguments.of(":irc.example.net 431 alice :No nickname given", ":irc.example.net 431 alice :No nickname given"),
                Arguments.of(":irc.example.net 432 alice bad!nick :Erroneous nickname", ":irc.example.net 432 alice bad!nick :Erroneous nickname"),
                Arguments.of(":irc.example.net 433 alice bob :Nickname is already in use", ":irc.example.net 433 alice bob :Nickname is already in use"),
                Arguments.of(":irc.example.net 436 alice bob :Nickname collision", ":irc.example.net 436 alice bob :Nickname collision"),
                Arguments.of(":irc.example.net 441 alice bob #chat :They aren't on that channel", ":irc.example.net 441 alice bob #chat :They aren't on that channel"),
                Arguments.of(":irc.example.net 442 alice #chat :You're not on that channel", ":irc.example.net 442 alice #chat :You're not on that channel"),
                Arguments.of(":irc.example.net 443 alice bob #chat :User is already on channel", ":irc.example.net 443 alice bob #chat :is already on channel"),
                Arguments.of(":irc.example.net 451 alice :You have not registered", ":irc.example.net 451 alice :You have not registered"),
                Arguments.of(":irc.example.net 461 alice JOIN :Not enough parameters", ":irc.example.net 461 alice JOIN :Not enough parameters"),
                Arguments.of(":irc.example.net 462 alice :You may not reregister", ":irc.example.net 462 alice :You may not reregister"),
                Arguments.of(":irc.example.net 464 alice :Password incorrect", ":irc.example.net 464 alice :Password incorrect"),
                Arguments.of(":irc.example.net 465 alice :You are banned from this server", ":irc.example.net 465 alice :You are banned from this server"),
                Arguments.of(":irc.example.net 471 alice #chat :Channel is full", ":irc.example.net 471 alice #chat :Channel is full"),
                Arguments.of(":irc.example.net 472 alice x :is unknown mode char to me", ":irc.example.net 472 alice x :is unknown mode char to me"),
                Arguments.of(":irc.example.net 473 alice #chat :Invite only channel", ":irc.example.net 473 alice #chat :Invite only channel"),
                Arguments.of(":irc.example.net 474 alice #chat :You are banned from this channel", ":irc.example.net 474 alice #chat :You are banned from this channel"),
                Arguments.of(":irc.example.net 475 alice #chat :Bad channel key", ":irc.example.net 475 alice #chat :Bad channel key"),
                Arguments.of(":irc.example.net 476 alice badmask :Bad channel mask", ":irc.example.net 476 alice badmask :Bad channel mask"),
                Arguments.of(":irc.example.net 481 alice :Permission denied", ":irc.example.net 481 alice :Permission denied"),
                Arguments.of(":irc.example.net 482 alice #chat :You're not a channel operator", ":irc.example.net 482 alice #chat :You're not a channel operator"),
                Arguments.of(":irc.example.net 483 alice :You can't kill a server", ":irc.example.net 483 alice :You can't kill a server"),
                Arguments.of(":irc.example.net 491 alice :No O-lines for your host", ":irc.example.net 491 alice :No O-lines for your host"),
                Arguments.of(":irc.example.net 501 alice :Unknown MODE flag", ":irc.example.net 501 alice :Unknown MODE flag"),
                Arguments.of(":irc.example.net 502 alice :Cannot change mode for other users", ":irc.example.net 502 alice :Cannot change mode for other users"),
                Arguments.of(":irc.example.net 524 alice JOIN :Help not found", ":irc.example.net 524 alice JOIN :Help not found"),
                Arguments.of(":irc.example.net 525 alice #chat :Key is not well-formed", ":irc.example.net 525 alice #chat :Key is not well-formed"),
                Arguments.of(":irc.example.net 670 alice :STARTTLS successful", ":irc.example.net 670 alice :STARTTLS successful"),
                Arguments.of(":irc.example.net 671 alice bob :is using a secure connection", ":irc.example.net 671 alice bob :is using a secure connection"),
                Arguments.of(":irc.example.net 691 alice :STARTTLS failed", ":irc.example.net 691 alice :STARTTLS failed"),
                Arguments.of(":irc.example.net 696 alice #chat k badkey :Invalid mode parameter", ":irc.example.net 696 alice #chat k badkey :Invalid mode parameter"),
                Arguments.of(":irc.example.net 704 alice subject :Help for JOIN", ":irc.example.net 704 alice subject :Help for JOIN"),
                Arguments.of(":irc.example.net 705 alice subject :JOIN <channel>", ":irc.example.net 705 alice subject :JOIN <channel>"),
                Arguments.of(":irc.example.net 706 alice subject :End of HELP", ":irc.example.net 706 alice subject :End of HELP"),
                Arguments.of(":irc.example.net 723 alice priv :Insufficient privileges", ":irc.example.net 723 alice priv :Insufficient privileges"),
                Arguments.of(
                        "@a=1;b=hello\\sworld :nick!user@host PRIVMSG #chan :hi there",
                        "@a=1;b=hello\\sworld :nick!user@host PRIVMSG #chan :hi there"
                ),
                Arguments.of("CAP LS 302", "CAP LS 302"),
                Arguments.of("CAP LIST", "CAP LIST"),
                Arguments.of("CAP REQ :multi-prefix sasl -batch", "CAP REQ :multi-prefix sasl -batch"),
                Arguments.of("CAP END", "CAP END"),
                Arguments.of("CAP nick ACK :multi-prefix -batch", "CAP nick ACK :multi-prefix -batch"),
                Arguments.of("CAP nick NAK :multi-prefix -batch", "CAP nick NAK :multi-prefix -batch"),
                Arguments.of("CAP nick NEW :multi-prefix sasl", "CAP nick NEW :multi-prefix sasl"),
                Arguments.of("CAP nick DEL :multi-prefix sasl", "CAP nick DEL :multi-prefix sasl"),
                Arguments.of("CAP nick LS :multi-prefix sasl", "CAP nick LS :multi-prefix sasl"),
                Arguments.of("CAP nick LS * :multi-prefix sasl", "CAP nick LS * :multi-prefix sasl"),
                Arguments.of("CAP nick LIST :multi-prefix sasl", "CAP nick LIST :multi-prefix sasl"),
                Arguments.of("CAP nick LIST * :multi-prefix sasl", "CAP nick LIST * :multi-prefix sasl"),
                Arguments.of("KICK #c nick :", "KICK #c nick"),
                Arguments.of("QUIT :", "QUIT"),
                Arguments.of("PART #chan :", "PART #chan"),
                Arguments.of("ping :12345", "PING :12345"),
                Arguments.of(":nick@host PRIVMSG #c :hi", ":nick@host PRIVMSG #c :hi"),
                Arguments.of(":nick!user PRIVMSG #c :hi", ":nick!user PRIVMSG #c :hi"),
                Arguments.of("@a=1 PRIVMSG #c :hi", "@a=1 PRIVMSG #c :hi"),
                Arguments.of("@a=;b PRIVMSG #c :hi", "@a;b PRIVMSG #c :hi"),
                Arguments.of("@a=hello\\:world PRIVMSG #c :hi", "@a=hello\\:world PRIVMSG #c :hi"),
                Arguments.of("@a=hello\\nworld PRIVMSG #c :hi", "@a=hello\\nworld PRIVMSG #c :hi"),
                Arguments.of("@a=hello\\rworld PRIVMSG #c :hi", "@a=hello\\rworld PRIVMSG #c :hi"),
                Arguments.of("@a=hello\\\\world PRIVMSG #c :hi", "@a=hello\\\\world PRIVMSG #c :hi"),
                Arguments.of("@a=1;a=2 PRIVMSG #c :hi", "@a=2 PRIVMSG #c :hi"),
                Arguments.of("PRIVMSG #a,#b :hi", "PRIVMSG #a,#b :hi"),
                Arguments.of("NOTICE #a,#b :hi", "NOTICE #a,#b :hi"),
                Arguments.of("JOIN #a,#b key1,key2", "JOIN #a,#b key1,key2"),
                Arguments.of("PING :12345   ", "PING :12345   "),
                Arguments.of("PING abc:def", "PING :abc:def"),
                Arguments.of("PRIVMSG #c ::hi", "PRIVMSG #c ::hi"),
                Arguments.of("NOTICE #c ::hi", "NOTICE #c ::hi"),
                Arguments.of("ERROR ::oops", "ERROR ::oops"),
                Arguments.of("QUIT :bye", "QUIT :bye"),
                Arguments.of("MODE #chan +b *!*@bad.host", "MODE #chan +b *!*@bad.host"),
                Arguments.of("MODE #chan +k key", "MODE #chan +k key"),
                Arguments.of("JOIN #a,#b", "JOIN #a,#b"),
                Arguments.of("JOIN #a,#b :", "JOIN #a,#b"),
                Arguments.of("@a=hello\\qworld PRIVMSG #c :hi", "@a=hello\\\\qworld PRIVMSG #c :hi"),
                Arguments.of("@a=hello\\ PRIVMSG #c :hi", "@a=hello\\\\ PRIVMSG #c :hi"),
                Arguments.of("@=1 PRIVMSG #c :hi", "PRIVMSG #c :hi"),
                Arguments.of("@a==b PRIVMSG #c :hi", "@a==b PRIVMSG #c :hi"),
                Arguments.of("CAP nick ACK :multi-prefix", "CAP nick ACK :multi-prefix"),
                Arguments.of("CAP nick NAK :multi-prefix", "CAP nick NAK :multi-prefix"),
                Arguments.of("CAP REQ :multi-prefix", "CAP REQ :multi-prefix"),
                Arguments.of("CAP nick DEL :multi-prefix", "CAP nick DEL :multi-prefix"),
                Arguments.of("PRIVMSG #c :", "PRIVMSG #c :"),
                Arguments.of("NOTICE #c :", "NOTICE #c :")
        );
    }

    @ParameterizedTest(name = "{index}: type for \"{0}\" is {1}")
    @MethodSource("messageTypes")
    void unmarshalProducesExpectedType(String input, Class<? extends IRCMessage> expectedType) {
        IRCMessage parsed = unmarshaller.unmarshal(new IRCServerParameters(), StandardCharsets.UTF_8, input);
        assertInstanceOf(expectedType, parsed);
    }

    static Stream<Arguments> messageTypes() {
        return Stream.of(
                // Basic messages
                Arguments.of("PING :12345", IRCMessagePING.class),
                Arguments.of("PONG :12345", IRCMessagePONG.class),
                Arguments.of("ERROR :Closing Link: 10.0.0.1 (Ping timeout)", IRCMessageERROR.class),
                Arguments.of("JOIN 0", IRCMessageJOIN0.class),
                Arguments.of("JOIN #chan", IRCMessageJOINNormal.class),
                Arguments.of("JOIN #chan1,#chan2 key1,key2", IRCMessageJOINNormal.class),
                Arguments.of("KICK #chan nick", IRCMessageKICK.class),
                Arguments.of("KICK #chan nick :some reason", IRCMessageKICK.class),
                Arguments.of("PART #chan", IRCMessagePART.class),
                Arguments.of("PART #chan1,#chan2 :bye", IRCMessagePART.class),
                Arguments.of("MODE #chan", IRCMessageMODE.class),
                Arguments.of("MODE #chan -ovl nick1 nick2 50", IRCMessageMODE.class),
                Arguments.of("NICK newnick", IRCMessageNICK.class),
                Arguments.of("NOTICE #chan :hi", IRCMessageNOTICE.class),
                Arguments.of("PASS whatever", IRCMessagePASS.class),
                Arguments.of("PRIVMSG #chan :hi", IRCMessagePRIVMSG.class),
                Arguments.of("USER myuser 0 * :Real Name", IRCMessageUSER.class),
                Arguments.of("NONSENSE arg1 arg2", IRCMessageUnsupported.class),
                Arguments.of("CAP LS 302", IRCMessageCAPLSRequest.class),
                Arguments.of("CAP LIST", IRCMessageCAPLISTRequest.class),
                Arguments.of("CAP REQ :multi-prefix sasl -batch", IRCMessageCAPREQ.class),
                Arguments.of("CAP END", IRCMessageCAPEND.class),
                Arguments.of("CAP nick ACK :multi-prefix -batch", IRCMessageCAPACK.class),
                Arguments.of("CAP nick NAK :multi-prefix -batch", IRCMessageCAPNAK.class),
                Arguments.of("CAP nick NEW :multi-prefix sasl", IRCMessageCAPNEW.class),
                Arguments.of("CAP nick DEL :multi-prefix sasl", IRCMessageCAPDEL.class),
                Arguments.of("CAP nick LS :multi-prefix sasl", IRCMessageCAPLSResponse.class),
                Arguments.of("CAP nick LS * :multi-prefix sasl", IRCMessageCAPLSResponse.class),
                Arguments.of("CAP nick LIST :multi-prefix sasl", IRCMessageCAPLISTResponse.class),
                Arguments.of("CAP nick LIST * :multi-prefix sasl", IRCMessageCAPLISTResponse.class),
                Arguments.of(":irc.example.net 001 alice :Welcome to the Example IRC Network alice!alice@host", IRCMessage001.class),
                Arguments.of(":irc.example.net 002 alice :Your host is irc.example.net, running version example-1.0", IRCMessage002.class),
                Arguments.of(":irc.example.net 003 alice :This server was created Mon Dec 1 2025", IRCMessage003.class),
                Arguments.of(":irc.example.net 004 alice irc.example.net example-1.0 iowghraAsORTVSxNCWqBzvdHtGp lvhopsmntikrRcaqOALQbSeIKVf", IRCMessage004.class),
                Arguments.of(":irc.example.net 005 alice CHANTYPES=# PREFIX=(ov)@+ NETWORK=ExampleNet :are supported by this server", IRCMessage005.class),
                Arguments.of(":irc.example.net 010 alice irc.backup.example.net 6667 :Please connect to this server", IRCMessage010.class),
                Arguments.of(":irc.example.net 212 alice JOIN 12345", IRCMessage212.class),
                Arguments.of(":irc.example.net 219 alice STATS :End of STATS report", IRCMessage219.class),
                Arguments.of(":irc.example.net 221 alice +iw", IRCMessage221.class),
                Arguments.of(":irc.example.net 242 alice :Server up 5 days, 3:42:10", IRCMessage242.class),
                Arguments.of(":irc.example.net 251 alice :There are 42 users and 10 services on 3 servers", IRCMessage251.class),
                Arguments.of(":irc.example.net 252 alice 5 :operator(s) online", IRCMessage252.class),
                Arguments.of(":irc.example.net 253 alice 1 :unknown connection(s)", IRCMessage253.class),
                Arguments.of(":irc.example.net 254 alice 20 :channels formed", IRCMessage254.class),
                Arguments.of(":irc.example.net 255 alice :I have 42 clients and 3 servers", IRCMessage255.class),
                Arguments.of(":irc.example.net 256 alice :Administrative info for irc.example.net", IRCMessage256.class),
                Arguments.of(":irc.example.net 257 alice :Example IRC Network", IRCMessage257.class),
                Arguments.of(":irc.example.net 258 alice :Somewhere on the Internet", IRCMessage258.class),
                Arguments.of(":irc.example.net 259 alice :admin@example.net", IRCMessage259.class),
                Arguments.of(":irc.example.net 263 alice WHO :Please wait a while and try again", IRCMessage263.class),
                Arguments.of(":irc.example.net 265 alice 40 50 :Current local users 40, max 50", IRCMessage265.class),
                Arguments.of(":irc.example.net 266 alice 42 100 :Current global users 42, max 100", IRCMessage266.class),
                Arguments.of(":irc.example.net 276 alice bob :has client certificate fingerprint ABCDEF123456", IRCMessage276.class),
                Arguments.of(":irc.example.net 301 alice bob :Gone to lunch", IRCMessage301.class),
                Arguments.of(":irc.example.net 302 alice bob=+bob!bob@host", IRCMessage302.class),
                Arguments.of(":irc.example.net 305 alice :You are no longer marked as away", IRCMessage305.class),
                Arguments.of(":irc.example.net 306 alice :You have been marked as away", IRCMessage306.class),
                Arguments.of(":irc.example.net 307 alice bob :is a registered nickname", IRCMessage307.class),
                Arguments.of(":irc.example.net 311 alice bob bob host.example.com * :Bob Example", IRCMessage311.class),
                Arguments.of(":irc.example.net 312 alice bob irc.example.net :Example Server", IRCMessage312.class),
                Arguments.of(":irc.example.net 313 alice bob :is an IRC operator", IRCMessage313.class),
                Arguments.of(":irc.example.net 314 alice oldnick user oldhost * :Old Realname", IRCMessage314.class),
                Arguments.of(":irc.example.net 315 alice bob :End of WHO list", IRCMessage315.class),
                Arguments.of(":irc.example.net 317 alice bob 120 1700000000 :seconds idle, signon time", IRCMessage317.class),
                Arguments.of(":irc.example.net 318 alice bob :End of WHOIS", IRCMessage318.class),
                Arguments.of(":irc.example.net 319 alice bob :@#chat #help", IRCMessage319.class),
                Arguments.of(":irc.example.net 320 alice bob :is identified to services", IRCMessage320.class),
                Arguments.of(":irc.example.net 321 alice Channel Users Name", IRCMessage321.class),
                Arguments.of(":irc.example.net 322 alice #chat 12 :General discussion", IRCMessage322.class),
                Arguments.of(":irc.example.net 323 alice :End of /LIST", IRCMessage323.class),
                Arguments.of(":irc.example.net 324 alice #chat +nt", IRCMessage324.class),
                Arguments.of(":irc.example.net 329 alice #chat 1700000000", IRCMessage329.class),
                Arguments.of(":irc.example.net 330 alice bob bob_account :is logged in as", IRCMessage330.class),
                Arguments.of(":irc.example.net 331 alice #chat :No topic is set", IRCMessage331.class),
                Arguments.of(":irc.example.net 332 alice #chat :Welcome to #chat", IRCMessage332.class),
                Arguments.of(":irc.example.net 333 alice #chat bob 1700001000", IRCMessage333.class),
                Arguments.of(":irc.example.net 336 alice #chat bob", IRCMessage336.class),
                Arguments.of(":irc.example.net 337 alice #chat :End of invite list", IRCMessage337.class),
                Arguments.of(":irc.example.net 338 alice bob bob@real.host 127.0.0.1 :actually using host", IRCMessage338.class),
                Arguments.of(":irc.example.net 341 alice bob #chat", IRCMessage341.class),
                Arguments.of(":irc.example.net 346 alice #chat *!*@trusted.host", IRCMessage346.class),
                Arguments.of(":irc.example.net 347 alice #chat :End of invite exception list", IRCMessage347.class),
                Arguments.of(":irc.example.net 348 alice #chat *!*@except.host", IRCMessage348.class),
                Arguments.of(":irc.example.net 349 alice #chat :End of exception list", IRCMessage349.class),
                Arguments.of(":irc.example.net 351 alice example-1.0 irc.example.net :Modern IRCd", IRCMessage351.class),
                Arguments.of(":irc.example.net 352 alice #chat bob host irc.example.net bob H@ :0 Bob Example", IRCMessage352.class),
                Arguments.of(":irc.example.net 353 alice = #chat :@bob +eve alice", IRCMessage353.class),
                Arguments.of(":irc.example.net 364 alice irc.example.net irc.example.net :0 Example server", IRCMessage364.class),
                Arguments.of(":irc.example.net 365 alice :End of LINKS list", IRCMessage365.class),
                Arguments.of(":irc.example.net 366 alice #chat :End of /NAMES list", IRCMessage366.class),
                Arguments.of(":irc.example.net 367 alice #chat *!*@bad.host", IRCMessage367.class),
                Arguments.of(":irc.example.net 368 alice #chat :End of ban list", IRCMessage368.class),
                Arguments.of(":irc.example.net 369 alice oldnick :End of WHOWAS", IRCMessage369.class),
                Arguments.of(":irc.example.net 371 alice :This server follows modern IRC specs", IRCMessage371.class),
                Arguments.of(":irc.example.net 372 alice :- Welcome to ExampleNet", IRCMessage372.class),
                Arguments.of(":irc.example.net 374 alice :End of INFO", IRCMessage374.class),
                Arguments.of(":irc.example.net 375 alice :- Message of the Day -", IRCMessage375.class),
                Arguments.of(":irc.example.net 376 alice :End of MOTD", IRCMessage376.class),
                Arguments.of(":irc.example.net 378 alice bob :is connecting from host.example.com", IRCMessage378.class),
                Arguments.of(":irc.example.net 379 alice bob :is using modes +iw", IRCMessage379.class),
                Arguments.of(":irc.example.net 381 alice :You are now an IRC operator", IRCMessage381.class),
                Arguments.of(":irc.example.net 382 alice test.txt :Rehashing server config", IRCMessage382.class),
                Arguments.of(":irc.example.net 391 alice irc.example.net :2025-12-13 12:00:00 UTC", IRCMessage391.class),
                Arguments.of(":irc.example.net 391 alice irc.example.net 12321321 :2025-12-13 12:00:00 UTC", IRCMessage391.class),
                Arguments.of(":irc.example.net 391 alice irc.example.net 13213213 -05:00 :2025-12-13 12:00:00 UTC", IRCMessage391.class),
                Arguments.of(":irc.example.net 400 alice PACK :Unknown error", IRCMessage400.class),
                Arguments.of(":irc.example.net 401 alice badnick :No such nick", IRCMessage401.class),
                Arguments.of(":irc.example.net 402 alice bad.server :No such server", IRCMessage402.class),
                Arguments.of(":irc.example.net 403 alice #missing :No such channel", IRCMessage403.class),
                Arguments.of(":irc.example.net 404 alice #chat :Cannot send to channel", IRCMessage404.class),
                Arguments.of(":irc.example.net 405 alice #chat :You have joined too many channels", IRCMessage405.class),
                Arguments.of(":irc.example.net 406 alice oldnick :No such nick", IRCMessage406.class),
                Arguments.of(":irc.example.net 409 alice :No origin specified", IRCMessage409.class),
                Arguments.of(":irc.example.net 411 alice :No recipient given", IRCMessage411.class),
                Arguments.of(":irc.example.net 412 alice :No text to send", IRCMessage412.class),
                Arguments.of(":irc.example.net 417 alice :Input line too long", IRCMessage417.class),
                Arguments.of(":irc.example.net 421 alice FOO :Unknown command", IRCMessage421.class),
                Arguments.of(":irc.example.net 422 alice :MOTD File is missing", IRCMessage422.class),
                Arguments.of(":irc.example.net 431 alice :No nickname given", IRCMessage431.class),
                Arguments.of(":irc.example.net 432 alice bad!nick :Erroneous nickname", IRCMessage432.class),
                Arguments.of(":irc.example.net 433 alice bob :Nickname is already in use", IRCMessage433.class),
                Arguments.of(":irc.example.net 436 alice bob :Nickname collision", IRCMessage436.class),
                Arguments.of(":irc.example.net 441 alice bob #chat :They aren't on that channel", IRCMessage441.class),
                Arguments.of(":irc.example.net 442 alice #chat :You're not on that channel", IRCMessage442.class),
                Arguments.of(":irc.example.net 443 alice bob #chat :User is already on channel", IRCMessage443.class),
                Arguments.of(":irc.example.net 451 alice :You have not registered", IRCMessage451.class),
                Arguments.of(":irc.example.net 461 alice JOIN :Not enough parameters", IRCMessage461.class),
                Arguments.of(":irc.example.net 462 alice :You may not reregister", IRCMessage462.class),
                Arguments.of(":irc.example.net 464 alice :Password incorrect", IRCMessage464.class),
                Arguments.of(":irc.example.net 465 alice :You are banned from this server", IRCMessage465.class),
                Arguments.of(":irc.example.net 471 alice #chat :Channel is full", IRCMessage471.class),
                Arguments.of(":irc.example.net 472 alice x :Unknown mode", IRCMessage472.class),
                Arguments.of(":irc.example.net 473 alice #chat :Invite only channel", IRCMessage473.class),
                Arguments.of(":irc.example.net 474 alice #chat :You are banned from this channel", IRCMessage474.class),
                Arguments.of(":irc.example.net 475 alice #chat :Bad channel key", IRCMessage475.class),
                Arguments.of(":irc.example.net 476 alice badmask :Bad channel mask", IRCMessage476.class),
                Arguments.of(":irc.example.net 481 alice :Permission denied", IRCMessage481.class),
                Arguments.of(":irc.example.net 482 alice #chat :You're not a channel operator", IRCMessage482.class),
                Arguments.of(":irc.example.net 483 alice :You can't kill a server", IRCMessage483.class),
                Arguments.of(":irc.example.net 491 alice :No O-lines for your host", IRCMessage491.class),
                Arguments.of(":irc.example.net 501 alice :Unknown MODE flag", IRCMessage501.class),
                Arguments.of(":irc.example.net 502 alice :Cannot change mode for other users", IRCMessage502.class),
                Arguments.of(":irc.example.net 524 alice JOIN :Help not found", IRCMessage524.class),
                Arguments.of(":irc.example.net 525 alice #chat :Key is not well-formed", IRCMessage525.class),
                Arguments.of(":irc.example.net 670 alice :STARTTLS successful", IRCMessage670.class),
                Arguments.of(":irc.example.net 671 alice bob :is using a secure connection", IRCMessage671.class),
                Arguments.of(":irc.example.net 691 alice :STARTTLS failed", IRCMessage691.class),
                Arguments.of(":irc.example.net 696 alice #chat k badkey :Invalid mode parameter", IRCMessage696.class),
                Arguments.of(":irc.example.net 704 alice subject :Help for JOIN", IRCMessage704.class),
                Arguments.of(":irc.example.net 705 alice subject :JOIN <channel>", IRCMessage705.class),
                Arguments.of(":irc.example.net 706 alice subject :End of HELP", IRCMessage706.class),
                Arguments.of(":irc.example.net 723 alice priv :Insufficient privileges", IRCMessage723.class)
        );
    }

    @ParameterizedTest(name = "{index}: parse error for \"{0}\"")
    @MethodSource("parseErrorMessages")
    void unmarshalProducesParseError(String input) {
        IRCMessage parsed = unmarshaller.unmarshal(new IRCServerParameters(), StandardCharsets.UTF_8, input);
        assertInstanceOf(IRCMessageParseError.class, parsed);
    }

    static Stream<Arguments> parseErrorMessages() {
        return Stream.of(
                Arguments.of("PING"),
                Arguments.of("PONG"),
                Arguments.of("JOIN"),
                Arguments.of("KICK #chan"),
                Arguments.of("MODE"),
                Arguments.of("NOTICE #chan"),
                Arguments.of("PRIVMSG #chan"),
                Arguments.of("PASS"),
                Arguments.of("NICK"),
                Arguments.of("USER myuser 0 *"),
                Arguments.of(":irc.example.net 010 alice irc.backup.example.net notaport :Please connect"),
                Arguments.of(":irc.example.net 265 alice notInt 50 :Current local users"),
                Arguments.of(":irc.example.net 329 alice #chat notATimestamp"),
                Arguments.of(":irc.example.net 333 alice #chat bob notATimestamp"),
                Arguments.of(":irc.example.net 317 alice bob notInt 1700000000 :seconds idle, signon time"),
                Arguments.of(":irc.example.net 317 alice bob 120 notALong :seconds idle, signon time"),
                Arguments.of(":irc.example.net 472 alice :missing modechar"),
                Arguments.of(":irc.example.net 696 alice #chat k :missing parameter and description"),
                Arguments.of("CAP LS"),
                Arguments.of("CAP REQ"),
                Arguments.of("CAP nick ACK"),
                Arguments.of("CAP nick ACK :"),
                Arguments.of("CAP nick LS"),
                Arguments.of("CAP nick DEL"),
                Arguments.of("CAP nick FOO :bar"),
                Arguments.of(":nick! PRIVMSG #c :hi"),
                Arguments.of(":nick! PRIVMSG #c :hi"),
                Arguments.of(":nick@ PRIVMSG #c :hi"),
                Arguments.of(":nick!user@ PRIVMSG #c :hi"),
                Arguments.of(":nick!user@host@extra PRIVMSG #c :hi"),
                Arguments.of(":@host PRIVMSG #c :hi"),
                Arguments.of("PING :"),
                Arguments.of("ERROR :"),
                Arguments.of("PASS :"),
                Arguments.of("NICK :"),
                Arguments.of(":irc.example.net 322 alice #chat 12 :"),
                Arguments.of(":irc.example.net 001 alice :"),
                Arguments.of(":irc.example.net 400 alice KICK :"),
                Arguments.of("PONG irc.example.com :"),
                Arguments.of(":irc.example.net 256 alice irc.example.net :"),
                Arguments.of(":irc.example.net 391 alice irc.example.net 1700000000 -05:00 :"),
                Arguments.of(":irc.example.net 010 alice irc.backup.example.net 999999999999999999999 :Please connect"),
                Arguments.of(":irc.example.net 317 alice bob 999999999999999999999 1700000000 :seconds idle, signon time"),
                Arguments.of(":irc.example.net 317 alice bob 120 999999999999999999999 :seconds idle, signon time"),
                Arguments.of("CAP REQ :"),
                Arguments.of("CAP nick NAK :"),
                Arguments.of("CAP nick ACK :"),
                Arguments.of("JOIN :"),
                Arguments.of("PART :"),
                Arguments.of("PRIVMSG :hi"),
                Arguments.of("NOTICE :hi"),
                Arguments.of("PONG :"),
                Arguments.of("PRIVMSG #c\t:hi"),
                Arguments.of("PING  "),
                Arguments.of("PRIVMSG #c"),
                Arguments.of("NOTICE #c"),
                Arguments.of("ERROR"),
                Arguments.of("CAP"),
                Arguments.of("CAP nick"),
                Arguments.of("CAP nick ACK")
        );
    }

    @ParameterizedTest(name = "{index}: invalid grammar is unsupported for \"{0}\"")
    @MethodSource("invalidGrammarMessages")
    void unmarshalInvalidGrammarIsUnsupported(String input) {
        IRCMessage parsed = unmarshaller.unmarshal(new IRCServerParameters(), StandardCharsets.UTF_8, input);
        assertInstanceOf(IRCMessageUnsupported.class, parsed);
        assertFalse(parsed instanceof IRCMessageParseError);
    }

    static Stream<Arguments> invalidGrammarMessages() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of("   "),
                Arguments.of("@"),
                Arguments.of(":"),
                Arguments.of("@a=1 :"),
                Arguments.of("!@#$"),
                Arguments.of("PRIVMSG\u0000#chan :hi"),
                Arguments.of("PING :hi\r\nPONG :x"),
                Arguments.of("@a=1;b=2"),
                Arguments.of("@a=1;b=2  "),
                Arguments.of("@a=1;b=2 :nick"),
                Arguments.of("@a=1 b=2 PRIVMSG #c :hi"),
                Arguments.of(": PRIVMSG #c :hi"),
                Arguments.of("PRIVMSG\t#c :hi"),
                Arguments.of("PRIVMSG #c :hi\u0000there")
        );
    }

    @ParameterizedTest(name = "{index}: length ok \"{0}\"")
    @MethodSource("lengthOkMessages")
    void lengthOkMessages(String input, String expected) {
        IRCMessage parsed = unmarshaller.unmarshal(new IRCServerParameters(), StandardCharsets.UTF_8, input);
        assertFalse(parsed instanceof IRCMessageParseError);
        assertFalse(parsed instanceof IRCMessageUnsupported);
        String marshalled = marshaller.marshal(parsed);
        assertEquals(expected, marshalled);
    }

    static Stream<Arguments> lengthOkMessages() {
        String tagValue8191 = "a".repeat(8191 - "@a=".length() - " ".length());
        String tagMax = "@a=" + tagValue8191 + " PRIVMSG #c :hi";

        int bodyPrefixLen = "PRIVMSG #c :".length();
        String bodyValue510 = "a".repeat(510 - bodyPrefixLen);
        String bodyMax = "PRIVMSG #c :" + bodyValue510;

        return Stream.of(
                Arguments.of(tagMax, tagMax),
                Arguments.of(bodyMax, bodyMax)
        );
    }

    @ParameterizedTest(name = "{index}: length invalid is unsupported for \"{0}\"")
    @MethodSource("lengthInvalidMessages")
    void lengthInvalidMessages(String input) {
        IRCMessage parsed = unmarshaller.unmarshal(new IRCServerParameters(), StandardCharsets.UTF_8, input);
        assertInstanceOf(IRCMessageUnsupported.class, parsed);
        assertFalse(parsed instanceof IRCMessageParseError);
    }

    static Stream<Arguments> lengthInvalidMessages() {
        String tagValue8192 = "a".repeat(8192 - "@a=".length() - " ".length());
        String tagTooLong = "@a=" + tagValue8192 + " PRIVMSG #c :hi";

        int bodyPrefixLen = "PRIVMSG #c :".length();
        String bodyValue511 = "a".repeat(511 - bodyPrefixLen);
        String bodyTooLong = "PRIVMSG #c :" + bodyValue511;

        String messageTooLong = "A".repeat(8702);

        return Stream.of(
                Arguments.of(tagTooLong),
                Arguments.of(bodyTooLong),
                Arguments.of(messageTooLong)
        );
    }
}
