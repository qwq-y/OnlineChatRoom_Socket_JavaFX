package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.util.ArrayList;
import java.util.List;

public class ChatRecord {
  private List<String> names = new ArrayList<>();   // the opposite user
  private List<Message> messages = new ArrayList<>();

  public ChatRecord(List<String> names) {
    this.names = names;
  }

  public List<String> getNames() {
    return names;
  }

  public List<Message> getMessages() {
    return messages;
  }

  public void updateMessage(Message m) {
    messages.add(m);
  }

  @Override
  public String toString() {
    return String.join(",", names);
  }
}
