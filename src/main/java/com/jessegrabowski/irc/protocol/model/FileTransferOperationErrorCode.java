package com.jessegrabowski.irc.protocol.model;

public enum FileTransferOperationErrorCode {
    BROKEN_PIPE(1),
    UNKNOWN(-1);

    private final int value;

    FileTransferOperationErrorCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FileTransferOperationErrorCode fromValue(int value) {
        for (FileTransferOperationErrorCode fileTransferOperationCode : FileTransferOperationErrorCode.values()) {
            if (fileTransferOperationCode.getValue() == value) {
                return fileTransferOperationCode;
            }
        }
        return UNKNOWN;
    }
}
