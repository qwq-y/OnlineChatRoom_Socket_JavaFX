package cn.edu.sustech.cs209.chatting.common;

public class Message {

    private Long timestamp;   // System.currentTimeMillis()

    private String sentBy;   // "server" or username

    private String[] sendTo;   // "default" in server-client, username in client-server-client

    private String data;
    /**
     *     LOGIN: data = sentBy = username
     *     SUCCESS/WARING: data = relative info
     *     REQUEST: data = "userList"
     *     RESPOND: data = resources
     *     CHAT: data = dialogs
     *     EXIT: data = "exit"
     */

    private MessageType type;   //  LOGIN, REQUIRE, EXIT, CHAT, WARNING, SUCCESS

    public Message(Long timestamp, String sentBy, String[] sendTo, String data, MessageType type) {
        this.timestamp = timestamp;
        this.sentBy = sentBy;
        this.sendTo = sendTo;
        this.data = data;
        this.type = type;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public String getSentBy() {
        return sentBy;
    }

    public String[] getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public MessageType getType() {
        return type;
    }
}


