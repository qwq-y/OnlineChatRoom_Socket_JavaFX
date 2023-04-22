package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Controller implements Initializable {

  @FXML
  ListView<Message> chatContentList;
//  ObservableList<Message> messageList = FXCollections.observableArrayList();

  private final String HOST = "localhost";
  private final int PORT = 8080;
  private Socket socket;
  private ObjectInputStream in;
  private ObjectOutputStream out;

  String username;
  List<ChatRecord> records = new ArrayList<>();

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {

    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");
    Optional<String> input = dialog.showAndWait();

    if (input.isPresent() && !input.get().isEmpty()) {
      socket = new Socket();
      try {
        InetAddress addre = InetAddress.getByName(HOST);

        InetSocketAddress socketAddress = new InetSocketAddress(addre, PORT);
        socket.connect(socketAddress);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        System.out.println("streams created");

        // login with duplicate name check
        boolean isNameDup = true;
        do {
          username = input.get();
          System.out.println("input username: " + username);
          Message sndmsg = new Message(System.currentTimeMillis(), username,
              new String[]{"default"},
              username, MessageType.LOGIN);
          out.writeObject(sndmsg);
          out.flush();
          System.out.println("client sndmsg: " + sndmsg.getData());

          Message rsvmsg = (Message) in.readObject();
          System.out.println("client rsvmsg: " + rsvmsg.getData());
          if (rsvmsg.getType() == MessageType.SUCCESS) {
            isNameDup = false;
//            messageList.add(rsvmsg);
          }
        } while (isNameDup);

      } catch (Exception e) {
        e.printStackTrace();
      }

    } else {
      System.out.println("Invalid username " + input + ", exiting");
      Platform.exit();
    }

//    chatContentList.setItems(messageList);
//    chatContentList.setCellFactory(new MessageCellFactory());
  }

  @FXML
  public void createPrivateChat() {

    // get user list
    List<String> userList = new ArrayList<>();
    try {
      Message sndmsg = new Message(System.currentTimeMillis(), username, new String[]{"default"}, "userList", MessageType.REQUEST);
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

    // show users
    ObservableList<ChatRecord> chatRecords = FXCollections.observableArrayList(records);
    ListView<ChatRecord> listView = new ListView<>(chatRecords);

    listView.setCellFactory(new Callback<ListView<ChatRecord>, ListCell<ChatRecord>>() {
      @Override
      public ListCell<ChatRecord> call(ListView<ChatRecord> listView) {
        return new ListCell<ChatRecord>() {
          @Override
          protected void updateItem(ChatRecord record, boolean empty) {
            super.updateItem(record, empty);
            if (record != null && !empty) {
              setText(record.getNames().toString() + ": " + record.getMessages().get(record.getMessages().size() - 1));
            } else {
              setText(null);
            }
          }
        };
      }
    });

    listView.setOnMouseClicked(event -> {
      if (!listView.getSelectionModel().isEmpty()) {
        ChatRecord record = listView.getSelectionModel().getSelectedItem();
        // Do something with the selected record
      }
    });



    // create checkboxes for each user
    List<CheckBox> userCheckboxes = new ArrayList<>();
    for (String user : userList) {
      CheckBox checkBox = new CheckBox(user);
      userCheckboxes.add(checkBox);
    }

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      // get selected users
      List<String> selectedUsers = new ArrayList<>();
      for (CheckBox checkBox : userCheckboxes) {
        if (checkBox.isSelected()) {
          selectedUsers.add(checkBox.getText());
        }
      }
      // check if chat already exists for selected users
      boolean chatExists = false;
      for (ChatRecord record : records) {
        if (record.getNames().containsAll(selectedUsers)) {
          // chat already exists, select the existing chat item
          chatList.getSelectionModel().select(record);
          chatExists = true;
          break;
        }
      }
      if (!chatExists) {
        // create new chat item and select it
        ChatRecord record = new ChatRecord(selectedUsers);
        records.add(record);
        chatList.getSelectionModel().select(record);
      }
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


  @FXML
//  public void createPrivateChat() {
//    AtomicReference<String> user = new AtomicReference<>();
//
//    Stage stage = new Stage();
//    ComboBox<String> userSel = new ComboBox<>();
//
//    try {
//      Message sndmsg = new Message(System.currentTimeMillis(), username,
//          new String[]{"default"},
//          "userList", MessageType.REQUEST);
//      out.writeObject(sndmsg);
//      out.flush();
//      System.out.println("client sndmsg: " + sndmsg.getData());
//
//      Message rsvmsg = (Message) in.readObject();
//      System.out.println("client rsvmsg: " + rsvmsg.getData());
//      String userListStr = rsvmsg.getData();
//      List<String> userList = new ArrayList<>(Arrays.asList(userListStr.split(",")));
////      userList.remove(username);
//
//      userSel.getItems().addAll(userList);
//
//    } catch (IOException | ClassNotFoundException e) {
//      e.printStackTrace();
//    }
//
//    Button okBtn = new Button("OK");
//    okBtn.setOnAction(e -> {
//      user.set(userSel.getSelectionModel().getSelectedItem());
//      stage.close();
//    });
//
//    HBox box = new HBox(10);
//    box.setAlignment(Pos.CENTER);
//    box.setPadding(new Insets(20, 20, 20, 20));
//    box.getChildren().addAll(userSel, okBtn);
//    stage.setScene(new Scene(box));
//    stage.showAndWait();
//
//    // TODO: if the current user already chatted with the selected user, just open the chat with that user
//    // TODO: otherwise, create a new chat item in the left panel, the title should be the selected user's name
//
//  }

  /**
   * A new dialog should contain a multi-select list, showing all user's name. You can select
   * several users that will be joined in the group chat, including yourself.
   * <p>
   * The naming rule for group chats is similar to WeChat: If there are > 3 users: display the first
   * three usernames, sorted in lexicographic order, then use ellipsis with the number of users, for
   * example: UserA, UserB, UserC... (10) If there are <= 3 users: do not display the ellipsis, for
   * example: UserA, UserB (2)
   */
  @FXML
  public void createGroupChat() {
  }


  public void openChat(ChatRecord record) {
    // 创建 UI 元素
    BorderPane root = new BorderPane();
    TextArea chatArea = new TextArea();
    TextField inputField = new TextField();
    Button sendButton = new Button("Send");

    // 设置 UI 元素属性
    chatArea.setEditable(false);
    chatArea.setWrapText(true);
    inputField.setPromptText("Type your message here...");

    // 添加 UI 元素到容器中
    root.setCenter(chatArea);
    HBox inputBox = new HBox(10, inputField, sendButton);
    inputBox.setPadding(new Insets(10));
    root.setBottom(inputBox);

    // 创建一个 Scene 并设置到 Stage 中
    Scene scene = new Scene(root, 400, 400);
    Stage stage = new Stage();
    stage.setTitle(record.getNames());
    stage.setScene(scene);

    // 在 sendButton 被点击时发送消息
    sendButton.setOnAction(event -> {
      String message = inputField.getText().trim();
      if (!message.isEmpty()) {
        String formattedMessage = String.format("[%s] %s\n", LocalDateTime.now(), message);
        chatArea.appendText(formattedMessage);
        inputField.clear();
      }
    });

    // 在窗口关闭时通知聊天记录保存
//      stage.setOnCloseRequest(event -> {
//        // 保存聊天记录
//        record.saveChatRecord(chatArea.getText());
//      });

    // 加载保存的聊天记录
    List<String> messages = record.getMessages();
    if (messages != null && !messages.isEmpty()) {
      String messagesStr = String.join("\n", messages);
      chatArea.setText(messagesStr);
    }

    // 显示窗口
    stage.show();
  }


  /**
   * Sends the message to the <b>currently selected</b> chat.
   * <p>
   * Blank messages are not allowed. After sending the message, you should clear the text input
   * field.
   */
  @FXML
  public void doSendMessage() {
    try {
//      Message sndmsg = new Message(System.currentTimeMillis(), username,
//          new String[]{"default"},
//          "userList", MessageType.CHAT);
//      out.writeObject(sndmsg);
//      out.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
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
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model. Hint: you
   * may also define a cell factory for the chats displayed in the left panel, or simply override
   * the toString method.
   */
  private class MessageCellFactory implements Callback<ListView<Message>, ListCell<Message>> {

    @Override
    public ListCell<Message> call(ListView<Message> param) {
      return new ListCell<Message>() {

        @Override
        public void updateItem(Message msg, boolean empty) {
          super.updateItem(msg, empty);
          if (empty || Objects.isNull(msg)) {
            return;
          }

          HBox wrapper = new HBox();
          Label nameLabel = new Label(msg.getSentBy());
          Label msgLabel = new Label(msg.getData());

          nameLabel.setPrefSize(50, 20);
          nameLabel.setWrapText(true);
          nameLabel.setStyle("-fx-border-color: black; -fx-border-width: 1px;");

          if (username.equals(msg.getSentBy())) {
            wrapper.setAlignment(Pos.TOP_RIGHT);
            wrapper.getChildren().addAll(msgLabel, nameLabel);
            msgLabel.setPadding(new Insets(0, 20, 0, 0));
          } else {
            wrapper.setAlignment(Pos.TOP_LEFT);
            wrapper.getChildren().addAll(nameLabel, msgLabel);
            msgLabel.setPadding(new Insets(0, 0, 0, 20));
          }

          setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
          setGraphic(wrapper);
        }
      };
    }
  }
}
