package com.jessegrabowski.irc.protocol.model;

public final class FileTransferOperationAck extends FileTransferOperation {

    public static final FileTransferOperationCode OP_CODE = FileTransferOperationCode.ACK;

    public FileTransferOperationAck() {
        super(OP_CODE);
    }
}
