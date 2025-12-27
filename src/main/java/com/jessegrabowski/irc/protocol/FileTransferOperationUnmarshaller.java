package com.jessegrabowski.irc.protocol;

import com.jessegrabowski.irc.protocol.model.FileTransferOperation;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationAck;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationCode;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationError;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationErrorCode;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationGoodbye;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationHello;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationIReceive;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationISend;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationRole;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationVersion;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.UUID;

public class FileTransferOperationUnmarshaller {

    public FileTransferOperation unmarshal(DataInputStream dis) throws IOException, FileTransferFramingException, FileTransferUnknownOpCodeException {
        try {
            int startOfFrame = dis.readInt();
            if (startOfFrame != 0xDEADBEEF) {
                throw new FileTransferFramingException("Misaligned frame (expected 0xDEADBEEF, received 0x%08X)".formatted(startOfFrame));
            }

            int rawOpCode = dis.readUnsignedByte();
            FileTransferOperationCode opCode = FileTransferOperationCode.fromValue(rawOpCode);
            return switch (opCode) {
                case HELLO -> unmarshalHello(dis);
                case I_SEND -> unmarshalISend(dis);
                case I_RECEIVE -> unmarshalIReceive(dis);
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

    private FileTransferOperationISend unmarshalISend(DataInputStream dis) throws IOException {
        long size = dis.readLong();
        return new FileTransferOperationISend(size);
    }

    private FileTransferOperationIReceive unmarshalIReceive(DataInputStream dis) throws IOException {
        long size = dis.readLong();
        return new FileTransferOperationIReceive(size);
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
