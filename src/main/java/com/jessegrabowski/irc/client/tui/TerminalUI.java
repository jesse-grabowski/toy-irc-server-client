package com.jessegrabowski.irc.client.tui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class TerminalUI {

  private final List<Consumer<String>> inputHandlers = new ArrayList<>();

  private volatile boolean running = false;
  private Thread thread;

  public void addInputHandler(Consumer<String> handler) {
    inputHandlers.add(handler);
  }

  protected void dispatchInput(String line) {
    for (Consumer<String> handler : inputHandlers) {
      handler.accept(line);
    }
  }

  public synchronized void start() {
    if (running) {
      return;
    }

    running = true;

    initialize();

    thread =
        new Thread(
            () -> {
              try {
                while (running) {
                  process();
                }
              } finally {
                running = false;
              }
            },
            "com.jessegrabowski.irc.client.tui.TerminalUI-Loop");

    thread.setDaemon(true);
    thread.start();
  }

  public synchronized void stop() {
    running = false;
    if (thread != null) {
      thread.interrupt();
      try {
        thread.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public abstract void setPrompt(RichString prompt);

  public abstract void setStatus(RichString status);

  public abstract void println(TerminalMessage message);

  protected abstract void initialize();

  protected abstract void process();
}
