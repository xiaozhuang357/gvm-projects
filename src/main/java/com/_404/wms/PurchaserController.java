package com._404.wms;

import com._404.wms.model.*;
import com._404.wms.network.Message;
import com._404.wms.network.SocketClient;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 采购员主界面控制器 (PurchaserController)
 * <p>
 * 负责采购员角色的所有业务交互，主要功能包括：
 * 1. 浏览产品目录及查看库存状态。
 * 2. 创建并提交新的采购订单 (Purchase Order)。
 * 3. 查看和管理（取消）个人历史订单。
 * 4. 接收服务器推送的低库存预警。
 * 5. 维护与服务器的长连接及数据自动刷新。
 */
public class PurchaserController implements Initializable {

    // --- 核心数据与服务 ---
    private User currentUser;
    private SocketClient socketClient;

    // --- 数据模型 (ObservableList 用于绑定 JavaFX 表格) ---
    // 所有产品列表
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    // 采购订单列表
    private ObservableList<PurchaseOrder> orderList = FXCollections.observableArrayList();
    // 包装后的过滤列表，用于支持按状态筛选订单
    private FilteredList<PurchaseOrder> filteredOrders = new FilteredList<>(orderList, order -> true);
    // 当前正在创建的订单明细项
    private ObservableList<OrderItem> currentOrderItems = FXCollections.observableArrayList();

    // --- 状态控制 ---
    // 缓存已提示过的低库存商品ID，避免在同一次登录期间重复弹窗打扰用户
    private Set<String> alertedLowStockIds = new HashSet<>();

    // 控制后台轮询线程的标志位，使用 volatile 保证线程间可见性
    private volatile boolean keepRunning = true;
    // 后台轮询线程引用，用于定期从服务器拉取最新数据
    private Thread refreshThread;

    // --- FXML UI 组件注入 ---

    // 顶部栏
    @FXML
    private Label userInfoLabel;
    @FXML
    private Button logoutButton;

    // 左侧导航菜单
    @FXML
    private Button createOrderMenuButton;
    @FXML
    private Button orderListMenuButton;
    @FXML
    private Button productListMenuButton;

    // 内容区域容器 (使用 StackPane 实现页面切换)
    @FXML
    private StackPane contentPane;
    @FXML
    private VBox createOrderPane; // 创建订单面板
    @FXML
    private VBox orderListPane; // 订单列表面板
    @FXML
    private VBox productListPane; // 产品列表面板

    // 1. 创建订单界面控件
    @FXML
    private TextField orderIdField; // 订单号（自动生成）
    @FXML
    private TextField supplierField; // 供应商输入
    @FXML
    private DatePicker deliveryDatePicker;// 预计交货日期
    @FXML
    private TextArea orderRemarkArea; // 备注
    @FXML
    private TableView<OrderItem> orderItemTableView; // 订单明细表
    @FXML
    private TableColumn<OrderItem, String> itemProductColumn;
    @FXML
    private TableColumn<OrderItem, String> itemPriceColumn;
    @FXML
    private TableColumn<OrderItem, String> itemQuantityColumn;
    @FXML
    private TableColumn<OrderItem, String> itemSubtotalColumn;
    @FXML
    private TableColumn<OrderItem, Void> itemActionColumn; // 删除按钮列
    @FXML
    private Text totalAmountText; // 总金额显示

    // 2. 订单列表界面控件
    @FXML
    private ComboBox<String> orderStatusCombo; // 状态筛选下拉框
    @FXML
    private TableView<PurchaseOrder> orderTableView;
    @FXML
    private TableColumn<PurchaseOrder, String> orderIdColumn;
    @FXML
    private TableColumn<PurchaseOrder, String> supplierColumn;
    @FXML
    private TableColumn<PurchaseOrder, String> totalAmountColumn;
    @FXML
    private TableColumn<PurchaseOrder, String> orderStatusColumn;
    @FXML
    private TableColumn<PurchaseOrder, String> createTimeColumn;
    @FXML
    private TableColumn<PurchaseOrder, String> deliveryDateColumn;
    @FXML
    private TableColumn<PurchaseOrder, Void> orderActionColumn; // 操作按钮列

