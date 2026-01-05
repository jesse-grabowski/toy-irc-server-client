# RitsIRC

This repository contains my solution to the final assignment for Ritsumeikan University’s 2025–2026 **53346 Network Systems** course.

RitsIRC is a mostly specification-compliant [Modern IRC](https://modern.ircdocs.horse/) client and server written in pure Java. **This is a toy project and is not intended for production use**. While it is reasonably complete from a functional perspective, it lacks many security features and is not optimized for performance.

## Team Members

- Jesse Grabowski (2600230465-0)

## Project Structure

RitsIRC is built using [Apache Maven](https://maven.apache.org/) and generally follows Maven’s standard project layout, with a few additional directories:

- **`doc/`**  
  Project documentation, primarily architecture descriptions and UML diagrams used in the final report.

- **`src/`**  
  Source code for the client and server implementations, along with supporting resources such as configuration files.

- **`test/`**  
  Testing and validation resources, including Docker configurations for third-party IRC servers (used for compatibility testing), modifications to `irctest` for specification compliance, and a simple performance test used for profiling.

## Setup and Building

This project requires **Java 25 or later** to build and run. Before building, ensure that:

* The `JAVA_HOME` environment variable points to your Java installation
* The `java --version` command reports a version starting with `25` (or newer)

The specific Java vendor does not matter. This project was developed using Microsoft’s OpenJDK distribution, but it should work with any compliant JDK.

```
% java --version
openjdk 25.0.1 2025-10-21 LTS
OpenJDK Runtime Environment Microsoft-12574220 (build 25.0.1+8-LTS)
OpenJDK 64-Bit Server VM Microsoft-12574220 (build 25.0.1+8-LTS, mixed mode, sharing)
```

In addition, ensure that **Apache Maven** is configured to use Java 25 or later. You can verify this with:

```
% mvn --version
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /opt/homebrew/Cellar/maven/3.9.9/libexec
Java version: 25.0.1, vendor: Microsoft, runtime: /Library/Java/JavaVirtualMachines/microsoft-25.jdk/Contents/Home
Default locale: en_US, platform encoding: UTF-8
OS name: "mac os x", version: "14.7.8", arch: "aarch64", family: "mac"
```

For managing multiple Java versions on a single machine, I recommend using [jEnv](https://www.jenv.be/). When used with the Maven plugin enabled, jEnv will automatically detect the `.java-version` file in the project root and configure Maven to use the correct Java version.

To build the project and produce a JAR file, run:

```
mvn clean package -DskipTests
```

This will generate a `target/` directory containing a single JAR file named:

```
irc-<version>-SNAPSHOT.jar
```

## Running

Both the client and server can be run using the `java` command, with the JAR file produced by Maven included on the classpath:

```
java -cp target/irc-1.0.0-SNAPSHOT.jar <main class>
```

### Server

To view a list of options supported by the server, run the following command (note the `--help` flag):

```
java -cp target/irc-1.0.0-SNAPSHOT.jar com.jessegrabowski.irc.server.IRCServer --help
Pure-Java IRC Server

Usage:
	java -cp [jarfile] com.jessegrabowski.irc.server.IRCServer [options] [args]

Options:
	-p, --port <value> : port of the IRC server (default 6667)
	-H, --host <value> : host name of the IRC server (default: auto-detected; not reliable behind NAT — configure explicitly for production)
	-l, --log-file <value> : log file pattern, supports %u and %g formats for rotation
	-L, --log-level <value> : log level, integer or j.u.l.Level well-known name
	-P, --password <value> : password for the IRC server
	-f, --ping-frequency <value> : client heartbeat frequency (ms)
	-i, --idle-timeout <value> : duration of inactivity before a client is considered dead (ms)
	-I, --isupport-properties <value> : location of RPL_ISUPPORT definitions (default classpath:/isupport.properties)
	-S, --server-name <value> : name of the irc server
	-o, --operator-name <value> : operator account name
	-O, --operator-password <value> : operator account password
	-N, --nickname-history <value> : maximum depth of nickname history (default 200)
	-M, --motd <value> : location of MOTD file (.txt)
	-D, --dcc-port <value> : port for DCC connections (default 49152-65535)
```

For the purposes of this assignment, the two most important flags are:

- `-H` / `--host`  
  The IP address or hostname at which the server should be accessible to clients. Automatic detection is unreliable, so this **must** be set explicitly for DCC file transfers to function correctly. Defaults to `127.0.0.1`.

- `-L` / `--log-level`  
  I recommend setting this to `FINE` to view incoming and outgoing raw IRC messages. Defaults to `INFO`.

To start the server for evaluation, run the following command (replacing `192.168.40.129` with the IP address of your machine):

```
java -cp target/irc-1.0.0-SNAPSHOT.jar com.jessegrabowski.irc.server.IRCServer -H 192.168.40.129 -L FINE
```

### Client

To view a list of options supported by the client, run the following command (note the `--help` flag):

```
java -cp target/irc-1.0.0-SNAPSHOT.jar com.jessegrabowski.irc.client.IRCClient --help
Pure-Java IRC Client

Usage:
	java -cp [jarfile] com.jessegrabowski.irc.client.IRCClient [options] [args]

Options:
	-p, --port <value> : port of the IRC server (default 6667)
	-r, --read-timeout <value> : idle timeout before closing connection (default 600000)
	-c, --connect-timeout <value> : timeout for establishing server connection (default 10000)
	-C, --charset <value> : charset used for communication with the server (default UTF-8)
	-s, --simple-ui : use non-interactive mode (no cursor repositioning or dynamic updates; required on some terminals)
	-n, --nickname <value> : nickname of the IRC user
	-R, --real-name <value> : real name of the IRC user
	-P, --password <value> : password for the IRC server
	-l, --log-file <value> : log file pattern, supports %u and %g formats for rotation
	-L, --log-level <value> : log level, integer or j.u.l.Level well-known name
	-m, --my-address <value> : public address to use for CTCP DCC

Positionals:
	arg0 (required) : hostname of the IRC server
```

For the purposes of this assignment, the two most important flags are:

- `-n` / `--nickname`  
  The nickname to use for the client. This will be your username on the IRC server and must be unique. Nicknames should be no more than 9 characters long; longer values will be truncated by the server. Defaults to `auto`, which generates a random nickname.

- `-s` / `--simple-ui`  
  Enables a simplified, non-interactive UI that avoids terminal-specific features. This mode does not support all client functionality and should only be used if the standard client cannot run correctly on your system.

Additionally, `arg0` specifies the hostname or IP address of the IRC server to connect to.

To start the client for evaluation, run the following command (replacing `192.168.40.129` with the IP address of your machine and `jesse` with your own nickname):

```
java -cp target/irc-1.0.0-SNAPSHOT.jar com.jessegrabowski.irc.client.IRCClient -n jesse 192.168.40.129
```

## Evaluation

### Getting Started

1. Launch the server and clients as described above. You may start them in any order; in this case I started the clients first to show what happens when a server cannot be found. If you start the server first, the clients will automatically connect to it.
![getting-started.png](doc/getting-started.png)

2. If a client does not connect to the server automatically, use `/connect` to try again.
![manual-connect.png](doc/manual-connect.png)

3. If you configure multiple clients to use the same nickname, you may see an error stating that your nickname is already in use. To fix this, select a new nickname using the `/nick <name>` command on the offending client. Note that this is temporary and will be reset when the client reconnects.
![fix-nickname.png](doc/fix-nickname.png)

4. Once you are fully connected, you will receive some informational messages from the server, including statistics on connected clients, available channels (if any), and a message of the day (MOTD). This guide focuses on the criteria of the assignment, but you can view additional capabilities using the `/help` command.
![help.png](doc/help.png)

### Listing Users

To view a list of users currently connected to the server, use the `/who` command.

![who.png](doc/who.png)

### Listing Channels

To view a list of channels currently available on the server, use the `/list` command. Note that channels are not created automatically, a user must first `/join` a channel for it to appear in the list.

![list.png](doc/list.png)

### Direct Messages

Direct messages can be sent between clients using the `/msg <nickname> <message>` command.

![msg.png](doc/msg.png)

### Sending Files

Files can be sent between clients using the `/send <nickname> <path>` command. File paths resolve relative to the directory the client was launched from.

![send.png](doc/send.png)

Upon initiating a send, both the sender and receiver will be given a numeric tracking ID that can be used to interact with the transfer (these numbers will likely be different for each client). To accept the transfer, the receiver should use the command `/accept <id>`.

![accept.png](doc/accept.png)

While small files will generally finish quite quickly, larger files may take longer to transfer. In those cases, both the sender and receiver will be able to view transfer progress in the status bar.

![send-progress.png](doc/send-progress.png)

Upon completion, the sender will be shown a success message. The receiver will be given the path to the transferred file. By default, this will be placed into a `downloads` directory relative to the directory the client was launched from.

![send-success.png](doc/send-success.png)

### Channels

Upon joining the server, a user will not be in any channels. If they wish to join or create one, they may do so using the `/join <channel>` command. Generally, IRC channels generally must begin with the prefix `#` or `&`, although this is server-dependent.

![join.png](doc/join.png)

Doing so will cause the channel to be focused (shown in the prompt). Any text typed that isn't a valid command will be send to the focused channel as if the user had typed `/msg <channel> <text>`.

![join-focus.png](doc/join-focus.png)

By default, a channel has no topic. A topic may be set by using `/topic <topic>` and the current topic may be viewed using `/topic`.

![topic.png](doc/topic.png)

Users may focus a different channel using the `/focus <channel>` command.

![focus.png](doc/focus.png)

Finally, users may leave a channel using the `/part <channel>` command. This will cause the client to disconnect from the channel, and their focus will be set to the last channel they interacted with before it, if any.

![part.png](doc/part.png)

### Exiting the Server

Users may gracefully disconnect from the server using the `/quit` command, with an optional message (`/quit <message>`) that will be shown to any users that share a channel with them.

![quit.png](doc/quit.png)

Finally, users may exit the client gracefully by using the `/exit` command. Both the client and server also handle JVM shutdown signals (`CTRL+C`); this is the expected way to close the server.