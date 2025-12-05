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

public class PurchaserController implements Initializable {

    private User currentUser;
    private SocketClient socketClient;
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private ObservableList<PurchaseOrder> orderList = FXCollections.observableArrayList();
    private FilteredList<PurchaseOrder> filteredOrders = new FilteredList<>(orderList, order -> true);
    private ObservableList<OrderItem> currentOrderItems = FXCollections.observableArrayList();
    // 已提示的低库存商品集合（避免重复弹窗）
    private Set<String> alertedLowStockIds = new HashSet<>();

    // 顶部栏
    @FXML
    private Label userInfoLabel;
    @FXML
    private Button logoutButton;

    // 左侧菜单
    @FXML
    private Button createOrderMenuButton;
    @FXML
    private Button orderListMenuButton;
    @FXML
    private Button productListMenuButton;

    // 内容面板
    @FXML
    private StackPane contentPane;
    @FXML
    private VBox createOrderPane;
    @FXML
    private VBox orderListPane;
    @FXML
    private VBox productListPane;

    // 创建订单
    @FXML
    private TextField orderIdField;
    @FXML
    private TextField supplierField;
    @FXML
    private DatePicker deliveryDatePicker;
    @FXML
    private TextArea orderRemarkArea;
    @FXML
    private TableView<OrderItem> orderItemTableView;
    @FXML
    private TableColumn<OrderItem, String> itemProductColumn;
    @FXML
    private TableColumn<OrderItem, String> itemPriceColumn;
    @FXML
    private TableColumn<OrderItem, String> itemQuantityColumn;
    @FXML
    private TableColumn<OrderItem, String> itemSubtotalColumn;
    @FXML
    private TableColumn<OrderItem, Void> itemActionColumn;
    @FXML
    private Text totalAmountText;

    // 订单列表
    @FXML
    private ComboBox<String> orderStatusCombo;
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
    private TableColumn<PurchaseOrder, Void> orderActionColumn;

    // 产品目录
    @FXML
    private TextField productSearchField;
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeOrderItemTable();
        initializeOrderTable();
        initializeProductTable();

        // 生成订单编号
        orderIdField.setText(generateOrderId());
        orderIdField.setEditable(false);

        // 初始化订单状态下拉框
        orderStatusCombo.setItems(FXCollections.observableArrayList(
                "全部", "待提交", "待审批", "已批准", "已拒绝", "已完成"));
        orderStatusCombo.setValue("全部");
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.socketClient = LoginController.getSocketClient();
        userInfoLabel.setText("当前用户: " + user.getFullName() + " (采购员)");

        loadDataFromServer();

