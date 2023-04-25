package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class ServerMain {

  private static final String NAME = "server";
  private static final int PORT = 8080;
  private static HashMap<String, ObjectOutputStream> users = new HashMap<>();

  public static void main(String[] args) {
    System.out.println("Starting server");

    try (ServerSocket serversocket = new ServerSocket(PORT)) {
      while (true) {
        try {
          UserThread userThread = new UserThread(serversocket.accept());
          userThread.start();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static class UserThread extends Thread {

    private String username;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public UserThread(Socket socket) {
      try {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void run() {
      try {
        while (true) {
          Message rsvmsg = (Message) in.readObject();
          System.out.println("server rsvmsg: " + rsvmsg.getType() + " " + rsvmsg.getData());
          if (rsvmsg != null) {
            switch (rsvmsg.getType()) {
              case LOGIN:
                login(rsvmsg);
                break;
              case REQUEST:
                if (rsvmsg.getData().equals("userList")) {
                  getUserList();
                }
                break;
              case EXIT:
                socket.close();
                break;
              case CHAT:
                chat(rsvmsg);
                break;
              default:
                Message sndmsg = new Message(System.currentTimeMillis(), NAME, "default",
                    "illegal request", MessageType.WARNING);
                out.writeObject(sndmsg);
                out.flush();
            }
          }
        }
      } catch (EOFException e) {
        System.out.println("EOF");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    private void login(Message rsvmsg) throws Exception {
      String tempname = rsvmsg.getData();
      Message sndmsg;
      if (isDuplicateName(tempname)) {
        sndmsg = new Message(System.currentTimeMillis(), NAME, "default", "duplicate name",
            MessageType.WARNING);
        out.writeObject(sndmsg);
        out.flush();
      } else {
        username = tempname;
        users.put(username, out);
        sndmsg = new Message(System.currentTimeMillis(), NAME, "default", "username ok",
            MessageType.SUCCESS);
        out.writeObject(sndmsg);
        out.flush();
      }
      System.out.println("server sndmsg: " + sndmsg.getData());
    }

    private boolean isDuplicateName(String name) {
      Iterator<String> itr = users.keySet().iterator();
      while (itr.hasNext()) {
        String user = itr.next();
        if (user.equals(name)) {
          return true;
        }
      }
      return false;
    }

    private void getUserList() throws Exception {
      String[] userList = users.keySet().toArray(new String[users.size()]);
      String userListStr = String.join(",", userList);
      Message sndmsg = new Message(System.currentTimeMillis(), NAME, "default", userListStr,
          MessageType.RESPOND);
      out.writeObject(sndmsg);
      out.flush();
      System.out.println("server sndmsg: " + sndmsg.getData());
    }

    private void chat(Message msg) {
      System.out.println("server will relay message to: " + msg.getSendTo());
      HashSet<ObjectOutputStream> receiverStreams = new HashSet<>();
      String[] sendToArr = msg.getSendTo().split(",");
      for (String name : sendToArr) {
        ObjectOutputStream stream = users.get(name);
        receiverStreams.add(stream);
      }
      try {
        Iterator<ObjectOutputStream> itr = receiverStreams.iterator();
        while (itr.hasNext()) {
          ObjectOutputStream stream = itr.next();
          stream.writeObject(msg);
          stream.flush();
          System.out.println("server sndmsg: " + msg.getData());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}