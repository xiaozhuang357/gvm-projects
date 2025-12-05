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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Predicate;
import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WarehouseAdminController implements Initializable {

    @FXML
    private ComboBox<String> stockOutProductCombo;
    @FXML
    private TextField stockOutQuantityField;
    @FXML
    private TextField recipientField;
    @FXML
    private TextField recipientDeptField;
    @FXML
    private TextArea stockOutRemarkArea;

    @FXML
    private TableView<StockRecord> stockRecordTableView;
    @FXML
    private ComboBox<String> recordTypeCombo;
    @FXML
    private TableColumn<StockRecord, String> recordIdColumn;
    @FXML
    private TableColumn<StockRecord, String> recordTypeColumn;
    @FXML
    private TableColumn<StockRecord, String> recordProductColumn;
    @FXML
    private TableColumn<StockRecord, String> recordQuantityColumn;
    @FXML
    private TableColumn<StockRecord, String> recordOperatorColumn;
    @FXML
    private TableColumn<StockRecord, String> recordTimeColumn;
    @FXML
    private TableColumn<StockRecord, String> recordRemarkColumn;

    // 用户管理
    @FXML
    private TableView<User> userTableView;
    @FXML
    private TableColumn<User, String> userIdColumn;
    @FXML
    private TableColumn<User, String> usernameColumn;
    @FXML
    private TableColumn<User, String> fullNameColumn;
    @FXML
    private TableColumn<User, String> roleColumn;
    @FXML
    private TableColumn<User, String> createTimeColumn;
    @FXML
    private TableColumn<User, String> lastLoginColumn;
    @FXML
    private TableColumn<User, Void> userActionColumn;

    // 报表统计
    @FXML
    private Text totalProductsText;
    @FXML
    private Text lowStockText;
    @FXML
    private Text totalUsersText;

    // 公共字段与数据
    private User currentUser;
    private SocketClient socketClient;
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts;
    private Predicate<Product> quickSearchPredicate = product -> true;
    private Predicate<Product> advancedProductPredicate = product -> true;
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private ObservableList<StockRecord> stockRecordList = FXCollections.observableArrayList();
    private ObservableList<PurchaseOrder> allOrderList = FXCollections.observableArrayList();

    @FXML
    private Label userInfoLabel;
    @FXML
    private Button logoutButton;

    // 页签容器
    @FXML
    private Pane productPane;
    @FXML
    private Pane stockPane;
    @FXML
    private Pane userPane;
    @FXML
    private Pane reportPane;

    // 产品相关控件
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
    private TableColumn<Product, String> currentStockColumn;
    @FXML
    private TableColumn<Product, String> unitColumn;
    @FXML
    private TableColumn<Product, String> minStockColumn;
    @FXML
    private TableColumn<Product, String> maxStockColumn;
    @FXML
    private TableColumn<Product, String> supplierColumn;
    @FXML
    private TableColumn<Product, Void> productActionColumn;

    // 入库/出库控件
    @FXML
    private TextField batchNumberField;
    @FXML
    private ComboBox<String> stockInProductCombo;
    @FXML
    private TextField stockInQuantityField;
    @FXML
    private TextArea stockInRemarkArea;
    @FXML
    private Button stockInBtn;
    @FXML
    private Button stockOutBtn;
    @FXML
    private TextField warehouseLocationField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeProductTable();
        initializeUserTable();
        initializeStockRecordTable();

        // 自动生成批次号
        batchNumberField.setText(generateBatchNumber());
        batchNumberField.setEditable(false);

        // 初始化记录类型下拉框
        recordTypeCombo.setItems(FXCollections.observableArrayList("全部", "入库", "出库"));
        recordTypeCombo.setValue("全部");
    }

    /**
     * 设置当前用户
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.socketClient = LoginController.getSocketClient();
        userInfoLabel.setText("当前用户: " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")");

        // 加载数据
        loadDataFromServer();

        // 后台轮询刷新，确保入库/到货/审批变更能及时反映到界面
        new Thread(() -> {
            while (LoginController.getSocketClient().isConnected()) {
                try {
                    Thread.sleep(5000);
                    loadDataFromServer();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "Warehouse-Refresh-Thread").start();
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
                    updateProductCombos();
                    updateStatistics();
                });
            }

            // 加载用户列表
            Message userResponse = socketClient.getUserList();
            if (userResponse.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<User> users = (List<User>) userResponse.getData();
                Platform.runLater(() -> {
                    userList.clear();
                    userList.addAll(users);
                    updateStatistics();
                });
            }

            // 加载采购订单（用于仓库查看已批准订单）
            Message ordersResp = socketClient.sendAndReceive(new Message(Message.MessageType.PURCHASE_ORDER_LIST));
            if (ordersResp != null && ordersResp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<PurchaseOrder> orders = (List<PurchaseOrder>) ordersResp.getData();
                Platform.runLater(() -> {
                    allOrderList.clear();
                    if (orders != null)
                        allOrderList.addAll(orders);
                });
            }
        }).start();
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
        currentStockColumn.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getCurrentStock())));
        unitColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUnit()));
        minStockColumn
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getMinStock())));
        maxStockColumn
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getMaxStock())));
        supplierColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSupplier()));

        // 操作列
        productActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("编辑");
            private final Button deleteBtn = new Button("删除");
            private final HBox hBox = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                hBox.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> handleEditProduct(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDeleteProduct(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hBox);
            }
        });

        filteredProducts = new FilteredList<>(productList, product -> true);
        productTableView.setItems(filteredProducts);
    }

    private void refreshProductFilter() {
        if (filteredProducts != null) {
            filteredProducts.setPredicate(product -> quickSearchPredicate.test(product)
                    && advancedProductPredicate.test(product));
        }
    }

    /**
     * 初始化用户表格
     */
    private void initializeUserTable() {
        userIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserId()));
        usernameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        fullNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole().getDisplayName()));
        createTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCreateTime() != null
                        ? data.getValue().getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : ""));
        lastLoginColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getLastLoginTime() != null
                        ? data.getValue().getLastLoginTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                        : "未登录"));

        // 操作列
        userActionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("编辑");
            private final Button deleteBtn = new Button("删除");
            private final HBox hBox = new HBox(10, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");
                hBox.setAlignment(Pos.CENTER);

                editBtn.setOnAction(e -> handleEditUser(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDeleteUser(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hBox);
            }
        });

        userTableView.setItems(userList);
    }

    /**
     * 初始化库存记录表格
     */
    private void initializeStockRecordTable() {
        recordIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecordId()));
        recordTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        recordProductColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        recordQuantityColumn
                .setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getQuantity())));
        recordOperatorColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getOperator()));
        recordTimeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTime()));
        recordRemarkColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRemark()));
    }

    /**
     * 更新产品下拉框
     */
    private void updateProductCombos() {
        ObservableList<String> productNames = FXCollections.observableArrayList();
        for (Product product : productList) {
            productNames.add(product.getProductName() + " (" + product.getProductId() + ")");
        }
        stockInProductCombo.setItems(productNames);
        stockOutProductCombo.setItems(productNames);
    }

    /**
     * 更新统计数据
     */
    private void updateStatistics() {
        totalProductsText.setText(String.valueOf(productList.size()));

        long lowStockCount = productList.stream()
                .filter(Product::needsStockAlert)
                .count();
        lowStockText.setText(String.valueOf(lowStockCount));

        totalUsersText.setText(String.valueOf(userList.size()));
    }

    /**
     * 生成批次号
     */
    private String generateBatchNumber() {
        return "BATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    // ==================== 菜单切换 ====================

    @FXML
    void showProductManagement(ActionEvent event) {
        productPane.setVisible(true);
        stockPane.setVisible(false);
        userPane.setVisible(false);
        reportPane.setVisible(false);
    }

    @FXML
    void showStockManagement(ActionEvent event) {
        productPane.setVisible(false);
        stockPane.setVisible(true);
        userPane.setVisible(false);
        reportPane.setVisible(false);
    }

    @FXML
    void showUserManagement(ActionEvent event) {
        productPane.setVisible(false);
        stockPane.setVisible(false);
        userPane.setVisible(true);
        reportPane.setVisible(false);
    }

    @FXML
    void showReportManagement(ActionEvent event) {
        productPane.setVisible(false);
        stockPane.setVisible(false);
        userPane.setVisible(false);
        reportPane.setVisible(true);
        updateStatistics();
    }

    // ==================== 产品管理功能 ====================

    @FXML
    void handleAddProduct(ActionEvent event) {
        showProductDialog(null);
    }

    @FXML
    void handleEditProduct(Product product) {
        if (product == null)
            return;
        showProductDialog(product);
    }

    @FXML
    void handleDeleteProduct(Product product) {
        if (product == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("确定要删除产品: " + product.getProductName() + "?");
        confirm.setContentText("此操作不可撤销！");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    Message msg = socketClient.sendAndReceive(
                            new Message(Message.MessageType.PRODUCT_DELETE, product.getProductId()));

                    Platform.runLater(() -> {
                        if (msg.isSuccess()) {
                            productList.remove(product);
                            showAlert("成功", "产品删除成功", Alert.AlertType.INFORMATION);
                            updateStatistics();
                        } else {
                            showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }).start();
            }
        });
    }

    @FXML
    void handleSearchProduct(ActionEvent event) {
        if (filteredProducts == null)
            return;

        String keyword = productSearchField.getText().trim();
        if (keyword.isEmpty()) {
            quickSearchPredicate = product -> true;
        } else {
            quickSearchPredicate = product -> product != null && (product.getProductName().contains(keyword) ||
                    product.getProductId().contains(keyword) ||
                    (product.getCategory() != null && product.getCategory().contains(keyword)));
        }

        refreshProductFilter();
    }

    @FXML
    void handleRefreshProducts(ActionEvent event) {
        quickSearchPredicate = product -> true;
        advancedProductPredicate = product -> true;
        refreshProductFilter();
        productSearchField.clear();
        loadDataFromServer();
    }

    /**
     * 显示商品添加/编辑对话框
     */
    private void showProductDialog(Product product) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "添加商品" : "编辑商品");
        dialog.setHeaderText(product == null ? "请填写商品信息" : "编辑商品: " + product.getProductName());

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField productIdField = new TextField();
        productIdField.setPromptText("自动生成");
        productIdField.setEditable(false);
        if (product != null)
            productIdField.setText(product.getProductId());

        TextField nameField = new TextField();
        nameField.setPromptText("商品名称");
        if (product != null)
            nameField.setText(product.getProductName());

        TextField categoryField = new TextField();
        categoryField.setPromptText("商品类别");
        if (product != null)
            categoryField.setText(product.getCategory());

        TextField specField = new TextField();
        specField.setPromptText("规格型号");
        if (product != null)
            specField.setText(product.getSpecification());

        TextField unitField = new TextField();
        unitField.setPromptText("单位");
        if (product != null)
            unitField.setText(product.getUnit());

        TextField priceField = new TextField();
        priceField.setPromptText("采购价格");
        if (product != null)
            priceField.setText(String.valueOf(product.getPurchasePrice()));

        TextField sellingPriceField = new TextField();
        sellingPriceField.setPromptText("销售价格");
        if (product != null)
            sellingPriceField.setText(String.valueOf(product.getSellingPrice()));

        TextField minStockField = new TextField();
        minStockField.setPromptText("最小库存");
        if (product != null)
            minStockField.setText(String.valueOf(product.getMinStock()));

        TextField maxStockField = new TextField();
        maxStockField.setPromptText("最大库存");
        if (product != null)
            maxStockField.setText(String.valueOf(product.getMaxStock()));

        TextField supplierField = new TextField();
        supplierField.setPromptText("供应商");
        if (product != null)
            supplierField.setText(product.getSupplier());

        TextArea descArea = new TextArea();
        descArea.setPromptText("商品描述");
        descArea.setPrefRowCount(3);
        if (product != null)
            descArea.setText(product.getDescription());

        grid.add(new Label("商品编号:"), 0, 0);
        grid.add(productIdField, 1, 0);
        grid.add(new Label("商品名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("商品类别:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("规格型号:"), 0, 3);
        grid.add(specField, 1, 3);
        grid.add(new Label("单位:"), 0, 4);
        grid.add(unitField, 1, 4);
        grid.add(new Label("采购价格:"), 0, 5);
        grid.add(priceField, 1, 5);
        grid.add(new Label("销售价格:"), 0, 6);
        grid.add(sellingPriceField, 1, 6);
        grid.add(new Label("最小库存:"), 0, 7);
        grid.add(minStockField, 1, 7);
        grid.add(new Label("最大库存:"), 0, 8);
        grid.add(maxStockField, 1, 8);
        grid.add(new Label("供应商:"), 0, 9);
        grid.add(supplierField, 1, 9);
        grid.add(new Label("商品描述:"), 0, 10);
        grid.add(descArea, 1, 10);

        dialog.getDialogPane().setContent(grid);

        // 添加输入验证
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty() ||
                    categoryField.getText().trim().isEmpty() ||
                    unitField.getText().trim().isEmpty()) {
                showAlert("错误", "商品名称、类别和单位不能为空", Alert.AlertType.ERROR);
                event.consume(); // 阻止对话框关闭
                return;
            }

            try {
                Double.parseDouble(priceField.getText().trim());
                Double.parseDouble(sellingPriceField.getText().trim());
                Integer.parseInt(minStockField.getText().trim());
                Integer.parseInt(maxStockField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert("错误", "请输入有效的数字", Alert.AlertType.ERROR);
                event.consume(); // 阻止对话框关闭
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Product p = product != null ? product : new Product();
                    if (product == null) {
                        p.setProductId("P" + System.currentTimeMillis());
                        p.setCurrentStock(0);
                    }
                    p.setProductName(nameField.getText().trim());
                    p.setCategory(categoryField.getText().trim());
                    p.setSpecification(specField.getText().trim());
                    p.setUnit(unitField.getText().trim());
                    p.setPurchasePrice(Double.parseDouble(priceField.getText().trim()));
                    p.setSellingPrice(Double.parseDouble(sellingPriceField.getText().trim()));
                    p.setMinStock(Integer.parseInt(minStockField.getText().trim()));
                    p.setMaxStock(Integer.parseInt(maxStockField.getText().trim()));
                    p.setSupplier(supplierField.getText().trim());
                    p.setDescription(descArea.getText().trim());
                    return p;
                } catch (NumberFormatException e) {
                    showAlert("错误", "请输入有效的数字", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            if (p != null) {
                new Thread(() -> {
                    Message.MessageType type = product == null ? Message.MessageType.PRODUCT_ADD
                            : Message.MessageType.PRODUCT_UPDATE;
                    Message msg = socketClient.sendAndReceive(new Message(type, p));

                    Platform.runLater(() -> {
                        if (msg.isSuccess()) {
                            if (product == null) {
                                productList.add(p);
                            } else {
                                int index = productList.indexOf(product);
                                if (index >= 0)
                                    productList.set(index, p);
                            }
                            showAlert("成功", product == null ? "商品添加成功" : "商品更新成功",
                                    Alert.AlertType.INFORMATION);
                            loadDataFromServer();
                        } else {
                            showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }).start();
            }
        });
    }

    // ==================== 库存管理功能 ====================

    @FXML
    void handleStockIn(ActionEvent event) {
        String productSelection = stockInProductCombo.getValue();
        String quantityStr = stockInQuantityField.getText().trim();
        String batchNumber = batchNumberField.getText();
        String location = warehouseLocationField.getText().trim();
        String remark = stockInRemarkArea.getText().trim();

        if (productSelection == null || quantityStr.isEmpty() || location.isEmpty()) {
            showAlert("错误", "请填写完整的入库信息", Alert.AlertType.ERROR);
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                showAlert("错误", "入库数量必须大于0", Alert.AlertType.ERROR);
                return;
            }

            // 提取产品ID
            String productId = productSelection.substring(productSelection.lastIndexOf("(") + 1,
                    productSelection.lastIndexOf(")"));

            StockInRecord record = new StockInRecord();
            record.setRecordId("IN-" + System.currentTimeMillis());
            record.setProductId(productId);
            record.setQuantity(quantity);
            record.setBatchNumber(batchNumber);
            record.setWarehouseLocation(location);
            record.setOperatorId(currentUser.getUserId());
            record.setOperatorName(currentUser.getFullName());
            record.setTimestamp(LocalDateTime.now());
            record.setRemark(remark);

            new Thread(() -> {
                Message msg = socketClient.sendAndReceive(
                        new Message(Message.MessageType.STOCK_IN_ADD, record));

                Platform.runLater(() -> {
                    if (msg.isSuccess()) {
                        showAlert("成功", "入库登记成功", Alert.AlertType.INFORMATION);
                        handleClearStockInForm(null);
                        loadDataFromServer();
                    } else {
                        showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            showAlert("错误", "数量必须是有效的数字", Alert.AlertType.ERROR);
        }
    }

    @FXML
    void handleStockOut(ActionEvent event) {
        String productSelection = stockOutProductCombo.getValue();
        String quantityStr = stockOutQuantityField.getText().trim();
        String recipient = recipientField.getText().trim();
        String recipientDept = recipientDeptField.getText().trim();
        String remark = stockOutRemarkArea.getText().trim();

        if (productSelection == null || quantityStr.isEmpty() || recipient.isEmpty()) {
            showAlert("错误", "请填写完整的出库信息", Alert.AlertType.ERROR);
            return;
        }

        try {
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                showAlert("错误", "出库数量必须大于0", Alert.AlertType.ERROR);
                return;
            }

            String productId = productSelection.substring(productSelection.lastIndexOf("(") + 1,
                    productSelection.lastIndexOf(")"));

            StockOutRecord record = new StockOutRecord();
            record.setRecordId("OUT-" + System.currentTimeMillis());
            record.setProductId(productId);
            record.setQuantity(quantity);
            record.setRecipient(recipient);
            record.setRecipientDept(recipientDept);
            record.setOperatorId(currentUser.getUserId());
            record.setOperatorName(currentUser.getFullName());
            record.setTimestamp(LocalDateTime.now());
            record.setRemark(remark);

            new Thread(() -> {
                Message msg = socketClient.sendAndReceive(
                        new Message(Message.MessageType.STOCK_OUT_ADD, record));

                Platform.runLater(() -> {
                    if (msg.isSuccess()) {
                        showAlert("成功", "出库登记成功", Alert.AlertType.INFORMATION);
                        handleClearStockOutForm(null);
                        loadDataFromServer();
                    } else {
                        showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }).start();

        } catch (NumberFormatException e) {
            showAlert("错误", "数量必须是有效的数字", Alert.AlertType.ERROR);
        }
    }

    @FXML
    void handleClearStockInForm(ActionEvent event) {
        stockInProductCombo.setValue(null);
        stockInQuantityField.clear();
        batchNumberField.setText(generateBatchNumber());
        warehouseLocationField.clear();
        stockInRemarkArea.clear();
    }

    @FXML
    void handleClearStockOutForm(ActionEvent event) {
        stockOutProductCombo.setValue(null);
        stockOutQuantityField.clear();
        recipientField.clear();
        recipientDeptField.clear();
        stockOutRemarkArea.clear();
    }

    @FXML
    void handleRefreshStockRecords(ActionEvent event) {
        new Thread(() -> {
            Message msg = socketClient.sendAndReceive(
                    new Message(Message.MessageType.STOCK_RECORD_LIST));

            if (msg.isSuccess()) {
                List<?> rawList = (List<?>) msg.getData();
                List<StockRecord> records = new ArrayList<>();

                for (Object obj : rawList) {
                    if (obj instanceof com._404.wms.network.WMSServer.StockRecordDTO) {
                        com._404.wms.network.WMSServer.StockRecordDTO dto = (com._404.wms.network.WMSServer.StockRecordDTO) obj;
                        StockRecord record = new StockRecord();
                        record.setRecordId(dto.getRecordId());
                        record.setType(dto.getType());
                        record.setProductName(dto.getProductName());
                        record.setQuantity(dto.getQuantity());
                        record.setOperator(dto.getOperator());
                        record.setTime(dto.getTime());
                        record.setRemark(dto.getRemark());
                        records.add(record);
                    }
                }

                Platform.runLater(() -> {
                    stockRecordTableView.setItems(FXCollections.observableArrayList(records));
                });
            }
        }).start();
    }

    /**
     * 获取已被经理审批通过的采购订单，并可确认到货（将订单商品逐项入库）
     */
    @FXML
    void handleFetchApprovedOrders(ActionEvent event) {
        new Thread(() -> {
            Message msg = socketClient.sendAndReceive(new Message(Message.MessageType.PURCHASE_ORDER_LIST));

            if (!msg.isSuccess()) {
                Platform.runLater(() -> showAlert("错误", "获取订单失败: " + msg.getMessage(), Alert.AlertType.ERROR));
                return;
            }

            @SuppressWarnings("unchecked")
            List<PurchaseOrder> orders = (List<PurchaseOrder>) msg.getData();
            List<PurchaseOrder> approved = new ArrayList<>();
            for (PurchaseOrder o : orders) {
                if (o.getStatus() == PurchaseOrder.OrderStatus.APPROVED) {
                    approved.add(o);
                }
            }

            if (approved.isEmpty()) {
                Platform.runLater(() -> showAlert("提示", "当前没有已批准的采购订单", Alert.AlertType.INFORMATION));
                return;
            }

            // 选择要确认到货的订单：使用双表对话框显示订单列表和明细
            Platform.runLater(() -> {
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("已批准订单");
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                // 左侧：订单表
                TableView<PurchaseOrder> ordersTable = new TableView<>();
                TableColumn<PurchaseOrder, String> oidCol = new TableColumn<>("订单号");
                oidCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrderId()));
                TableColumn<PurchaseOrder, String> purchaserCol = new TableColumn<>("采购员");
                purchaserCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPurchaserName()));
                TableColumn<PurchaseOrder, String> supplierCol = new TableColumn<>("供应商");
                supplierCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSupplier()));
                TableColumn<PurchaseOrder, String> amountCol = new TableColumn<>("金额");
                amountCol.setCellValueFactory(
                        d -> new SimpleStringProperty(String.format("%.2f", d.getValue().getTotalAmount())));
                ordersTable.getColumns().addAll(oidCol, purchaserCol, supplierCol, amountCol);

                ObservableList<PurchaseOrder> approvedObs = FXCollections.observableArrayList(approved);
                ordersTable.setItems(approvedObs);
                ordersTable.getSelectionModel().selectFirst();

                // 右侧：明细表
                TableView<PurchaseOrder.OrderItem> itemsTable = new TableView<>();
                TableColumn<PurchaseOrder.OrderItem, String> itemNameCol = new TableColumn<>("商品名称");
                itemNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));
                TableColumn<PurchaseOrder.OrderItem, String> itemIdCol = new TableColumn<>("商品ID");
                itemIdCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductId()));
                TableColumn<PurchaseOrder.OrderItem, String> itemSpecCol = new TableColumn<>("规格");
                itemSpecCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpecification()));
                TableColumn<PurchaseOrder.OrderItem, String> itemQtyCol = new TableColumn<>("数量");
                itemQtyCol
                        .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
                TableColumn<PurchaseOrder.OrderItem, String> itemPriceCol = new TableColumn<>("单价");
                itemPriceCol.setCellValueFactory(
                        c -> new SimpleStringProperty(String.format("%.2f", c.getValue().getPrice())));
                itemsTable.getColumns().addAll(itemNameCol, itemIdCol, itemSpecCol, itemQtyCol, itemPriceCol);

                // 选中订单时更新明细
                ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null) {
                        itemsTable.setItems(FXCollections.observableArrayList(newV.getItems()));
                    } else {
                        itemsTable.setItems(FXCollections.observableArrayList());
                    }
                });

                // 初始显示第一个订单的明细
                if (!approvedObs.isEmpty()) {
                    itemsTable.setItems(FXCollections.observableArrayList(approvedObs.get(0).getItems()));
                }

                HBox content = new HBox(10, ordersTable, itemsTable);
                content.setPrefSize(900, 400);
                dialog.getDialogPane().setContent(content);

                dialog.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        PurchaseOrder selected = ordersTable.getSelectionModel().getSelectedItem();
                        if (selected == null)
                            return;

                        // 确认到货并为每项创建入库记录
                        new Thread(() -> {
                            for (PurchaseOrder.OrderItem item : selected.getItems()) {
                                StockInRecord in = new StockInRecord();
                                in.setRecordId("IN-" + System.currentTimeMillis() + "-" + item.getProductId());
                                in.setProductId(item.getProductId());
                                in.setQuantity(item.getQuantity());
                                in.setBatchNumber(generateBatchNumber());
                                in.setWarehouseLocation("采购入库");
                                in.setOperatorId(currentUser.getUserId());
                                in.setOperatorName(currentUser.getFullName());
                                in.setTimestamp(java.time.LocalDateTime.now());
                                in.setRemark("采购订单入库: " + selected.getOrderId());

                                Message inMsg = socketClient
                                        .sendAndReceive(new Message(Message.MessageType.STOCK_IN_ADD, in));
                                if (!inMsg.isSuccess()) {
                                    final String err = inMsg.getMessage();
                                    Platform.runLater(() -> showAlert("错误", "入库失败: " + err, Alert.AlertType.ERROR));
                                    return;
                                }
                            }

                            Message confirmMsg = socketClient.sendAndReceive(
                                    new Message(Message.MessageType.PURCHASE_ORDER_ARRIVAL_CONFIRM,
                                            selected.getOrderId()));
                            if (confirmMsg.isSuccess()) {
                                Platform.runLater(() -> {
                                    showAlert("成功", "已确认到货并更新库存", Alert.AlertType.INFORMATION);
                                    loadDataFromServer();
                                });
                            } else {
                                Platform.runLater(() -> showAlert("错误", "确认到货失败: " + confirmMsg.getMessage(),
                                        Alert.AlertType.ERROR));
                            }
                        }).start();
                    }
                });
            });
        }).start();
    }

    /**
     * 查看并导出操作日志
     */
    @FXML
    void handleViewLogs(ActionEvent event) {
        new Thread(() -> {
            Message msg = socketClient.sendAndReceive(new Message(Message.MessageType.LOG_LIST));
            if (!msg.isSuccess()) {
                Platform.runLater(() -> showAlert("错误", "获取日志失败: " + msg.getMessage(), Alert.AlertType.ERROR));
                return;
            }

            @SuppressWarnings("unchecked")
            List<OperationLog> logs = (List<OperationLog>) msg.getData();

            Platform.runLater(() -> {
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("操作日志");
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

                StringBuilder sb = new StringBuilder();
                for (OperationLog log : logs) {
                    sb.append(String.format("[%s] %s - %s - %s - %s\n",
                            log.getOperationTime(), log.getUsername(), log.getModule(), log.getOperation(),
                            log.getDetails()));
                }

                TextArea area = new TextArea(sb.toString());
                area.setEditable(false);
                area.setWrapText(true);
                area.setPrefWidth(800);
                area.setPrefHeight(600);
                dialog.getDialogPane().setContent(area);

                // 添加导出按钮
                ButtonType exportType = new ButtonType("导出CSV", ButtonBar.ButtonData.OTHER);
                dialog.getDialogPane().getButtonTypes().add(exportType);

                dialog.showAndWait().ifPresent(bt -> {
                    if (bt == exportType) {
                        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                        fc.setTitle("导出日志");
                        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
                        java.io.File file = fc.showSaveDialog(reportPane.getScene().getWindow());
                        if (file != null) {
                            try (java.io.PrintWriter writer = new java.io.PrintWriter(file, "UTF-8")) {
                                writer.println("时间,用户,模块,操作,详情");
                                for (OperationLog log : logs) {
                                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                                            log.getOperationTime(), log.getUsername(), log.getModule(),
                                            log.getOperation(), log.getDetails());
                                }
                                showAlert("成功", "日志已导出: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
                            } catch (Exception e) {
                                showAlert("错误", "导出失败: " + e.getMessage(), Alert.AlertType.ERROR);
                            }
                        }
                    }
                });
            });
        }).start();
    }

    // ==================== 用户管理功能 ====================

    @FXML
    void handleAddUser(ActionEvent event) {
        showAlert("提示", "添加用户功能开发中", Alert.AlertType.INFORMATION);
    }

    @FXML
    void handleEditUser(User user) {
        if (user == null)
            return;
        showAlert("提示", "编辑用户功能开发中", Alert.AlertType.INFORMATION);
    }

    @FXML
    void handleDeleteUser(User user) {
        if (user == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText("确定要删除用户: " + user.getUsername() + "?");
        confirm.setContentText("此操作不可撤销！");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                new Thread(() -> {
                    Message msg = socketClient.sendAndReceive(
                            new Message(Message.MessageType.USER_DELETE, user.getUserId()));

                    Platform.runLater(() -> {
                        if (msg.isSuccess()) {
                            userList.remove(user);
                            showAlert("成功", "用户删除成功", Alert.AlertType.INFORMATION);
                            updateStatistics();
                        } else {
                            showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }).start();
            }
        });
    }

    // ==================== 报表功能 ====================

    @FXML
    void handleExportReport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出报表");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"),
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(reportPane.getScene().getWindow());
        if (file != null) {
            showAlert("提示", "报表导出功能开发中\n将导出到: " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
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
                // 禁用按钮防止重复点击
                if (logoutButton != null) {
                    logoutButton.setDisable(true);
                }

                // 在后台线程执行网络操作，避免阻塞 UI 线程
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
                            // 获取当前窗口并关闭
                            if (logoutButton != null && logoutButton.getScene() != null) {
                                Stage stage = (Stage) logoutButton.getScene().getWindow();
                                if (stage != null) {
                                    stage.close();
                                }
                            }

                            // 重新打开登录界面
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

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // 内部类：库存记录（用于显示）
    public static class StockRecord {
        private String recordId;
        private String type;
        private String productName;
        private int quantity;
        private String operator;
        private String time;
        private String remark;

        // Getters and Setters
        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    // ==================== 高级筛选功能 ====================

    /**
     * 高级筛选功能：按类别和库存范围筛选
     */
    @FXML
    void handleAdvancedFilter(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("高级筛选");
        dialog.setHeaderText("按条件筛选商品");

        ButtonType filterButtonType = new ButtonType("筛选", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(filterButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().add("全部类别");
        categoryCombo.getItems().addAll(
                productList.stream()
                        .map(Product::getCategory)
                        .filter(c -> c != null && !c.isEmpty())
                        .distinct()
                        .sorted()
                        .toList());
        categoryCombo.setValue("全部类别");

        TextField minStockField = new TextField();
        minStockField.setPromptText("最小库存下限");

        TextField maxStockField = new TextField();
        maxStockField.setPromptText("最大库存上限");

        CheckBox lowStockOnly = new CheckBox("仅显示低库存商品");
        CheckBox overStockOnly = new CheckBox("仅显示超库存商品");

        grid.add(new Label("商品类别:"), 0, 0);
        grid.add(categoryCombo, 1, 0);
        grid.add(new Label("当前库存下限:"), 0, 1);
        grid.add(minStockField, 1, 1);
        grid.add(new Label("当前库存上限:"), 0, 2);
        grid.add(maxStockField, 1, 2);
        grid.add(lowStockOnly, 0, 3, 2, 1);
        grid.add(overStockOnly, 0, 4, 2, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == filterButtonType) {
                String category = categoryCombo.getValue();
                String minStr = minStockField.getText().trim();
                String maxStr = maxStockField.getText().trim();

                Integer minValue = null;
                Integer maxValue = null;
                try {
                    if (!minStr.isEmpty()) {
                        minValue = Integer.parseInt(minStr);
                    }
                } catch (NumberFormatException ignored) {
                }

                try {
                    if (!maxStr.isEmpty()) {
                        maxValue = Integer.parseInt(maxStr);
                    }
                } catch (NumberFormatException ignored) {
                }

                final Integer minThreshold = minValue;
                final Integer maxThreshold = maxValue;
                final boolean lowStock = lowStockOnly.isSelected();
                final boolean overStock = overStockOnly.isSelected();
                final String selectedCategory = category;

                advancedProductPredicate = product -> {
                    if (product == null)
                        return false;

                    if (!"全部类别".equals(selectedCategory)) {
                        if (product.getCategory() == null || !product.getCategory().equals(selectedCategory)) {
                            return false;
                        }
                    }

                    if (minThreshold != null && product.getCurrentStock() < minThreshold) {
                        return false;
                    }
                    if (maxThreshold != null && product.getCurrentStock() > maxThreshold) {
                        return false;
                    }
                    if (lowStock && !product.needsStockAlert()) {
                        return false;
                    }
                    if (overStock && !product.isOverStock()) {
                        return false;
                    }
                    return true;
                };

                refreshProductFilter();
                showAlert("筛选结果", "找到 " + (filteredProducts != null ? filteredProducts.size() : 0) + " 个匹配的商品",
                        Alert.AlertType.INFORMATION);
            }
        });
    }

    // ==================== 库存调整功能 ====================

    /**
     * 库存调整功能：盘点和修正
     */
    @FXML
    void handleStockAdjustment(ActionEvent event) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("库存调整");
        dialog.setHeaderText("库存盘点与调整");

        ButtonType adjustButtonType = new ButtonType("调整", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adjustButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        ComboBox<String> productCombo = new ComboBox<>();
        productList.forEach(p -> {
            productCombo.getItems().add(p.getProductName() + " (" + p.getProductId() + ")");
        });

        Label currentStockLabel = new Label("当前库存: --");
        TextField actualStockField = new TextField();
        actualStockField.setPromptText("实际盘点数量");

        Label differenceLabel = new Label("差异: --");

        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll("盘点调整", "损耗", "盘盈", "系统错误修正", "其他");
        reasonCombo.setValue("盘点调整");

        TextArea remarkArea = new TextArea();
        remarkArea.setPromptText("调整原因说明");
        remarkArea.setPrefRowCount(3);

        // 选择商品时更新当前库存
        productCombo.setOnAction(e -> {
            String selection = productCombo.getValue();
            if (selection != null) {
                String productId = selection.substring(selection.lastIndexOf("(") + 1,
                        selection.lastIndexOf(")"));
                productList.stream()
                        .filter(p -> p.getProductId().equals(productId))
                        .findFirst()
                        .ifPresent(p -> {
                            currentStockLabel.setText("当前库存: " + p.getCurrentStock() + " " +
                                    (p.getUnit() != null ? p.getUnit() : ""));
                            actualStockField.setText(String.valueOf(p.getCurrentStock()));
                        });
            }
        });

        // 输入实际数量时计算差异
        actualStockField.textProperty().addListener((obs, old, newVal) -> {
            String selection = productCombo.getValue();
            if (selection != null && !newVal.trim().isEmpty()) {
                try {
                    int actualStock = Integer.parseInt(newVal.trim());
                    String productId = selection.substring(selection.lastIndexOf("(") + 1,
                            selection.lastIndexOf(")"));
                    productList.stream()
                            .filter(p -> p.getProductId().equals(productId))
                            .findFirst()
                            .ifPresent(p -> {
                                int difference = actualStock - p.getCurrentStock();
                                differenceLabel.setText("差异: " + (difference >= 0 ? "+" : "") +
                                        difference + " " + (p.getUnit() != null ? p.getUnit() : ""));
                                differenceLabel.setStyle(difference == 0 ? ""
                                        : (difference > 0 ? "-fx-text-fill: green;" : "-fx-text-fill: red;"));
                            });
                } catch (NumberFormatException ex) {
                    differenceLabel.setText("差异: --");
                }
            }
        });

        grid.add(new Label("选择商品:"), 0, 0);
        grid.add(productCombo, 1, 0);
        grid.add(currentStockLabel, 0, 1, 2, 1);
        grid.add(new Label("实际数量:"), 0, 2);
        grid.add(actualStockField, 1, 2);
        grid.add(differenceLabel, 0, 3, 2, 1);
        grid.add(new Label("调整原因:"), 0, 4);
        grid.add(reasonCombo, 1, 4);
        grid.add(new Label("详细说明:"), 0, 5);
        grid.add(remarkArea, 1, 5);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == adjustButtonType) {
                String selection = productCombo.getValue();
                String actualStockStr = actualStockField.getText().trim();

                if (selection == null || actualStockStr.isEmpty()) {
                    showAlert("错误", "请选择商品并输入实际数量", Alert.AlertType.ERROR);
                    return;
                }

                try {
                    int actualStock = Integer.parseInt(actualStockStr);
                    String productId = selection.substring(selection.lastIndexOf("(") + 1,
                            selection.lastIndexOf(")"));

                    Product product = productList.stream()
                            .filter(p -> p.getProductId().equals(productId))
                            .findFirst()
                            .orElse(null);

                    if (product == null) {
                        showAlert("错误", "未找到选中的商品", Alert.AlertType.ERROR);
                        return;
                    }

                    int difference = actualStock - product.getCurrentStock();

                    if (difference == 0) {
                        showAlert("提示", "库存数量无变化，无需调整", Alert.AlertType.INFORMATION);
                        return;
                    }

                    // 创建调整记录
                    Map<String, Object> adjustmentData = new HashMap<>();
                    adjustmentData.put("productId", productId);
                    adjustmentData.put("oldStock", product.getCurrentStock());
                    adjustmentData.put("newStock", actualStock);
                    adjustmentData.put("difference", difference);
                    adjustmentData.put("reason", reasonCombo.getValue());
                    adjustmentData.put("remark", remarkArea.getText().trim());
                    adjustmentData.put("operatorId", currentUser.getUserId());
                    adjustmentData.put("operatorName", currentUser.getFullName());
                    adjustmentData.put("timestamp", LocalDateTime.now());

                    new Thread(() -> {
                        Message msg = socketClient.sendAndReceive(
                                new Message(Message.MessageType.STOCK_ADJUSTMENT, adjustmentData));

                        Platform.runLater(() -> {
                            if (msg.isSuccess()) {
                                showAlert("成功",
                                        "库存调整成功\n" +
                                                "商品: " + product.getProductName() + "\n" +
                                                "原库存: " + product.getCurrentStock() + "\n" +
                                                "新库存: " + actualStock + "\n" +
                                                "差异: " + (difference >= 0 ? "+" : "") + difference,
                                        Alert.AlertType.INFORMATION);
                                loadDataFromServer();
                            } else {
                                showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                            }
                        });
                    }).start();

                } catch (NumberFormatException e) {
                    showAlert("错误", "请输入有效的数字", Alert.AlertType.ERROR);
                }
            }
        });
    }
}
