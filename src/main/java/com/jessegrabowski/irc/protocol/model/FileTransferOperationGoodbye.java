package com.jessegrabowski.irc.protocol.model;

public final class FileTransferOperationGoodbye extends FileTransferOperation {

    public static final FileTransferOperationCode OP_CODE = FileTransferOperationCode.GOODBYE;

    public FileTransferOperationGoodbye() {
        super(OP_CODE);
    }
}
