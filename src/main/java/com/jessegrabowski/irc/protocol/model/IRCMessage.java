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
package com.jessegrabowski.irc.protocol.model;

import java.util.SequencedMap;

public abstract sealed class IRCMessage
        permits
                // capability negotiation
                IRCMessageCAPACK,
                IRCMessageCAPDEL,
                IRCMessageCAPEND,
                IRCMessageCAPLISTRequest,
                IRCMessageCAPLISTResponse,
                IRCMessageCAPLSRequest,
                IRCMessageCAPLSResponse,
                IRCMessageCAPNAK,
                IRCMessageCAPNEW,
                IRCMessageCAPREQ,
                // standard messages
                IRCMessageERROR,
                IRCMessageJOIN0,
                IRCMessageJOINNormal,
                IRCMessageKICK,
                IRCMessageMODE,
                IRCMessageNICK,
                IRCMessageNOTICE,
                IRCMessageOPER,
                IRCMessagePART,
                IRCMessagePASS,
                IRCMessagePING,
                IRCMessagePONG,
                IRCMessagePRIVMSG,
                IRCMessageUSER,
                IRCMessageQUIT,
                IRCMessageTOPIC,
                // numerics
                IRCMessage001,
                IRCMessage002,
                IRCMessage003,
                IRCMessage004,
                IRCMessage005,
                IRCMessage010,
                IRCMessage212,
                IRCMessage219,
                IRCMessage221,
                IRCMessage242,
                IRCMessage251,
                IRCMessage252,
                IRCMessage253,
                IRCMessage254,
                IRCMessage255,
                IRCMessage256,
                IRCMessage257,
                IRCMessage258,
                IRCMessage259,
                IRCMessage263,
                IRCMessage265,
                IRCMessage266,
                IRCMessage276,
                IRCMessage301,
                IRCMessage302,
                IRCMessage305,
                IRCMessage306,
                IRCMessage307,
                IRCMessage311,
                IRCMessage312,
                IRCMessage313,
                IRCMessage314,
                IRCMessage315,
                IRCMessage317,
                IRCMessage318,
                IRCMessage319,
                IRCMessage320,
                IRCMessage321,
                IRCMessage322,
                IRCMessage323,
                IRCMessage324,
                IRCMessage329,
                IRCMessage330,
                IRCMessage331,
                IRCMessage332,
                IRCMessage333,
                IRCMessage336,
                IRCMessage337,
                IRCMessage338,
                IRCMessage341,
                IRCMessage346,
                IRCMessage347,
                IRCMessage348,
                IRCMessage349,
                IRCMessage351,
                IRCMessage352,
                IRCMessage353,
                IRCMessage364,
                IRCMessage365,
                IRCMessage366,
                IRCMessage367,
                IRCMessage368,
                IRCMessage369,
                IRCMessage371,
                IRCMessage372,
                IRCMessage374,
                IRCMessage375,
                IRCMessage376,
                IRCMessage378,
                IRCMessage379,
                IRCMessage381,
                IRCMessage382,
                IRCMessage391,
                IRCMessage400,
                IRCMessage401,
                IRCMessage402,
                IRCMessage403,
                IRCMessage404,
                IRCMessage405,
                IRCMessage406,
                IRCMessage409,
                IRCMessage411,
                IRCMessage412,
                IRCMessage417,
                IRCMessage421,
                IRCMessage422,
                IRCMessage431,
                IRCMessage432,
                IRCMessage433,
                IRCMessage436,
                IRCMessage441,
                IRCMessage442,
                IRCMessage443,
                IRCMessage451,
                IRCMessage461,
                IRCMessage462,
                IRCMessage464,
                IRCMessage465,
                IRCMessage471,
                IRCMessage472,
                IRCMessage473,
                IRCMessage474,
                IRCMessage475,
                IRCMessage476,
                IRCMessage481,
                IRCMessage482,
                IRCMessage483,
                IRCMessage491,
                IRCMessage501,
                IRCMessage502,
                IRCMessage524,
                IRCMessage525,
                IRCMessage670,
                IRCMessage671,
                IRCMessage691,
                IRCMessage696,
                IRCMessage704,
                IRCMessage705,
                IRCMessage706,
                IRCMessage723,
                // other / unsupported
                IRCMessageUnsupported,
                IRCMessageParseError {

    private final String rawMessage;

    private final SequencedMap<String, String> tags;
    private final String prefixName;
    private final String prefixUser;
    private final String prefixHost;
    private final String command;

    protected IRCMessage(
            String command,
            String rawMessage,
            SequencedMap<String, String> tags,
            String prefixName,
            String prefixUser,
            String prefixHost) {
        this.rawMessage = rawMessage;
        this.tags = tags;
        this.prefixName = prefixName;
        this.prefixUser = prefixUser;
        this.prefixHost = prefixHost;
        this.command = command;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public SequencedMap<String, String> getTags() {
        return tags;
    }

    public String getPrefixName() {
        return prefixName;
    }

    public String getPrefixUser() {
        return prefixUser;
    }

    public String getPrefixHost() {
        return prefixHost;
    }

    public String getCommand() {
        return command;
    }
}
