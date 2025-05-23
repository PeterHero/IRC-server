package cz.cuni.mff.hrdinap1.ircserver;

/** Class containing all numeric constants for message replies
 */
public final class Numerics {
    public static final int RPL_LISTSTART = 321;
    public static final int RPL_LIST = 322;
    public static final int RPL_LISTEND = 323;
    public static final int RPL_NOTOPIC = 331;
    public static final int RPL_TOPIC = 332;
    public static final int RPL_NAMREPLY = 353;
    public static final int RPL_ENDOFNAMES = 366;

    public static final int ERR_UNKNOWNERROR = 400;
    public static final int ERR_NOSUCHNICK = 401;
    public static final int ERR_NOSUCHCHANNEL = 403;
    public static final int ERR_NORECIPIENT = 411;
    public static final int ERR_NONICKNAMEGIVEN = 431;
    public static final int ERR_ERRONEUSNICKNAME = 432;
    public static final int ERR_NICKNAMEINUSE = 433;
    public static final int ERR_USERNOTINCHANNEL = 441;
    public static final int ERR_NOTONCHANNEL = 442;
    public static final int ERR_NOTREGISTERED = 451;
    public static final int ERR_NEEDMOREPARAMS = 461;
    public static final int ERR_ALREADYREGISTERED = 462;
    public static final int ERR_BADCHANMASK = 476;
    public static final int ERR_CHANOPRIVSNEEDED = 482;
}
