package com.jessegrabowski.irc.protocol;

public class FileTransferUnknownOpCodeException extends Exception {

    private final int opCode;

    public FileTransferUnknownOpCodeException(String message, int opCode) {
        super(message);
        this.opCode = opCode;
    }

    public int getOpCode() {
        return opCode;
    }
}
