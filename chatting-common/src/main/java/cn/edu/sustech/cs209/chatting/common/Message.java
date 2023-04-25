package cn.edu.sustech.cs209.chatting.common;

import java.io.Serializable;

public class Message implements Serializable {

  private Long timestamp;
  // System.currentTimeMillis()

  private String sentBy;
  // "system"(notice) or "server" or username

  private String sendTo;
  // str.split(","); String.join(",", record.getNames());
  // "server" in client to server, "default" in server to client, usernames in client-server-client

  private String group;
  // used in group chat
  // set by server, when sentTo > 1

  private String data;
  /**
   * LOGIN: data = sentBy = username
   * SUCCESS/WARING: data = relative info
   * REQUEST: data = "userList"
   * RESPOND: data = resources
   * CHAT: data = dialogs
   * EXIT: data = "bye"(exiter to server) or username of the exiter(server to others)
   * NOTICE: used notice friend left, do not send
   */

  private MessageType type;
  //  LOGIN, REQUIRE, RESPOND, EXIT, CHAT, WARNING, SUCCESS

  public Message(Long timestamp, String sentBy, String sendTo, String data, MessageType type) {
    this.timestamp = timestamp;
    this.sentBy = sentBy;
    this.sendTo = sendTo;
    this.data = data;
    this.type = type;
    group = sentBy + "," + sendTo;
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

  public String getGroup() {
    return group;
  }

  public String getData() {
    return data;
  }

  public MessageType getType() {
    return type;
  }
}


