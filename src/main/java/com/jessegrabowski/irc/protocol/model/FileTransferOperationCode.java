package com.jessegrabowski.irc.protocol.model;

public enum FileTransferOperationCode {
    HELLO(1),
    I_SEND(2),
    I_RECEIVE(3),
    ACK(4),
    GOODBYE(5),
    ERROR(254),
    UNKNOWN(255);

    private final int value;

    FileTransferOperationCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FileTransferOperationCode fromValue(int value) {
        for (FileTransferOperationCode fileTransferOperationCode : FileTransferOperationCode.values()) {
            if (fileTransferOperationCode.getValue() == value) {
                return fileTransferOperationCode;
            }
        }
        return UNKNOWN;
    }
}
