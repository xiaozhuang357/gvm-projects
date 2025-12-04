package com._404.wms;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
	
	@FXML
	private Button loginButton;
	
	@FXML
	private TextField passwordField;
	
	@FXML
	private TextField usernameField;
	
	@FXML
	void login(ActionEvent event) {
		// 1. 获取输入的用户名和密码
		String username = usernameField.getText().trim();
		String password = passwordField.getText().trim();
		
		// 2. 简单验证（可替换为数据库/配置文件验证）
		if (username.isEmpty() || password.isEmpty()) {
			showAlert("错误", "用户名/密码不能为空！", Alert.AlertType.ERROR);
			return;
		}
		// 假设：1开头的用户名 + 密码为 "manager123" 是经理
		if (username.startsWith("1") && "123".equals(password)) {
			try {
				// 3. 关闭登录窗口
				Stage loginStage = (Stage) loginButton.getScene().getWindow();
				loginStage.close();
				
				// 4. 加载经理界面 FXML
				FXMLLoader fxmlLoader = new FXMLLoader(LoginController.class.getResource("manager.fxml"));
				Scene managerScene = new Scene(fxmlLoader.load());
				
				// 5. 创建并显示经理窗口
				Stage managerStage = new Stage();
				managerStage.setTitle("经理管理界面");
				managerStage.setScene(managerScene);
				managerStage.setResizable(false); // 固定窗口大小（可选）
				managerStage.show();
				
			} catch (IOException e) {
				// 处理FXML加载失败异常
				showAlert("错误", "经理界面加载失败：" + e.getMessage(), Alert.AlertType.ERROR);
				e.printStackTrace();
			}
		} else {
			showAlert("错误", "用户名/密码错误，或无经理权限！", Alert.AlertType.ERROR);
		}
	
	}
	// 封装弹窗提示方法（复用）
	private void showAlert(String title, String content, Alert.AlertType type) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
