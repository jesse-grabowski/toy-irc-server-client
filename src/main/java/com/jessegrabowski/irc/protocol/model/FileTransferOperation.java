package com.jessegrabowski.irc.protocol.model;

public abstract sealed class FileTransferOperation permits FileTransferOperationAck, FileTransferOperationError, FileTransferOperationGoodbye, FileTransferOperationHello, FileTransferOperationIReceive, FileTransferOperationISend {

    private final FileTransferOperationCode opCode;

    protected FileTransferOperation(FileTransferOperationCode opCode) {
        this.opCode = opCode;
    }

    public FileTransferOperationCode getOpCode() {
        return opCode;
    }

}