    // 3. 产品目录界面控件
    @FXML
    private TextField productSearchField; // 产品搜索框
    @FXML
    private TableView<Product> productTableView;
    @FXML
    private TableColumn<Product, String> productIdColumn;
    @FXML
    private TableColumn<Product, String> productNameColumn;
    @FXML
    private TableColumn<Product, String> categoryColumn;
    @FXML
    private TableColumn<Product, String> priceColumn;
    @FXML
    private TableColumn<Product, String> stockColumn;
    @FXML
    private TableColumn<Product, String> unitColumn;
    @FXML
    private TableColumn<Product, String> supplierNameColumn;

    /**
     * JavaFX 初始化方法。
     * 在 FXML 文件加载完成后自动调用，用于设置表格列工厂和初始化 UI 状态。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化三个主要表格的列映射
        initializeOrderItemTable();
        initializeOrderTable();
        initializeProductTable();

        // 预生成一个订单编号并锁定编辑
        orderIdField.setText(generateOrderId());
        orderIdField.setEditable(false);

        // 初始化订单状态筛选下拉框
        orderStatusCombo.setItems(FXCollections.observableArrayList(
                "全部", "待提交", "待审批", "已批准", "已拒绝", "已完成"));
        orderStatusCombo.setValue("全部");
    }

    /**
     * 设置当前登录用户，并启动后台数据同步。
     * 此方法通常由 LoginController 在跳转时调用。
     *
     * @param user 登录成功的用户对象
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.socketClient = LoginController.getSocketClient();
        userInfoLabel.setText("当前用户: " + user.getFullName() + " (采购员)");

        // 1. 立即加载一次数据
        loadDataFromServer();

        // 2. 启动后台守护线程进行轮询 (Polling)
        // 目的：实时获取库存变化（如其他操作员出库）和订单审批状态更新
        refreshThread = new Thread(() -> {
            int consecutiveFailures = 0;
            // 只要 keepRunning 为 true 且 Socket 连接正常，就持续运行
            while (keepRunning && LoginController.getSocketClient().isConnected()) {
                try {
                    // 每 5 秒轮询一次
                    Thread.sleep(5000);

                    // 双重检查连接状态
                    if (!keepRunning || !socketClient.isConnected()) {
                        System.out.println("连接已断开或用户退出，停止数据轮询");
                        break;
                    }

                    loadDataFromServer();
                    consecutiveFailures = 0; // 成功一次即重置失败计数
                } catch (InterruptedException e) {
                    System.out.println("轮询线程被中断");
                    break;
                } catch (Exception e) {
                    consecutiveFailures++;
                    System.err.println("数据加载失败 (第" + consecutiveFailures + "次): " + e.getMessage());

                    // 熔断机制：连续失败3次自动停止，避免死循环报错
                    if (consecutiveFailures >= 3) {
                        System.err.println("连续失败3次，停止数据轮询");
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("连接错误");
                            alert.setHeaderText("与服务器的连接已丢失");
                            alert.setContentText("请重新登录以恢复连接");
                            alert.showAndWait();
                        });
                        break;
                    }
                }
            }
            System.out.println("轮询线程已退出");
        }, "Purchaser-Refresh-Thread");

        refreshThread.setDaemon(true); // 设置为守护线程，JVM 退出时自动销毁
        refreshThread.start();

        // 3. 注册窗口关闭钩子
        // 确保用户点击窗口右上角 "X" 时，能优雅地关闭 Socket 和线程
        Platform.runLater(() -> {
            if (logoutButton != null && logoutButton.getScene() != null) {
                Stage stage = (Stage) logoutButton.getScene().getWindow();
                if (stage != null) {
                    stage.setOnCloseRequest(event -> {
                        System.out.println("窗口正在关闭，停止后台线程...");
                        keepRunning = false;

                        // 立即中断睡眠中的线程
                        if (refreshThread != null && refreshThread.isAlive()) {
                            refreshThread.interrupt();
                        }

                        // 开启新线程处理断开连接，避免阻塞 UI 关闭动画
                        Thread cleanupThread = new Thread(() -> {
                            // 等待轮询线程结束
                            if (refreshThread != null && refreshThread.isAlive()) {
                                try {
                                    refreshThread.join(2000);
                                } catch (InterruptedException ignored) {
                                }
                            }

                            // 发送登出消息并断开 TCP 连接
                            if (socketClient != null && socketClient.isConnected()) {
                                try {
                                    socketClient.logout();
                                } catch (Exception e) {
                                    System.err.println("发送登出消息失败: " + e.getMessage());
                                }
                                socketClient.closeConnection();
                                System.out.println("已断开Socket连接");
                            }
                        }, "Window-Close-Cleanup");
                        cleanupThread.setDaemon(true);
                        cleanupThread.start();
                    });
                }
            }
        });
    }

    /**
     * 核心数据加载方法。
     * 异步从服务器获取：1. 产品列表 2. 当前用户的采购订单。
     */
    private void loadDataFromServer() {
        new Thread(() -> {
            try {
                // 前置检查
                if (!socketClient.isConnected()) {
                    System.err.println("未连接到服务器，跳过数据加载");
                    return;
                }

                // 1. 获取产品列表
                Message response = socketClient.sendAndReceive(new Message(Message.MessageType.PRODUCT_LIST));
                if (response != null && response.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<Product> products = (List<Product>) response.getData();
                    if (products != null) {
                        // 切换回 UI 线程更新数据
                        Platform.runLater(() -> {
                            productList.clear();
                            productList.addAll(products);
                            // 数据加载完成后，检查是否有低库存警报
                            checkLowStock();
                        });
                    }
                } else {
                    String errorMsg = response != null ? response.getMessage() : "null response";
                    throw new RuntimeException("加载产品失败: " + errorMsg);
                }

                // 2. 获取所有采购订单并筛选
                Message orderResponse = socketClient.getPurchaseOrders();
                if (orderResponse != null && orderResponse.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<PurchaseOrder> orders = (List<PurchaseOrder>) orderResponse.getData();
                    if (orders != null && currentUser != null) {
                        Platform.runLater(() -> {
                            orderList.clear();
                            // 客户端过滤：只显示当前用户创建的订单
                            // 注意：在生产环境中，建议由服务器端根据 Session 过滤，减少数据传输量
                            for (PurchaseOrder order : orders) {
                                if (order.getPurchaserId() != null
                                        && order.getPurchaserId().equals(currentUser.getUserId())) {
                                    orderList.add(order);
                                }
                            }
                        });
                    }
                } else {
                    String errorMsg = orderResponse != null ? orderResponse.getMessage() : "null response";
                    throw new RuntimeException("加载订单失败: " + errorMsg);
                }
            } catch (Exception e) {
                System.err.println("Error loading data from server: " + e.getMessage());
                e.printStackTrace();
                // 在异步线程中不宜直接抛出 RuntimeException，实际项目中应记录日志或通知 UI
            }
        }, "Purchaser-Load-Data").start();
    }

