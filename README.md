# IRC server - user documentation

## How to launch the application

Start as a normal java application. Requires Java runtime 23.

Takes port number as an optional parameter, the default is 6667. Starts a server listening on localhost:port.

## How to test the application

The easiest way to test the application is to connect to the port on TCP and send and receive raw text.
For this any tool can be used, on linux a good one is netcat.

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

Parameters: `<channel>{,<channel>}`

Joins user to channel(s). If the channel does not exist it is created with the user as an operator.
If user joins the channel, server replies channel topic (if it exists) and names of channel members.

Numeric replies:
-  ERR_NEEDMOREPARAMS (461)
-  ERR_BADCHANMASK (476)

Example:
`JOIN #foobar                    ; join channel #foobar.`

### KICK

Parameters: `<channel> <user> *( "," <user> ) [<comment>]`

The KICK command can be used to request the forced removal of a user from a channel.
It causes the <user> to be removed from the <channel> by force.

Numeric replies:
-  ERR_NEEDMOREPARAMS (461)
-  ERR_NOSUCHCHANNEL (403)
-  ERR_NOTONCHANNEL (442)
-  ERR_CHANOPRIVSNEEDED (482)
-  ERR_USERNOTINCHANNEL (441)

Example:
`KICK #Finnish Matthew           ; Command to kick Matthew from #Finnish`

### LIST

Parameters: `[<channel>{,<channel>}]`

The LIST command is used to get a list of channels along with some information about each channel.
If no parameter is given all server's channels are listed

Example:
`LIST                            ; Command to list all channels`

### NAMES

Parameters: `<channel>{,<channel>}`

The NAMES command is used to view the nicknames joined to a channel.

Example:
`NAMES #twilight_zone,#42        ; List all visible users on "#twilight_zone" and "#42".`

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

Parameters: `<target>{,<target>} <text to be sent>`

The PRIVMSG command is used to send private messages between users, as well as to send messages to channels.
<target> is the nickname of a client or the name of a channel.

Numeric replies:
-  ERR_NORECIPIENT (411)
-  ERR_NOSUCHNICK (401)

Example:
`PRIVMSG Angel :yes I'm receiving it !      ; Command to send a message to Angel.`

### PART

Parameters: `<channel>{,<channel>} [<reason>]`

The PART command removes the client from the given channel(s).
On sending a successful PART command, the user will receive a PART message from the server for each channel they have been removed from.
<reason> is the reason that the client has left the channel(s).

Numeric replies:
-  ERR_NEEDMOREPARAMS (461)
-  ERR_NOSUCHCHANNEL (403)
-  ERR_NOTONCHANNEL (442)

### TOPIC

Parameters: `<channel> [<topic>]`

The TOPIC command is used to change or view the topic of the given channel.
If <topic> is not given, either RPL_TOPIC or RPL_NOTOPIC is returned specifying the current channel topic or lack of one.
If <topic> is an empty string, the topic for the channel will be cleared.

Numeric replies:
-  ERR_NEEDMOREPARAMS (461)
-  ERR_NOTONCHANNEL (442)
-  ERR_UNKNOWNERROR (400)

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


