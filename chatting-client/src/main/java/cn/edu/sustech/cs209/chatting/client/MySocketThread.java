package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class MySocketThread extends Thread{
  private final Socket socket;
  private final BlockingQueue<Message> messageQueue;

  public MySocketThread(Socket socket, BlockingQueue<Message> messageQueue) {
    this.socket = socket;
    this.messageQueue = messageQueue;
  }

  @Override
  public void run() {
    try {
      ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
      while (!Thread.currentThread().isInterrupted()) {
        Message rsvmsg = (Message) in.readObject();
        messageQueue.put(rsvmsg);
      }
    } catch (EOFException e) {
      System.out.println("EOF");
    } catch (IOException | ClassNotFoundException | InterruptedException e) {
      e.printStackTrace();
    } finally {
      try {
        socket.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
