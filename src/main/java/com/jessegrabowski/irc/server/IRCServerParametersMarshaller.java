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
package com.jessegrabowski.irc.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IRCServerParametersMarshaller {

    public SequencedMap<String, String> marshal(IRCServerParameters parameters) {
        SequencedMap<String, String> map = new LinkedHashMap<>();
        map.put("AWAYLEN", Integer.toString(parameters.getAwayLength()));
        map.put("CASEMAPPING", parameters.getCaseMapping().getCasemapping());
        map.put("CHANLIMIT", marshalChanlimit(parameters.getChannelLimits()));
        map.put(
                "CHANMODES",
                join(
                        ",",
                        List.of(
                                join("", parameters.getTypeAChannelModes()),
                                join("", parameters.getTypeBChannelModes()),
                                join("", parameters.getTypeCChannelModes()),
                                join("", parameters.getTypeDChannelModes()))));
        map.put("CHANNELLEN", Integer.toString(parameters.getChannelLength()));
        map.put("CHANTYPES", join("", parameters.getChannelTypes()));
        map.put("EXCEPTS", String.valueOf(parameters.getExcepts()));
        map.put("EXTBAN", parameters.getExtendedBanPrefix() + "," + join("", parameters.getExtendedBanModes()));
        map.put("HOSTLEN", Integer.toString(parameters.getHostLength()));
        map.put("INVEX", String.valueOf(parameters.getInviteExceptions()));
        map.put("KICKLEN", Integer.toString(parameters.getKickLength()));
        map.put("MAXLIST", marshalChanlimit(parameters.getMaxList()));
        map.put("MAXTARGETS", Integer.toString(parameters.getMaxTargets()));
        map.put("MODES", Integer.toString(parameters.getModes()));
        map.put("NETWORK", parameters.getNetwork());
        map.put("NICKLEN", Integer.toString(parameters.getNickLength()));
        map.put("PREFIX", marshalPrefix(parameters.getPrefixes()));
        if (parameters.isSafeList()) {
            map.put("SAFELIST", null);
        }
        map.put("SILENCE", marshalNonNull(parameters.getSilence(), Object::toString));
        map.put("STATUSMSG", join("", parameters.getStatusMessage()));
        map.put("TARGMAX", marshalTargmax(parameters.getTargetMax()));
        map.put("TOPICLEN", Integer.toString(parameters.getTopicLength()));
        map.put("USERLEN", Integer.toString(parameters.getUserLength()));
        return map;
    }

    private String marshalChanlimit(Map<Character, Integer> limits) {
        Map<Integer, String> results = new HashMap<>();
        for (Map.Entry<Character, Integer> entry : limits.entrySet()) {
            results.compute(entry.getValue(), (k, v) -> (v == null ? "" : v) + entry.getKey());
        }
        return results.entrySet().stream()
                .map(e -> e.getValue() + ":" + e.getKey())
                .collect(Collectors.joining(","));
    }

    private <T> String join(String delimiter, Collection<T> collection) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object o : collection) {
            if (!first) {
                sb.append(delimiter);
            }
            sb.append(o);
            first = false;
        }
        return sb.toString();
    }

    private String marshalPrefix(Map<Character, Character> prefixes) {
        StringBuilder m = new StringBuilder().append('(');
        StringBuilder p = new StringBuilder();
        for (Map.Entry<Character, Character> entry : prefixes.entrySet()) {
            m.append(entry.getKey());
            p.append(entry.getValue());
        }
        return m.append(')').append(p).toString();
    }

    private String marshalTargmax(Map<String, Integer> limits) {
        return limits.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining(","));
    }

    private <T> String marshalNonNull(T value, Function<T, String> marshaller) {
        return value == null ? null : marshaller.apply(value);
    }
}
