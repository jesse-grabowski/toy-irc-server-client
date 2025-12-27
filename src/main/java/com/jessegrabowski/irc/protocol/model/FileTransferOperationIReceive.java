package com.jessegrabowski.irc.protocol.model;

public final class FileTransferOperationIReceive extends FileTransferOperation {

    public static final FileTransferOperationCode OP_CODE = FileTransferOperationCode.I_RECEIVE;

    private final long size;

    public FileTransferOperationIReceive(long size) {
        super(OP_CODE);
        this.size = size;
    }

    public long getSize() {
        return size;
    }
}
