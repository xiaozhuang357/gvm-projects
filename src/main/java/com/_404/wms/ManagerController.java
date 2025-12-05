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

public class ManagerController implements Initializable {

	private User currentUser;
	private SocketClient socketClient;

	// 缓存所有订单数据
	private List<PurchaseOrder> allOrders = new ArrayList<>();
	private ObservableList<ApprovalTask> allTasks = FXCollections.observableArrayList();
	private FilteredList<ApprovalTask> filteredTasks = new FilteredList<>(allTasks, task -> true);

	@FXML
	private Button approveButton;

	@FXML
	private Button logoutButton;

	@FXML
	private Button rejectButton;

	@FXML
	private Button approvalMenuButton;

	@FXML
	private Text userNameText;

	// 审批中心相关控件
	@FXML
	private BorderPane approvalCenterPane;

	@FXML
	private ComboBox<String> approvalStatusComboBox;

	@FXML
	private ComboBox<String> approvalTypeComboBox;

	@FXML
	private TableView<ApprovalTask> approvalTableView;

	@FXML
	private TableColumn<ApprovalTask, String> approvalIdColumn;

	@FXML
	private TableColumn<ApprovalTask, String> approvalTypeColumn;

	@FXML
	private TableColumn<ApprovalTask, String> applicantColumn;

	@FXML
	private TableColumn<ApprovalTask, String> approvalTitleColumn;

	@FXML
	private TableColumn<ApprovalTask, Double> approvalAmountColumn;

	@FXML
	private TableColumn<ApprovalTask, String> applyDateColumn;

	@FXML
	private TableColumn<ApprovalTask, String> priorityColumn;

	@FXML
	private TableColumn<ApprovalTask, String> approvalStatusColumn;

	@FXML
	private Text pendingCountText;

	/**
	 * 设置当前用户
	 */
	public void setCurrentUser(User user) {
		this.currentUser = user;
		this.socketClient = LoginController.getSocketClient();

		// 更新界面显示用户信息
		if (userNameText != null) {
			userNameText.setText("欢迎，" + user.getRealName() + " (" + user.getRole().getDisplayName() + ")");
		}

		// 加载服务器数据
		loadDataFromServer();

		// 启动后台轮询，定期刷新（避免需要重新登录才能看到更新）
		new Thread(() -> {
			while (LoginController.getSocketClient().isConnected()) {
				try {
					Thread.sleep(3000);
					loadDataFromServer();
				} catch (InterruptedException e) {
					break;
				}
			}
		}, "Manager-Refresh-Thread").start();
	}

	/**
	 * 从服务器加载数据
	 */
	private void loadDataFromServer() {
		new Thread(() -> {
			// 加载采购订单列表
			Message request = new Message(Message.MessageType.PURCHASE_ORDER_LIST);
			Message response = socketClient.sendAndReceive(request);

			if (response.isSuccess()) {
				@SuppressWarnings("unchecked")
				List<PurchaseOrder> orders = (List<PurchaseOrder>) response.getData();

				Platform.runLater(() -> {
					updateApprovalTableWithOrders(orders);
				});
			}
		}).start();
	}

