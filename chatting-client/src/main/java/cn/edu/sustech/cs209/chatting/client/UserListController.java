package cn.edu.sustech.cs209.chatting.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class UserListController implements Initializable {
  @FXML
  VBox mainvbox;
  List<String> userList;
  CheckBox[] userCheckboxes;
  List<ChatRecord> records = new ArrayList<>();

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    userCheckboxes = new CheckBox[userList.size()];
    for (int i = 0; i < userList.size(); i++) {
      userCheckboxes[i] = new CheckBox(userList.get(i));
    }
    mainvbox.getChildren().addAll(userCheckboxes);

  }
  @FXML
  private void okBtn(){
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
    //Node source = (Node) event.getSource();
    Stage stage = (Stage) mainvbox.getScene().getWindow();
    stage.close();

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
  public void openChat(ChatRecord record){
    System.out.println("ok");
  }
}
