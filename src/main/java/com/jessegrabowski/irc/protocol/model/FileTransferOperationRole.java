package com.jessegrabowski.irc.protocol.model;

public enum FileTransferOperationRole {
    SENDER(1),
    RECEIVER(2),
    UNKNOWN(-1);

    private final int value;

    FileTransferOperationRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static FileTransferOperationRole fromValue(int value) {
        for (FileTransferOperationRole role : FileTransferOperationRole.values()) {
            if (role.value == value) {
                return role;
            }
        }
        return UNKNOWN;
    }
}