	/**
	 * 用服务器数据更新审批表格
	 */
	private void updateApprovalTableWithOrders(List<PurchaseOrder> orders) {
		this.allOrders = orders;
		allTasks.clear();

		for (PurchaseOrder order : orders) {
			// 只显示待审批的订单
			if (order.getStatus() == PurchaseOrder.OrderStatus.PENDING_DEPT_APPROVAL ||
					order.getStatus() == PurchaseOrder.OrderStatus.PENDING_GENERAL_APPROVAL) {

				ApprovalTask task = new ApprovalTask(
						order.getOrderId(),
						"采购申请",
						order.getPurchaserName(),
						"采购订单: " + order.getSupplier(),
						order.getTotalAmount(),
						order.getCreateTime().toLocalDate().toString(),
						order.getTotalAmount() >= 50000 ? "紧急" : "普通",
						order.getStatus().getDisplayName(),
						order);
				allTasks.add(task);
			}
		}

		if (approvalTableView != null) {
			approvalTableView.setItems(filteredTasks);
			updatePendingCount();
			applyApprovalFilter();
		}
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		// 初始化审批状态和类型下拉框
		approvalStatusComboBox.setItems(FXCollections.observableArrayList(
				"全部状态", "待审批", "已通过", "已退回"));
		approvalStatusComboBox.setValue("待审批");

		approvalTypeComboBox.setItems(FXCollections.observableArrayList(
				"全部类型", "采购申请", "出库申请", "入库申请", "调拨申请"));
		approvalTypeComboBox.setValue("全部类型");

		// 初始化审批任务数据
		initializeApprovalTableData();

		// 添加筛选监听器
		approvalStatusComboBox.setOnAction(e -> applyApprovalFilter());
		approvalTypeComboBox.setOnAction(e -> applyApprovalFilter());

		// 添加表格行双击事件，查看详情
		approvalTableView.setOnMouseClicked(event -> {
			if (event.getClickCount() == 2) {
				ApprovalTask selected = approvalTableView.getSelectionModel().getSelectedItem();
				if (selected != null) {
					showOrderDetails(selected);
				}
			}
		});

		// 默认显示审批中心界面
		showApprovalCenter(null);
	}

	/**
	 * 初始化审批任务表格数据
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
	 * 应用审批筛选
	 */
	private void applyApprovalFilter() {
		String statusFilter = approvalStatusComboBox.getValue();
		String typeFilter = approvalTypeComboBox.getValue();

		filteredTasks.setPredicate(task -> {
			boolean matchStatus = "全部状态".equals(statusFilter) ||
					task.getStatus().contains(statusFilter.replace("已", "").replace("待", "待"));
			boolean matchType = "全部类型".equals(typeFilter) || task.getType().equals(typeFilter);
			return matchStatus && matchType;
		});
		updatePendingCount();
	}

	@FXML
	void handleRefreshApprovals(ActionEvent event) {
		loadDataFromServer();
	}

	/**
	 * 显示订单详情
	 */
	private void showOrderDetails(ApprovalTask task) {
		if (task.getOrder() == null) {
			showAlert("提示", "未找到订单详情", Alert.AlertType.WARNING);
			return;
		}

		PurchaseOrder order = task.getOrder();
		StringBuilder details = new StringBuilder();
		details.append("订单号: ").append(order.getOrderId()).append("\n");
		details.append("采购员: ").append(order.getPurchaserName()).append("\n");
		details.append("供应商: ").append(order.getSupplier()).append("\n");
		details.append("总金额: ¥").append(String.format("%.2f", order.getTotalAmount())).append("\n");
		details.append("状态: ").append(order.getStatus().getDisplayName()).append("\n");
		details.append("创建时间: ").append(order.getCreateTime()).append("\n\n");
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
			details.append("  数量: ").append(item.getQuantity());
			details.append("  小计: ¥").append(String.format("%.2f", item.getSubtotal()));
			details.append("\n");
		}

		if (order.getRemark() != null && !order.getRemark().isEmpty()) {
			details.append("\n备注: ").append(order.getRemark());
		}

		// 显示详情对话框
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("订单详情");
		alert.setHeaderText("采购订单明细信息");

		TextArea textArea = new TextArea(details.toString());
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setPrefRowCount(20);

