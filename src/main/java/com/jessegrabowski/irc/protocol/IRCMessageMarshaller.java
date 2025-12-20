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
package com.jessegrabowski.irc.protocol;

import static java.util.function.Predicate.not;

import com.jessegrabowski.irc.protocol.model.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IRCMessageMarshaller {

    public String marshal(IRCMessage message) {
        return switch (message) {
            case IRCMessageAWAY m -> marshal(m, this::marshalAway);
            case IRCMessageCAPACK m -> marshal(m, this::marshalCapACK);
            case IRCMessageCAPDEL m -> marshal(m, this::marshalCapDEL);
            case IRCMessageCAPEND m -> marshal(m, this::marshalCapEND);
            case IRCMessageCAPLISTRequest m -> marshal(m, this::marshalCapLISTRequest);
            case IRCMessageCAPLISTResponse m -> marshal(m, this::marshalCapLISTResponse);
            case IRCMessageCAPLSRequest m -> marshal(m, this::marshalCapLSRequest);
            case IRCMessageCAPLSResponse m -> marshal(m, this::marshalCapLSResponse);
            case IRCMessageCAPNAK m -> marshal(m, this::marshalCapNAK);
            case IRCMessageCAPNEW m -> marshal(m, this::marshalCapNEW);
            case IRCMessageCAPREQ m -> marshal(m, this::marshalCapREQ);
            case IRCMessageERROR m -> marshal(m, this::marshalError);
            case IRCMessageJOIN0 m -> marshal(m, this::marshalJoin0);
            case IRCMessageJOINNormal m -> marshal(m, this::marshalJoinNormal);
            case IRCMessageKICK m -> marshal(m, this::marshalKick);
            case IRCMessageKILL m -> marshal(m, this::marshalKill);
            case IRCMessageMODE m -> marshal(m, this::marshalMode);
            case IRCMessageNAMES m -> marshal(m, this::marshalNames);
            case IRCMessageNICK m -> marshal(m, this::marshalNick);
            case IRCMessageNOTICE m -> marshal(m, this::marshalNotice);
            case IRCMessageOPER m -> marshal(m, this::marshalOper);
            case IRCMessagePART m -> marshal(m, this::marshalPart);
            case IRCMessagePASS m -> marshal(m, this::marshalPass);
            case IRCMessagePING m -> marshal(m, this::marshalPing);
            case IRCMessagePONG m -> marshal(m, this::marshalPong);
            case IRCMessagePRIVMSG m -> marshal(m, this::marshalPrivmsg);
            case IRCMessageUSER m -> marshal(m, this::marshalUser);
            case IRCMessageQUIT m -> marshal(m, this::marshalQuit);
            case IRCMessageTOPIC m -> marshal(m, this::marshalTopic);
            case IRCMessage001 m -> marshal(m, this::marshal001);
            case IRCMessage002 m -> marshal(m, this::marshal002);
            case IRCMessage003 m -> marshal(m, this::marshal003);
            case IRCMessage004 m -> marshal(m, this::marshal004);
            case IRCMessage005 m -> marshal(m, this::marshal005);
            case IRCMessage010 m -> marshal(m, this::marshal010);
            case IRCMessage212 m -> marshal(m, this::marshal212);
            case IRCMessage219 m -> marshal(m, this::marshal219);
            case IRCMessage221 m -> marshal(m, this::marshal221);
            case IRCMessage242 m -> marshal(m, this::marshal242);
            case IRCMessage251 m -> marshal(m, this::marshal251);
            case IRCMessage252 m -> marshal(m, this::marshal252);
            case IRCMessage253 m -> marshal(m, this::marshal253);
            case IRCMessage254 m -> marshal(m, this::marshal254);
            case IRCMessage255 m -> marshal(m, this::marshal255);
            case IRCMessage256 m -> marshal(m, this::marshal256);
            case IRCMessage257 m -> marshal(m, this::marshal257);
            case IRCMessage258 m -> marshal(m, this::marshal258);
            case IRCMessage259 m -> marshal(m, this::marshal259);
            case IRCMessage263 m -> marshal(m, this::marshal263);
            case IRCMessage265 m -> marshal(m, this::marshal265);
            case IRCMessage266 m -> marshal(m, this::marshal266);
            case IRCMessage276 m -> marshal(m, this::marshal276);
            case IRCMessage301 m -> marshal(m, this::marshal301);
            case IRCMessage302 m -> marshal(m, this::marshal302);
            case IRCMessage305 m -> marshal(m, this::marshal305);
            case IRCMessage306 m -> marshal(m, this::marshal306);
            case IRCMessage307 m -> marshal(m, this::marshal307);
            case IRCMessage311 m -> marshal(m, this::marshal311);
            case IRCMessage312 m -> marshal(m, this::marshal312);
            case IRCMessage313 m -> marshal(m, this::marshal313);
            case IRCMessage314 m -> marshal(m, this::marshal314);
            case IRCMessage315 m -> marshal(m, this::marshal315);
            case IRCMessage317 m -> marshal(m, this::marshal317);
            case IRCMessage318 m -> marshal(m, this::marshal318);
            case IRCMessage319 m -> marshal(m, this::marshal319);
            case IRCMessage320 m -> marshal(m, this::marshal320);
            case IRCMessage321 m -> marshal(m, this::marshal321);
            case IRCMessage322 m -> marshal(m, this::marshal322);
            case IRCMessage323 m -> marshal(m, this::marshal323);
            case IRCMessage324 m -> marshal(m, this::marshal324);
            case IRCMessage329 m -> marshal(m, this::marshal329);
            case IRCMessage330 m -> marshal(m, this::marshal330);
            case IRCMessage331 m -> marshal(m, this::marshal331);
            case IRCMessage332 m -> marshal(m, this::marshal332);
            case IRCMessage333 m -> marshal(m, this::marshal333);
            case IRCMessage336 m -> marshal(m, this::marshal336);
            case IRCMessage337 m -> marshal(m, this::marshal337);
            case IRCMessage338 m -> marshal(m, this::marshal338);
            case IRCMessage341 m -> marshal(m, this::marshal341);
            case IRCMessage346 m -> marshal(m, this::marshal346);
            case IRCMessage347 m -> marshal(m, this::marshal347);
            case IRCMessage348 m -> marshal(m, this::marshal348);
            case IRCMessage349 m -> marshal(m, this::marshal349);
            case IRCMessage351 m -> marshal(m, this::marshal351);
            case IRCMessage352 m -> marshal(m, this::marshal352);
            case IRCMessage353 m -> marshal(m, this::marshal353);
            case IRCMessage364 m -> marshal(m, this::marshal364);
            case IRCMessage365 m -> marshal(m, this::marshal365);
            case IRCMessage366 m -> marshal(m, this::marshal366);
            case IRCMessage367 m -> marshal(m, this::marshal367);
            case IRCMessage368 m -> marshal(m, this::marshal368);
            case IRCMessage369 m -> marshal(m, this::marshal369);
            case IRCMessage371 m -> marshal(m, this::marshal371);
            case IRCMessage372 m -> marshal(m, this::marshal372);
            case IRCMessage374 m -> marshal(m, this::marshal374);
            case IRCMessage375 m -> marshal(m, this::marshal375);
            case IRCMessage376 m -> marshal(m, this::marshal376);
            case IRCMessage378 m -> marshal(m, this::marshal378);
            case IRCMessage379 m -> marshal(m, this::marshal379);
            case IRCMessage381 m -> marshal(m, this::marshal381);
            case IRCMessage382 m -> marshal(m, this::marshal382);
            case IRCMessage391 m -> marshal(m, this::marshal391);
            case IRCMessage400 m -> marshal(m, this::marshal400);
            case IRCMessage401 m -> marshal(m, this::marshal401);
            case IRCMessage402 m -> marshal(m, this::marshal402);
            case IRCMessage403 m -> marshal(m, this::marshal403);
            case IRCMessage404 m -> marshal(m, this::marshal404);
            case IRCMessage405 m -> marshal(m, this::marshal405);
            case IRCMessage406 m -> marshal(m, this::marshal406);
            case IRCMessage409 m -> marshal(m, this::marshal409);
            case IRCMessage411 m -> marshal(m, this::marshal411);
            case IRCMessage412 m -> marshal(m, this::marshal412);
            case IRCMessage417 m -> marshal(m, this::marshal417);
            case IRCMessage421 m -> marshal(m, this::marshal421);
            case IRCMessage422 m -> marshal(m, this::marshal422);
            case IRCMessage431 m -> marshal(m, this::marshal431);
            case IRCMessage432 m -> marshal(m, this::marshal432);
            case IRCMessage433 m -> marshal(m, this::marshal433);
            case IRCMessage436 m -> marshal(m, this::marshal436);
            case IRCMessage441 m -> marshal(m, this::marshal441);
            case IRCMessage442 m -> marshal(m, this::marshal442);
            case IRCMessage443 m -> marshal(m, this::marshal443);
            case IRCMessage451 m -> marshal(m, this::marshal451);
            case IRCMessage461 m -> marshal(m, this::marshal461);
            case IRCMessage462 m -> marshal(m, this::marshal462);
            case IRCMessage464 m -> marshal(m, this::marshal464);
            case IRCMessage465 m -> marshal(m, this::marshal465);
            case IRCMessage471 m -> marshal(m, this::marshal471);
            case IRCMessage472 m -> marshal(m, this::marshal472);
            case IRCMessage473 m -> marshal(m, this::marshal473);
            case IRCMessage474 m -> marshal(m, this::marshal474);
            case IRCMessage475 m -> marshal(m, this::marshal475);
            case IRCMessage476 m -> marshal(m, this::marshal476);
            case IRCMessage481 m -> marshal(m, this::marshal481);
            case IRCMessage482 m -> marshal(m, this::marshal482);
            case IRCMessage483 m -> marshal(m, this::marshal483);
            case IRCMessage491 m -> marshal(m, this::marshal491);
            case IRCMessage501 m -> marshal(m, this::marshal501);
            case IRCMessage502 m -> marshal(m, this::marshal502);
            case IRCMessage524 m -> marshal(m, this::marshal524);
            case IRCMessage525 m -> marshal(m, this::marshal525);
            case IRCMessage670 m -> marshal(m, this::marshal670);
            case IRCMessage671 m -> marshal(m, this::marshal671);
            case IRCMessage691 m -> marshal(m, this::marshal691);
            case IRCMessage696 m -> marshal(m, this::marshal696);
            case IRCMessage704 m -> marshal(m, this::marshal704);
            case IRCMessage705 m -> marshal(m, this::marshal705);
            case IRCMessage706 m -> marshal(m, this::marshal706);
            case IRCMessage723 m -> marshal(m, this::marshal723);
            case IRCMessageUnsupported m -> m.getRawMessage();
            case IRCMessageParseError m -> m.getRawMessage();
            case IRCMessageTooLong m -> m.getRawMessage();
            case IRCMessageNotEnoughParameters m -> m.getRawMessage();
        };
    }

    private <T extends IRCMessage> String marshal(T message, Function<T, List<String>> paramMarshaller) {
        StringBuilder messageBuilder = new StringBuilder();
        if (message.getTags() != null && !message.getTags().isEmpty()) {
            messageBuilder.append('@');
            messageBuilder.append(marshalMap(message.getTags(), ";", this::escapeTag));
            messageBuilder.append(' ');
        }

        if (message.getPrefixName() != null) {
            messageBuilder.append(':');
            messageBuilder.append(
                    marshalPrefix(message.getPrefixName(), message.getPrefixUser(), message.getPrefixHost()));
            messageBuilder.append(' ');
        }

        messageBuilder.append(message.getCommand());

        List<String> params = paramMarshaller.apply(message).stream()
                .filter(Objects::nonNull)
                .filter(not(String::isBlank))
                .toList();
        if (params.isEmpty()) {
            return messageBuilder.toString();
        }

        for (String param : params) {
            messageBuilder.append(' ');
            messageBuilder.append(param);
        }

        return messageBuilder.toString();
    }

    private String escapeTag(String value) {
        StringBuilder result = new StringBuilder(value.length());

        for (char c : value.toCharArray()) {
            switch (c) {
                case ';' -> result.append("\\:");
                case ' ' -> result.append("\\s");
                case '\\' -> result.append("\\\\");
                case '\r' -> result.append("\\r");
                case '\n' -> result.append("\\n");
                default -> result.append(c);
            }
        }

        return result.toString();
    }

    private String marshalPrefix(String name, String user, String host) {
        if (user != null && host != null) {
            return name + "!" + user + "@" + host;
        } else if (user != null) {
            return name + "!" + user;
        } else if (host != null) {
            return name + "@" + host;
        } else {
            return name;
        }
    }

    private List<String> marshalAway(IRCMessageAWAY message) {
        return l(trailing(message.getText()));
    }

    private List<String> marshalCapACK(IRCMessageCAPACK message) {
        return l(
                message.getNick(),
                "ACK",
                trailing(ieList(message.getEnableCapabilities(), message.getDisableCapabilities())));
    }

    private List<String> marshalCapDEL(IRCMessageCAPDEL message) {
        return l(message.getNick(), "DEL", trailing(delimited(" ", message.getCapabilities())));
    }

    private List<String> marshalCapEND(IRCMessageCAPEND message) {
        return l("END");
    }

    private List<String> marshalCapLISTRequest(IRCMessageCAPLISTRequest message) {
        return l("LIST");
    }

    private List<String> marshalCapLISTResponse(IRCMessageCAPLISTResponse message) {
        return l(
                message.getNick(),
                "LIST",
                when(message.isHasMore(), "*"),
                trailing(delimited(" ", message.getCapabilities()), true));
    }

    private List<String> marshalCapLSRequest(IRCMessageCAPLSRequest message) {
        return l("LS", message.getVersion());
    }

    private List<String> marshalCapLSResponse(IRCMessageCAPLSResponse message) {
        return l(
                message.getNick(),
                "LS",
                when(message.isHasMore(), "*"),
                trailing(marshalMap(message.getCapabilities(), " ", Function.identity())));
    }

    private List<String> marshalCapNAK(IRCMessageCAPNAK message) {
        return l(
                message.getNick(),
                "NAK",
                trailing(ieList(message.getEnableCapabilities(), message.getDisableCapabilities())));
    }

    private List<String> marshalCapNEW(IRCMessageCAPNEW message) {
        return l(message.getNick(), "NEW", trailing(marshalMap(message.getCapabilities(), " ", Function.identity())));
    }

    private List<String> marshalCapREQ(IRCMessageCAPREQ message) {
        return l("REQ", trailing(ieList(message.getEnableCapabilities(), message.getDisableCapabilities())));
    }

    private List<String> marshalError(IRCMessageERROR message) {
        return l(trailing(message.getReason()));
    }

    private List<String> marshalJoin0(IRCMessageJOIN0 message) {
        return l("0");
    }

    private List<String> marshalJoinNormal(IRCMessageJOINNormal message) {
        return l(delimited(",", message.getChannels()), delimited(",", message.getKeys()));
    }

    private List<String> marshalKick(IRCMessageKICK message) {
        return l(message.getChannel(), message.getNick(), trailing(message.getReason()));
    }

    private List<String> marshalKill(IRCMessageKILL message) {
        return l(message.getNickname(), trailing(message.getComment()));
    }

    private List<String> marshalMode(IRCMessageMODE message) {
        return l(message.getTarget(), message.getModeString(), delimited(" ", message.getModeArguments()));
    }

    private List<String> marshalNames(IRCMessageNAMES message) {
        return l(delimited(",", message.getChannels()));
    }

    private List<String> marshalNick(IRCMessageNICK message) {
        return l(message.getNick());
    }

    private List<String> marshalNotice(IRCMessageNOTICE message) {
        return l(delimited(",", message.getTargets()), trailing(message.getMessage()));
    }

    private List<String> marshalOper(IRCMessageOPER message) {
        return l(message.getName(), message.getPassword());
    }

    private List<String> marshalPart(IRCMessagePART message) {
        return l(delimited(",", message.getChannels()), trailing(message.getReason()));
    }

    private List<String> marshalPass(IRCMessagePASS message) {
        return l(message.getPass());
    }

    private List<String> marshalPing(IRCMessagePING message) {
        return l(trailing(message.getToken()));
    }

    private List<String> marshalPong(IRCMessagePONG message) {
        return l(message.getServer(), trailing(message.getToken()));
    }

    private List<String> marshalPrivmsg(IRCMessagePRIVMSG message) {
        return l(delimited(",", message.getTargets()), trailing(message.getMessage()));
    }

    private List<String> marshalUser(IRCMessageUSER message) {
        return l(message.getUser(), "0", "*", trailing(message.getRealName()));
    }

    private List<String> marshalQuit(IRCMessageQUIT message) {
        return l(trailing(message.getReason()));
    }

    private List<String> marshalTopic(IRCMessageTOPIC message) {
        return l(message.getChannel(), trailing(message.getTopic()));
    }

    private List<String> marshal001(IRCMessage001 message) {
        return l(message.getClient(), trailing(message.getMessage()));
    }

    private List<String> marshal002(IRCMessage002 message) {
        return l(message.getClient(), trailing(message.getMessage()));
    }

    private List<String> marshal003(IRCMessage003 message) {
        return l(message.getClient(), trailing(message.getMessage()));
    }

    private List<String> marshal004(IRCMessage004 message) {
        return l(
                message.getClient(),
                message.getServerName(),
                message.getVersion(),
                message.getUserModes(),
                message.getChannelModes(),
                message.getChannelModesWithParam());
    }

    private List<String> marshal005(IRCMessage005 message) {
        return l(
                message.getClient(),
                marshalMap(message.getParameters(), " ", Function.identity()),
                trailing("are supported by this server"));
    }

    private List<String> marshal010(IRCMessage010 message) {
        return l(message.getClient(), message.getHostname(), message.getPort(), trailing(message.getInfo()));
    }

    private List<String> marshal212(IRCMessage212 message) {
        return l(
                message.getClient(),
                message.getTargetCommand(),
                message.getCount(),
                message.getByteCount(),
                message.getRemoteCount());
    }

    private List<String> marshal219(IRCMessage219 message) {
        return l(message.getClient(), message.getStatsLetter(), trailing("End of /STATS report"));
    }

    private List<String> marshal221(IRCMessage221 message) {
        return l(message.getClient(), message.getUserModes());
    }

    private List<String> marshal242(IRCMessage242 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal251(IRCMessage251 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal252(IRCMessage252 message) {
        return l(message.getClient(), message.getOps(), trailing("operator(s) online"));
    }

    private List<String> marshal253(IRCMessage253 message) {
        return l(message.getClient(), message.getConnections(), trailing("unknown connection(s)"));
    }

    private List<String> marshal254(IRCMessage254 message) {
        return l(message.getClient(), message.getChannels(), trailing("channels formed"));
    }

    private List<String> marshal255(IRCMessage255 message) {
        return l(message.getClient(), trailing(message.getMessage()));
    }

    private List<String> marshal256(IRCMessage256 message) {
        return l(message.getClient(), message.getServer(), trailing("Administrative info"));
    }

    private List<String> marshal257(IRCMessage257 message) {
        return l(message.getClient(), trailing(message.getInfo()));
    }

    private List<String> marshal258(IRCMessage258 message) {
        return l(message.getClient(), trailing(message.getInfo()));
    }

    private List<String> marshal259(IRCMessage259 message) {
        return l(message.getClient(), trailing(message.getInfo()));
    }

    private List<String> marshal263(IRCMessage263 message) {
        return l(message.getClient(), message.getTargetCommand(), trailing(message.getText()));
    }

    private List<String> marshal265(IRCMessage265 message) {
        return l(message.getClient(), message.getLocalUsers(), message.getMaxLocalUsers(), trailing(message.getText()));
    }

    private List<String> marshal266(IRCMessage266 message) {
        return l(
                message.getClient(),
                message.getGlobalUsers(),
                message.getMaxGlobalUsers(),
                trailing(message.getText()));
    }

    private List<String> marshal276(IRCMessage276 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal301(IRCMessage301 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getAwayMessage()));
    }

    private List<String> marshal302(IRCMessage302 message) {
        return l(message.getClient(), trailing(delimited(" ", message.getUserhosts())));
    }

    private List<String> marshal305(IRCMessage305 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal306(IRCMessage306 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal307(IRCMessage307 message) {
        return l(message.getClient(), message.getNick(), trailing("has identified for this nick"));
    }

    private List<String> marshal311(IRCMessage311 message) {
        return l(
                message.getClient(),
                message.getNick(),
                message.getUser(),
                message.getHost(),
                "*",
                trailing(message.getRealName()));
    }

    private List<String> marshal312(IRCMessage312 message) {
        return l(message.getClient(), message.getNick(), message.getServer(), trailing(message.getServerInfo()));
    }

    private List<String> marshal313(IRCMessage313 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal314(IRCMessage314 message) {
        return l(
                message.getClient(),
                message.getNick(),
                message.getUser(),
                message.getHost(),
                "*",
                trailing(message.getRealName()));
    }

    private List<String> marshal315(IRCMessage315 message) {
        return l(message.getClient(), message.getMask(), trailing("End of WHO list"));
    }

    private List<String> marshal317(IRCMessage317 message) {
        return l(
                message.getClient(),
                message.getNick(),
                message.getSecondsIdle(),
                message.getSignOnTime(),
                trailing("%s, %s"
                        .formatted(
                                message.getSecondsIdle(),
                                DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(message.getSignOnTime())))));
    }

    private List<String> marshal318(IRCMessage318 message) {
        return l(message.getClient(), message.getNick(), trailing("End of /WHOIS list"));
    }

    private List<String> marshal319(IRCMessage319 message) {
        return l(
                message.getClient(),
                message.getNick(),
                trailing(delimited(" ", zip(message.getPrefixes(), message.getChannels()))));
    }

    private List<String> marshal320(IRCMessage320 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal321(IRCMessage321 message) {
        return l(message.getClient(), "Channel", trailing("Users  Name"));
    }

    private List<String> marshal322(IRCMessage322 message) {
        return l(message.getClient(), message.getChannel(), message.getClientCount(), trailing(message.getTopic()));
    }

    private List<String> marshal323(IRCMessage323 message) {
        return l(message.getClient(), trailing("End of /LIST"));
    }

    private List<String> marshal324(IRCMessage324 message) {
        return l(
                message.getClient(),
                message.getChannel(),
                message.getModeString(),
                delimited(" ", message.getModeArguments()));
    }

    private List<String> marshal329(IRCMessage329 message) {
        return l(message.getClient(), message.getChannel(), message.getCreationTime());
    }

    private List<String> marshal330(IRCMessage330 message) {
        return l(message.getClient(), message.getNick(), message.getAccount(), trailing("is logged in as"));
    }

    private List<String> marshal331(IRCMessage331 message) {
        return l(message.getClient(), message.getChannel(), trailing("No topic is set"));
    }

    private List<String> marshal332(IRCMessage332 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getTopic()));
    }

    private List<String> marshal333(IRCMessage333 message) {
        return l(message.getClient(), message.getChannel(), message.getSetBy(), message.getSetAt());
    }

    private List<String> marshal336(IRCMessage336 message) {
        return l(message.getClient(), message.getChannel());
    }

    private List<String> marshal337(IRCMessage337 message) {
        return l(message.getClient(), trailing("End of /INVITE list"));
    }

    // probably need to get some actual information here but that's out of scope for now
    private List<String> marshal338(IRCMessage338 message) {
        if (message.getHostname() != null && message.getIp() != null) {
            return l(
                    message.getClient(),
                    message.getNick(),
                    message.getUsername() + "@" + message.getHostname(),
                    message.getIp(),
                    trailing("is actually using host"));
        }
        if (message.getHostname() != null) {
            return l(message.getClient(), message.getNick(), message.getHostname(), trailing("Is actually using host"));
        }
        if (message.getIp() != null) {
            return l(message.getClient(), message.getNick(), message.getIp(), trailing("Is actually using host"));
        }
        return l(message.getClient(), message.getNick(), trailing("is actually ..."));
    }

    private List<String> marshal341(IRCMessage341 message) {
        return l(message.getClient(), message.getNick(), message.getChannel());
    }

    private List<String> marshal346(IRCMessage346 message) {
        return l(message.getClient(), message.getChannel(), message.getMask());
    }

    private List<String> marshal347(IRCMessage347 message) {
        return l(message.getClient(), message.getChannel(), trailing("End of Channel Invite Exception List"));
    }

    private List<String> marshal348(IRCMessage348 message) {
        return l(message.getClient(), message.getChannel(), message.getMask());
    }

    private List<String> marshal349(IRCMessage349 message) {
        return l(message.getClient(), message.getChannel(), trailing("End of channel exception list"));
    }

    private List<String> marshal351(IRCMessage351 message) {
        return l(message.getClient(), message.getVersion(), message.getServer(), trailing(message.getComments()));
    }

    private List<String> marshal352(IRCMessage352 message) {
        return l(
                message.getClient(),
                message.getChannel(),
                message.getUser(),
                message.getHost(),
                message.getServer(),
                message.getNick(),
                message.getFlags(),
                trailing(message.getHopcount() + " " + message.getRealName()));
    }

    private List<String> marshal353(IRCMessage353 message) {
        return l(
                message.getClient(),
                message.getSymbol(),
                message.getChannel(),
                trailing(delimited(" ", zip(message.getModes(), message.getNicks()))));
    }

    private List<String> marshal364(IRCMessage364 message) {
        return l(
                message.getClient(),
                message.getServer1(),
                message.getServer2(),
                trailing(message.getHopCount() + " " + message.getServerInfo()));
    }

    private List<String> marshal365(IRCMessage365 message) {
        return l(message.getClient(), "*", trailing("End of /LINKS list"));
    }

    private List<String> marshal366(IRCMessage366 message) {
        return l(message.getClient(), message.getChannel(), trailing("End of /NAMES list"));
    }

    private List<String> marshal367(IRCMessage367 message) {
        return l(message.getClient(), message.getChannel(), message.getMask(), message.getSetBy(), message.getSetAt());
    }

    private List<String> marshal368(IRCMessage368 message) {
        return l(message.getClient(), message.getChannel(), trailing("End of channel ban list"));
    }

    private List<String> marshal369(IRCMessage369 message) {
        return l(message.getClient(), message.getNick(), trailing("End of WHOWAS"));
    }

    private List<String> marshal371(IRCMessage371 message) {
        return l(message.getClient(), trailing(message.getInfo()));
    }

    private List<String> marshal372(IRCMessage372 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal374(IRCMessage374 message) {
        return l(message.getClient(), trailing("End of INFO list"));
    }

    private List<String> marshal375(IRCMessage375 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal376(IRCMessage376 message) {
        return l(message.getClient(), trailing("End of /MOTD command"));
    }

    private List<String> marshal378(IRCMessage378 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal379(IRCMessage379 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getModes()));
    }

    private List<String> marshal381(IRCMessage381 message) {
        return l(message.getClient(), trailing("You are now an IRC operator"));
    }

    private List<String> marshal382(IRCMessage382 message) {
        return l(message.getClient(), message.getConfigFile(), trailing("Rehashing"));
    }

    private List<String> marshal391(IRCMessage391 message) {
        return l(
                message.getClient(),
                message.getServer(),
                message.getTimestamp(),
                message.getTsOffset(),
                trailing(message.getTimeString()));
    }

    private List<String> marshal400(IRCMessage400 message) {
        return l(message.getClient(), delimited(" ", message.getCommands()), trailing(message.getInfo()));
    }

    private List<String> marshal401(IRCMessage401 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal402(IRCMessage402 message) {
        return l(message.getClient(), message.getServer(), trailing(message.getText()));
    }

    private List<String> marshal403(IRCMessage403 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal404(IRCMessage404 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal405(IRCMessage405 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal406(IRCMessage406 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal409(IRCMessage409 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal411(IRCMessage411 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal412(IRCMessage412 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal417(IRCMessage417 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal421(IRCMessage421 message) {
        return l(message.getClient(), message.getInvalidCommand(), trailing("Unknown command"));
    }

    private List<String> marshal422(IRCMessage422 message) {
        return l(message.getClient(), trailing("MOTD File is missing"));
    }

    private List<String> marshal431(IRCMessage431 message) {
        return l(message.getClient(), trailing("No nickname given"));
    }

    private List<String> marshal432(IRCMessage432 message) {
        return l(message.getClient(), message.getNick(), trailing("Erroneous nickname"));
    }

    private List<String> marshal433(IRCMessage433 message) {
        return l(message.getClient(), message.getNick(), trailing("Nickname is already in use"));
    }

    private List<String> marshal436(IRCMessage436 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal441(IRCMessage441 message) {
        return l(message.getClient(), message.getNick(), message.getChannel(), trailing("They aren't on that channel"));
    }

    private List<String> marshal442(IRCMessage442 message) {
        return l(message.getClient(), message.getChannel(), trailing("You're not on that channel"));
    }

    private List<String> marshal443(IRCMessage443 message) {
        return l(message.getClient(), message.getNick(), message.getChannel(), trailing("is already on channel"));
    }

    private List<String> marshal451(IRCMessage451 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal461(IRCMessage461 message) {
        return l(message.getClient(), message.getInvalidCommand(), trailing(message.getText()));
    }

    private List<String> marshal462(IRCMessage462 message) {
        return l(message.getClient(), trailing("You may not reregister"));
    }

    private List<String> marshal464(IRCMessage464 message) {
        return l(message.getClient(), trailing("Password incorrect"));
    }

    private List<String> marshal465(IRCMessage465 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal471(IRCMessage471 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal472(IRCMessage472 message) {
        return l(message.getClient(), message.getMode(), trailing(message.getText()));
    }

    private List<String> marshal473(IRCMessage473 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal474(IRCMessage474 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal475(IRCMessage475 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal476(IRCMessage476 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal481(IRCMessage481 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal482(IRCMessage482 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal483(IRCMessage483 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal491(IRCMessage491 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal501(IRCMessage501 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal502(IRCMessage502 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal524(IRCMessage524 message) {
        return l(message.getClient(), message.getSubject(), trailing(message.getText()));
    }

    private List<String> marshal525(IRCMessage525 message) {
        return l(message.getClient(), message.getChannel(), trailing(message.getText()));
    }

    private List<String> marshal670(IRCMessage670 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal671(IRCMessage671 message) {
        return l(message.getClient(), message.getNick(), trailing(message.getText()));
    }

    private List<String> marshal691(IRCMessage691 message) {
        return l(message.getClient(), trailing(message.getText()));
    }

    private List<String> marshal696(IRCMessage696 message) {
        return l(
                message.getClient(),
                message.getTarget(),
                message.getMode(),
                message.getParameter(),
                trailing(message.getDescription()));
    }

    private List<String> marshal704(IRCMessage704 message) {
        return l(message.getClient(), message.getSubject(), trailing(message.getText()));
    }

    private List<String> marshal705(IRCMessage705 message) {
        return l(message.getClient(), message.getSubject(), trailing(message.getText()));
    }

    private List<String> marshal706(IRCMessage706 message) {
        return l(message.getClient(), message.getSubject(), trailing(message.getText()));
    }

    private List<String> marshal723(IRCMessage723 message) {
        return l(message.getClient(), message.getPriv(), trailing(message.getText()));
    }

    private <T> String marshalMap(SequencedMap<String, String> map, String delimiter, Function<String, String> mapper) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        return map.sequencedEntrySet().stream()
                .map(e -> e.getValue() == null || e.getValue().isEmpty()
                        ? e.getKey()
                        : e.getKey() + "=" + mapper.apply(e.getValue()))
                .collect(Collectors.joining(delimiter));
    }

    private List<String> l(Object... values) {
        List<String> result = new ArrayList<>();
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            result.add(value.toString());
        }
        return result;
    }

    private String when(boolean flag, String value) {
        return flag ? value : null;
    }

    private String ieList(List<String> inclusion, List<String> exclusion) {
        return Stream.concat(inclusion.stream(), exclusion.stream().map(s -> "-" + s))
                .collect(Collectors.joining(" "));
    }

    private String trailing(String value) {
        if (value == null) {
            return null;
        }
        return ":" + value;
    }

    private String trailing(String value, boolean sendBlank) {
        if (value == null && sendBlank) {
            return ":";
        }
        return ":" + value;
    }

    private String delimited(String delimiter, List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(delimiter, values);
    }

    private <A, B> List<String> zip(List<A> a, List<B> b) {
        List<String> results = new ArrayList<>();
        for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
            String result = "";
            if (i < a.size()) {
                result += Objects.requireNonNullElse(a.get(i), "");
            }
            if (i < b.size()) {
                result += Objects.requireNonNullElse(b.get(i), "");
            }
            if (!result.isBlank()) {
                results.add(result);
            }
        }
        return results;
    }
}
