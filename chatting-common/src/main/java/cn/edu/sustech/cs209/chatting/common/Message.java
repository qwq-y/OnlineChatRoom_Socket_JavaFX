package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class Message implements Serializable {

    private Long timestamp;
    // System.currentTimeMillis()

    private String sentBy;        // 要再加一个属性，区分在群聊中发消息的发送者和群聊，sentBy这时为群聊名
    // "server" or username

    private String sendTo;
    // str.split(","); String.join(",", record.getNames());
    // "default" in server-client, usernames in client-server-client

    private String data;
    /**
     *     LOGIN: data = sentBy = username
     *     SUCCESS/WARING: data = relative info
     *     REQUEST: data = "userList"
     *     RESPOND: data = resources
     *     CHAT: data = dialogs
     *     EXIT: data = "exit"
     */

    private MessageType type;
    //  LOGIN, REQUIRE, RESPOND, EXIT, CHAT, WARNING, SUCCESS

    public Message(Long timestamp, String sentBy, String sendTo, String data, MessageType type) {
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

    public String getSendTo() {
        return sendTo;
    }

    public String getData() {
        return data;
    }

    public MessageType getType() {
        return type;
    }
}


