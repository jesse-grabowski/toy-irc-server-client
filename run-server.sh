#!/bin/bash

mvn exec:java -Dexec.mainClass="com.jessegrabowski.irc.server.IRCServer" \
              -Dexec.args="-L FINE"