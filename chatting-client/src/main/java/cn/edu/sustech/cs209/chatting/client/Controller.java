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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class Controller implements Initializable {

  @FXML
  ListView<Message> chatContentList;
  ObservableList<Message> messageList = FXCollections.observableArrayList();
  private final String HOST = "localhost";
  private final int PORT = 8080;
  String username;
  private Socket socket;
  private ObjectInputStream in;
  private ObjectOutputStream out;
  List<ChatRecord> chatRecords = new ArrayList<>();

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
        InetAddress addre=InetAddress.getByName(HOST);

        InetSocketAddress socketAddress=new InetSocketAddress(addre,PORT);
        socket.connect(socketAddress);

        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
//        BufferedReader bin = new BufferedReader(in);
        System.out.println("streams created");

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
            messageList.add(rsvmsg);
          }
        } while (isNameDup);

      } catch (Exception e) {
        e.printStackTrace();
      }

    } else {
      System.out.println("Invalid username " + input + ", exiting");
      Platform.exit();
    }

    chatContentList.setItems(messageList);
    chatContentList.setCellFactory(new MessageCellFactory());
  }

  @FXML
  public void createPrivateChat() {
    AtomicReference<List<String>> selectedUsers = new AtomicReference<>(new ArrayList<>());

    Stage stage = new Stage();
    ListView<String> userSel = new ListView<>();
//    ComboBox<String> userSel = new ComboBox<>();

    List<String> userList = getUserList();
    userSel.getItems().addAll(userList);

    Button okBtn = new Button("OK");
    okBtn.setOnAction(e -> {
      selectedUsers.set(new ArrayList<>(userSel.getSelectionModel().getSelectedItems()));
      stage.close();
    });

    HBox box = new HBox(10);
    box.setAlignment(Pos.CENTER);
    box.setPadding(new Insets(20, 20, 20, 20));
    box.getChildren().addAll(userSel, okBtn);

    // Set up the ListView with multiple selection enabled
    userSel.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    userSel.setPrefHeight(100);

    stage.setScene(new Scene(box));
    stage.showAndWait();

    List<String> users = selectedUsers.get();
    if (!users.isEmpty()) {
      String chatName = String.join(", ", users);
      ChatRecord record = getRecordByName(chatName);
      if (record == null) {
        ChatRecord newRecord = new ChatRecord(chatName);
        openChat(newRecord, stage);
      } else {
        openChat(record, stage);
      }
    }
  }

  private List<String> getUserList() {
    List<String> userList = null;
    try {
      Message sndmsg = new Message(System.currentTimeMillis(), username,
          new String[]{"default"},
          "userList", MessageType.REQUEST);
      out.writeObject(sndmsg);
      out.flush();
      System.out.println("client sndmsg: " + sndmsg.getData());

      Message rsvmsg = (Message)in.readObject();
      System.out.println("client rsvmsg: " + rsvmsg.getData());
      String userListStr = rsvmsg.getData();
      userList = new ArrayList<>(Arrays.asList(userListStr.split(",")));
      userList.remove(username);

    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }
    return userList;
  }

  private ChatRecord getRecordByName(String name) {
    for (ChatRecord record : chatRecords) {
      if (record.getUser().equals((name))) {
        return record;
      }
    }
    return null;
  }

  private void openChat(ChatRecord record, Stage stage) {
//    Stage stage = new Stage();
    ChatPanel panel = new ChatPanel(record);

  }

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

  class ChatRecord {
    private String user;    // the opposite user
    private List<String> messages = new ArrayList<>();

    public ChatRecord(String user) {
      this.user = user;
    }

    public String getUser() {
      return user;
    }

    public List<String> getMessages() {
      return messages;
    }

    public boolean deletePieceOfMessage(String m) {
      return messages.remove(m);
    }
  }

  class ChatPanel extends BorderPane {
    private Label titleLabel = new Label();
    private ListView<String> messageListView = new ListView<>();

    public ChatPanel(ChatRecord record) {
      setTitle(record.getUser());
      setMessages(record.getMessages());
      this.setTop(titleLabel);
      this.setCenter(messageListView);
    }

    public void setTitle(String title) {
      titleLabel.setText(title);
    }

    public void setMessages(List<String> messages) {
      messageListView.getItems().addAll(messages);
    }

    public void addMessage(String message) {
      messageListView.getItems().add(message);
    }
  }

  /**
   * You may change the cell factory if you changed the design of {@code Message} model.
   * Hint: you may also define a cell factory for the chats displayed in the left panel,
   * or simply override the toString method.
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
