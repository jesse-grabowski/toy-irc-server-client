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

import static com.jessegrabowski.irc.protocol.dsl.DSL.greedyOptional;
import static com.jessegrabowski.irc.protocol.dsl.DSL.greedyRequired;
import static com.jessegrabowski.irc.protocol.dsl.DSL.greedyRequiredMap;
import static com.jessegrabowski.irc.protocol.dsl.DSL.optional;
import static com.jessegrabowski.irc.protocol.dsl.DSL.optionalAllowEmpty;
import static com.jessegrabowski.irc.protocol.dsl.DSL.required;
import static com.jessegrabowski.irc.protocol.dsl.DSL.requiredAllowEmpty;
import static com.jessegrabowski.irc.protocol.dsl.DSL.splitRequired;
import static com.jessegrabowski.irc.protocol.dsl.DSL.splitString;
import static java.util.function.Predicate.not;

import com.jessegrabowski.irc.protocol.dsl.NotEnoughParametersException;
import com.jessegrabowski.irc.protocol.dsl.ParameterExtractor;
import com.jessegrabowski.irc.protocol.dsl.ParameterPlanner;
import com.jessegrabowski.irc.protocol.dsl.ParserErrorException;
import com.jessegrabowski.irc.protocol.dsl.SplittingParameterInjector;
import com.jessegrabowski.irc.protocol.model.*;
import com.jessegrabowski.irc.server.IRCServerParameters;
import com.jessegrabowski.irc.util.Pair;
import com.jessegrabowski.irc.util.ThrowingFunction;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IRCMessageUnmarshaller {

    // This is really hard to read, but it's just splitting out the tags/prefix/command/params parts
    // as defined in the modern IRC grammar (but not processing individual tags/params yet)
    private static final Pattern MESSAGE_PATTERN = Pattern.compile(
            "^(?:@(?<tags>[^\\s\\u0000]+)\\x20+)?(?::(?<prefix>[^\\s\\u0000]+)\\x20+)?(?<command>(?:[A-Za-z]+)|(?:\\d{3}))(?:\\x20+(?<params>[^\\u0000\\r\\n]+))?\\x20*$");

    private static final Pattern PREFIX_PATTERN = Pattern.compile(
            "^(?<name>[^\\s\\u0000!@]+)(?:!(?<user>[^\\s\\u0000!@]+))?(?:@(?<host>[^\\s\\u0000!@]+))?$");

    /**
     * Unmarshals the provided raw IRC message into an {@link IRCMessage} object.
     *
     * @param serverParameters server parameters from RPL_ISUPPORT, mostly for prefix detection
     * @param charset the {@link Charset} used to interpret the encoding of the input message.
     * @param message the raw IRC message as a {@link String} to be unmarshaled, without trailing
     *     newline.
     * @return an {@link IRCMessage} representing the unmarshaled input.
     */
    public IRCMessage unmarshal(IRCServerParameters serverParameters, Charset charset, String message) {
        try {
            enforceLength(charset, message);
        } catch (MessageTooLongException e) {
            return new IRCMessageTooLong(null, message, new LinkedHashMap<>(), null, null, null);
        } catch (IllegalArgumentException e) {
            return new IRCMessageUnsupported(null, message, new LinkedHashMap<>(), null, null, null, e.getMessage());
        }
        Matcher matcher = MESSAGE_PATTERN.matcher(message);
        if (!matcher.matches()) {
            return new IRCMessageUnsupported(
                    null, message, new LinkedHashMap<>(), null, null, null, "message is malformed");
        }

        String command = matcher.group("command").toUpperCase(Locale.ROOT);

        SequencedMap<String, String> tags = null;
        PrefixParts prefix = null;
        List<String> params = null;
        try {
            tags = parseTags(matcher.group("tags"));
            prefix = parsePrefix(matcher.group("prefix"));
            params = parseParams(matcher.group("params"));
        } catch (Exception e) {
            return new IRCMessageParseError(command, message, tags, null, null, null, e.getMessage());
        }
        Parameters parameters = new Parameters(
                new HashSet<>(serverParameters.getPrefixes().values()), message, command, tags, prefix, params);

        try {
            return switch (command) {
                case IRCMessageAWAY.COMMAND -> parseAway(parameters);
                case "CAP" -> parseCap(parameters);
                case IRCMessageERROR.COMMAND -> parseExact(parameters, "message", IRCMessageERROR::new);
                case IRCMessageHELP.COMMAND -> parseHelp(parameters);
                case IRCMessageHELPOP.COMMAND -> parseHelpOp(parameters);
                case IRCMessageINFO.COMMAND -> parseExact(parameters, IRCMessageINFO::new);
                case IRCMessageINVITE.COMMAND -> parseInvite(parameters);
                case "JOIN" -> parseJoin(parameters);
                case IRCMessageKICK.COMMAND -> parseKick(parameters);
                case IRCMessageKILL.COMMAND -> parseExact(parameters, "nickname", "comment", IRCMessageKILL::new);
                case IRCMessageLIST.COMMAND -> parseList(parameters);
                case IRCMessageLUSERS.COMMAND -> parseExact(parameters, IRCMessageLUSERS::new);
                case IRCMessageMODE.COMMAND -> parseMode(parameters);
                case IRCMessageMOTD.COMMAND -> parseMOTD(parameters);
                case IRCMessageNAMES.COMMAND -> parseNames(parameters);
                case IRCMessageNICK.COMMAND -> parseExact(parameters, "nickname", IRCMessageNICK::new);
                case IRCMessageNOTICE.COMMAND -> parseNotice(parameters);
                case IRCMessageOPER.COMMAND -> parseExact(parameters, "name", "password", IRCMessageOPER::new);
                case IRCMessagePART.COMMAND -> parsePart(parameters);
                case IRCMessagePASS.COMMAND -> parseExact(parameters, "password", IRCMessagePASS::new);
                case IRCMessagePING.COMMAND -> parsePing(parameters);
                case IRCMessagePONG.COMMAND -> parsePong(parameters);
                case IRCMessagePRIVMSG.COMMAND -> parsePrivmsg(parameters);
                case IRCMessageUSER.COMMAND -> parseUser(parameters);
                case IRCMessageUSERHOST.COMMAND -> parseUserHost(parameters);
                case IRCMessageQUIT.COMMAND -> parseQuit(parameters);
                case IRCMessageTOPIC.COMMAND -> parseTopic(parameters);
                case IRCMessageWHO.COMMAND -> parseExact(parameters, "mask", IRCMessageWHO::new);
                case IRCMessageWHOIS.COMMAND -> parseWhoIs(parameters);
                case IRCMessageWHOWAS.COMMAND -> parseWhoWas(parameters);
                case IRCMessage001.COMMAND -> parseExact(parameters, "client", "message", IRCMessage001::new);
                case IRCMessage002.COMMAND -> parseExact(parameters, "client", "message", IRCMessage002::new);
                case IRCMessage003.COMMAND -> parseExact(parameters, "client", "message", IRCMessage003::new);
                case IRCMessage004.COMMAND -> parse004(parameters);
                case IRCMessage005.COMMAND -> parse005(parameters);
                case IRCMessage010.COMMAND -> parse010(parameters);
                case IRCMessage212.COMMAND -> parse212(parameters);
                case IRCMessage219.COMMAND -> parseExact(parameters, "client", "stats letter", IRCMessage219::new);
                case IRCMessage221.COMMAND -> parseExact(parameters, "client", "user modes", IRCMessage221::new);
                case IRCMessage242.COMMAND -> parseExact(parameters, "client", "message", IRCMessage242::new);
                case IRCMessage251.COMMAND -> parseExact(parameters, "client", "message", IRCMessage251::new);
                case IRCMessage252.COMMAND -> parse252(parameters);
                case IRCMessage253.COMMAND -> parse253(parameters);
                case IRCMessage254.COMMAND -> parse254(parameters);
                case IRCMessage255.COMMAND -> parseExact(parameters, "client", "message", IRCMessage255::new);
                case IRCMessage256.COMMAND -> parse256(parameters);
                case IRCMessage257.COMMAND -> parseExact(parameters, "client", "info", IRCMessage257::new);
                case IRCMessage258.COMMAND -> parseExact(parameters, "client", "info", IRCMessage258::new);
                case IRCMessage259.COMMAND -> parseExact(parameters, "client", "info", IRCMessage259::new);
                case IRCMessage263.COMMAND ->
                    parseExact(parameters, "client", "command", "message", IRCMessage263::new);
                case IRCMessage265.COMMAND -> parse265(parameters);
                case IRCMessage266.COMMAND -> parse266(parameters);
                case IRCMessage276.COMMAND -> parseExact(parameters, "client", "nick", "message", IRCMessage276::new);
                case IRCMessage301.COMMAND -> parseExact(parameters, "client", "nick", "message", IRCMessage301::new);
                case IRCMessage302.COMMAND -> parse302(parameters);
                case IRCMessage305.COMMAND -> parseExact(parameters, "client", "message", IRCMessage305::new);
                case IRCMessage306.COMMAND -> parseExact(parameters, "client", "message", IRCMessage306::new);
                case IRCMessage307.COMMAND -> parseExact(parameters, "client", "nick", IRCMessage307::new);
                case IRCMessage311.COMMAND -> parse311(parameters);
                case IRCMessage312.COMMAND ->
                    parseExact(parameters, "client", "nick", "server", "server info", IRCMessage312::new);
                case IRCMessage313.COMMAND -> parseExact(parameters, "client", "nick", "message", IRCMessage313::new);
                case IRCMessage314.COMMAND -> parse314(parameters);
                case IRCMessage315.COMMAND -> parseExact(parameters, "client", "mask", IRCMessage315::new);
                case IRCMessage317.COMMAND -> parse317(parameters);
                case IRCMessage318.COMMAND -> parseExact(parameters, "client", "nick", IRCMessage318::new);
                case IRCMessage319.COMMAND -> parse319(parameters);
                case IRCMessage320.COMMAND -> parseExact(parameters, "client", "nick", "message", IRCMessage320::new);
                case IRCMessage321.COMMAND -> parseExact(parameters, "client", IRCMessage321::new);
                case IRCMessage322.COMMAND -> parse322(parameters);
                case IRCMessage323.COMMAND -> parseExact(parameters, "client", IRCMessage323::new);
                case IRCMessage324.COMMAND -> parse324(parameters);
                case IRCMessage329.COMMAND -> parse329(parameters);
                case IRCMessage330.COMMAND -> parseExact(parameters, "client", "nick", "account", IRCMessage330::new);
                case IRCMessage331.COMMAND -> parseExact(parameters, "client", "channel", IRCMessage331::new);
                case IRCMessage332.COMMAND -> parseExact(parameters, "client", "channel", "topic", IRCMessage332::new);
                case IRCMessage333.COMMAND -> parse333(parameters);
                case IRCMessage336.COMMAND -> parseExact(parameters, "client", "channel", IRCMessage336::new);
                case IRCMessage337.COMMAND -> parseExact(parameters, "client", IRCMessage337::new);
                case IRCMessage338.COMMAND -> parse338(parameters);
                case IRCMessage341.COMMAND -> parseExact(parameters, "client", "nick", "channel", IRCMessage341::new);
                case IRCMessage346.COMMAND -> parseExact(parameters, "client", "channel", "mask", IRCMessage346::new);
                case IRCMessage347.COMMAND -> parseExact(parameters, "client", "channel", IRCMessage347::new);
                case IRCMessage348.COMMAND -> parseExact(parameters, "client", "channel", "mask", IRCMessage348::new);
                case IRCMessage349.COMMAND -> parseExact(parameters, "client", "channel", IRCMessage349::new);
                case IRCMessage351.COMMAND ->
                    parseExact(parameters, "client", "version", "server", "comments", IRCMessage351::new);
                case IRCMessage352.COMMAND -> parse352(parameters);
                case IRCMessage353.COMMAND -> parse353(parameters);
                case IRCMessage364.COMMAND -> parse364(parameters);
                case IRCMessage365.COMMAND -> parseExact(parameters, "client", IRCMessage365::new);
                case IRCMessage366.COMMAND -> parseExact(parameters, "client", "channel", IRCMessage366::new);
                case IRCMessage367.COMMAND -> parse367(parameters);
                case IRCMessage368.COMMAND -> parseExact(parameters, "client", "channel", IRCMessage368::new);
                case IRCMessage369.COMMAND -> parseExact(parameters, "client", "nick", IRCMessage369::new);
                case IRCMessage371.COMMAND -> parseExact(parameters, "client", "string", IRCMessage371::new);
                case IRCMessage372.COMMAND -> parseExact(parameters, "client", "line", IRCMessage372::new);
                case IRCMessage374.COMMAND -> parseExact(parameters, "client", IRCMessage374::new);
                case IRCMessage375.COMMAND -> parseExact(parameters, "client", "motd", IRCMessage375::new);
                case IRCMessage376.COMMAND -> parseExact(parameters, "client", IRCMessage376::new);
                case IRCMessage378.COMMAND -> parseExact(parameters, "client", "nick", "text", IRCMessage378::new);
                case IRCMessage379.COMMAND -> parseExact(parameters, "client", "nick", "modes", IRCMessage379::new);
                case IRCMessage381.COMMAND -> parseExact(parameters, "client", IRCMessage381::new);
                case IRCMessage382.COMMAND -> parseExact(parameters, "client", "file", IRCMessage382::new);
                case IRCMessage391.COMMAND -> parse391(parameters);
                case IRCMessage400.COMMAND -> parse400(parameters);
                case IRCMessage401.COMMAND -> parseExact(parameters, "client", "nickname", "text", IRCMessage401::new);
                case IRCMessage402.COMMAND ->
                    parseExact(parameters, "client", "server name", "text", IRCMessage402::new);
                case IRCMessage403.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage403::new);
                case IRCMessage404.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage404::new);
                case IRCMessage405.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage405::new);
                case IRCMessage406.COMMAND -> parseExact(parameters, "client", "nickname", "text", IRCMessage406::new);
                case IRCMessage409.COMMAND -> parseExact(parameters, "client", "text", IRCMessage409::new);
                case IRCMessage410.COMMAND -> parseExact(parameters, "client", "command", "text", IRCMessage410::new);
                case IRCMessage411.COMMAND -> parseExact(parameters, "client", "text", IRCMessage411::new);
                case IRCMessage412.COMMAND -> parseExact(parameters, "client", "text", IRCMessage412::new);
                case IRCMessage417.COMMAND -> parseExact(parameters, "client", "text", IRCMessage417::new);
                case IRCMessage421.COMMAND -> parseExact(parameters, "client", "command", IRCMessage421::new);
                case IRCMessage422.COMMAND -> parseExact(parameters, "client", IRCMessage422::new);
                case IRCMessage431.COMMAND -> parseExact(parameters, "client", IRCMessage431::new);
                case IRCMessage432.COMMAND -> parseExact(parameters, "client", "nick", IRCMessage432::new);
                case IRCMessage433.COMMAND -> parseExact(parameters, "client", "nick", IRCMessage433::new);
                case IRCMessage436.COMMAND -> parseExact(parameters, "client", "nick", "text", IRCMessage436::new);
                case IRCMessage441.COMMAND -> parseExact(parameters, "client", "nick", "channel", IRCMessage441::new);
                case IRCMessage442.COMMAND -> parseExact(parameters, "client", "channel", IRCMessage442::new);
                case IRCMessage443.COMMAND -> parseExact(parameters, "client", "nick", "channel", IRCMessage443::new);
                case IRCMessage451.COMMAND -> parseExact(parameters, "client", "text", IRCMessage451::new);
                case IRCMessage461.COMMAND -> parseExact(parameters, "client", "command", "text", IRCMessage461::new);
                case IRCMessage462.COMMAND -> parseExact(parameters, "client", IRCMessage462::new);
                case IRCMessage464.COMMAND -> parseExact(parameters, "client", IRCMessage464::new);
                case IRCMessage465.COMMAND -> parseExact(parameters, "client", "text", IRCMessage465::new);
                case IRCMessage471.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage471::new);
                case IRCMessage472.COMMAND -> parse472(parameters);
                case IRCMessage473.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage473::new);
                case IRCMessage474.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage474::new);
                case IRCMessage475.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage475::new);
                case IRCMessage476.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage476::new);
                case IRCMessage481.COMMAND -> parseExact(parameters, "client", "text", IRCMessage481::new);
                case IRCMessage482.COMMAND -> parseExact(parameters, "client", "channel", "text", IRCMessage482::new);
                case IRCMessage483.COMMAND -> parseExact(parameters, "client", "text", IRCMessage483::new);
                case IRCMessage491.COMMAND -> parseExact(parameters, "client", "text", IRCMessage491::new);
                case IRCMessage501.COMMAND -> parseExact(parameters, "client", "text", IRCMessage501::new);
                case IRCMessage502.COMMAND -> parseExact(parameters, "client", "text", IRCMessage502::new);
                case IRCMessage524.COMMAND -> parseExact(parameters, "client", "subject", "text", IRCMessage524::new);
                case IRCMessage525.COMMAND ->
                    parseExact(parameters, "client", "target chan", "text", IRCMessage525::new);
                case IRCMessage670.COMMAND -> parseExact(parameters, "client", "text", IRCMessage670::new);
                case IRCMessage671.COMMAND -> parseExact(parameters, "client", "nick", "text", IRCMessage671::new);
                case IRCMessage691.COMMAND -> parseExact(parameters, "client", "text", IRCMessage691::new);
                case IRCMessage696.COMMAND -> parse696(parameters);
                case IRCMessage704.COMMAND -> parseExact(parameters, "client", "subject", "text", IRCMessage704::new);
                case IRCMessage705.COMMAND -> parseExact(parameters, "client", "subject", "text", IRCMessage705::new);
                case IRCMessage706.COMMAND -> parseExact(parameters, "client", "subject", "text", IRCMessage706::new);
                case IRCMessage723.COMMAND -> parseExact(parameters, "client", "priv", "text", IRCMessage723::new);
                default ->
                    new IRCMessageUnsupported(
                            command,
                            message,
                            tags,
                            prefix.name(),
                            prefix.user(),
                            prefix.host(),
                            "command not recognized");
            };
        } catch (NotEnoughParametersException e) {
            return new IRCMessageNotEnoughParameters(
                    command, message, tags, prefix.name(), prefix.user(), prefix.host());
        } catch (Exception e) {
            return new IRCMessageParseError(
                    command, message, tags, prefix.name(), prefix.user(), prefix.host(), e.getMessage());
        }
    }

    private void enforceLength(Charset charset, String message) throws MessageTooLongException {
        // empty methods can't be valid, no need to even check
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message is empty");
        }
        // 4095 (tags) + 510 (body), quick sanity check before we get more involved
        if (message.length() > 4605) {
            throw new MessageTooLongException();
        }
        int i = 0;
        if (message.charAt(0) == '@') {
            i = message.indexOf(' ', 1);
            if (i == -1) {
                throw new IllegalArgumentException("message contains only tags");
            }
            while (i < message.length() && message.charAt(i) == ' ') {
                i++;
            }
            if (message.substring(0, i).getBytes(charset).length > 4095) {
                throw new MessageTooLongException();
            }
        }
        if (message.substring(i).getBytes(charset).length > 510) {
            throw new MessageTooLongException();
        }
    }

    private SequencedMap<String, String> parseTags(String tags) {
        SequencedMap<String, String> map = new LinkedHashMap<>();
        if (tags == null || tags.isBlank()) {
            return map;
        }

        String[] entries = tags.split(";");
        for (String entry : entries) {
            String[] kv = entry.split("=", 2);
            if (kv[0].isEmpty()) {
                continue;
            }
            // per IRCv3 duplicates are clobbered (last wins)
            if (kv.length == 1) {
                map.put(kv[0], "");
            } else {
                map.put(kv[0], unescapeTag(kv[1]));
            }
        }
        return map;
    }

    private String unescapeTag(String tag) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (char c : tag.toCharArray()) {
            if (escaped) {
                escaped = false;
                switch (c) {
                    case ':' -> result.append(';');
                    case 's' -> result.append(' ');
                    case '\\' -> result.append('\\');
                    case 'r' -> result.append('\r');
                    case 'n' -> result.append('\n');
                    default -> result.append('\\').append(c);
                }
            } else if (c == '\\') {
                escaped = true;
            } else {
                result.append(c);
            }
        }
        if (escaped) {
            result.append('\\');
        }
        return result.toString();
    }

    private PrefixParts parsePrefix(String prefix) {
        if (prefix == null) {
            return new PrefixParts();
        }

        Matcher matcher = PREFIX_PATTERN.matcher(prefix);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid prefix: " + prefix);
        }
        String name = matcher.group("name");
        String user = matcher.group("user");
        String host = matcher.group("host");
        return new PrefixParts(name, user, host);
    }

    private List<String> parseParams(String paramString) {
        if (paramString == null || paramString.isBlank()) {
            return List.of();
        }

        int length = paramString.length();
        if (paramString.charAt(0) == ':') {
            return List.of(paramString.substring(1));
        }

        int i = 0;
        List<String> params = new ArrayList<>();
        while (i < length) {
            int start = i;
            while (i < length && paramString.charAt(i) != ' ') {
                i++;
            }
            params.add(paramString.substring(start, i));
            while (i < length && paramString.charAt(i) == ' ') {
                i++;
            }
            if (i < length && paramString.charAt(i) == ':') {
                params.add(paramString.substring(i + 1));
                break;
            }
        }

        return params;
    }

    private IRCMessageAWAY parseAway(Parameters parameters) throws Exception {
        return parameters.inject(optional("text"), IRCMessageAWAY::new);
    }

    private IRCMessage parseCap(Parameters parameters) throws Exception {
        return parameters
                .injectConditionally()
                .ifIndexEquals(0, "END", p -> p.inject(IRCMessageCAPEND::new))
                .ifIndexEquals(0, "LS", p -> p.inject(optional("version"), IRCMessageCAPLSRequest::new))
                .ifIndexEquals(0, "LIST", p -> p.inject(IRCMessageCAPLISTRequest::new))
                .ifIndexEquals(0, "REQ", p -> {
                    SplittingParameterInjector<List<String>, List<String>> splitter =
                            splitRequired("extension", this::splitEnabledDisabledCaps);
                    return p.inject(splitter.left(List.of()), splitter.right(List.of()), IRCMessageCAPREQ::new);
                })
                .ifIndexEquals(1, "ACK", p -> {
                    SplittingParameterInjector<List<String>, List<String>> splitter =
                            splitRequired("extension", this::splitEnabledDisabledCaps);
                    return p.inject(
                            required("nick"),
                            splitter.left(List.of()),
                            splitter.right(List.of()),
                            IRCMessageCAPACK::new);
                })
                .ifIndexEquals(
                        1,
                        "DEL",
                        p -> p.inject(
                                required("nick"),
                                required("extension", splitToListDelimited("\\s+")),
                                IRCMessageCAPDEL::new))
                .ifIndexEquals(
                        1,
                        "LS",
                        p -> p.inject(
                                required("nick"),
                                optional("*", s -> true, false),
                                required("extensions", splitToMapDelimited("\\s+")),
                                IRCMessageCAPLSResponse::new))
                .ifIndexEquals(
                        1,
                        "LIST",
                        p -> p.inject(
                                required("nick"),
                                optional("*", s -> true, false),
                                required("extension", splitToListDelimited("\\s+")),
                                IRCMessageCAPLISTResponse::new))
                .ifIndexEquals(1, "NAK", p -> {
                    SplittingParameterInjector<List<String>, List<String>> splitter =
                            splitRequired("extension", this::splitEnabledDisabledCaps);
                    return p.inject(
                            required("nick"),
                            splitter.left(List.of()),
                            splitter.right(List.of()),
                            IRCMessageCAPNAK::new);
                })
                .ifIndexEquals(
                        1,
                        "NEW",
                        p -> p.inject(
                                required("nick"),
                                required("extensions", splitToMapDelimited("\\s+")),
                                IRCMessageCAPNEW::new))
                .ifNoneMatch(p -> p.inject(optional("command", x -> x, "<NONE>"), IRCMessageCAPInvalid::new))
                .inject();
    }

    private Pair<List<String>, List<String>> splitEnabledDisabledCaps(String caps) {
        if (caps.isBlank()) {
            throw new IllegalArgumentException("No capabilities specified");
        }
        List<String> enabledCapabilities = new ArrayList<>();
        List<String> disabledCapabilities = new ArrayList<>();
        for (String cap : caps.split("\\s+")) {
            if (cap.startsWith("-")) {
                disabledCapabilities.add(cap.substring(1));
            } else {
                enabledCapabilities.add(cap);
            }
        }
        return new Pair<>(enabledCapabilities, disabledCapabilities);
    }

    private IRCMessageHELP parseHelp(Parameters parameters) throws Exception {
        return parameters.inject(optional("subject"), IRCMessageHELP::new);
    }

    private IRCMessageHELPOP parseHelpOp(Parameters parameters) throws Exception {
        return parameters.inject(optional("subject"), IRCMessageHELPOP::new);
    }

    private IRCMessageINVITE parseInvite(Parameters parameters) throws Exception {
        return parameters.inject(optional("nickname"), optional("channel"), IRCMessageINVITE::new);
    }

    private IRCMessage parseJoin(Parameters parameters) throws Exception {
        return parameters
                .injectConditionally()
                .ifIndexEquals(0, "0", p -> p.inject(IRCMessageJOIN0::new))
                .ifNoneMatch(p -> p.inject(
                        required("channel", this::splitToList),
                        optional("key", this::splitToList, List.<String>of()),
                        IRCMessageJOINNormal::new))
                .inject();
    }

    private IRCMessageKICK parseKick(Parameters parameters) throws Exception {
        return parameters.inject(required("channel"), required("nick"), optional("comment"), IRCMessageKICK::new);
    }

    private IRCMessageLIST parseList(Parameters parameters) throws Exception {
        return parameters.inject(optional("channel", this::splitToList, List.<String>of()), IRCMessageLIST::new);
    }

    private IRCMessageMODE parseMode(Parameters parameters) throws Exception {
        return parameters.inject(
                required("target"), optional("modestring"), greedyOptional("mode arguments"), IRCMessageMODE::new);
    }

    private IRCMessageMOTD parseMOTD(Parameters parameters) throws Exception {
        return parameters.inject(optional("target"), IRCMessageMOTD::new);
    }

    private IRCMessageNAMES parseNames(Parameters parameters) throws Exception {
        return parameters.inject(required("channel", this::splitToList), IRCMessageNAMES::new);
    }

    private IRCMessage parseNotice(Parameters parameters) throws Exception {
        return parameters
                .injectConditionally(false)
                .ifIndex(1, this::isCTCP, this::parseCTCP)
                .ifNoneMatch(p -> p.inject(
                        required("target", this::splitToList),
                        requiredAllowEmpty("text to be sent"),
                        IRCMessageNOTICE::new))
                .inject();
    }

    private IRCMessagePART parsePart(Parameters parameters) throws Exception {
        return parameters.inject(required("channel", this::splitToList), optional("reason"), IRCMessagePART::new);
    }

    private IRCMessagePING parsePing(Parameters parameters) throws Exception {
        return parameters.inject(requiredAllowEmpty("token"), IRCMessagePING::new);
    }

    private IRCMessagePONG parsePong(Parameters parameters) throws Exception {
        return parameters.inject(optional("server"), required("token"), IRCMessagePONG::new);
    }

    private IRCMessage parsePrivmsg(Parameters parameters) throws Exception {
        return parameters
                .injectConditionally(false)
                .ifIndex(1, this::isCTCP, this::parseCTCP)
                .ifNoneMatch(p -> p.inject(
                        required("target", this::splitToList),
                        requiredAllowEmpty("text to be sent"),
                        IRCMessagePRIVMSG::new))
                .inject();
    }

    private IRCMessageUSER parseUser(Parameters parameters) throws Exception {
        return parameters
                .discard(1, 2)
                .inject(required("username"), requiredAllowEmpty("realname"), IRCMessageUSER::new);
    }

    private IRCMessageUSERHOST parseUserHost(Parameters parameters) throws Exception {
        return parameters.inject(greedyRequired("nickname"), IRCMessageUSERHOST::new);
    }

    private IRCMessageQUIT parseQuit(Parameters parameters) throws Exception {
        return parameters.inject(optional("reason"), IRCMessageQUIT::new);
    }

    private IRCMessageTOPIC parseTopic(Parameters parameters) throws Exception {
        return parameters.inject(required("channel"), optionalAllowEmpty("topic"), IRCMessageTOPIC::new);
    }

    private IRCMessageWHOIS parseWhoIs(Parameters parameters) throws Exception {
        return parameters.inject(optional("target"), required("nickname"), IRCMessageWHOIS::new);
    }

    private IRCMessageWHOWAS parseWhoWas(Parameters parameters) throws Exception {
        return parameters.inject(required("nick"), optional("count", Integer::parseInt), IRCMessageWHOWAS::new);
    }

    private IRCMessage004 parse004(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("servername"),
                required("version"),
                required("available user modes"),
                required("available channel modes"),
                optional("channel modes with a parameter"),
                IRCMessage004::new);
    }

    private IRCMessage005 parse005(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                greedyRequiredMap("tokens", this::splitToEntry),
                required("text"),
                IRCMessage005::new);
    }

    private IRCMessage010 parse010(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("hostname"),
                required("port", Integer::parseInt),
                required("info"),
                IRCMessage010::new);
    }

    private IRCMessage212 parse212(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("command"),
                required("count", Integer::parseInt),
                optional("byte count", Integer::parseInt),
                optional("remote count", Integer::parseInt),
                IRCMessage212::new);
    }

    private IRCMessage252 parse252(Parameters parameters) throws Exception {
        return parameters.inject(required("client"), required("ops", Integer::parseInt), IRCMessage252::new);
    }

    private IRCMessage253 parse253(Parameters parameters) throws Exception {
        return parameters.inject(required("client"), required("connections", Integer::parseInt), IRCMessage253::new);
    }

    private IRCMessage254 parse254(Parameters parameters) throws Exception {
        return parameters.inject(required("client"), required("channels", Integer::parseInt), IRCMessage254::new);
    }

    private IRCMessage256 parse256(Parameters parameters) throws Exception {
        return parameters.inject(required("client"), optional("server"), required("text"), IRCMessage256::new);
    }

    private IRCMessage265 parse265(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                optional("u", Integer::parseInt),
                optional("m", Integer::parseInt),
                required("text"),
                IRCMessage265::new);
    }

    private IRCMessage266 parse266(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                optional("u", Integer::parseInt),
                optional("m", Integer::parseInt),
                required("text"),
                IRCMessage266::new);
    }

    private IRCMessage302 parse302(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"), required("reply", splitToListDelimited("\\s+")), IRCMessage302::new);
    }

    private IRCMessage311 parse311(Parameters parameters) throws Exception {
        return parameters
                .discard(4)
                .inject(
                        required("client"),
                        required("nick"),
                        required("username"),
                        required("host"),
                        required("realname"),
                        IRCMessage311::new);
    }

    private IRCMessage314 parse314(Parameters parameters) throws Exception {
        return parameters
                .discard(4)
                .inject(
                        required("client"),
                        required("nick"),
                        required("username"),
                        required("host"),
                        required("realname"),
                        IRCMessage314::new);
    }

    private IRCMessage317 parse317(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("nick"),
                required("secs", Integer::parseInt),
                required("signon", Long::parseLong),
                IRCMessage317::new);
    }

    private IRCMessage319 parse319(Parameters parameters) throws Exception {
        SplittingParameterInjector<List<String>, List<Character>> splitter =
                splitRequired("[prefix]nick", splitForPrefixes(parameters.getChannelMembershipPrefixes()));
        return parameters.inject(
                required("client"),
                required("nick"),
                splitter.left(List.of()),
                splitter.right(List.of()),
                IRCMessage319::new);
    }

    private IRCMessage322 parse322(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("channel"),
                required("client count", Integer::parseInt),
                requiredAllowEmpty("topic"),
                IRCMessage322::new);
    }

    private IRCMessage324 parse324(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("channel"),
                required("modestring"),
                greedyOptional("mode arguments"),
                IRCMessage324::new);
    }

    private IRCMessage329 parse329(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"), required("channel"), required("creationtime", Long::parseLong), IRCMessage329::new);
    }

    private IRCMessage333 parse333(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("channel"),
                required("nick"),
                required("setat", Long::parseLong),
                IRCMessage333::new);
    }

    private IRCMessage parse338(Parameters parameters) throws Exception {
        return parameters
                .injectConditionally(false)
                .ifIndex(2, s -> s.contains("@"), p -> {
                    SplittingParameterInjector<String, String> splitter = splitString("host@hostname", "@");
                    return p.inject(
                            required("client"),
                            required("nick"),
                            splitter.left(null),
                            splitter.right(null),
                            required("ip"),
                            required("text"),
                            IRCMessage338::new);
                })
                .ifIndex(
                        2,
                        this::isIP,
                        p -> p.inject(
                                required("client"),
                                required("nick"),
                                required("ip"),
                                required("text"),
                                IRCMessage338::forIp))
                .ifIndex(
                        2,
                        not(this::isIP),
                        p -> p.inject(
                                required("client"),
                                required("nick"),
                                required("host"),
                                required("text"),
                                IRCMessage338::forHost))
                .ifNoneMatch(p ->
                        p.inject(required("client"), required("nick"), required("text"), IRCMessage338::forClientNick))
                .inject();
    }

    private IRCMessage352 parse352(Parameters parameters) throws Exception {
        SplittingParameterInjector<String, String> splitter = splitString("<hopcount> <realname>", "\\s+");
        return parameters.inject(
                required("client"),
                required("channel"),
                required("username"),
                required("host"),
                required("server"),
                required("nick"),
                required("flags"),
                splitter.left(null),
                splitter.right(null),
                IRCMessage352::new);
    }

    private IRCMessage353 parse353(Parameters parameters) throws Exception {
        SplittingParameterInjector<List<String>, List<Character>> splitter =
                splitRequired("[prefix]nick", splitForPrefixes(parameters.getChannelMembershipPrefixes()));
        return parameters.inject(
                required("client"),
                required("symbol"),
                required("channel"),
                splitter.left(List.of()),
                splitter.right(List.of()),
                IRCMessage353::new);
    }

    private IRCMessage364 parse364(Parameters parameters) throws Exception {
        SplittingParameterInjector<String, String> splitter = splitString("<hopcount> <server info>", "\\s+");
        return parameters.inject(
                required("client"),
                required("server1"),
                required("server2"),
                splitter.left(null),
                splitter.right(null),
                IRCMessage364::new);
    }

    private IRCMessage367 parse367(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("channel"),
                required("mask"),
                optional("who"),
                optional("set-ts", Long::parseLong),
                IRCMessage367::new);
    }

    private IRCMessage391 parse391(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("server"),
                optional("timestamp", Long::parseLong),
                optional("ts offset"),
                required("human-readable time"),
                IRCMessage391::new);
    }

    private IRCMessage400 parse400(Parameters parameters) throws Exception {
        return parameters.inject(required("client"), greedyRequired("command"), required("info"), IRCMessage400::new);
    }

    private IRCMessage472 parse472(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"), required("modechar", s -> s.charAt(0)), required("text"), IRCMessage472::new);
    }

    private IRCMessage696 parse696(Parameters parameters) throws Exception {
        return parameters.inject(
                required("client"),
                required("target chan/user"),
                required("mode char", s -> s.charAt(0)),
                required("parameter"),
                required("description"),
                IRCMessage696::new);
    }

    private IRCMessage parseCTCP(Parameters parameters) throws Exception {
        return parameters
                .injectConditionally(false)
                .ifIndex(1, isCTCPCommand("ACTION"), this::parseCTCPAction)
                .ifIndex(1, isCTCPCommand("CLIENTINFO"), this::parseCTCPClientInfo)
                .ifIndex(1, isCTCPCommand("DCC SEND"), this::parseCTCPDCCSend)
                .ifIndex(1, isCTCPCommand("PING"), this::parseCTCPPing)
                .ifIndex(1, isCTCPCommand("VERSION"), this::parseCTCPVersion)
                .ifNoneMatch(p -> p.inject(
                        required("target", this::splitToList),
                        requiredAllowEmpty("message"),
                        "PRIVMSG".equals(p.command) ? IRCMessagePRIVMSG::new : IRCMessageNOTICE::new))
                .inject();
    }

    private IRCMessage parseCTCPAction(Parameters parameters) throws Exception {
        return parameters
                .hoist(1, tokenizeCTCP(2))
                .discard(1) // drop ACTION
                .inject(required("target", this::splitToList), required("text"), IRCMessageCTCPAction::new);
    }

    private IRCMessage parseCTCPClientInfo(Parameters parameters) throws Exception {
        return parameters
                .hoist(1, tokenizeCTCP(Integer.MAX_VALUE))
                .discard(1) // drop CLIENTINFO
                .injectConditionally(false)
                .ifIndex(
                        1,
                        _ -> true,
                        p -> p.inject(
                                required("target", this::splitToList),
                                greedyRequired("token"),
                                IRCMessageCTCPClientInfoResponse::new))
                .ifNoneMatch(p -> p.inject(required("target", this::splitToList), IRCMessageCTCPClientInfoRequest::new))
                .inject();
    }

    private IRCMessage parseCTCPDCCSend(Parameters parameters) throws Exception {
        return parameters
                .hoist(1, this::tokenizeDCCSend)
                .discard(1, 2) // drop DCC SEND
                .inject(
                        required("target", this::splitToList),
                        required("filename"),
                        required("host"),
                        required("port", Integer::parseInt),
                        optional("filesize", Integer::parseInt, null),
                        IRCMessageCTCPDCCSend::new);
    }

    private IRCMessage parseCTCPPing(Parameters parameters) throws Exception {
        return parameters
                .hoist(1, tokenizeCTCP(2))
                .discard(1) // drop PING
                .inject(
                        required("target", this::splitToList),
                        required("args"),
                        "PRIVMSG".equals(parameters.command)
                                ? IRCMessageCTCPPingRequest::new
                                : IRCMessageCTCPPingResponse::new);
    }

    private IRCMessage parseCTCPVersion(Parameters parameters) throws Exception {
        return parameters
                .hoist(1, tokenizeCTCP(2))
                .discard(1) // drop VERSION
                .injectConditionally(false)
                .ifIndex(
                        1,
                        _ -> true,
                        p -> p.inject(
                                required("target", this::splitToList),
                                required("arg"),
                                IRCMessageCTCPVersionResponse::new))
                .ifNoneMatch(p -> p.inject(required("target", this::splitToList), IRCMessageCTCPVersionRequest::new))
                .inject();
    }

    private Predicate<String> isCTCPCommand(String command) {
        return s -> s.startsWith("\u0001" + command + " ")
                || s.startsWith("\u0001" + command + "\u0001")
                || s.equals("\u0001" + command);
    }

    private ThrowingFunction<String, List<String>> tokenizeCTCP(int limit) {
        return body -> {
            String stripped = body.replaceAll("^\u0001|\u0001$", "");
            return Arrays.asList(stripped.split("\\s+", limit));
        };
    }

    private List<String> tokenizeDCCSend(String body) {
        String stripped = body.replaceAll("^\u0001|\u0001$", "");
        List<String> tokens = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < stripped.length(); i++) {
            char c = stripped.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (Character.isWhitespace(c) && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    // generic functions to parse the majority of numerics, which are rather simple
    private <T extends IRCMessage> T parseExact(Parameters parameters, IRCMessageFactory0<T> factory) throws Exception {
        return parameters.inject(factory);
    }

    private <T extends IRCMessage> T parseExact(
            Parameters parameters, String arg0, IRCMessageFactory1<T, String> factory) throws Exception {
        return parameters.inject(required(arg0), factory);
    }

    private <T extends IRCMessage> T parseExact(
            Parameters parameters, String arg0, String arg1, IRCMessageFactory2<T, String, String> factory)
            throws Exception {
        return parameters.inject(required(arg0), required(arg1), factory);
    }

    private <T extends IRCMessage> T parseExact(
            Parameters parameters,
            String arg0,
            String arg1,
            String arg2,
            IRCMessageFactory3<T, String, String, String> factory)
            throws Exception {
        return parameters.inject(required(arg0), required(arg1), required(arg2), factory);
    }

    private <T extends IRCMessage> T parseExact(
            Parameters parameters,
            String arg0,
            String arg1,
            String arg2,
            String arg3,
            IRCMessageFactory4<T, String, String, String, String> factory)
            throws Exception {
        return parameters.inject(required(arg0), required(arg1), required(arg2), required(arg3), factory);
    }

    // BEYOND THIS POINT IS SCARY PARSING CODE TO ENABLE MY NICE DSL

    private boolean isCTCP(String value) {
        return value.startsWith("\u0001");
    }

    private boolean isIP(String value) {
        try {
            InetAddress.ofLiteral(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private List<String> splitToList(String raw) {
        return Arrays.asList(raw.split(","));
    }

    private ThrowingFunction<String, List<String>> splitToListDelimited(String regex) {
        return raw -> Arrays.asList(raw.split(regex));
    }

    private ThrowingFunction<String, SequencedMap<String, String>> splitToMapDelimited(String regex) {
        return raw -> Arrays.stream(raw.split(regex))
                .map(this::splitToEntry)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private Map.Entry<String, String> splitToEntry(String raw) {
        String[] parts = raw.split("=", 2);
        if (parts.length == 1) {
            return Map.entry(parts[0], "");
        } else {
            return Map.entry(parts[0], parts[1]);
        }
    }

    private ThrowingFunction<String, Pair<List<String>, List<Character>>> splitForPrefixes(
            Set<Character> validPrefixes) {
        return raw -> {
            List<String> values = new ArrayList<>();
            List<Character> prefixes = new ArrayList<>();
            if (raw != null) {
                String[] splitRaw = raw.split("\\s+", -1);
                for (String part : splitRaw) {
                    if (part.isBlank()) {
                        continue;
                    }

                    if (validPrefixes.contains(part.charAt(0))) {
                        prefixes.add(part.charAt(0));
                        values.add(part.substring(1));
                    } else {
                        prefixes.add(null);
                        values.add(part);
                    }
                }
            }
            return new Pair<>(values, prefixes);
        };
    }

    private record PrefixParts(String name, String user, String host) {
        PrefixParts() {
            this(null, null, null);
        }
    }

    private static class Parameters {

        private final Set<Character> channelMembershipPrefixes;
        private final String raw;
        private final String command;
        private final SequencedMap<String, String> tags;
        private final PrefixParts prefix;
        private final List<String> params;

        public Parameters(
                Set<Character> channelMembershipPrefixes,
                String raw,
                String command,
                SequencedMap<String, String> tags,
                PrefixParts prefix,
                List<String> params) {
            this.channelMembershipPrefixes = channelMembershipPrefixes;
            this.raw = raw;
            this.command = command;
            this.tags = tags;
            this.prefix = prefix;
            this.params = params;
        }

        public Set<Character> getChannelMembershipPrefixes() {
            return channelMembershipPrefixes;
        }

        public Parameters discard(int... indexes) throws NotEnoughParametersException {
            List<String> filteredParameters = new ArrayList<>(params);
            List<Integer> removalIndexes = Arrays.stream(indexes)
                    .distinct()
                    .boxed()
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (int i : removalIndexes) {
                if (i >= filteredParameters.size()) {
                    throw new NotEnoughParametersException("expected at least %d parameters".formatted(i + 1));
                }
                filteredParameters.remove(i);
            }
            return new Parameters(channelMembershipPrefixes, raw, command, tags, prefix, filteredParameters);
        }

        public Parameters hoist(int index, ThrowingFunction<String, List<String>> tokenizer) throws Exception {
            if (index >= params.size()) {
                throw new NotEnoughParametersException("expected at least %d parameters".formatted(index));
            }
            List<String> results = new ArrayList<>();
            results.addAll(params.subList(0, index));
            results.addAll(tokenizer.apply(params.get(index)));
            if (index + 1 < params.size()) {
                results.addAll(params.subList(index + 1, params.size()));
            }
            return new Parameters(channelMembershipPrefixes, raw, command, tags, prefix, results);
        }

        public ConditionalInjectionBuilder injectConditionally() {
            return injectConditionally(true);
        }

        public ConditionalInjectionBuilder injectConditionally(boolean consume) {
            return new ConditionalInjectionBuilder(this, consume);
        }

        public <T extends IRCMessage> T inject(IRCMessageFactory0<T> constructor) {
            return constructor.create(raw, tags, prefix.name(), prefix.user(), prefix.host());
        }

        public <T extends IRCMessage, A> T inject(ParameterExtractor<A> arg0, IRCMessageFactory1<T, A> constructor)
                throws Exception {
            ParameterPlanner planner = new ParameterPlanner(params.size(), arg0);
            return constructor.create(
                    raw, tags, prefix.name(), prefix.user(), prefix.host(), planner.get(0, params, arg0));
        }

        public <T extends IRCMessage, A, B> T inject(
                ParameterExtractor<A> arg0, ParameterExtractor<B> arg1, IRCMessageFactory2<T, A, B> constructor)
                throws Exception {
            ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1);
            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1));
        }

        public <T extends IRCMessage, A, B, C> T inject(
                ParameterExtractor<A> arg0,
                ParameterExtractor<B> arg1,
                ParameterExtractor<C> arg2,
                IRCMessageFactory3<T, A, B, C> constructor)
                throws Exception {
            ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2);
            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1),
                    planner.get(2, params, arg2));
        }

        public <T extends IRCMessage, A, B, C, D> T inject(
                ParameterExtractor<A> arg0,
                ParameterExtractor<B> arg1,
                ParameterExtractor<C> arg2,
                ParameterExtractor<D> arg3,
                IRCMessageFactory4<T, A, B, C, D> constructor)
                throws Exception {
            ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3);
            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1),
                    planner.get(2, params, arg2),
                    planner.get(3, params, arg3));
        }

        public <T extends IRCMessage, A, B, C, D, E> T inject(
                ParameterExtractor<A> arg0,
                ParameterExtractor<B> arg1,
                ParameterExtractor<C> arg2,
                ParameterExtractor<D> arg3,
                ParameterExtractor<E> arg4,
                IRCMessageFactory5<T, A, B, C, D, E> constructor)
                throws Exception {
            ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4);
            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1),
                    planner.get(2, params, arg2),
                    planner.get(3, params, arg3),
                    planner.get(4, params, arg4));
        }

        public <T extends IRCMessage, A, B, C, D, E, F> T inject(
                ParameterExtractor<A> arg0,
                ParameterExtractor<B> arg1,
                ParameterExtractor<C> arg2,
                ParameterExtractor<D> arg3,
                ParameterExtractor<E> arg4,
                ParameterExtractor<F> arg5,
                IRCMessageFactory6<T, A, B, C, D, E, F> constructor)
                throws Exception {
            ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5);
            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1),
                    planner.get(2, params, arg2),
                    planner.get(3, params, arg3),
                    planner.get(4, params, arg4),
                    planner.get(5, params, arg5));
        }

        public <T extends IRCMessage, A, B, C, D, E, F, G> T inject(
                ParameterExtractor<A> arg0,
                ParameterExtractor<B> arg1,
                ParameterExtractor<C> arg2,
                ParameterExtractor<D> arg3,
                ParameterExtractor<E> arg4,
                ParameterExtractor<F> arg5,
                ParameterExtractor<G> arg6,
                IRCMessageFactory7<T, A, B, C, D, E, F, G> constructor)
                throws Exception {

            ParameterPlanner planner = new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5, arg6);

            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1),
                    planner.get(2, params, arg2),
                    planner.get(3, params, arg3),
                    planner.get(4, params, arg4),
                    planner.get(5, params, arg5),
                    planner.get(6, params, arg6));
        }

        public <T extends IRCMessage, A, B, C, D, E, F, G, H> T inject(
                ParameterExtractor<A> arg0,
                ParameterExtractor<B> arg1,
                ParameterExtractor<C> arg2,
                ParameterExtractor<D> arg3,
                ParameterExtractor<E> arg4,
                ParameterExtractor<F> arg5,
                ParameterExtractor<G> arg6,
                ParameterExtractor<H> arg7,
                IRCMessageFactory8<T, A, B, C, D, E, F, G, H> constructor)
                throws Exception {

            ParameterPlanner planner =
                    new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7);

            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1),
                    planner.get(2, params, arg2),
                    planner.get(3, params, arg3),
                    planner.get(4, params, arg4),
                    planner.get(5, params, arg5),
                    planner.get(6, params, arg6),
                    planner.get(7, params, arg7));
        }

        public <T extends IRCMessage, A, B, C, D, E, F, G, H, I> T inject(
                ParameterExtractor<A> arg0,
                ParameterExtractor<B> arg1,
                ParameterExtractor<C> arg2,
                ParameterExtractor<D> arg3,
                ParameterExtractor<E> arg4,
                ParameterExtractor<F> arg5,
                ParameterExtractor<G> arg6,
                ParameterExtractor<H> arg7,
                ParameterExtractor<I> arg8,
                IRCMessageFactory9<T, A, B, C, D, E, F, G, H, I> constructor)
                throws Exception {

            ParameterPlanner planner =
                    new ParameterPlanner(params.size(), arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);

            return constructor.create(
                    raw,
                    tags,
                    prefix.name(),
                    prefix.user(),
                    prefix.host(),
                    planner.get(0, params, arg0),
                    planner.get(1, params, arg1),
                    planner.get(2, params, arg2),
                    planner.get(3, params, arg3),
                    planner.get(4, params, arg4),
                    planner.get(5, params, arg5),
                    planner.get(6, params, arg6),
                    planner.get(7, params, arg7),
                    planner.get(8, params, arg8));
        }
    }

    private record ConditionalInjection(
            int index, Predicate<String> predicate, ThrowingFunction<Parameters, IRCMessage> injection) {}

    private static class ConditionalInjectionBuilder {

        private final Parameters parameters;
        private final List<ConditionalInjection> injections;
        private final boolean consumeTestParameter;

        private ThrowingFunction<Parameters, IRCMessage> defaultInjection;

        public ConditionalInjectionBuilder(Parameters parameters, boolean consumeTestParameter) {
            this.parameters = parameters;
            this.injections = new ArrayList<>();
            this.consumeTestParameter = consumeTestParameter;
        }

        public ConditionalInjectionBuilder ifIndexEquals(
                int index, String value, ThrowingFunction<Parameters, IRCMessage> injection) {
            injections.add(new ConditionalInjection(index, value::equals, injection));
            return this;
        }

        public ConditionalInjectionBuilder ifIndex(
                int index, Predicate<String> predicate, ThrowingFunction<Parameters, IRCMessage> injection) {
            injections.add(new ConditionalInjection(index, predicate, injection));
            return this;
        }

        public ConditionalInjectionBuilder ifNoneMatch(ThrowingFunction<Parameters, IRCMessage> injection) {
            defaultInjection = injection;
            return this;
        }

        public IRCMessage inject() throws Exception {
            for (ConditionalInjection injection : injections) {
                if (parameters.params.size() > injection.index()
                        && injection.predicate().test(parameters.params.get(injection.index()))) {
                    if (consumeTestParameter) {
                        List<String> newParams = new ArrayList<>(parameters.params);
                        newParams.remove(injection.index());
                        return injection
                                .injection()
                                .apply(new Parameters(
                                        parameters.channelMembershipPrefixes,
                                        parameters.raw,
                                        parameters.command,
                                        parameters.tags,
                                        parameters.prefix,
                                        newParams));
                    } else {
                        return injection.injection().apply(parameters);
                    }
                }
            }
            if (defaultInjection != null) {
                return defaultInjection.apply(parameters);
            }
            throw new ParserErrorException("could not determine appropriate subcommand");
        }
    }
}