    /**
     * 检查库存预警逻辑。
     * 向服务器查询低库存商品，如果存在且未提醒过，则弹出 Alert。
     */
    private void checkLowStock() {
        new Thread(() -> {
            Message msg = socketClient.sendAndReceive(new Message(Message.MessageType.PRODUCT_STOCK_ALERT));
            if (!msg.isSuccess())
                return;

            @SuppressWarnings("unchecked")
            List<Product> lows = (List<Product>) msg.getData();
            if (lows == null || lows.isEmpty())
                return;

            // 过滤逻辑：只提醒 ID 不在 alertedLowStockIds 集合中的商品
            List<Product> toShow = new ArrayList<>();
            for (Product p : lows) {
                if (!alertedLowStockIds.contains(p.getProductId())) {
                    toShow.add(p);
                }
            }

            if (toShow.isEmpty())
                return;

            // 构建弹窗内容
            Platform.runLater(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("以下商品库存接近或低于警戒值:\n\n");
                for (Product p : toShow) {
                    sb.append(String.format("%s [%s] - 当前库存: %d, 预警值: %d\n",
                            p.getProductName(), p.getProductId(), p.getCurrentStock(), p.getMinStock()));
                    // 标记为已提示
                    alertedLowStockIds.add(p.getProductId());
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("库存预警");
                alert.setHeaderText("库存不足提醒");
                TextArea ta = new TextArea(sb.toString());
                ta.setEditable(false);
                ta.setWrapText(true);
                ta.setPrefWidth(500);
                ta.setPrefHeight(300);
                alert.getDialogPane().setContent(ta);

                ButtonType createOrder = new ButtonType("立即生成采购单");
                alert.getButtonTypes().setAll(createOrder, ButtonType.CLOSE);

                // 用户点击“立即生成采购单”则跳转界面
                alert.showAndWait().ifPresent(bt -> {
                    if (bt == createOrder) {
                        showCreateOrder(null);
                    }
                });
            });
        }).start();
    }

