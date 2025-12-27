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

import com.jessegrabowski.irc.protocol.model.FileTransferOperation;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationAck;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationCode;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationError;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationErrorCode;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationGoodbye;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationHello;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationISend;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationReady;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationRole;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationVersion;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

public class FileTransferOperationUnmarshaller {

    public FileTransferOperation unmarshal(DataInputStream dis)
            throws IOException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        try {
            int startOfFrame = dis.readInt();
            if (startOfFrame != 0xDEADBEEF) {
                throw new FileTransferFramingException(
                        "Misaligned frame (expected 0xDEADBEEF, received 0x%08X)".formatted(startOfFrame));
            }

            int rawOpCode = dis.readUnsignedByte();
            FileTransferOperationCode opCode = FileTransferOperationCode.fromValue(rawOpCode);
            return switch (opCode) {
                case HELLO -> unmarshalHello(dis);
                case READY -> unmarshalReady(dis);
                case I_SEND -> unmarshalISend(dis);
                case ACK -> unmarshalAck(dis);
                case GOODBYE -> unmarshalGoodbye(dis);
                case ERROR -> unmarshalError(dis);
                case UNKNOWN -> throw new FileTransferUnknownOpCodeException("Unknown op code", rawOpCode);
            };
        } catch (EOFException e) {
            throw new FileTransferFramingException("Truncated frame", e);
        }
    }

    private FileTransferOperationHello unmarshalHello(DataInputStream dis) throws IOException {
        FileTransferOperationVersion version = FileTransferOperationVersion.fromValue(dis.readUnsignedByte());
        UUID token = new UUID(dis.readLong(), dis.readLong());
        FileTransferOperationRole role = FileTransferOperationRole.fromValue(dis.readUnsignedByte());
        return new FileTransferOperationHello(version, token, role);
    }

    private FileTransferOperationReady unmarshalReady(DataInputStream dis) {
        return new FileTransferOperationReady();
    }

    private FileTransferOperationISend unmarshalISend(DataInputStream dis) throws IOException {
        long size = dis.readLong();
        return new FileTransferOperationISend(size);
    }

    private FileTransferOperationAck unmarshalAck(DataInputStream dis) {
        return new FileTransferOperationAck();
    }

    private FileTransferOperationGoodbye unmarshalGoodbye(DataInputStream dis) {
        return new FileTransferOperationGoodbye();
    }

    private FileTransferOperationError unmarshalError(DataInputStream dis) throws IOException {
        FileTransferOperationErrorCode errorCode = FileTransferOperationErrorCode.fromValue(dis.readUnsignedByte());
        return new FileTransferOperationError(errorCode);
    }
}
