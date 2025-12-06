package com._404.wms;

import com._404.wms.model.PurchaseOrder;
import com._404.wms.model.User;
import com._404.wms.network.Message;
import com._404.wms.network.SocketClient;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * 管理员主界面控制器 (ManagerController)
 * <p>
 * 负责处理部门经理(Department Manager)和总经理(General Manager)的业务逻辑。
 * 主要功能包括：
 * 1. 展示待审批的采购订单列表。
 * 2. 根据角色权限过滤订单（部门经理审批 < 5w，总经理审批 >= 5w）。
 * 3. 执行审批通过或驳回操作。
 * 4. 自动轮询服务器获取最新数据。
 */
public class ManagerController implements Initializable {

	// 当前登录的用户信息
	private User currentUser;
	// 网络通信客户端实例
	private SocketClient socketClient;

	// --- 数据模型 ---
	// 缓存从服务器获取的原始订单列表
	private List<PurchaseOrder> allOrders = new ArrayList<>();
	// 包装后的可观察列表，用于 TableView 显示
	private ObservableList<ApprovalTask> allTasks = FXCollections.observableArrayList();
	// 过滤后的列表，支持前端根据状态筛选（如：只看"待审批"）
	private FilteredList<ApprovalTask> filteredTasks = new FilteredList<>(allTasks, task -> true);

	// --- FXML UI 控件注入 ---
	@FXML
	private Button approveButton; // 批准按钮

	@FXML
	private Button logoutButton; // 退出按钮

	@FXML
	private Button rejectButton; // 驳回按钮

	@FXML
	private Button approvalMenuButton; // 左侧菜单按钮

	@FXML
	private Text userNameText; // 显示用户名和角色的文本

	// 审批中心主面板
	@FXML
	private BorderPane approvalCenterPane;

	// 状态筛选下拉框
	@FXML
	private ComboBox<String> approvalStatusComboBox;

	// --- 表格及列定义 ---
	@FXML
	private TableView<ApprovalTask> approvalTableView;

	@FXML
	private TableColumn<ApprovalTask, String> approvalIdColumn; // 订单ID列

	@FXML
	private TableColumn<ApprovalTask, String> approvalTypeColumn; // 类型列

	@FXML
	private TableColumn<ApprovalTask, String> applicantColumn; // 申请人列

	@FXML
	private TableColumn<ApprovalTask, String> approvalTitleColumn; // 标题列

	@FXML
	private TableColumn<ApprovalTask, Double> approvalAmountColumn; // 金额列

	@FXML
	private TableColumn<ApprovalTask, String> applyDateColumn; // 日期列

	@FXML
	private TableColumn<ApprovalTask, String> priorityColumn; // 优先级列

	@FXML
	private TableColumn<ApprovalTask, String> approvalStatusColumn; // 状态列

	@FXML
	private Text pendingCountText; // 待处理数量显示

	/**
	 * 初始化当前用户信息并启动数据加载。
	 * 通常在登录成功并跳转到此界面后由 LoginController 调用。
	 *
	 * @param user 登录成功的用户对象
	 */
	public void setCurrentUser(User user) {
		this.currentUser = user;
		// 获取全局唯一的 Socket 客户端实例
		this.socketClient = LoginController.getSocketClient();

		// 1. 更新界面顶部的欢迎语
		if (userNameText != null) {
			userNameText.setText("欢迎，" + user.getRealName() + " (" + user.getRole().getDisplayName() + ")");
		}

		// 2. 首次加载服务器数据
		loadDataFromServer();

		// 3. 启动后台轮询线程
		// 每隔3秒自动刷新一次数据，确保管理员能实时看到新提交的申请，
		// 而无需手动刷新或重新登录。
		new Thread(() -> {
			while (LoginController.getSocketClient().isConnected()) {
				try {
					Thread.sleep(3000);
					// 在后台线程中请求数据
					loadDataFromServer();
				} catch (InterruptedException e) {
					break;
				}
			}
		}, "Manager-Refresh-Thread").start();
	}

	/**
	 * 异步从服务器加载采购订单列表。
	 * 发送请求后，在回调中通过 Platform.runLater 更新 UI。
	 */
	private void loadDataFromServer() {
		new Thread(() -> {
			// 构建请求消息：获取采购订单列表
			Message request = new Message(Message.MessageType.PURCHASE_ORDER_LIST);
			Message response = socketClient.sendAndReceive(request);

			if (response.isSuccess()) {
				@SuppressWarnings("unchecked")
				List<PurchaseOrder> orders = (List<PurchaseOrder>) response.getData();

				// 切换回 JavaFX 主线程更新 UI 组件
				Platform.runLater(() -> {
					updateApprovalTableWithOrders(orders);
				});
			}
		}).start();
	}

