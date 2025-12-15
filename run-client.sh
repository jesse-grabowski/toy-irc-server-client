#!/bin/bash

# Find the single directory inside out/production
PROD_DIR=$(find out/production -mindepth 1 -maxdepth 1 -type d | head -n 1)

# Run the Java program using that directory as the classpath
java -cp "$PROD_DIR" IRCClient -P abc -L FINE localhost
