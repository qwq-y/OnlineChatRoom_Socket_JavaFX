package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Controller extends Application {

  private final String HOST = "localhost";
  private final int PORT = 8080;
  private Socket socket;
  private ObjectInputStream in;
  private ObjectOutputStream out;

  private volatile Message rsvmsg;

  String username;
  List<ChatRecord> records = new ArrayList<>();
  ListView<String> chatHistoryListView = new ListView<>();

  @Override
  public void start(Stage primaryStage) {

    try {
      socket = new Socket();
      InetAddress addre = InetAddress.getByName(HOST);
      InetSocketAddress socketAddress = new InetSocketAddress(addre, PORT);
      socket.connect(socketAddress);

      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());
      System.out.println("streams created");

    } catch (Exception e) {
      e.printStackTrace();
    }

    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        while (true) {
          try {
            rsvmsg = (Message) in.readObject();
            System.out.println("client rsvmsg: " + rsvmsg.getType() + " " + rsvmsg.getData());
            switch (rsvmsg.getType()) {
              case CHAT:
                receiveChatMessage(rsvmsg);
                break;
              case RESPOND:
                String userListStr = rsvmsg.getData();
                List<String> userList = new ArrayList<>(Arrays.asList(userListStr.split(",")));
                userList.remove(username);
                createChat(userList);
                break;
              case SUCCESS:
                if (rsvmsg.getData().equals("username ok")) {

                }
                break;
              case WARNING:
                if (rsvmsg.getData().equals("duplicate name")) {
                  login();
                }
                break;
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    });
    thread.start();

    login();

    buildChatRoom(primaryStage);

  }

  public void receiveChatMessage(Message rsvmsg) {
    Platform.runLater(() -> {
      List<String> names = Arrays.asList(
          rsvmsg.getSentBy().split(","));
      // TODO: 要再加一个属性，区分在群聊中发消息的发送者和群聊，sentBy这时为群聊名
      ChatRecord existingRecord = getExistingRecord(names);
      if (existingRecord != null) {
        existingRecord.updateMessage(rsvmsg);
        // TODO: 好像应该先判断下是否已经打开
        openChat(existingRecord);
      } else {
        ChatRecord newRecord = new ChatRecord(names);
        newRecord.updateMessage(rsvmsg);
        records.add(newRecord);
        openChat(newRecord);
      }
      chatHistoryListView.refresh();
    });
  }

  public void login() {
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");
    Optional<String> input = dialog.showAndWait();

    if (input.isPresent() && !input.get().isEmpty()) {
        try {
          username = input.get();
          System.out.println("input username: " + username);
          Message sndmsg = new Message(System.currentTimeMillis(), username, "default",
              username, MessageType.LOGIN);
          out.writeObject(sndmsg);
          out.flush();
          System.out.println("client sndmsg: " + sndmsg.getData());

        } catch (Exception e) {
          e.printStackTrace();
        }

    } else {
      System.out.println("Invalid username " + input + ", exiting");
      Platform.exit();
    }
  }

  public void buildChatRoom(Stage primaryStage) {
    BorderPane root = new BorderPane();

    // 创建一个包含“新建聊天”按钮的顶部面板
    HBox topPane = new HBox();
    topPane.setAlignment(Pos.CENTER);
    topPane.setPadding(new Insets(10));
    topPane.setSpacing(10);
    topPane.getChildren().addAll(createNewChatButton());

    // 创建一个显示聊天历史记录的列表视图
    chatHistoryListView.getItems().addAll("聊天室1", "聊天室2", "聊天室3", "聊天室4");
    ObservableList<String> recordStrs = FXCollections.observableArrayList();
    for (ChatRecord record : records) {
      recordStrs.add(record.toString());
    }
    chatHistoryListView.setItems(recordStrs);
    chatHistoryListView.setOnMouseClicked(event -> {
      // 处理列表项的点击事件，打开相应的聊天窗口
      String selectedItem = chatHistoryListView.getSelectionModel().getSelectedItem();
      if (selectedItem != null) {
        ChatRecord record = getRecordByName(selectedItem);
        try {
          openChat(record);
        } catch (NullPointerException e) {
          e.printStackTrace();
        }
      }
    });

    // 创建一个包含聊天历史记录列表视图的中心面板
    VBox centerPane = new VBox();
    centerPane.setAlignment(Pos.CENTER);
    centerPane.setPadding(new Insets(10));
    centerPane.setSpacing(10);
    centerPane.getChildren().addAll(new Label("history"), chatHistoryListView);

    // 将顶部面板和中心面板添加到主面板中
    root.setTop(topPane);
    root.setCenter(centerPane);

    // 创建场景并设置主面板
    Scene scene = new Scene(root, 400, 400);
    primaryStage.setScene(scene);
    primaryStage.setTitle("ChatRoom: " + username);
    primaryStage.show();
  }

  private Button createNewChatButton() {
    Button newChatButton = new Button("new chat");
    newChatButton.setOnAction(event -> {
      getUserList();
    });
    return newChatButton;
  }

  private ChatRecord getRecordByName(String names) {
    List<String> namesList = Arrays.asList(names.split(","));
    for (ChatRecord record : records) {
      if (record.getNames().containsAll(namesList) && namesList.containsAll(record.getNames())) {
        return record;
      }
    }
    return null;
  }

  public void getUserList() {
    try {
      Message sndmsg = new Message(System.currentTimeMillis(), username, "default",
          "userList", MessageType.REQUEST);
      out.writeObject(sndmsg);
      out.flush();
      System.out.println("client sndmsg: " + sndmsg.getData());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void createChat(List<String> userList) {
    // create user checkboxes, and listen on selection
    CheckBox[] userCheckboxes = new CheckBox[userList.size()];
    for (int i = 0; i < userList.size(); i++) {
      userCheckboxes[i] = new CheckBox(userList.get(i));
    }
    Button okBtn = new Button("OK");
    okBtn.setOnAction(event -> {
      List<String> selectedUsers = new ArrayList<>();
      for (CheckBox cb : userCheckboxes) {
        if (cb.isSelected()) {
          selectedUsers.add(cb.getText());
        }
      }

      // check if chat records with these users already exist
      ChatRecord existingRecord = getExistingRecord(selectedUsers);
      if (existingRecord != null) {
        openChat(existingRecord);
      } else {
        // create new chat record
        ChatRecord newRecord = new ChatRecord(selectedUsers);
        records.add(newRecord);
        openChat(newRecord);
      }

      // close dialog
      Node source = (Node) event.getSource();
      Stage stage = (Stage) source.getScene().getWindow();
      stage.close();
    });

    Platform.runLater(() -> {
      VBox box = new VBox(10);
      box.setAlignment(Pos.CENTER);
      box.setPadding(new Insets(20, 20, 20, 20));
      box.getChildren().addAll(userCheckboxes);
      box.getChildren().add(okBtn);
      Stage stage = new Stage();
      stage.setScene(new Scene(box));
      stage.showAndWait();
    });
  }

  private ChatRecord getExistingRecord(List<String> names) {
    ChatRecord existingRecord = null;
    for (ChatRecord record : records) {
      List<String> recordNames = record.getNames();
      if (recordNames.size() == names.size() && recordNames.containsAll(names)) {
        existingRecord = record;
        break;
      }
    }
    return existingRecord;
  }

  public void openChat(ChatRecord record) {
    // create UI elements
    BorderPane root = new BorderPane();
    TextArea chatArea = new TextArea();
    TextField inputField = new TextField();
    Button sendButton = new Button("Send");

    // set UI elements' property
    chatArea.setEditable(false);
    chatArea.setWrapText(true);
    inputField.setPromptText("Type your message here...");

    // add UI elements into container
    root.setCenter(chatArea);
    HBox inputBox = new HBox(10, inputField, sendButton);
    inputBox.setPadding(new Insets(10));
    root.setBottom(inputBox);

    // create a  Scene, put it into Stage
    Scene scene = new Scene(root, 400, 400);
    Stage stage = new Stage();
    String names = String.join(",", record.getNames());
    stage.setTitle(names);
    stage.setScene(scene);

    // set action on sendButton
    sendButton.setOnAction(event -> {
      String message = inputField.getText().trim();
      if (!message.isEmpty()) {
        String formattedMessage = String.format("[%s] %s\n", LocalDateTime.now(), message);
        chatArea.appendText(formattedMessage);
        inputField.clear();

        try {
          Message sndmsg = new Message(System.currentTimeMillis(), username, names, message,
              MessageType.CHAT);
          out.writeObject(sndmsg);
          out.flush();
          System.out.println("client choose user to chat with: " + names);
          System.out.println("client sndmsg: " + sndmsg.getData());
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    // store records before close
//      stage.setOnCloseRequest(event -> {
//        record.saveChatRecord(chatArea.getText());
//      });

    // load records
    List<Message> messages = record.getMessages();
    if (messages != null && !messages.isEmpty()) {
      StringBuilder messagesSb = null;
      for (Message m : messages) {
        messagesSb.append(m + "\n");
      }
      int len = messagesSb.length();
      if (len > 0 && messagesSb.charAt(len - 1) == '\n') {
        messagesSb.deleteCharAt(len - 1);
      }
      String messagesStr = messagesSb.toString();
      chatArea.setText(messagesStr);
    }

    stage.show();
  }

  private class ChatRecord {

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

}
