package com.jessegrabowski.irc.protocol;

public class FileTransferFramingException extends Exception {
    public FileTransferFramingException(String message) {
        super(message);
    }

    public FileTransferFramingException(String message, Throwable cause) {
        super(message, cause);
    }
}