	/**
	 * 核心业务逻辑：根据订单列表更新审批表格。
	 * 此方法包含权限过滤逻辑（Dept Manager vs General Manager）。
	 *
	 * @param orders 从服务器获取的最新的订单列表
	 */
	private void updateApprovalTableWithOrders(List<PurchaseOrder> orders) {
		this.allOrders = orders;
		allTasks.clear(); // 清空当前显示列表

		for (PurchaseOrder order : orders) {
			// 1. 初步筛选：只显示与审批流程相关的状态
			boolean isRelevant = order.getStatus() == PurchaseOrder.OrderStatus.PENDING_DEPT_APPROVAL ||
					order.getStatus() == PurchaseOrder.OrderStatus.PENDING_GENERAL_APPROVAL ||
					order.getStatus() == PurchaseOrder.OrderStatus.APPROVED ||
					order.getStatus() == PurchaseOrder.OrderStatus.REJECTED;

			if (!isRelevant) {
				continue;
			}

			// 2. 权限筛选：根据用户角色和订单金额进行过滤
			if (currentUser != null) {
				if (currentUser.getRole() == User.UserRole.DEPARTMENT_MANAGER) {
					// 部门经理权限：只能查看金额 < 50,000 的订单
					// 如果金额 >= 50,000，该订单应直接流转给总经理，部门经理不可见
					if (order.getTotalAmount() >= 50000) {
						continue;
					}
				} else if (currentUser.getRole() == User.UserRole.GENERAL_MANAGER) {
					// 总经理权限：只能查看金额 >= 50,000 的订单
					// 小额订单由部门经理全权负责
					if (order.getTotalAmount() < 50000) {
						continue;
					}
				}
			}

			// 3. 数据转换：将后端实体 PurchaseOrder 转换为前端视图模型 ApprovalTask
			ApprovalTask task = new ApprovalTask(
					order.getOrderId(),
					"采购申请",
					order.getPurchaserName(),
					"采购订单: " + order.getSupplier(),
					order.getTotalAmount(),
					order.getCreateTime().toLocalDate().toString(),
					order.getTotalAmount() >= 50000 ? "紧急" : "普通", // 简单的优先级逻辑示例
					order.getStatus().getDisplayName(),
					order);
			allTasks.add(task);
		}

		// 4. 刷新表格和计数器
		if (approvalTableView != null) {
			approvalTableView.setItems(filteredTasks); // 重新绑定 FilteredList
			updatePendingCount(); // 更新左下角待处理数字
			applyApprovalFilter(); // 应用当前的下拉框筛选（如：是否只看待审批）
		}
	}

