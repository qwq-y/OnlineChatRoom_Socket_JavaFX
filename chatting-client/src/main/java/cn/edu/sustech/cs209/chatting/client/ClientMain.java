package cn.edu.sustech.cs209.chatting.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {

  public static void main(String[] args) {
    launch();
  }

  @Override
  public void start(Stage primaryStage) {
    try {
      FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainController.fxml"));
      primaryStage.setScene(new Scene(fxmlLoader.load()));
      primaryStage.setTitle("Chatting Client");
      primaryStage.show();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

