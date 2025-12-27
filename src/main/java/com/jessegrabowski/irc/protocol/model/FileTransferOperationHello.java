package com.jessegrabowski.irc.protocol.model;

import java.util.UUID;

public final class FileTransferOperationHello extends FileTransferOperation {

    public static final FileTransferOperationCode OP_CODE = FileTransferOperationCode.HELLO;

    private final FileTransferOperationVersion version;
    private final UUID token;
    private final FileTransferOperationRole role;

    public FileTransferOperationHello(FileTransferOperationVersion version, UUID token, FileTransferOperationRole role) {
        super(OP_CODE);
        this.version = version;
        this.token = token;
        this.role = role;
    }

    public FileTransferOperationVersion getVersion() {
        return version;
    }

    public UUID getToken() {
        return token;
    }

    public FileTransferOperationRole getRole() {
        return role;
    }
}
