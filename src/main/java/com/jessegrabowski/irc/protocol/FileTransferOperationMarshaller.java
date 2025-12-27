package com.jessegrabowski.irc.protocol;

import com.jessegrabowski.irc.protocol.model.FileTransferOperation;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationAck;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationError;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationGoodbye;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationHello;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationIReceive;
import com.jessegrabowski.irc.protocol.model.FileTransferOperationISend;

import java.io.DataOutputStream;
import java.io.IOException;

public class FileTransferOperationMarshaller {

    public void marshal(DataOutputStream out, FileTransferOperation op) throws IOException {
        out.writeInt(0xDEADBEEF);
        out.writeByte(op.getOpCode().getValue());

        switch (op) {
            case FileTransferOperationHello o -> marshalHello(out, o);
            case FileTransferOperationISend o -> marshalISend(out, o);
            case FileTransferOperationIReceive o -> marshalIReceive(out, o);
            case FileTransferOperationAck o -> marshalAck(out, o);
            case FileTransferOperationGoodbye o -> marshalGoodbye(out, o);
            case FileTransferOperationError o -> marshalError(out, o);
        }
    }

    public void marshalHello(DataOutputStream out, FileTransferOperationHello op) throws IOException {
        out.writeByte(op.getVersion().getValue());
        out.writeLong(op.getToken().getMostSignificantBits());
        out.writeLong(op.getToken().getLeastSignificantBits());
        out.writeByte(op.getRole().getValue());
    }

    public void marshalISend(DataOutputStream out, FileTransferOperationISend op) throws IOException {
        out.writeLong(op.getSize());
    }

    public void marshalIReceive(DataOutputStream out, FileTransferOperationIReceive op) throws IOException {
        out.writeLong(op.getSize());
    }

    public void marshalAck(DataOutputStream out, FileTransferOperationAck op) {}

    public void marshalGoodbye(DataOutputStream out, FileTransferOperationGoodbye op) {}

    public void marshalError(DataOutputStream out, FileTransferOperationError op) throws IOException {
        out.writeByte(op.getErrorCode().getValue());
    }
}
