package cn.edu.sustech.cs209.chatting.server;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.stream.Collectors;

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
        whileLabel:
        while (true) {
          if (socket.isClosed()) {
            exit();
          }
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
                exit();
                break whileLabel;
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
      } catch (SocketException e) {
        System.out.println("socket closed");
      } catch (Exception e) {
        e.printStackTrace();
      }

      System.out.println(username + " thread closed.");
    }

    private void exit() throws Exception {
      users.remove(username);
      String sendToStr = users.keySet().stream().collect(Collectors.joining(","));

      if (sendToStr.length() > 0) {

        // get reveiverStreams
        String[] sendToArr = sendToStr.split(",");
        HashSet<ObjectOutputStream> reveiverStreams = new HashSet<>();
        for (String name : sendToArr) {
          ObjectOutputStream stream = users.get(name);
          reveiverStreams.add(stream);
        }

        // notice others
        Message sndmsg = new Message(System.currentTimeMillis(), NAME, sendToStr, username,
            MessageType.EXIT);
        try {
          Iterator<ObjectOutputStream> itr = reveiverStreams.iterator();
          while (itr.hasNext()) {
            ObjectOutputStream stream = itr.next();
            stream.writeObject(sndmsg);
            stream.flush();
            System.out.println(
                "server sndmsg to " + sndmsg.getSendTo() + ": " + sndmsg.getType() + " "
                    + sndmsg.getData());
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      socket.close();
      System.out.println(username + " socket closed.");
    }

    private void sendMessage(String sentBy, String sendTo, String data, MessageType type)
        throws Exception {
      Message sndmsg = new Message(System.currentTimeMillis(), sentBy, sendTo, data, type);
      System.out.println(
          "server sndmsg to " + sendTo + ": " + sndmsg.getType() + " " + sndmsg.getData());
      out.writeObject(sndmsg);
      out.flush();
    }

    private void login(Message rsvmsg) throws Exception {
      String tempname = rsvmsg.getData();
      Message sndmsg;
      if (isDuplicateName(tempname)) {
        sendMessage(NAME, "default", "duplicate name", MessageType.WARNING);
      } else {
        username = tempname;
        users.put(username, out);
        sendMessage(NAME, "default", "username ok",
            MessageType.SUCCESS);
      }
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
      sendMessage(NAME, "default", userListStr,
          MessageType.RESPOND);
    }

    private void chat(Message msg) {
      System.out.println("server try to relay message to: " + msg.getSendTo());
      HashSet<ObjectOutputStream> receiverStreams = new HashSet<>();
      String[] sendToArr = msg.getSendTo().split(",");
      for (String name : sendToArr) {
        if (users.containsKey(name)) {
          ObjectOutputStream stream = users.get(name);
          receiverStreams.add(stream);
        }
      }
      try {
        Iterator<ObjectOutputStream> itr = receiverStreams.iterator();
        while (itr.hasNext()) {
          ObjectOutputStream stream = itr.next();
          stream.writeObject(msg);
          stream.flush();
          System.out.println("server sndmsg: " + msg.getData());
          System.out.println("server sndmsg to " + msg.getSendTo() + ": " + msg.getType() + " "
              + msg.getData());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}