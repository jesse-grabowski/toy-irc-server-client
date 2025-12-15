#!/bin/bash

# We need to disable cleanupDaemonThreads because System.in doesn't support
# interrupts and this will hang for a while on shutdown if we try to wait
# for it. It's a daemon thread so the JVM will kill it once the event loop
# terminates.

mvn exec:java -Dexec.mainClass="com.jessegrabowski.irc.client.IRCClient" \
              -Dexec.args="-L FINE localhost" \
              -Dexec.cleanupDaemonThreads=false