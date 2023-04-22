/*

public class Controller implements Initializable {
  // ...

  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;

  // ...

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    Dialog<String> dialog = new TextInputDialog();
    dialog.setTitle("Login");
    dialog.setHeaderText(null);
    dialog.setContentText("Username:");

    Optional<String> input = dialog.showAndWait();
    if (input.isPresent() && !input.get().isEmpty()) {
        username = input.get();

        try (Socket socket = new Socket("localhost", 8080);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(username);
            out.flush();

            while (true) {
                String response = (String) in.readObject();
                if ("OK".equals(response)) {
                    // 如果服务器确认该用户名可用，则退出循环并继续执行
                    break;
                } else {
                    // 如果服务器报告用户名重复，则提醒用户并重新获取用户名
                    System.out.println("Username " + username + " is not available, please choose another one.");
                    input = getUserInput();
                    if (input.isPresent() && !input.get().isEmpty()) {
                        username = input.get();
                        out.writeObject(username);
                        out.flush();
                    } else {
                        System.out.println("Invalid username " + input + ", exiting");
                        Platform.exit();
                        return;
                    }
                }
            }

            // 在此处执行其他操作，以回应服务器确认

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    } else {
        System.out.println("Invalid username " + input + ", exiting");
        Platform.exit();
    }


  // ...
}









  @FXML
  public void createPrivateChat() {
    AtomicReference<String> user = new AtomicReference<>();

    // get user list from server
    List<String> userList = new ArrayList<>();
    try {
      out.writeObject("getUserList");
      out.flush();
      userList = (List<String>) in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    // exclude current user
    userList.remove(username);

    // show user selection dialog
    ChoiceDialog<String> dialog = new ChoiceDialog<>(userList.get(0), userList);
    dialog.setTitle("Private Chat");
    dialog.setHeaderText(null);
    dialog.setContentText("Choose a user:");

    Optional<String> result = dialog.showAndWait();
    if (result.isPresent()) {
      String targetUser = result.get();

      // check if there is an existing chat panel
      boolean found = false;
      for (Tab tab : chatPanelTabPane.getTabs()) {
        if (tab.getText().equals(targetUser)) {
          chatPanelTabPane.getSelectionModel().select(tab);
          found = true;
          break;
        }
      }

      // create a new chat panel if not found
      if (!found) {
        ChatPanel chatPanel = new ChatPanel(username, targetUser, out, in);
        Tab tab = new Tab(targetUser);
        tab.setContent(chatPanel);
        chatPanelTabPane.getTabs().add(tab);
        chatPanelTabPane.getSelectionModel().select(tab);
      }
    }
  }





*/