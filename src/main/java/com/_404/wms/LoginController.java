package com._404.wms;

import com._404.wms.model.User;
import com._404.wms.network.Message;
import com._404.wms.network.SocketClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

	private static SocketClient socketClient;

	@FXML
	private Button loginButton;

	@FXML
	private PasswordField passwordField;

	@FXML
	private TextField usernameField;

	/**
	 * 获取全局Socket客户端实例
	 */
	public static SocketClient getSocketClient() {
		if (socketClient == null) {
			socketClient = new SocketClient();
		}
		return socketClient;
	}

	@FXML
	void login(ActionEvent event) {
		// 1. 获取输入的用户名和密码
		String username = usernameField.getText().trim();
		String password = passwordField.getText().trim();

		// 2. 验证输入
		if (username.isEmpty() || password.isEmpty()) {
			showAlert("错误", "用户名/密码不能为空！", Alert.AlertType.ERROR);
			return;
		}

		// 3. 连接服务器
		SocketClient client = getSocketClient();
		if (!client.isConnected()) {
			if (!client.connect()) {
				showAlert("错误", "无法连接到服务器！请确保服务器已启动。", Alert.AlertType.ERROR);
				return;
			}
		}

		// 4. 发送登录请求
		loginButton.setDisable(true);
		loginButton.setText("登录中...");

		new Thread(() -> {
			Message response = client.login(username, password);

			Platform.runLater(() -> {
				loginButton.setDisable(false);
				loginButton.setText("登录");

				if (response.isSuccess()) {
					User user = (User) response.getData();
					try {
						// 关闭登录窗口
						Stage loginStage = (Stage) loginButton.getScene().getWindow();
						loginStage.close();

						// 根据用户角色打开不同界面
						openRoleBasedInterface(user);

					} catch (IOException e) {
						showAlert("错误", "界面加载失败：" + e.getMessage(), Alert.AlertType.ERROR);
						e.printStackTrace();
					}
				} else {
					showAlert("登录失败", response.getMessage(), Alert.AlertType.ERROR);
				}
			});
		}).start();
	}

	/**
	 * 根据用户角色打开对应界面
	 */
	private void openRoleBasedInterface(User user) throws IOException {
		String fxmlFile;
		String title;

		// 调试输出
		System.out.println("用户角色: " + user.getRole());
		System.out.println("用户名: " + user.getUsername());

		switch (user.getRole()) {
			case WAREHOUSE_ADMIN:
				fxmlFile = "warehouse_admin.fxml";
				title = "仓库管理员工作台";
				break;
			case PURCHASER:
				fxmlFile = "purchaser.fxml";
				title = "采购员工作台";
				break;
			case DEPARTMENT_MANAGER:
			case GENERAL_MANAGER:
				fxmlFile = "manager.fxml";
				title = user.getRole().getDisplayName() + "工作台";
				break;
			default:
				fxmlFile = "manager.fxml";
				title = "管理工作台";
		}

		System.out.println("将要加载的界面: " + fxmlFile);

		try {
			FXMLLoader fxmlLoader = new FXMLLoader(LoginController.class.getResource(fxmlFile));
			Scene scene = new Scene(fxmlLoader.load());

			// 获取控制器并设置用户信息
			Object controller = fxmlLoader.getController();
			if (controller instanceof ManagerController) {
				((ManagerController) controller).setCurrentUser(user);
			} else if (controller instanceof WarehouseAdminController) {
				((WarehouseAdminController) controller).setCurrentUser(user);
			} else if (controller instanceof PurchaserController) {
				((PurchaserController) controller).setCurrentUser(user);
			}

			Stage stage = new Stage();
			stage.setTitle(title);
			stage.setScene(scene);
			stage.setMaximized(true);
			stage.show();

			// 窗口关闭时断开连接
			stage.setOnCloseRequest(e -> {
				if (getSocketClient().isConnected()) {
					getSocketClient().logout();
					getSocketClient().disconnect();
				}
			});

		} catch (IOException e) {
			System.err.println("加载界面失败: " + e.getMessage());
			e.printStackTrace();
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
