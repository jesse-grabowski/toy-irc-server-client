package com.jessegrabowski.irc.protocol.model;

public final class FileTransferOperationISend extends FileTransferOperation {

    public static final FileTransferOperationCode OP_CODE = FileTransferOperationCode.I_SEND;

    private final long size;

    public FileTransferOperationISend(long size) {
        super(OP_CODE);
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}