		alert.getDialogPane().setContent(textArea);
		alert.getDialogPane().setPrefWidth(600);
		alert.showAndWait();
	}

	/**
	 * 退出登录
	 */
	@FXML
	void logout(ActionEvent event) {
		Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
		confirm.setTitle("确认退出");
		confirm.setHeaderText("确定要退出系统吗?");

		confirm.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				if (logoutButton != null) {
					logoutButton.setDisable(true);
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
						Platform.runLater(() -> {
							if (logoutButton != null && logoutButton.getScene() != null) {
								Stage stage = (Stage) logoutButton.getScene().getWindow();
								if (stage != null) {
									stage.close();
								}
							}

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
	 * 显示审批中心界面
	 */
	@FXML
	void showApprovalCenter(ActionEvent event) {
		approvalCenterPane.setVisible(true);

		// 更新菜单按钮样式
		approvalMenuButton.setStyle("-fx-background-color: #3498DB; -fx-border-width: 0;");
	}

	@FXML
	void approve(ActionEvent event) {
		ApprovalTask selectedTask = approvalTableView.getSelectionModel().getSelectedItem();
		if (selectedTask == null) {
			showAlert("提示", "请先选择要审批的任务", Alert.AlertType.WARNING);
			return;
		}

		// 创建审批数据
		Map<String, Object> approvalData = new HashMap<>();
		approvalData.put("orderId", selectedTask.getId());
		approvalData.put("comment", "审批通过");

		// 发送到服务器
		approveButton.setDisable(true);
		new Thread(() -> {
			Message request = new Message(Message.MessageType.PURCHASE_ORDER_APPROVE, approvalData);
			Message response = socketClient.sendAndReceive(request);

			Platform.runLater(() -> {
				approveButton.setDisable(false);

				if (response.isSuccess()) {
					showAlert("审批成功", "已通过审批：" + selectedTask.getTitle(), Alert.AlertType.INFORMATION);

					// 从待审批列表中移除
					allTasks.remove(selectedTask);
					approvalTableView.refresh();

					// 更新待处理数量
					updatePendingCount();

					// 重新加载数据以同步服务器状态
					loadDataFromServer();
				} else {
					showAlert("审批失败", response.getMessage(), Alert.AlertType.ERROR);
				}
			});
		}).start();
	}

	@FXML
	void reject(ActionEvent event) {
		ApprovalTask selectedTask = approvalTableView.getSelectionModel().getSelectedItem();
		if (selectedTask == null) {
			showAlert("提示", "请先选择要退回的任务", Alert.AlertType.WARNING);
			return;
		}

		// 创建退回数据
		Map<String, Object> rejectData = new HashMap<>();
		rejectData.put("orderId", selectedTask.getId());
		rejectData.put("comment", "需要修改");

		// 发送到服务器
		rejectButton.setDisable(true);
		new Thread(() -> {
			Message request = new Message(Message.MessageType.PURCHASE_ORDER_REJECT, rejectData);
			Message response = socketClient.sendAndReceive(request);

			Platform.runLater(() -> {
				rejectButton.setDisable(false);

				if (response.isSuccess()) {
					showAlert("退回成功", "已退回：" + selectedTask.getTitle(), Alert.AlertType.INFORMATION);

					// 从待审批列表中移除
					allTasks.remove(selectedTask);
					approvalTableView.refresh();

					// 更新待处理数量
					updatePendingCount();

					// 重新加载数据以同步服务器状态
					loadDataFromServer();
				} else {
					showAlert("退回失败", response.getMessage(), Alert.AlertType.ERROR);
				}
			});
		}).start();
	}

	/**
	 * 更新待处理审批数量
	 */
	private void updatePendingCount() {
		long count = approvalTableView.getItems().stream()
				.filter(task -> "待审批".equals(task.getStatus()))
				.count();
		pendingCountText.setText(String.valueOf(count));
	}

	/**
	 * 显示提示对话框
	 */
	private void showAlert(String title, String content, Alert.AlertType type) {
		Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(content);
		alert.showAndWait();
	}

	// 内部类：审批任务数据模型
	public static class ApprovalTask {
		private String id;
		private String type;
		private String applicant;
		private String title;
		private double amount;
		private String applyDate;
		private String priority;
		private String status;
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
