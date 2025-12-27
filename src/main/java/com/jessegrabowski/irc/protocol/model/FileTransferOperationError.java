package com.jessegrabowski.irc.protocol.model;

public final class FileTransferOperationError extends FileTransferOperation {

    public static final FileTransferOperationCode OP_CODE = FileTransferOperationCode.ERROR;

    private final FileTransferOperationErrorCode errorCode;

    public FileTransferOperationError(FileTransferOperationErrorCode errorCode) {
        super(OP_CODE);
        this.errorCode = errorCode;
    }

    public FileTransferOperationErrorCode getErrorCode() {
        return errorCode;
    }
}
