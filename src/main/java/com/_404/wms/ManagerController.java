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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class ManagerController implements Initializable {

	private User currentUser;
	private SocketClient socketClient;

	// 缓存所有订单数据
	private List<PurchaseOrder> allOrders = new java.util.ArrayList<>();
	private ObservableList<ApprovalTask> allTasks = FXCollections.observableArrayList();
	private FilteredList<ApprovalTask> filteredTasks = new FilteredList<>(allTasks, task -> true);

	@FXML
	private Button approveButton;

	@FXML
	private Button logoutButton;

	@FXML
	private Button rejectButton;

	@FXML
	private Button reportMenuButton;

	@FXML
	private Button approvalMenuButton;

	@FXML
	private Text userNameText;

	// 报表管理相关控件
	@FXML
	private BorderPane reportManagementPane;

	@FXML
	private DatePicker startDatePicker;

	@FXML
	private DatePicker endDatePicker;

	@FXML
	private ComboBox<String> categoryComboBox;

	@FXML
	private ComboBox<String> chartTypeComboBox;

	@FXML
	private Text purchaseCostText;

	@FXML
	private Text inventoryValueText;

	@FXML
	private Text pendingOrdersText;

	@FXML
	private Text outboundQuantityText;

	@FXML
	private BarChart<String, Number> barChart;

	@FXML
	private LineChart<String, Number> lineChart;

	@FXML
	private PieChart pieChart;

	@FXML
	private TableView<ReportData> reportTableView;

	@FXML
	private TableColumn<ReportData, String> dateColumn;

	@FXML
	private TableColumn<ReportData, String> categoryColumn;

	@FXML
	private TableColumn<ReportData, String> productNameColumn;

	@FXML
	private TableColumn<ReportData, Integer> quantityColumn;

	@FXML
	private TableColumn<ReportData, Double> unitPriceColumn;

	@FXML
	private TableColumn<ReportData, Double> totalAmountColumn;

	@FXML
	private TableColumn<ReportData, String> statusColumn;

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
					Thread.sleep(5000);
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
		// 初始化图表类型下拉框
		chartTypeComboBox.setItems(FXCollections.observableArrayList("柱状图", "折线图", "饼图"));
		chartTypeComboBox.setValue("柱状图");

		// 初始化商品类别下拉框
		categoryComboBox.setItems(FXCollections.observableArrayList(
				"全部类别", "电子产品", "食品饮料", "日用品", "办公用品", "服装鞋帽"));
		categoryComboBox.setValue("全部类别");

		// 初始化审批状态和类型下拉框
		approvalStatusComboBox.setItems(FXCollections.observableArrayList(
				"全部状态", "待审批", "已通过", "已退回"));
		approvalStatusComboBox.setValue("待审批");

		approvalTypeComboBox.setItems(FXCollections.observableArrayList(
				"全部类型", "采购申请", "出库申请", "入库申请", "调拨申请"));
		approvalTypeComboBox.setValue("全部类型");

		// 设置默认日期范围（最近30天）
		endDatePicker.setValue(LocalDate.now());
		startDatePicker.setValue(LocalDate.now().minusDays(30));

		// 初始化图表数据
		initializeChartData();

		// 初始化报表数据
		initializeReportTableData();

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

		// 默认显示报表管理界面
		showReportManagement(null);
	}

	/**
	 * 初始化图表数据
	 */
	private void initializeChartData() {
		// 柱状图数据
		XYChart.Series<String, Number> series1 = new XYChart.Series<>();
		series1.setName("采购成本");
		series1.getData().add(new XYChart.Data<>("1月", 95000));
		series1.getData().add(new XYChart.Data<>("2月", 105000));
		series1.getData().add(new XYChart.Data<>("3月", 118500));
		series1.getData().add(new XYChart.Data<>("4月", 128500));
		barChart.getData().add(series1);

		// 折线图数据
		XYChart.Series<String, Number> series2 = new XYChart.Series<>();
		series2.setName("库存价值");
		series2.getData().add(new XYChart.Data<>("第1周", 420000));
		series2.getData().add(new XYChart.Data<>("第2周", 435000));
		series2.getData().add(new XYChart.Data<>("第3周", 448000));
		series2.getData().add(new XYChart.Data<>("第4周", 456800));
		lineChart.getData().add(series2);

		// 饼图数据
		ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
				new PieChart.Data("电子产品", 35),
				new PieChart.Data("食品饮料", 25),
				new PieChart.Data("日用品", 20),
				new PieChart.Data("办公用品", 12),
				new PieChart.Data("服装鞋帽", 8));
		pieChart.setData(pieChartData);
	}

	/**
	 * 初始化报表表格数据
	 */
	private void initializeReportTableData() {
		dateColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDate()));
		categoryColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCategory()));
		productNameColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getProductName()));
		quantityColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getQuantity())
						.asObject());
		unitPriceColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getUnitPrice())
						.asObject());
		totalAmountColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getTotalAmount())
						.asObject());
		statusColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));

		// 添加示例数据
		ObservableList<ReportData> data = FXCollections.observableArrayList(
				new ReportData("2024-04-01", "电子产品", "笔记本电脑", 10, 5500.0, 55000.0, "已入库"),
				new ReportData("2024-04-02", "办公用品", "打印纸", 100, 25.0, 2500.0, "已入库"),
				new ReportData("2024-04-03", "食品饮料", "矿泉水", 200, 2.5, 500.0, "已入库"),
				new ReportData("2024-04-04", "日用品", "洗手液", 50, 15.0, 750.0, "待入库"));
		reportTableView.setItems(data);
	}

	/**
	 * 初始化审批任务表格数据
	 */
	private void initializeApprovalTableData() {
		approvalIdColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getId()));
		approvalTypeColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType()));
		applicantColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApplicant()));
		approvalTitleColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));
		approvalAmountColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAmount()).asObject());
		applyDateColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getApplyDate()));
		priorityColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getPriority()));
		approvalStatusColumn.setCellValueFactory(
				cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));

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
	 * 导出报表
	 */
	@FXML
	void exportReport(ActionEvent event) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("导出报表");
		fileChooser.getExtensionFilters().addAll(
				new FileChooser.ExtensionFilter("CSV文件", "*.csv"),
				new FileChooser.ExtensionFilter("所有文件", "*.*"));
		fileChooser.setInitialFileName("审批报表_" + LocalDate.now() + ".csv");

		File file = fileChooser.showSaveDialog(approvalTableView.getScene().getWindow());
		if (file != null) {
			try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
				// CSV头
				writer.println("订单号,类型,申请人,标题,金额,申请日期,优先级,状态");

				// 数据行
				for (ApprovalTask task : approvalTableView.getItems()) {
					writer.printf("%s,%s,%s,%s,%.2f,%s,%s,%s%n",
							task.getId(),
							task.getType(),
							task.getApplicant(),
							task.getTitle(),
							task.getAmount(),
							task.getApplyDate(),
							task.getPriority(),
							task.getStatus());
				}

				showAlert("成功", "报表已导出至: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
			} catch (Exception e) {
				showAlert("错误", "导出失败: " + e.getMessage(), Alert.AlertType.ERROR);
				e.printStackTrace();
			}
		}
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
				socketClient.logout();
				socketClient.disconnect();

				Stage stage = (Stage) logoutButton.getScene().getWindow();
				stage.close();

				try {
					new LoginApplication().start(new Stage());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * 显示报表管理界面
	 */
	@FXML
	void showReportManagement(ActionEvent event) {
		reportManagementPane.setVisible(true);
		approvalCenterPane.setVisible(false);

		// 更新菜单按钮样式
		reportMenuButton.setStyle("-fx-background-color: #3498DB; -fx-border-width: 0;");
		approvalMenuButton.setStyle("-fx-background-color: #34495E; -fx-border-width: 0;");
	}

	/**
	 * 显示审批中心界面
	 */
	@FXML
	void showApprovalCenter(ActionEvent event) {
		reportManagementPane.setVisible(false);
		approvalCenterPane.setVisible(true);

		// 更新菜单按钮样式
		reportMenuButton.setStyle("-fx-background-color: #34495E; -fx-border-width: 0;");
		approvalMenuButton.setStyle("-fx-background-color: #3498DB; -fx-border-width: 0;");
	}

	/**
	 * 切换图表类型
	 */
	@FXML
	void switchChartType(ActionEvent event) {
		String selectedType = chartTypeComboBox.getValue();

		barChart.setVisible(false);
		lineChart.setVisible(false);
		pieChart.setVisible(false);

		switch (selectedType) {
			case "柱状图":
				barChart.setVisible(true);
				break;
			case "折线图":
				lineChart.setVisible(true);
				break;
			case "饼图":
				pieChart.setVisible(true);
				break;
		}
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
		pendingOrdersText.setText(String.valueOf(count));
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

	// 内部类：报表数据模型
	public static class ReportData {
		private String date;
		private String category;
		private String productName;
		private int quantity;
		private double unitPrice;
		private double totalAmount;
		private String status;

		public ReportData(String date, String category, String productName,
				int quantity, double unitPrice, double totalAmount, String status) {
			this.date = date;
			this.category = category;
			this.productName = productName;
			this.quantity = quantity;
			this.unitPrice = unitPrice;
			this.totalAmount = totalAmount;
			this.status = status;
		}

		public String getDate() {
			return date;
		}

		public String getCategory() {
			return category;
		}

		public String getProductName() {
			return productName;
		}

		public int getQuantity() {
			return quantity;
		}

		public double getUnitPrice() {
			return unitPrice;
		}

		public double getTotalAmount() {
			return totalAmount;
		}

		public String getStatus() {
			return status;
		}
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
