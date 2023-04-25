package cn.edu.sustech.cs209.chatting.client;

import cn.edu.sustech.cs209.chatting.common.Message;
import cn.edu.sustech.cs209.chatting.common.MessageType;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;

public class ClientMain extends Application {

  private final String HOST = "localhost";
  private final int PORT = 8080;
  private Socket socket;
  BlockingQueue<Message> messageQueue;
  private ObjectOutputStream out;

  String username;
  List<ChatRecord> records = new ArrayList<>();
  ListView<String> chatHistoryListView = new ListView<>();

  @Override
  public void start(Stage primaryStage) {

    try {
      socket = new Socket(HOST, PORT);
      messageQueue = new LinkedBlockingQueue<>();

      MySocketThread socketThread = new MySocketThread(socket, messageQueue);
      socketThread.start();

      out = new ObjectOutputStream(socket.getOutputStream());

      login();

      Platform.setImplicitExit(false);

      Timer timer = new Timer();
      Runnable runnable = new Runnable() {
        @Override
        public void run() {
          Message rsvmsg = null;
          try {
            rsvmsg = messageQueue.poll(100, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          if (rsvmsg != null) {
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
                  buildChatRoom(primaryStage);
                }
                break;
              case WARNING:
                if (rsvmsg.getData().equals("duplicate name")) {
                  login();
                }
                break;
              case EXIT:
                friendExit(rsvmsg);
                break;
            }
          } else {
//                  System.out.println("do other things...");
          }
        }
      };

      TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
          Platform.runLater(runnable);
        }
      };
      timer.schedule(timerTask, 20, 100);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void sendMessage(String sendTo, String data, MessageType type) {
    try {
      Message sndmsg = new Message(System.currentTimeMillis(), username, sendTo,
          data, type);
      out.writeObject(sndmsg);
      out.flush();
      System.out.println(username + " sndmsg to " + sendTo + ": " + sndmsg.getData());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void receiveChatMessage(Message rsvmsg) {
    List<String> names0 = Arrays.asList(
        rsvmsg.getGroup().split(","));
    List<String> names = new ArrayList<>(names0);
    names.remove(username);

    ChatRecord existingRecord = getExistingRecord(names);
    if (existingRecord != null) {
      existingRecord.updateMessage(rsvmsg);
      openExistChat(existingRecord);
    } else {
      ChatRecord newRecord = new ChatRecord(username, names, new Stage(), new TextArea());
      newRecord.updateMessage(rsvmsg);
      records.add(newRecord);
      openNewChat(newRecord);
    }

    // update history
    updateHistoryListView();
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
        sendMessage("default", username, MessageType.LOGIN);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("Invalid username " + input + ", exiting");
      Platform.exit();
    }
  }

  private void updateHistoryListView() {
    setHistoryListView();
    Stage primaryStage = (Stage) chatHistoryListView.getScene().getWindow();
    primaryStage.show();
  }

  private void setHistoryListView() {
    ObservableList<String> recordStrs = FXCollections.observableArrayList();
    for (ChatRecord record : records) {
      recordStrs.add(record.toString());
    }
    chatHistoryListView.setItems(recordStrs);
    if (recordStrs.size() == 0) {
      chatHistoryListView.getItems().add("no history...");
    }

    chatHistoryListView.setOnMouseClicked(event -> {
      String selectedItem = chatHistoryListView.getSelectionModel().getSelectedItem();
      if (selectedItem != null && selectedItem != "no history...") {
        String withNames = selectedItem.split(": ")[0];
        String names = withNames.substring(5);
        ChatRecord record = getRecordByName(names);
        try {
          openExistChat(record);
        } catch (NullPointerException e) {
          e.printStackTrace();
        }
      }
    });

  }

  public void buildChatRoom(Stage primaryStage) {
    BorderPane root = new BorderPane();

    // 创建一个包含“新建聊天”按钮的顶部面板
    HBox topPane = new HBox();
    topPane.setAlignment(Pos.CENTER);
    topPane.setPadding(new Insets(10));
    topPane.setSpacing(10);

    // 创建顶部面板里的按钮，并绑定事件
    Button newChatButton = new Button("new chat");
    newChatButton.setOnAction(event -> {
      getUserList();
    });

    Button exitButton = new Button("exit");
    exitButton.setOnAction(event -> {
      exit();
    });

    topPane.getChildren().addAll(newChatButton, exitButton);

    // 创建一个显示聊天历史记录的列表视图
    setHistoryListView();

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

  public void exit() {
    sendMessage("server", "bye", MessageType.EXIT);
    Stage primaryStage = (Stage) chatHistoryListView.getScene().getWindow();
    primaryStage.close();
  }

  private void noticeFriendLeave(ChatRecord record, TextArea chatArea, String friendName) {
    List<Message> messages = record.getMessages();
    String noticeStr = "\n------- " + friendName + " has left the room -------\n";
    Message noticeMsg = new Message(System.currentTimeMillis(), "system", username, noticeStr, MessageType.NOTICE);
    messages.add(noticeMsg);
    loadRecords(record, chatArea);
  }

  public void friendExit(Message rsvmsg) {
    String friendName = rsvmsg.getData();
    Iterator<ChatRecord> iterator = records.iterator();
    while (iterator.hasNext()) {
      ChatRecord record = iterator.next();
      if (record.getNames().contains(friendName)) {
        Stage stage = record.getStage();
//        if (record.getNames().size() == 1) {
//          HBox hbox = (HBox) ((BorderPane) stage.getScene().getRoot()).getBottom();
//          Button sendButton = (Button) hbox.getChildren().get(2);
//          hbox.getChildren().remove(sendButton);
//        }
//        record.removeName(friendName);
        noticeFriendLeave(record, record.getChatArea(), friendName);
        stage.show();
      }
    }
  }


  private ChatRecord getRecordByName(String names) {
    List<String> namesList = Arrays.asList(names.split(","));
    for (ChatRecord record : records) {
      if (record.getNames().size() == namesList.size() && namesList.containsAll(
          record.getNames())) {
        return record;
      }
    }
    return null;
  }

  public void getUserList() {
    try {
      sendMessage("default", "userList", MessageType.REQUEST);
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
        openExistChat(existingRecord);
      } else {
        // create new chat record
        ChatRecord newRecord = new ChatRecord(username, selectedUsers, new Stage(), new TextArea());
        records.add(newRecord);
        openNewChat(newRecord);
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

  private ChatRecord getExistingRecord(List<String> names0) {
    List<String> names = new ArrayList<>(names0);
    ChatRecord existingRecord = null;
    for (ChatRecord record : records) {
      List<String> recordNames = record.getNames();
      if (recordNames.size() == names.size() && recordNames.containsAll(names)) {
        existingRecord = record;
//        System.out.println("=============> found!");
        break;
      }
    }
    return existingRecord;
  }

  public void openExistChat(ChatRecord record) {
    Stage stage = record.getStage();
    TextArea chatArea = record.getChatArea();
    loadRecords(record, chatArea);
    stage.show();
  }

  public void openNewChat(ChatRecord record) {
    // create UI elements
    BorderPane root = new BorderPane();
    TextArea chatArea = record.getChatArea();
    TextField inputField = new TextField();
    Button sendButton = new Button("Send");
    Button emojiButton = new Button("😊");

    // set UI elements' property
    chatArea.setEditable(false);
    chatArea.setWrapText(true);
    inputField.setPromptText("chat as " + username + "...");

    // add UI elements into container
    root.setCenter(chatArea);
    HBox inputBox = new HBox(10, inputField, emojiButton, sendButton);
    inputBox.setPadding(new Insets(10));
    root.setBottom(inputBox);

    // create a  Scene, put it into Stage
    Scene scene = new Scene(root, 400, 400);
    Stage stage = record.getStage();
    String names = String.join(",", record.getNames());
    String title = record.getTitle();
    stage.setTitle(title);
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
          record.updateMessage(sndmsg);
          sendMessage(sndmsg.getSendTo(), sndmsg.getData(), sndmsg.getType());
          loadRecords(record, chatArea);
          stage.show();
          updateHistoryListView();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

// set action on emojiButton
    emojiButton.setFocusTraversable(false);   // remain cursor in inputField
    emojiButton.setOnAction(event -> {
      int position = inputField.getCaretPosition();
      // create emoji list popup
      Popup emojiPopup = new Popup();
      HBox emojiButtons = new HBox();
      String[] emojis = {"😊", "😃", "😍", "🤔", "😴", "🤢", "😘", "❤", "👌"};
      for (String emoji : emojis) {
        Button button = new Button(emoji);
        button.setStyle("-fx-font-size: 15px; -fx-background-color: transparent;");
        button.setOnAction(buttonEvent -> {
          inputField.insertText(position, emoji);
          inputField.positionCaret(position + emoji.length());
          emojiPopup.hide();
        });
        emojiButtons.getChildren().add(button);
      }
      VBox emojiBox = new VBox(emojiButtons);
      emojiBox.setStyle(
          "-fx-background-color: white; -fx-border-color: gray; -fx-border-width: 0.5px;");

      emojiPopup.getContent().add(emojiBox);

      // set style for emoji list popup
      emojiPopup.setAutoHide(true);
      emojiPopup.setAnchorLocation(PopupWindow.AnchorLocation.CONTENT_TOP_LEFT);

      // show emoji list popup
      emojiPopup.show(emojiButton.getScene().getWindow(),
          emojiButton.localToScene(emojiButton.getBoundsInLocal()).getMinX(),
          emojiButton.localToScene(emojiButton.getBoundsInLocal()).getMinY()
              + emojiButton.getHeight());
    });

    // enable typing Enter key to send message
    inputField.setOnKeyPressed(event -> {
      if (event.getCode() == KeyCode.ENTER) {
        sendButton.fire();
        event.consume();
      }
    });

    loadRecords(record, chatArea);

    stage.show();
  }

  private void loadRecords(ChatRecord record, TextArea chatArea) {
    List<Message> messages = record.getMessages();
    if (messages != null && !messages.isEmpty()) {
      StringBuilder messagesSb = new StringBuilder();
      for (Message m : messages) {
        messagesSb.append(m.getSentBy() + ": " + m.getData() + "\n");
      }
      int len = messagesSb.length();
      if (len > 0 && messagesSb.charAt(len - 1) == '\n') {
        messagesSb.deleteCharAt(len - 1);
      }
      String messagesStr = messagesSb.toString();
      chatArea.setText(messagesStr);
    }
  }

  private class ChatRecord {

    private String username;
    private List<String> names = new ArrayList<>();   // who will receive this sndmsg
    private List<Message> messages = new ArrayList<>();
    private Stage stage;
    private TextArea chatArea;

    public ChatRecord(String username, List<String> names, Stage stage, TextArea chatArea) {
      this.username = username;
      this.names = names;
      this.stage = stage;
      this.chatArea = chatArea;
    }

    public List<String> getNames() {
      return names;
    }

    public Stage getStage() {
      return stage;
    }

    public TextArea getChatArea() {
      return chatArea;
    }

    public void removeName(String name) {
      names.remove(name);
    }

    public List<Message> getMessages() {
      return messages;
    }

    public String getTitle() {
      if (names.size() == 1) {
        return names.get(0);
      } else if (names.size() == 2) {
        return username + "," + String.join(",", names);
      } else {
        return username + "," + names.get(0) + "," + names.get(1) + "(" + (names.size() + 1) + ")";
      }
    }

    public void updateMessage(Message m) {
      messages.add(m);
    }

    @Override
    public String toString() {
      String lastData = "";
      if (messages.size() > 0) {
        Message lastMsg = messages.get(messages.size() - 1);
        lastData = lastMsg.getData();
        Date date = new Date(lastMsg.getTimestamp());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = formatter.format(date);
        return "with " + String.join(",", names) + ": " + "[" + formattedDate + "] " + lastData;
      }

      return "no history...";
    }
  }
}