    /**
     * 配置“创建订单”页面的明细表格。
     * 包括自定义的“删除”按钮单元格。
     */
    private void initializeOrderItemTable() {
        itemProductColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        itemPriceColumn.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getPrice())));
        itemQuantityColumn
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        itemSubtotalColumn.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f", data.getValue().getPrice() * data.getValue().getQuantity())));

        // 配置操作列：每行添加一个删除按钮
        itemActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("删除");

            {
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    OrderItem item = getTableRow().getItem();
                    if (item != null) {
                        currentOrderItems.remove(item); // 从当前列表中移除
                        updateTotalAmount(); // 重新计算总金额
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
                setAlignment(Pos.CENTER);
            }
        });

        orderItemTableView.setItems(currentOrderItems);
    }

    /**
     * 配置“订单列表”表格。
     * 包括自定义的“查看”和“取消”按钮单元格。
     */
    private void initializeOrderTable() {
        orderIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOrderId()));
        supplierColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplier()));
        totalAmountColumn.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getTotalAmount())));
        orderStatusColumn
                .setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().getDisplayName()));
        createTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        deliveryDateColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getExpectedDeliveryDate() != null ? data.getValue().getExpectedDeliveryDate().toString()
                        : ""));

        // 配置操作列
        orderActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("查看");
            private final Button cancelBtn = new Button("取消");
            private final HBox hBox = new HBox(10, viewBtn, cancelBtn);

            {
                viewBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                hBox.setAlignment(Pos.CENTER);

                viewBtn.setOnAction(e -> handleViewOrder(getTableRow().getItem()));
                cancelBtn.setOnAction(e -> handleCancelOrder(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                PurchaseOrder order = getTableRow().getItem();
                if (empty || order == null) {
                    setGraphic(null);
                } else {
                    // 业务规则：只有“待提交”状态的订单可以被取消
                    cancelBtn.setVisible(order.getStatus() == PurchaseOrder.OrderStatus.PENDING_SUBMIT);
                    setGraphic(hBox);
                }
            }
        });

        orderTableView.setItems(filteredOrders); // 绑定 FilteredList 以支持搜索/筛选
    }

    /**
     * 配置“产品列表”表格。
     */
    private void initializeProductTable() {
        productIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductId()));
        productNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        categoryColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        priceColumn.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getPrice())));
        stockColumn.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getCurrentStock())));
        unitColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnit()));
        supplierNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplier()));

        productTableView.setItems(productList);
    }

    /**
     * 生成基于时间戳的唯一订单编号。
     * 格式: PO-yyyyMMddHHmmss
     */
    private String generateOrderId() {
        return "PO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 重新计算当前订单所有明细的总金额并更新 UI。
     */
    private void updateTotalAmount() {
        double total = 0.0;
        for (OrderItem item : currentOrderItems) {
            total += item.getPrice() * item.getQuantity();
        }
        totalAmountText.setText(String.format("¥%.2f", total));
    }

    // ==================== 菜单切换逻辑 (使用 visibility 控制) ====================

    @FXML
    void showCreateOrder(ActionEvent event) {
        createOrderPane.setVisible(true);
        orderListPane.setVisible(false);
        productListPane.setVisible(false);
    }

    @FXML
    void showOrderList(ActionEvent event) {
        createOrderPane.setVisible(false);
        orderListPane.setVisible(true);
        productListPane.setVisible(false);
        loadDataFromServer(); // 切换时刷新数据
    }

    @FXML
    void showProductList(ActionEvent event) {
        createOrderPane.setVisible(false);
        orderListPane.setVisible(false);
        productListPane.setVisible(true);
    }

    /**
     * 产品列表搜索功能。
     * 支持按 ID、名称或类别进行模糊搜索。
     */
    @FXML
    void handleSearchProduct(ActionEvent event) {
        String keyword = productSearchField.getText().trim();
        if (keyword.isEmpty()) {
            productTableView.setItems(productList);
            return;
        }

        ObservableList<Product> filteredList = productList
                .filtered(product -> product.getProductId().toLowerCase().contains(keyword.toLowerCase()) ||
                        product.getProductName().toLowerCase().contains(keyword.toLowerCase()) ||
                        product.getCategory().toLowerCase().contains(keyword.toLowerCase()));

        productTableView.setItems(filteredList);
    }

    // ==================== 创建订单功能区 ====================

    /**
     * 弹出对话框选择产品添加到当前订单。
     */
    @FXML
    void handleAddOrderItem(ActionEvent event) {
        if (productList.isEmpty()) {
            showAlert("提示", "当前没有可选产品", Alert.AlertType.INFORMATION);
            return;
        }

        // 构造友好的选择项字符串 (名称 [ID] - 库存)
        List<String> options = new ArrayList<>();
        Map<String, Product> map = new HashMap<>(); // 用于根据选择字符串反查 Product 对象
        for (Product p : productList) {
            String label = String.format("%s [%s] - 库存: %d %s", p.getProductName(), p.getProductId(),
                    p.getCurrentStock(), p.getUnit() == null ? "" : p.getUnit());
            options.add(label);
            map.put(label, p);
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.setTitle("添加产品");
        dialog.setHeaderText("选择要采购的产品");
        dialog.setContentText("产品:");

        // 第一步：选择产品
        dialog.showAndWait().ifPresent(selectedLabel -> {
            Product product = map.get(selectedLabel);
            if (product == null)
                return;

            // 第二步：输入数量
            TextInputDialog quantityDialog = new TextInputDialog("1");
            quantityDialog.setTitle("输入数量");
            quantityDialog.setHeaderText("产品: " + product.getProductName());
            quantityDialog.setContentText("采购数量:");

            quantityDialog.showAndWait().ifPresent(quantityStr -> {
                try {
                    int quantity = Integer.parseInt(quantityStr);
                    if (quantity > 0) {
                        // 创建新的明细项并添加到表格
                        OrderItem item = new OrderItem();
                        item.setProductId(product.getProductId());
                        item.setProductName(product.getProductName());
                        item.setPrice(product.getPrice());
                        item.setQuantity(quantity);
                        item.setUnit(product.getUnit());

                        currentOrderItems.add(item);
                        updateTotalAmount();
                    } else {
                        showAlert("错误", "数量必须大于0", Alert.AlertType.ERROR);
                    }
                } catch (NumberFormatException e) {
                    showAlert("错误", "请输入有效的数字", Alert.AlertType.ERROR);
                }
            });
        });
    }

    /**
     * 提交订单到服务器。
     * 流程：校验输入 -> 构建对象 -> 发送网络请求 -> 处理响应 -> 刷新数据。
     */
    @FXML
    void handleSubmitOrder(ActionEvent event) {
        String supplier = supplierField.getText().trim();
        LocalDate deliveryDate = deliveryDatePicker.getValue();
        String remark = orderRemarkArea.getText().trim();

        // --- 表单校验 ---
        if (supplier.isEmpty()) {
            showAlert("错误", "请输入供应商名称", Alert.AlertType.ERROR);
            return;
        }

        if (currentOrderItems.isEmpty()) {
            showAlert("错误", "请至少添加一个产品", Alert.AlertType.ERROR);
            return;
        }

        if (deliveryDate == null) {
            showAlert("错误", "请选择预计交货日期", Alert.AlertType.ERROR);
            return;
        }

        // --- 构建 PO 对象 ---
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(orderIdField.getText());
        order.setPurchaserId(currentUser.getUserId());
        order.setPurchaserName(currentUser.getFullName());
        order.setSupplier(supplier);
        order.setExpectedDeliveryDate(deliveryDate);
        order.setRemark(remark);
        order.setStatus(PurchaseOrder.OrderStatus.PENDING_SUBMIT);
        order.setCreateTime(LocalDateTime.now());

        for (OrderItem item : currentOrderItems) {
            order.addItem(item.getProductId(), item.getProductName(),
                    item.getPrice(), item.getQuantity(), item.getUnit());
        }

        // --- 异步提交 ---
        new Thread(() -> {
            try {
                Message msg = socketClient.sendAndReceive(
                        new Message(Message.MessageType.PURCHASE_ORDER_ADD, order));

                Platform.runLater(() -> {
                    if (msg != null && msg.isSuccess()) {
                        // 记录此次采购的商品ID，防止刚买完系统又弹低库存预警
                        List<String> productIds = new ArrayList<>();
                        for (OrderItem it : currentOrderItems) {
                            productIds.add(it.getProductId());
                        }

                        showAlert("成功", "采购订单提交成功！\n订单编号: " + order.getOrderId(), Alert.AlertType.INFORMATION);
                        handleClearOrder(null); // 清空表单准备下一次输入

                        // 更新预警过滤列表
                        for (String productId : productIds) {
                            alertedLowStockIds.add(productId);
                        }
                        loadDataFromServer(); // 刷新列表
                    } else {
                        String errorMsg = (msg != null) ? msg.getMessage() : "服务器无响应";
                        showAlert("失败", errorMsg, Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("失败", "提交订单时发生错误: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }, "Submit-Order-Thread").start();
    }

    /**
     * 清空创建订单界面的表单。
     */
    @FXML
    void handleClearOrder(ActionEvent event) {
        orderIdField.setText(generateOrderId()); // 重新生成ID
        supplierField.clear();
        deliveryDatePicker.setValue(null);
        orderRemarkArea.clear();
        currentOrderItems.clear();
        updateTotalAmount();
    }

    // ==================== 订单列表功能区 ====================

    @FXML
    void handleRefreshOrders(ActionEvent event) {
        loadDataFromServer();
    }

    /**
     * 查看订单详情弹窗。
     */
    @FXML
    void handleViewOrder(PurchaseOrder order) {
        if (order == null)
            return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("订单详情");
        alert.setHeaderText("订单编号: " + order.getOrderId());

        StringBuilder content = new StringBuilder();
        content.append("供应商: ").append(order.getSupplier()).append("\n");
        content.append("订单状态: ").append(order.getStatus().getDisplayName()).append("\n");
        content.append("订单金额: ¥").append(String.format("%.2f", order.getTotalAmount())).append("\n");
        content.append("创建时间: ").append(order.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .append("\n");
        content.append("预计交货: ").append(order.getExpectedDeliveryDate()).append("\n\n");
        content.append("订单明细:\n");

        for (PurchaseOrder.OrderItem item : order.getItems()) {
            content.append(String.format("  - %s × %d = ¥%.2f\n",
                    item.getProductName(), item.getQuantity(),
                    item.getPrice() * item.getQuantity()));
        }

        if (order.getRemark() != null && !order.getRemark().isEmpty()) {
            content.append("\n备注: ").append(order.getRemark());
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * 取消订单操作。
     * 仅向服务器发送删除请求，具体能否删除由服务器端校验状态（如是否已审批）。
     */
    @FXML
    void handleCancelOrder(PurchaseOrder order) {
        if (order == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认取消");
        confirm.setHeaderText("确定要取消订单: " + order.getOrderId() + "?");
        confirm.setContentText("取消后订单将被删除！");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    Message msg = socketClient.sendAndReceive(
                            new Message(Message.MessageType.PURCHASE_ORDER_DELETE, order.getOrderId()));

                    Platform.runLater(() -> {
                        if (msg.isSuccess()) {
                            orderList.remove(order); // UI 立即移除
                            showAlert("成功", "订单已取消", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }).start();
            }
        });
    }

    /**
     * 根据下拉框选择筛选订单列表。
     * 更新 FilteredList 的 Predicate。
     */
    @FXML
    void handleFilterOrders(ActionEvent event) {
        if (filteredOrders == null)
            return;

        String statusFilter = orderStatusCombo.getValue();

        if (statusFilter == null || "全部".equals(statusFilter)) {
            filteredOrders.setPredicate(order -> true);
            return;
        }

        filteredOrders.setPredicate(order -> {
            String orderStatus = order.getStatus().getDisplayName();
            // 简单匹配显示名称
            if (orderStatus.equals(statusFilter) || orderStatus.contains(statusFilter)) {
                return true;
            }
            // 特殊处理：下拉框选"待审批"时，匹配所有含"待...审批"的状态
            return "待审批".equals(statusFilter) &&
                    (orderStatus.contains("待") && orderStatus.contains("审批"));
        });
    }

    /**
     * 导出订单为 CSV 文件。
     * 使用 Java IO 将表格数据写入文件。
     */
    @FXML
    void handleExportOrders(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("导出订单报表");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("CSV文件", "*.csv"),
                new javafx.stage.FileChooser.ExtensionFilter("所有文件", "*.*"));
        fileChooser.setInitialFileName("采购订单_" + java.time.LocalDate.now() + ".csv");

        java.io.File file = fileChooser.showSaveDialog(orderTableView.getScene().getWindow());
        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
                // 写入表头
                writer.println("订单号,供应商,总金额,状态,创建时间,商品数量");

                // 写入数据行
                for (PurchaseOrder order : orderTableView.getItems()) {
                    writer.printf("%s,%s,%.2f,%s,%s,%d%n",
                            order.getOrderId(),
                            order.getSupplier(),
                            order.getTotalAmount(),
                            order.getStatus().getDisplayName(),
                            order.getCreateTime(),
                            order.getItems().size());
                }

                showAlert("成功", "报表已导出至: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("错误", "导出失败: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    // ==================== 退出系统 ====================

    /**
     * 处理用户登出。
     * 1. 停止轮询线程。
     * 2. 发送 logout 消息并断开 socket。
     * 3. 关闭当前窗口并跳转回登录页。
     */
    @FXML
    void handleLogout(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认退出");
        confirm.setHeaderText("确定要退出系统吗?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (logoutButton != null) {
                    logoutButton.setDisable(true);
                }

                // 1. 停止标志位
                keepRunning = false;

                // 2. 立即中断睡眠中的轮询线程
                if (refreshThread != null && refreshThread.isAlive()) {
                    refreshThread.interrupt();
                    System.out.println("已中断轮询线程");
                }

                // 获取当前 Stage 引用以便稍后关闭
                final Stage currentStage = (logoutButton != null && logoutButton.getScene() != null)
                        ? (Stage) logoutButton.getScene().getWindow()
                        : null;

                // 3. 在新线程中执行清理和跳转
                Thread logoutThread = new Thread(() -> {
                    try {
                        // 稍微等待 refreshThread 结束
                        if (refreshThread != null && refreshThread.isAlive()) {
                            refreshThread.join(3000);
                        }

                        if (socketClient != null && socketClient.isConnected()) {
                            try {
                                socketClient.logout();
                            } catch (Exception e) {
                                System.err.println("发送登出消息失败: " + e.getMessage());
                            }
                            socketClient.closeConnection();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // 4. UI跳转
                        Platform.runLater(() -> {
                            try {
                                javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(
                                        LoginApplication.class.getResource("login.fxml"));
                                javafx.scene.Scene scene = new javafx.scene.Scene(fxmlLoader.load());

                                Stage loginStage = new Stage();
                                loginStage.setTitle("仓库管理系统 - 登录");
                                loginStage.setScene(scene);
                                loginStage.show();

                                if (currentStage != null) {
                                    currentStage.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }, "Purchaser-Logout-Thread");
                logoutThread.setDaemon(false);
                logoutThread.start();
            }
        });
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * 内部类：订单项视图模型 (ViewModel)。
     * 仅用于 createOrderTableView 的显示，简化了 OrderItem 实体的属性绑定。
     */
    public static class OrderItem {
        private String productId;
        private String productName;
        private double price;
        private int quantity;
        private String unit;

        // Getters and Setters
        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public double getPrice() {
            return price;
        }

        public void setPrice(double price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }
}