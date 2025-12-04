package com._404.wms;

import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginApplication extends Application {
	@Override
	public void start (Stage stage) throws IOException {
		javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(LoginApplication.class.getResource("login.fxml"));
		javafx.scene.Scene scene = new javafx.scene.Scene(fxmlLoader.load());
		stage.setTitle("Login");
		stage.setScene(scene);
		stage.show();
	}
}
