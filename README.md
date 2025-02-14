# IRC server - user documentation

## How to launch the application

Start as a normal java application. Requires Java runtime 23.

Takes port number as an optional parameter, the default is 6667. Starts a server listening on localhost:port.

## How to test the application

The easiest way to test the application is to connect to the port on TCP and send and receive raw text.
For this any tool can be used, on linux good one is netcat.

Started as `nc localhost 6667` and then write and read text.

## The IRC protocol

IRC is an application layer text based protocol.
The protocol is based on commands and responses.

All messages have the same syntax:
`:<source> <command> <parameters>`

The source can be omitted. When server forwards a private message <source> is the sender. Or when server replies <source>
is the server name.

When server replies to user command, the command is a number representing a type of reply or an error number.

The implementation supports following commands:
- JOIN
- KICK
- LIST
- NAMES
- NICK
- PRIVMSG
- PART
- TOPIC
- USER

### Note on registration commands

Registration commands are NICK and USER. Both must be sent for the user to register.
Until user is registered they can only send commands NICK, USER, LIST, NAMES.

### JOIN

### KICK

### LIST

### NAMES

### NICK

Parameters: `<nickname>`

The NICK command is used to give the client a nickname or change the previous one.

The nickname must not contain
- no leading # character
- no leading colon (:)
- no ASCII space

Numeric replies:
-  ERR_NONICKNAMEGIVEN (431)
-  ERR_ERRONEUSNICKNAME (432)
-  ERR_NICKNAMEINUSE (433)

Example:
`NICK Wiz                  ; Requesting the new nick "Wiz".`

### PRIVMSG

### PART

### TOPIC

### USER

Parameters: `<username> 0 * <realname>`

The USER command is used at the beginning of a connection to specify the username and realname of a new user.

It must be noted that <realname> must be the last parameter because it may contain SPACE (' ', 0x20) characters, and must be prefixed with a colon (:).

Numeric replies:
-  ERR_NEEDMOREPARAMS (461)
-  ERR_ALREADYREGISTERED (462)

Example:
`USER guest 0 * :Ronnie Reagan ; User gets registered with username
                              "guest" and real name "Ronnie Reagan"
`


