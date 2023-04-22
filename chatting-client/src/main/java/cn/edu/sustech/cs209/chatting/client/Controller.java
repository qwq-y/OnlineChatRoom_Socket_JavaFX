package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import java.io.IOException;
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

  String username;
  List<ChatRecord> records = new ArrayList<>();

  @Override
  public void start (Stage primaryStage) {

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

    login();

    buildChatRoom(primaryStage);
  }

  public void login() {
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");
    Optional<String> input = dialog.showAndWait();

    if (input.isPresent() && !input.get().isEmpty()) {
      try {
        boolean isNameDup = true;
        do {
          username = input.get();
          System.out.println("input username: " + username);
          Message sndmsg = new Message(System.currentTimeMillis(), username, "default",
              username, MessageType.LOGIN);
          out.writeObject(sndmsg);
          out.flush();
          System.out.println("client sndmsg: " + sndmsg.getData());

          Message rsvmsg = (Message) in.readObject();
          System.out.println("client rsvmsg: " + rsvmsg.getData());
          if (rsvmsg.getType() == MessageType.SUCCESS) {
            isNameDup = false;
          }
        } while (isNameDup);

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

    // 创建一个包含标题标签和“新建聊天”按钮的顶部面板
    HBox topPane = new HBox();
    topPane.setAlignment(Pos.CENTER);
    topPane.setPadding(new Insets(10));
    topPane.setSpacing(10);
    topPane.getChildren().addAll(createNewChatButton());

    // 创建一个显示聊天历史记录的列表视图
    ListView<String> chatHistoryListView = new ListView<>();
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
    primaryStage.setTitle("ChatRoom");
    primaryStage.show();
  }

  private Button createNewChatButton() {
    Button newChatButton = new Button("new Chat");
    newChatButton.setOnAction(event -> {
      createChat();
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

  public void createChat() {
    // get user list
    List<String> userList = new ArrayList<>();
    try {
      Message sndmsg = new Message(System.currentTimeMillis(), username, "default",
          "userList", MessageType.REQUEST);
      out.writeObject(sndmsg);
      out.flush();
      System.out.println("client sndmsg: " + sndmsg.getData());

      Message rsvmsg = (Message) in.readObject();
      System.out.println("client rsvmsg: " + rsvmsg.getData());
      String userListStr = rsvmsg.getData();
      userList = new ArrayList<>(Arrays.asList(userListStr.split(",")));
      userList.remove(username);

    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

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
      ChatRecord existingRecord = null;
      for (ChatRecord record : records) {
        List<String> recordNames = record.getNames();
        if (recordNames.size() == selectedUsers.size() && recordNames.containsAll(selectedUsers)) {
          existingRecord = record;
          break;
        }
      }
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

    VBox box = new VBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userCheckboxes);
    box.getChildren().add(okBtn);
    Stage stage = new Stage();
    stage.setScene(new Scene(box));
    stage.showAndWait();

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
          Message sndmsg = new Message(System.currentTimeMillis(), username, names, message, MessageType.CHAT);
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
    List<String> messages = record.getMessages();
    if (messages != null && !messages.isEmpty()) {
      String messagesStr = String.join("\n", messages);
      chatArea.setText(messagesStr);
    }

    stage.show();
  }

  private class ChatRecord {

    List<String> names = new ArrayList<>();   // the opposite user
    List<String> messages = new ArrayList<>();

    public ChatRecord(List<String> names) {
      this.names = names;
    }

    public List<String> getNames() {
      return names;
    }

    public List<String> getMessages() {
      return messages;
    }

    @Override
    public String toString() {
      return String.join(",", names);
    }
  }
}