        // 后台轮询以获取服务器端最新订单/商品状态，减小需重新登录才能看到更新的情况
        new Thread(() -> {
            while (LoginController.getSocketClient().isConnected()) {
                try {
                    Thread.sleep(5000);
                    loadDataFromServer();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Purchaser-Refresh-Thread").start();
    }

    /**
     * 从服务器加载数据
     */
    private void loadDataFromServer() {
        new Thread(() -> {
            // 加载产品列表
            Message response = socketClient.sendAndReceive(new Message(Message.MessageType.PRODUCT_LIST));
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Product> products = (List<Product>) response.getData();
                Platform.runLater(() -> {
                    productList.clear();
                    productList.addAll(products);
                    // 检查库存预警（仅对未提示过的商品弹窗）
                    checkLowStock();
                });
            }

            // 加载我的订单
            Message orderResponse = socketClient.getPurchaseOrders();
            if (orderResponse.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<PurchaseOrder> orders = (List<PurchaseOrder>) orderResponse.getData();
                Platform.runLater(() -> {
                    // 只显示当前用户创建的订单
                    orderList.clear();
                    for (PurchaseOrder order : orders) {
                        if (order.getPurchaserId().equals(currentUser.getUserId())) {
                            orderList.add(order);
                        }
                    }
                });
            }
        }).start();
    }

    /**
     * 检查库存预警并提醒采购员
     */
    private void checkLowStock() {
        new Thread(() -> {
            Message msg = socketClient.sendAndReceive(new Message(Message.MessageType.PRODUCT_STOCK_ALERT));
            if (!msg.isSuccess()) {
                return;
            }

            @SuppressWarnings("unchecked")
            List<Product> lows = (List<Product>) msg.getData();
            if (lows == null || lows.isEmpty()) {
                return;
            }
            // 过滤掉已经提示过的商品
            List<Product> toShow = new ArrayList<>();
            for (Product p : lows) {
                if (!alertedLowStockIds.contains(p.getProductId())) {
                    toShow.add(p);
                }
            }

            if (toShow.isEmpty())
                return;

            Platform.runLater(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("以下商品库存接近或低于警戒值:\n\n");
                for (Product p : toShow) {
                    sb.append(String.format("%s [%s] - 当前库存: %d, 预警值: %d\n",
                            p.getProductName(), p.getProductId(), p.getCurrentStock(), p.getMinStock()));
                    // 标记为已提示，避免重复提示
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
                alert.showAndWait().ifPresent(bt -> {
                    if (bt == createOrder) {
                        // 跳转到创建订单页面
                        showCreateOrder(null);
                    }
                });
            });
        }).start();
    }

    /**
     * 初始化订单明细表格
     */
    private void initializeOrderItemTable() {
        itemProductColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        itemPriceColumn.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.2f", data.getValue().getPrice())));
        itemQuantityColumn
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        itemSubtotalColumn.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f", data.getValue().getPrice() * data.getValue().getQuantity())));

        // 操作列
        itemActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("删除");

            {
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setOnAction(e -> {
                    OrderItem item = getTableRow().getItem();
                    if (item != null) {
                        currentOrderItems.remove(item);
                        updateTotalAmount();
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
     * 初始化订单表格
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

        // 操作列
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
                    // 只有待提交的订单可以取消
                    cancelBtn.setVisible(order.getStatus() == PurchaseOrder.OrderStatus.PENDING_SUBMIT);
                    setGraphic(hBox);
                }
            }
        });

        orderTableView.setItems(filteredOrders);
    }

    /**
     * 初始化产品表格
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
     * 生成订单编号
     */
    private String generateOrderId() {
        return "PO-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /**
     * 更新订单总金额
     */
    private void updateTotalAmount() {
        double total = 0.0;
        for (OrderItem item : currentOrderItems) {
            total += item.getPrice() * item.getQuantity();
        }
        totalAmountText.setText(String.format("¥%.2f", total));
    }

    // ==================== 菜单切换 ====================

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
        loadDataFromServer();
    }

    @FXML
    void showProductList(ActionEvent event) {
        createOrderPane.setVisible(false);
        orderListPane.setVisible(false);
        productListPane.setVisible(true);
    }

    @FXML
    void handleSearchProduct(ActionEvent event) {
        String keyword = productSearchField.getText().trim();
        if (keyword.isEmpty()) {
            // 如果搜索框为空，显示所有产品
            productTableView.setItems(productList);
            return;
        }

        // 根据关键词过滤产品
        ObservableList<Product> filteredList = productList
                .filtered(product -> product.getProductId().toLowerCase().contains(keyword.toLowerCase()) ||
                        product.getProductName().toLowerCase().contains(keyword.toLowerCase()) ||
                        product.getCategory().toLowerCase().contains(keyword.toLowerCase()));

        productTableView.setItems(filteredList);
    }

    // ==================== 创建订单功能 ====================

    @FXML
    void handleAddOrderItem(ActionEvent event) {
        if (productList.isEmpty()) {
            showAlert("提示", "当前没有可选产品", Alert.AlertType.INFORMATION);
            return;
        }

        // 构造显示友好的字符串列表并映射回产品ID
        List<String> options = new ArrayList<>();
        Map<String, Product> map = new HashMap<>();
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

        dialog.showAndWait().ifPresent(selectedLabel -> {
            Product product = map.get(selectedLabel);
            if (product == null)
                return;
            TextInputDialog quantityDialog = new TextInputDialog("1");
            quantityDialog.setTitle("输入数量");
            quantityDialog.setHeaderText("产品: " + product.getProductName());
            quantityDialog.setContentText("采购数量:");

            quantityDialog.showAndWait().ifPresent(quantityStr -> {
                try {
                    int quantity = Integer.parseInt(quantityStr);
                    if (quantity > 0) {
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

    @FXML
    void handleSubmitOrder(ActionEvent event) {
        String supplier = supplierField.getText().trim();
        LocalDate deliveryDate = deliveryDatePicker.getValue();
        String remark = orderRemarkArea.getText().trim();

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

        // 创建采购订单
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(orderIdField.getText());
        order.setPurchaserId(currentUser.getUserId());
        order.setPurchaserName(currentUser.getFullName());
        order.setSupplier(supplier);
        order.setExpectedDeliveryDate(deliveryDate);
        order.setRemark(remark);
        order.setStatus(PurchaseOrder.OrderStatus.PENDING_SUBMIT);
        order.setCreateTime(LocalDateTime.now());

        // 设置订单明细
        for (OrderItem item : currentOrderItems) {
            order.addItem(item.getProductId(), item.getProductName(),
                    item.getPrice(), item.getQuantity(), item.getUnit());
        }

        // 提交到服务器
        new Thread(() -> {
            Message msg = socketClient.sendAndReceive(
                    new Message(Message.MessageType.PURCHASE_ORDER_ADD, order));

            Platform.runLater(() -> {
                if (msg.isSuccess()) {
                    showAlert("成功", "采购订单提交成功！\n订单编号: " + order.getOrderId(), Alert.AlertType.INFORMATION);
                    handleClearOrder(null);
                    // 将本次订单中的商品标记为已提醒，避免立即重复弹窗
                    for (OrderItem it : currentOrderItems) {
                        alertedLowStockIds.add(it.getProductId());
                    }
                    loadDataFromServer();
                } else {
                    showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                }
            });
        }).start();
    }

    @FXML
    void handleClearOrder(ActionEvent event) {
        orderIdField.setText(generateOrderId());
        supplierField.clear();
        deliveryDatePicker.setValue(null);
        orderRemarkArea.clear();
        currentOrderItems.clear();
        updateTotalAmount();
    }

    // ==================== 订单列表功能 ====================

    @FXML
    void handleRefreshOrders(ActionEvent event) {
        loadDataFromServer();
    }

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
                            orderList.remove(order);
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
     * 应用订单筛选
     */
    @FXML
    void handleFilterOrders(ActionEvent event) {
        if (filteredOrders == null)
            return;

        String statusFilter = orderStatusCombo.getValue();

        // 如果是"全部"，显示所有订单
        if (statusFilter == null || "全部".equals(statusFilter)) {
            filteredOrders.setPredicate(order -> true);
            return;
        }

        filteredOrders.setPredicate(order -> {
            String orderStatus = order.getStatus().getDisplayName();

            if (orderStatus.equals(statusFilter) || orderStatus.contains(statusFilter)) {
                return true;
            }

            return "待审批".equals(statusFilter) &&
                    (orderStatus.contains("待") && orderStatus.contains("审批"));
        });
    }

    /**
     * 导出订单报表
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
                // CSV头
                writer.println("订单号,供应商,总金额,状态,创建时间,商品数量");

                // 数据行
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

    // ==================== 通用功能 ====================

    @FXML
    void handleLogout(ActionEvent event) {
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

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // 内部类：订单明细项（用于显示）
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
