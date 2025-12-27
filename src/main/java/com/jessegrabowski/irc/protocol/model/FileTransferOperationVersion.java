package com.jessegrabowski.irc.protocol.model;

public enum FileTransferOperationVersion {
    V1(1),
    UNKNOWN(-1);

    private final int value;

    FileTransferOperationVersion(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FileTransferOperationVersion fromValue(int value) throws IllegalArgumentException {
        for (FileTransferOperationVersion v : FileTransferOperationVersion.values()) {
            if (v.getValue() == value) {
                return v;
            }
        }
        return UNKNOWN;
    }
}
