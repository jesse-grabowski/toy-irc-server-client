package com.jessegrabowski.irc.server.filetransfer;

import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FileTransferService {

    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("IRCServer-FileTransfer-", 0).factory()
    );
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            Thread.ofVirtual().name("IRCServer-FileTransfer-Scheduler").factory()
    );
    private final Map<String, FileTransferLease> leases = new ConcurrentHashMap<>();

    public void accept(Socket socket) {
        executor.execute(() -> handle(socket));
    }

    private void handle(Socket socket) {

    }
}