	/**
	 * JavaFX 初始化方法，在 FXML 加载完成后自动调用。
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// 1. 初始化下拉筛选框
		approvalStatusComboBox.setItems(FXCollections.observableArrayList(
				"全部状态", "待审批", "已通过", "已退回"));
		approvalStatusComboBox.setValue("待审批"); // 默认只看待办事项

		// 2. 配置表格列与数据模型的绑定
		initializeApprovalTableData();

		// 3. 绑定事件监听器
		approvalStatusComboBox.setOnAction(e -> applyApprovalFilter());

		// 4. 添加表格双击事件：查看详情
		approvalTableView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				ApprovalTask selected = approvalTableView.getSelectionModel().getSelectedItem();
				if (selected != null) {
					showOrderDetails(selected);
				}
			}
		});

		// 5. 默认显示审批中心面板
		showApprovalCenter(null);
	}

	/**
	 * 配置 TableView 的列工厂 (CellValueFactory)。
	 * 将 ApprovalTask 对象的属性映射到表格列。
	 */
	private void initializeApprovalTableData() {
		approvalIdColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getId()));
		approvalTypeColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getType()));
		applicantColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getApplicant()));
		approvalTitleColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getTitle()));
		// 金额列使用 DoubleProperty
		approvalAmountColumn.setCellValueFactory(
				cellData -> new SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
		applyDateColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getApplyDate()));
		priorityColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getPriority()));
		approvalStatusColumn.setCellValueFactory(
				cellData -> new SimpleStringProperty(cellData.getValue().getStatus()));

		approvalTableView.setItems(filteredTasks);
	}

	/**
	 * 根据下拉框的选择，过滤表格显示的内容。
	 * 这里处理了 UI 显示状态字符串到逻辑判断的映射。
	 */
	private void applyApprovalFilter() {
		String statusFilter = approvalStatusComboBox.getValue();

		filteredTasks.setPredicate(task -> {
			String status = task.getStatus();
			// 根据状态字符串判断类别
			boolean isPending = status.startsWith("待"); // 如 "待部门经理审批"
			boolean isApproved = status.contains("已批准") || status.contains("已通过");
			boolean isRejected = status.contains("已退回");

			// switch 表达式返回布尔值
			return switch (statusFilter) {
				case "全部状态" -> true;
				case "待审批" -> isPending;
				case "已通过" -> isApproved;
				case "已退回" -> isRejected;
				default -> true;
			};
		});

		// 筛选后重新计算待处理数量
		updatePendingCount();
	}

	/**
	 * 手动刷新按钮事件处理
	 */
	@FXML
	void handleRefreshApprovals(ActionEvent event) {
		loadDataFromServer();
	}

	/**
	 * 弹出对话框显示订单的详细商品明细。
	 *
	 * @param task 选中的审批任务包装对象
	 */
	private void showOrderDetails(ApprovalTask task) {
		if (task.getOrder() == null) {
			showAlert("提示", "未找到订单详情", Alert.AlertType.WARNING);
			return;
		}

		PurchaseOrder order = task.getOrder();
		StringBuilder details = new StringBuilder();

		// 构建头部信息
		details.append("订单号: ").append(order.getOrderId()).append("\n");
		details.append("采购员: ").append(order.getPurchaserName()).append("\n");
		details.append("供应商: ").append(order.getSupplier()).append("\n");
		details.append("总金额: ¥").append(String.format("%.2f", order.getTotalAmount())).append("\n");
		details.append("状态: ").append(order.getStatus().getDisplayName()).append("\n");
		details.append("创建时间: ").append(order.getCreateTime()).append("\n\n");

		// 构建商品明细列表
		details.append("商品明细:\n");
		details.append("----------------------------------------\n");

		int index = 1;
		for (PurchaseOrder.OrderItem item : order.getItems()) {
			details.append(index++).append(". ");
			details.append(item.getProductName());
			if (item.getSpecification() != null && !item.getSpecification().isEmpty()) {
				details.append(" (").append(item.getSpecification()).append(")");
			}
			details.append("\n");
			details.append("   单价: ¥").append(String.format("%.2f", item.getUnitPrice()));
			details.append("   数量: ").append(item.getQuantity());
			details.append("   小计: ¥").append(String.format("%.2f", item.getSubtotal()));
			details.append("\n");
		}

		if (order.getRemark() != null && !order.getRemark().isEmpty()) {
			details.append("\n备注: ").append(order.getRemark());
		}

		// 创建并显示 Alert
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("订单详情");
		alert.setHeaderText("采购订单明细信息");

		// 使用 TextArea 以支持长文本滚动和换行
		TextArea textArea = new TextArea(details.toString());
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setPrefRowCount(20);

		alert.getDialogPane().setContent(textArea);
		alert.getDialogPane().setPrefWidth(600);
		alert.showAndWait();
	}

	/**
	 * 退出登录逻辑：
	 * 1. 发送 Logout 请求。
	 * 2. 断开 Socket 连接。
	 * 3. 关闭当前窗口并重新打开登录窗口。
	 */
	@FXML
	void logout(ActionEvent event) {
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("确认退出");
		confirm.setHeaderText("确定要退出系统吗?");

		confirm.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				if (logoutButton != null) {
					logoutButton.setDisable(true); // 防止重复点击
				}

				new Thread(() -> {
					try {
						if (socketClient != null) {
							socketClient.logout();
							socketClient.disconnect();
						}
					} catch (Exception e) {
						System.err.println("Logout error: " + e.getMessage());
					} finally {
						// 必须在 JavaFX 线程中操作窗口
						Platform.runLater(() -> {
							// 关闭当前窗口
							if (logoutButton != null && logoutButton.getScene() != null) {
								Stage stage = (Stage) logoutButton.getScene().getWindow();
								if (stage != null) {
									stage.close();
								}
							}

							// 打开新登录窗口
							try {
								new LoginApplication().start(new Stage());
							} catch (Exception e) {
								e.printStackTrace();
							}
						});
					}
				}).start();
			}
		});
	}

	/**
	 * 切换到审批中心视图（主要用于侧边栏导航切换）
	 */
	@FXML
	void showApprovalCenter(ActionEvent event) {
		approvalCenterPane.setVisible(true);
		// 高亮选中当前菜单
		approvalMenuButton.setStyle("-fx-background-color: #3498DB; -fx-border-width: 0;");
	}

	/**
	 * 处理【通过】按钮点击事件
	 */
	@FXML
	void approve(ActionEvent event) {
		ApprovalTask selectedTask = approvalTableView.getSelectionModel().getSelectedItem();
		if (selectedTask == null) {
			showAlert("提示", "请先选择要审批的任务", Alert.AlertType.WARNING);
			return;
		}

		// 构造发送给服务器的审批参数
		Map<String, Object> approvalData = new HashMap<>();
		approvalData.put("orderId", selectedTask.getId());
		approvalData.put("comment", "审批通过");

		approveButton.setDisable(true); // 禁用按钮防止重复提交

		new Thread(() -> {
			Message request = new Message(Message.MessageType.PURCHASE_ORDER_APPROVE, approvalData);
			Message response = socketClient.sendAndReceive(request);

			Platform.runLater(() -> {
				approveButton.setDisable(false);

				if (response.isSuccess()) {
					showAlert("审批成功", "已通过审批：" + selectedTask.getTitle(), Alert.AlertType.INFORMATION);

					// 乐观更新：先从 UI 移除，随后 loadDataFromServer 会保证数据一致性
					allTasks.remove(selectedTask);
					approvalTableView.refresh();
					updatePendingCount();

					// 重新从服务器拉取最新状态
					loadDataFromServer();
				} else {
					showAlert("审批失败", response.getMessage(), Alert.AlertType.ERROR);
				}
			});
		}).start();
	}

	/**
	 * 处理【退回/驳回】按钮点击事件
	 */
	@FXML
	void reject(ActionEvent event) {
		ApprovalTask selectedTask = approvalTableView.getSelectionModel().getSelectedItem();
		if (selectedTask == null) {
			showAlert("提示", "请先选择要退回的任务", Alert.AlertType.WARNING);
			return;
		}

		Map<String, Object> rejectData = new HashMap<>();
		rejectData.put("orderId", selectedTask.getId());
		rejectData.put("comment", "需要修改"); // 默认驳回理由，实际项目中可扩展为弹窗输入

		rejectButton.setDisable(true);

		new Thread(() -> {
			Message request = new Message(Message.MessageType.PURCHASE_ORDER_REJECT, rejectData);
			Message response = socketClient.sendAndReceive(request);

			Platform.runLater(() -> {
				rejectButton.setDisable(false);

				if (response.isSuccess()) {
					showAlert("退回成功", "已退回：" + selectedTask.getTitle(), Alert.AlertType.INFORMATION);

					allTasks.remove(selectedTask);
					approvalTableView.refresh();
					updatePendingCount();

					loadDataFromServer();
				} else {
					showAlert("退回失败", response.getMessage(), Alert.AlertType.ERROR);
				}
			});
		}).start();
	}

	/**
	 * 计算并更新待处理（状态为"待审批"）的任务数量
	 */
	private void updatePendingCount() {
		long count = approvalTableView.getItems().stream()
				.filter(task -> "待审批".equals(task.getStatus()))
				.count();
		pendingCountText.setText(String.valueOf(count));
	}

	/**
	 * 辅助方法：显示标准弹窗
	 */
	private void showAlert(String title, String content, Alert.AlertType type) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	/**
	 * 内部类：审批任务视图模型 (ViewModel)
	 * <p>
	 * 该类用于包装 PurchaseOrder 实体，适配 JavaFX TableView 的数据绑定需求。
	 * 将复杂的对象属性打平为简单的 String/Double 属性供表格显示。
	 */
	public static class ApprovalTask {
		private String id;
		private String type;
		private String applicant;
		private String title;
		private double amount;
		private String applyDate;
		private String priority;
		private String status;
		// 持有原始订单对象的引用，以便查看详情或操作时使用
		private PurchaseOrder order;

		public ApprovalTask(String id, String type, String applicant, String title,
				double amount, String applyDate, String priority, String status) {
			this.id = id;
			this.type = type;
			this.applicant = applicant;
			this.title = title;
			this.amount = amount;
			this.applyDate = applyDate;
			this.priority = priority;
			this.status = status;
		}

		public ApprovalTask(String id, String type, String applicant, String title,
				double amount, String applyDate, String priority, String status, PurchaseOrder order) {
			this(id, type, applicant, title, amount, applyDate, priority, status);
			this.order = order;
		}

		// --- Getters and Setters ---

		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}

		public String getApplicant() {
			return applicant;
		}

		public String getTitle() {
			return title;
		}

		public double getAmount() {
			return amount;
		}

		public String getApplyDate() {
			return applyDate;
		}

		public String getPriority() {
			return priority;
		}

		public String getStatus() {
			return status;
		}

		public void setStatus(String status) {
			this.status = status;
		}

		public PurchaseOrder getOrder() {
			return order;
		}
	}
}