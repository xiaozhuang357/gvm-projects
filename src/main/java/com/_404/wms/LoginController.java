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

/**
 * 登录控制器 - 处理用户登录逻辑和界面路由
 * <p>
 * 功能说明:
 * 1. 管理用户登录界面(login.fxml)的交互逻辑
 * 2. 验证用户输入的用户名和密码
 * 3. 与服务器通信进行身份认证
 * 4. 根据用户角色路由到不同的工作台界面
 * 5. 维护全局SocketClient单例,供所有Controller使用
 * <p>
 * 角色路由规则:
 * - WAREHOUSE_ADMIN → warehouse_admin.fxml (仓库管理员工作台)
 * - PURCHASER → purchaser.fxml (采购员工作台)
 * - DEPARTMENT_MANAGER → manager.fxml (部门经理工作台)
 * - GENERAL_MANAGER → manager.fxml (总经理工作台)
 * <p>
 * 技术要点:
 * 1. 使用Platform.runLater()确保UI操作在JavaFX主线程执行
 * 2. 登录请求在独立线程中执行,避免阻塞UI
 * 3. 通过FXMLLoader.getController()获取控制器实例并传递用户信息
 * 4. 窗口关闭时自动断开服务器连接(setOnCloseRequest)
 * <p>
 * 使用示例:
 * 
 * <pre>
 * // 在LoginApplication中加载
 * FXMLLoader loader = new FXMLLoader(getClass().getResource("login.fxml"));
 * Scene scene = new Scene(loader.load());
 * 
 * // 获取全局SocketClient
 * SocketClient client = LoginController.getSocketClient();
 * </pre>
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class LoginController {

	/** 全局SocketClient实例,所有Controller共享 */
	private static SocketClient socketClient;

	/** 登录按钮,绑定到login.fxml */
	@FXML
	private Button loginButton;

	/** 密码输入框,绑定到login.fxml */
	@FXML
	private PasswordField passwordField;

	/** 用户名输入框,绑定到login.fxml */
	@FXML
	private TextField usernameField;

	/**
	 * 获取全局Socket客户端实例(单例模式)
	 * <p>
	 * 所有Controller通过此方法获取同一个SocketClient实例
	 * 确保与服务器只建立一个连接
	 * 
	 * @return SocketClient实例
	 */
	public static SocketClient getSocketClient() {
		if (socketClient == null) {
			socketClient = new SocketClient();
		}
		return socketClient;
	}

	/**
	 * 处理登录按钮点击事件
	 * <p>
	 * 执行流程:
	 * 1. 验证用户名和密码是否为空
	 * 2. 检查与服务器的连接,未连接则尝试连接
	 * 3. 在独立线程中发送登录请求(避免UI冻结)
	 * 4. 登录成功后根据角色打开对应工作台
	 * 5. 登录失败显示错误提示
	 * <p>
	 * 注意事项:
	 * - 登录过程中禁用登录按钮,防止重复点击
	 * - 所有UI更新必须在Platform.runLater()中执行
	 * 
	 * @param event 按钮点击事件
	 */
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
	 * 根据用户角色打开对应的工作台界面
	 * <p>
	 * 角色映射:
	 * - WAREHOUSE_ADMIN → warehouse_admin.fxml (仓库管理员工作台)
	 * - PURCHASER → purchaser.fxml (采购员工作台)
	 * - DEPARTMENT_MANAGER → manager.fxml (部门经理工作台)
	 * - GENERAL_MANAGER → manager.fxml (总经理工作台)
	 * <p>
	 * 执行步骤:
	 * 1. 根据用户角色确定FXML文件和窗口标题
	 * 2. 使用FXMLLoader加载对应的FXML文件
	 * 3. 获取Controller实例并调用setCurrentUser()传递用户信息
	 * 4. 创建新Stage并显示(最大化)
	 * 5. 设置窗口关闭监听器,关闭时自动断开服务器连接
	 * <p>
	 * 注意事项:
	 * - 登录窗口(Stage)在新窗口打开前已关闭
	 * - 所有工作台Controller都应实现setCurrentUser()方法
	 * 
	 * @param user 登录成功的用户对象
	 * @throws IOException FXML文件加载失败时抛出
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

	/**
	 * 显示弹窗提示信息
	 * <p>
	 * 封装Alert弹窗创建逻辑,统一提示信息样式
	 * <p>
	 * 使用场景:
	 * - 输入验证失败提示
	 * - 连接服务器失败提示
	 * - 登录失败提示
	 * - 界面加载失败提示
	 * 
	 * @param title   弹窗标题
	 * @param content 弹窗内容文本
	 * @param type    弹窗类型(ERROR、INFORMATION、WARNING等)
	 */
	private void showAlert(String title, String content, Alert.AlertType type) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}
}
