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

/**
 * 仓库管理员主界面控制器 (WarehouseAdminController)
 * <p>
 * 这是系统的核心管理控制台，集成了四个主要模块：
 * 1. 商品管理 (CRUD, 高级筛选)
 * 2. 库存管理 (入库, 出库, 盘点, 采购单收货)
 * 3. 用户管理 (仅管理员可见)
 * 4. 报表与日志 (查看操作记录)
 * <p>
 * 该控制器采用了轮询机制 (Polling) 来保持与服务器数据的同步。
 */
public class WarehouseAdminController implements Initializable {

    // --- 出库相关控件 ---
    @FXML
    private ComboBox<String> stockOutProductCombo; // 出库产品选择
    @FXML
    private TextField stockOutQuantityField; // 出库数量
    @FXML
    private TextField recipientField; // 领用人
    @FXML
    private TextField recipientDeptField; // 领用部门
    @FXML
    private TextArea stockOutRemarkArea; // 出库备注

    // --- 库存记录/流水控件 ---
    @FXML
    private TableView<StockRecord> stockRecordTableView;
    @FXML
    private ComboBox<String> recordTypeCombo; // 筛选记录类型(入库/出库)
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

    // --- 用户管理控件 ---
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
    private TableColumn<User, Void> userActionColumn; // 用户操作列(编辑/删除)

    // --- 仪表盘统计控件 ---
    @FXML
    private Text totalProductsText; // 商品总数
    @FXML
    private Text lowStockText; // 低库存预警数
    @FXML
    private Text totalUsersText; // 总用户数

    // --- 核心数据模型与服务 ---
    private User currentUser;
    private SocketClient socketClient;

    // ObservableList 自动绑定到 UI，数据变化时表格会自动刷新
    private ObservableList<Product> productList = FXCollections.observableArrayList();
    private FilteredList<Product> filteredProducts; // 包装列表，用于实现搜索和筛选

    // 组合筛选条件：快速搜索 + 高级筛选
    private Predicate<Product> quickSearchPredicate = product -> true;
    private Predicate<Product> advancedProductPredicate = product -> true;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private ObservableList<StockRecord> stockRecordList = FXCollections.observableArrayList();
    private ObservableList<PurchaseOrder> allOrderList = FXCollections.observableArrayList();

    @FXML
    private Label userInfoLabel; // 顶部用户信息展示
    @FXML
    private Button logoutButton;

    // --- 页面容器 (用于切换 Tab) ---
    @FXML
    private Pane productPane;
    @FXML
    private Pane stockPane;
    @FXML
    private Pane userPane;
    @FXML
    private Pane reportPane;

    // --- 产品表格控件 ---
    @FXML
    private TextField productSearchField; // 快速搜索框
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

    // --- 入库/操作控件 ---
    @FXML
    private TextField batchNumberField; // 批次号(自动生成)
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
    private TextField warehouseLocationField; // 库位

    /**
     * JavaFX 初始化方法，FXML 加载完成后自动调用。
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化各个表格的列映射和 CellFactory
        initializeProductTable();
        initializeUserTable();
        initializeStockRecordTable();

        // 界面初始化设置
        batchNumberField.setText(generateBatchNumber());
        batchNumberField.setEditable(false); // 批次号由系统生成，不可手动修改

        recordTypeCombo.setItems(FXCollections.observableArrayList("全部", "入库", "出库"));
        recordTypeCombo.setValue("全部");
    }

    /**
     * 设置当前登录用户，并启动后台数据同步。
     * 
     * @param user 登录成功的用户对象
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.socketClient = LoginController.getSocketClient();
        userInfoLabel.setText("当前用户: " + user.getFullName() + " (" + user.getRole().getDisplayName() + ")");

        // 1. 立即加载一次数据
        loadDataFromServer();

        // 2. 启动后台轮询线程 (Polling)
        // 目的：即使没有 WebSocket 推送，也能定期（每5秒）获取最新的库存、订单和用户变动。
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
     * 核心数据加载方法。
     * 在后台线程中请求数据，收到响应后通过 Platform.runLater 更新 UI。
     * 防止阻塞 JavaFX 主线程 (UI Freeze)。
     */
    private void loadDataFromServer() {
        new Thread(() -> {
            // 1. 加载产品列表
            Message response = socketClient.sendAndReceive(new Message(Message.MessageType.PRODUCT_LIST));
            if (response.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Product> products = (List<Product>) response.getData();
                Platform.runLater(() -> {
                    productList.clear();
                    productList.addAll(products);
                    updateProductCombos(); // 更新入库/出库下拉框的选项
                    updateStatistics(); // 更新顶部统计数字
                });
            }

            // 2. 加载用户列表
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

            // 3. 加载采购订单（用于仓库收货流程）
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
     * 初始化产品表格及自定义操作列（编辑/删除按钮）。
     */
    private void initializeProductTable() {
        productIdColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductId()));
        productNameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProductName()));
        // ... 其他简单属性绑定 ...
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

        // 自定义操作列：添加“编辑”和“删除”按钮
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

        // 绑定 FilteredList 以支持搜索和筛选
        filteredProducts = new FilteredList<>(productList, product -> true);
        productTableView.setItems(filteredProducts);
    }

    /**
     * 刷新产品列表的过滤器。
     * 将“快速搜索”和“高级筛选”的条件进行逻辑与 (AND) 操作。
     */
    private void refreshProductFilter() {
        if (filteredProducts != null) {
            filteredProducts.setPredicate(product -> quickSearchPredicate.test(product)
                    && advancedProductPredicate.test(product));
        }
    }

    // ... (User Table Initialization, similar to Product Table) ...
    private void initializeUserTable() {
        // ... (省略了常规列绑定代码，与 initializeProductTable 逻辑一致) ...
        // 绑定各列数据...
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

        // 用户操作列
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

    // ... (StockRecord Table Initialization) ...
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
     * 更新入库/出库表单中的下拉框选项。
     * 格式: 商品名称 (ID)
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
     * 更新顶部仪表盘的统计数据。
     */
    private void updateStatistics() {
        totalProductsText.setText(String.valueOf(productList.size()));

        // 计算低库存商品：调用 Product 实体中的业务逻辑判断
        long lowStockCount = productList.stream()
                .filter(Product::needsStockAlert)
                .count();
        lowStockText.setText(String.valueOf(lowStockCount));

        totalUsersText.setText(String.valueOf(userList.size()));
    }

    /**
     * 生成基于时间戳的唯一批次号。
     */
    private String generateBatchNumber() {
        return "BATCH-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    // ==================== 菜单切换逻辑 (使用 visibility 控制 Pane) ====================

    @FXML
    void showProductManagement(ActionEvent event) {
        productPane.setVisible(true);
        stockPane.setVisible(false);
        userPane.setVisible(false);
        reportPane.setVisible(false);
    }

    // ... (其他切换方法略) ...
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
        showProductDialog(null); // null 表示添加模式
    }

    @FXML
    void handleEditProduct(Product product) {
        if (product == null)
            return;
        showProductDialog(product); // 传入对象表示编辑模式
    }

    /**
     * 处理产品删除请求。
     * 发送删除指令到服务器 -> 接收响应 -> 更新本地列表。
     */
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

    /**
     * 快速搜索功能。
     * 根据输入框内容过滤 ID、名称或类别。
     */
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

    /**
     * 重置所有筛选条件并刷新数据。
     */
    @FXML
    void handleRefreshProducts(ActionEvent event) {
        quickSearchPredicate = product -> true;
        advancedProductPredicate = product -> true;
        refreshProductFilter();
        productSearchField.clear();
        loadDataFromServer();
    }

    /**
     * 显示添加/编辑商品的弹窗。
     * 这是一个较复杂的 Dialog，包含多个 TextField 和输入验证逻辑。
     * 
     * @param product 待编辑的商品，若为 null 则为添加模式。
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

        // ... (构建 Dialog UI 控件的代码，省略部分重复的 TextField 设置) ...
        TextField productIdField = new TextField();
        productIdField.setPromptText("自动生成");
        productIdField.setEditable(false);
        if (product != null)
            productIdField.setText(product.getProductId());

        TextField nameField = new TextField();
        // ... (其他字段初始化: name, category, price, stock 等) ...
        nameField.setPromptText("商品名称");
        if (product != null)
            nameField.setText(product.getProductName());

        TextField categoryField = new TextField();
        categoryField.setPromptText("商品类别");
        if (product != null)
            categoryField.setText(product.getCategory());

        TextField specField = new TextField();
        if (product != null)
            specField.setText(product.getSpecification());

        TextField unitField = new TextField();
        if (product != null)
            unitField.setText(product.getUnit());

        TextField priceField = new TextField();
        if (product != null)
            priceField.setText(String.valueOf(product.getPurchasePrice()));

        TextField sellingPriceField = new TextField();
        if (product != null)
            sellingPriceField.setText(String.valueOf(product.getSellingPrice()));

        TextField minStockField = new TextField();
        if (product != null)
            minStockField.setText(String.valueOf(product.getMinStock()));

        TextField maxStockField = new TextField();
        if (product != null)
            maxStockField.setText(String.valueOf(product.getMaxStock()));

        TextField supplierField = new TextField();
        if (product != null)
            supplierField.setText(product.getSupplier());

        TextArea descArea = new TextArea();
        if (product != null)
            descArea.setText(product.getDescription());

        // 布局添加到 Grid
        grid.add(new Label("商品编号:"), 0, 0);
        grid.add(productIdField, 1, 0);
        grid.add(new Label("商品名称:"), 0, 1);
        grid.add(nameField, 1, 1);
        // ... (其他 grid.add) ...
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

        // --- 输入验证逻辑 ---
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            // 必填项校验
            String name = nameField.getText();
            String category = categoryField.getText();
            String unit = unitField.getText();

            if (name == null || name.trim().isEmpty() ||
                    category == null || category.trim().isEmpty() ||
                    unit == null || unit.trim().isEmpty()) {
                showAlert("错误", "商品名称、类别和单位不能为空", Alert.AlertType.ERROR);
                event.consume(); // 阻止对话框关闭
                return;
            }

            // 数字格式校验
            try {
                Double.parseDouble(priceField.getText().trim());
                Double.parseDouble(sellingPriceField.getText().trim());
                Integer.parseInt(minStockField.getText().trim());
                Integer.parseInt(maxStockField.getText().trim());
            } catch (NumberFormatException e) {
                showAlert("错误", "价格和库存字段必须是有效的数字", Alert.AlertType.ERROR);
                event.consume();
            }
        });

        // 结果转换器：将输入转换为 Product 对象
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Product p = product != null ? product : new Product();
                    if (product == null) {
                        p.setProductId("P" + System.currentTimeMillis());
                        p.setCurrentStock(0); // 新商品默认库存为0
                    }
                    p.setProductName(getTextSafe(nameField));
                    p.setCategory(getTextSafe(categoryField));
                    // ... 设置其他属性 ...
                    p.setSpecification(getTextSafe(specField));
                    p.setUnit(getTextSafe(unitField));
                    p.setPurchasePrice(Double.parseDouble(getTextSafe(priceField)));
                    p.setSellingPrice(Double.parseDouble(getTextSafe(sellingPriceField)));
                    p.setMinStock(Integer.parseInt(getTextSafe(minStockField)));
                    p.setMaxStock(Integer.parseInt(getTextSafe(maxStockField)));
                    p.setSupplier(getTextSafe(supplierField));
                    p.setDescription(descArea.getText() != null ? descArea.getText().trim() : "");
                    return p;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return null;
        });

        // 处理保存结果：发送网络请求
        dialog.showAndWait().ifPresent(p -> {
            if (p != null) {
                new Thread(() -> {
                    Message.MessageType type = product == null ? Message.MessageType.PRODUCT_ADD
                            : Message.MessageType.PRODUCT_UPDATE;
                    Message msg = socketClient.sendAndReceive(new Message(type, p));

                    Platform.runLater(() -> {
                        if (msg.isSuccess()) {
                            // 乐观更新 UI
                            if (product == null) {
                                productList.add(p);
                            } else {
                                int index = productList.indexOf(product);
                                if (index >= 0)
                                    productList.set(index, p);
                            }
                            showAlert("成功", product == null ? "商品添加成功" : "商品更新成功",
                                    Alert.AlertType.INFORMATION);
                            loadDataFromServer(); // 再次从服务器拉取以确保一致性
                        } else {
                            showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                        }
                    });
                }).start();
            }
        });
    }

    // ==================== 库存管理功能 ====================

    /**
     * 处理“手动入库”操作。
     */
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

            // 从 "Name (ID)" 格式中提取 ID
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

    /**
     * 处理“手动出库”操作。
     */
    @FXML
    void handleStockOut(ActionEvent event) {
        // ... (逻辑与 handleStockIn 类似，构建 StockOutRecord 并发送) ...
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

    // ... (handleClearStockOutForm) ...
    @FXML
    void handleClearStockOutForm(ActionEvent event) {
        stockOutProductCombo.setValue(null);
        stockOutQuantityField.clear();
        recipientField.clear();
        recipientDeptField.clear();
        stockOutRemarkArea.clear();
    }

    /**
     * 刷新库存记录流水表。
     */
    @FXML
    void handleRefreshStockRecords(ActionEvent event) {
        new Thread(() -> {
            Message msg = socketClient.sendAndReceive(
                    new Message(Message.MessageType.STOCK_RECORD_LIST));

            if (msg.isSuccess()) {
                // 处理服务器返回的 DTO 数据并转换为本地显示对象
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
     * 核心业务逻辑：采购订单收货。
     * 1. 获取所有状态为 "APPROVED" 的采购订单。
     * 2. 展示订单及其明细。
     * 3. 用户确认收货后，系统自动为每项商品创建“入库记录”。
     * 4. 更新订单状态为 "COMPLETED/ARRIVED"。
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

            // --- 显示选择订单的对话框 ---
            Platform.runLater(() -> {
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("已批准订单");
                dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

                // 左侧：订单列表
                TableView<PurchaseOrder> ordersTable = new TableView<>();
                // ... (创建订单表格列: ID, 采购员, 供应商, 金额) ...
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

                // 右侧：明细列表
                TableView<PurchaseOrder.OrderItem> itemsTable = new TableView<>();
                // ... (创建明细表格列: 名称, ID, 规格, 数量, 单价) ...
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

                // 联动：选中订单显示对应明细
                ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
                    if (newV != null) {
                        itemsTable.setItems(FXCollections.observableArrayList(newV.getItems()));
                    } else {
                        itemsTable.setItems(FXCollections.observableArrayList());
                    }
                });

                // 初始化显示第一个订单的明细
                if (!approvedObs.isEmpty()) {
                    itemsTable.setItems(FXCollections.observableArrayList(approvedObs.get(0).getItems()));
                }

                HBox content = new HBox(10, ordersTable, itemsTable);
                content.setPrefSize(900, 400);
                dialog.getDialogPane().setContent(content);

                // --- 处理确认收货 ---
                dialog.showAndWait().ifPresent(bt -> {
                    if (bt == ButtonType.OK) {
                        PurchaseOrder selected = ordersTable.getSelectionModel().getSelectedItem();
                        if (selected == null)
                            return;

                        new Thread(() -> {
                            // 1. 为每个明细项创建入库记录
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
                                    return; // 中断流程
                                }
                            }

                            // 2. 发送确认到货消息，更新订单状态
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

    // ... (View Logs Logic) ...
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

                // 导出 CSV 功能
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

    // ==================== 用户管理功能 (仅管理员) ====================

    @FXML
    void handleAddUser(ActionEvent event) {
        // ... (构建用户添加 Dialog 的 UI 代码，包含 roleCombo 映射 UserRole 枚举) ...
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("添加用户");
        dialog.setHeaderText("请填写新用户信息");
        ButtonType confirmButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField userIdField = new TextField();
        userIdField.setPromptText("用户ID（如：U005）");
        TextField usernameField = new TextField();
        usernameField.setPromptText("用户名");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("密码");
        TextField realNameField = new TextField();
        realNameField.setPromptText("真实姓名");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("仓库管理员", "采购员", "部门经理", "总经理"));
        roleCombo.setValue("采购员");

        grid.add(new Label("用户ID:"), 0, 0);
        grid.add(userIdField, 1, 0);
        grid.add(new Label("用户名:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("密码:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("真实姓名:"), 0, 3);
        grid.add(realNameField, 1, 3);
        grid.add(new Label("角色:"), 0, 4);
        grid.add(roleCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                // ... (获取输入并进行非空校验) ...
                String userId = userIdField.getText().trim();
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();
                String realName = realNameField.getText().trim();
                String roleStr = roleCombo.getValue();

                if (userId.isEmpty() || username.isEmpty() || password.isEmpty() || realName.isEmpty()
                        || roleStr == null) {
                    Platform.runLater(() -> showAlert("错误", "所有字段都必须填写", Alert.AlertType.ERROR));
                    return null;
                }

                User.UserRole role = switch (roleStr) {
                    case "仓库管理员" -> User.UserRole.WAREHOUSE_ADMIN;
                    case "采购员" -> User.UserRole.PURCHASER;
                    case "部门经理" -> User.UserRole.DEPARTMENT_MANAGER;
                    case "总经理" -> User.UserRole.GENERAL_MANAGER;
                    default -> User.UserRole.PURCHASER;
                };

                User newUser = new User(userId, username, password, realName, role);
                newUser.setActive(true);
                return newUser;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(user -> {
            new Thread(() -> {
                Map<String, Object> data = new HashMap<>();
                data.put("user", user);
                Message msg = socketClient.sendAndReceive(new Message(Message.MessageType.USER_ADD, data));

                Platform.runLater(() -> {
                    if (msg.isSuccess()) {
                        loadDataFromServer();
                        showAlert("成功", "用户添加成功", Alert.AlertType.INFORMATION);
                        updateStatistics();
                    } else {
                        showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }).start();
        });
    }

    // ... (handleEditUser and handleDeleteUser Logic) ...
    @FXML
    void handleEditUser(User user) {
        if (user == null)
            return;
        // ... (类似 handleAddUser，但 userId 不可编辑，密码可留空) ...
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("编辑用户");
        dialog.setHeaderText("编辑用户信息: " + user.getUsername());
        ButtonType confirmButtonType = new ButtonType("确定", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField userIdField = new TextField(user.getUserId());
        userIdField.setDisable(true);
        TextField usernameField = new TextField(user.getUsername());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("留空则不修改密码");
        TextField realNameField = new TextField(user.getRealName());
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.setItems(FXCollections.observableArrayList("仓库管理员", "采购员", "部门经理", "总经理"));
        roleCombo.setValue(user.getRole().getDisplayName());

        grid.add(new Label("用户ID:"), 0, 0);
        grid.add(userIdField, 1, 0);
        grid.add(new Label("用户名:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("新密码:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("真实姓名:"), 0, 3);
        grid.add(realNameField, 1, 3);
        grid.add(new Label("角色:"), 0, 4);
        grid.add(roleCombo, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();
                String realName = realNameField.getText().trim();
                String roleStr = roleCombo.getValue();

                if (username.isEmpty() || realName.isEmpty() || roleStr == null) {
                    Platform.runLater(() -> showAlert("错误", "用户名、真实姓名和角色不能为空", Alert.AlertType.ERROR));
                    return null;
                }

                User.UserRole role = switch (roleStr) {
                    case "仓库管理员" -> User.UserRole.WAREHOUSE_ADMIN;
                    case "采购员" -> User.UserRole.PURCHASER;
                    case "部门经理" -> User.UserRole.DEPARTMENT_MANAGER;
                    case "总经理" -> User.UserRole.GENERAL_MANAGER;
                    default -> user.getRole();
                };

                // 若密码框为空，则保留原密码
                User updatedUser = new User(user.getUserId(), username,
                        password.isEmpty() ? user.getPassword() : password, realName, role);
                updatedUser.setActive(user.isActive());
                return updatedUser;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedUser -> {
            new Thread(() -> {
                Map<String, Object> data = new HashMap<>();
                data.put("user", updatedUser);
                Message msg = socketClient.sendAndReceive(new Message(Message.MessageType.USER_UPDATE, data));
                Platform.runLater(() -> {
                    if (msg.isSuccess()) {
                        loadDataFromServer();
                        showAlert("成功", "用户信息更新成功", Alert.AlertType.INFORMATION);
                    } else {
                        showAlert("失败", msg.getMessage(), Alert.AlertType.ERROR);
                    }
                });
            }).start();
        });
    }

    @FXML
    void handleDeleteUser(User user) {
        if (user == null)
            return;
        // 保护机制：禁止删除自己和其他管理员
        if (currentUser != null && user.getUserId().equals(currentUser.getUserId())) {
            showAlert("错误", "不能删除当前登录的用户", Alert.AlertType.ERROR);
            return;
        }
        if (user.getRole() == User.UserRole.WAREHOUSE_ADMIN) {
            showAlert("错误", "不能删除其他仓库管理员", Alert.AlertType.ERROR);
            return;
        }

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

    // ==================== 报表导出功能 (Placeholder) ====================
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

    // ==================== 退出系统 ====================
    @FXML
    void handleLogout(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认退出");
        confirm.setHeaderText("确定要退出系统吗?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                logoutButton.setDisable(true);

                Thread logoutThread = new Thread(() -> {
                    try {
                        if (socketClient != null) {
                            socketClient.logout();
                            socketClient.disconnect();
                        }
                    } finally {
                        Platform.runLater(() -> {
                            Stage stage = (Stage) logoutButton.getScene().getWindow();
                            stage.close();
                            try {
                                new LoginApplication().start(new Stage());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }, "Warehouse-Logout-Thread");

                logoutThread.setDaemon(true);
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

    // 内部类：库存记录视图模型
    public static class StockRecord {
        private String recordId;
        private String type;
        private String productName;
        private int quantity;
        private String operator;
        private String time;
        private String remark;

        // Getters and Setters ...
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
     * 高级筛选：弹窗允许用户按类别、库存范围、是否低库存等条件组合筛选。
     * 更新 advancedProductPredicate 并触发 refreshProductFilter()。
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
                // 解析筛选条件
                String category = categoryCombo.getValue();
                Integer minValue = null, maxValue = null;
                try {
                    if (!minStockField.getText().trim().isEmpty())
                        minValue = Integer.parseInt(minStockField.getText().trim());
                    if (!maxStockField.getText().trim().isEmpty())
                        maxValue = Integer.parseInt(maxStockField.getText().trim());
                } catch (NumberFormatException ignored) {
                }

                // Capture variables for lambda
                final Integer minThreshold = minValue;
                final Integer maxThreshold = maxValue;
                final boolean lowStock = lowStockOnly.isSelected();
                final boolean overStock = overStockOnly.isSelected();
                final String selectedCategory = category;

                // 构建 Predicate
                advancedProductPredicate = product -> {
                    if (product == null)
                        return false;
                    if (!"全部类别".equals(selectedCategory) &&
                            (product.getCategory() == null || !product.getCategory().equals(selectedCategory)))
                        return false;
                    if (minThreshold != null && product.getCurrentStock() < minThreshold)
                        return false;
                    if (maxThreshold != null && product.getCurrentStock() > maxThreshold)
                        return false;
                    if (lowStock && !product.needsStockAlert())
                        return false;
                    if (overStock && !product.isOverStock())
                        return false;
                    return true;
                };

                refreshProductFilter();
                showAlert("筛选结果", "找到 " + (filteredProducts != null ? filteredProducts.size() : 0) + " 个匹配的商品",
                        Alert.AlertType.INFORMATION);
            }
        });
    }

    // ==================== 库存调整功能 (盘点) ====================

    /**
     * 处理库存盘点与修正。
     * 用户输入实际盘点数量，系统计算差异并提交 STOCK_ADJUSTMENT 记录。
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
        productList.forEach(p -> productCombo.getItems().add(p.getProductName() + " (" + p.getProductId() + ")"));

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

        // 逻辑：选择商品后更新当前库存显示
        productCombo.setOnAction(e -> {
            String selection = productCombo.getValue();
            if (selection != null) {
                String productId = selection.substring(selection.lastIndexOf("(") + 1, selection.lastIndexOf(")"));
                productList.stream().filter(p -> p.getProductId().equals(productId)).findFirst()
                        .ifPresent(p -> {
                            currentStockLabel.setText(
                                    "当前库存: " + p.getCurrentStock() + " " + (p.getUnit() != null ? p.getUnit() : ""));
                            actualStockField.setText(String.valueOf(p.getCurrentStock()));
                        });
            }
        });

        // 逻辑：输入实际数量时实时计算差异
        actualStockField.textProperty().addListener((obs, old, newVal) -> {
            String selection = productCombo.getValue();
            if (selection != null && !newVal.trim().isEmpty()) {
                try {
                    int actualStock = Integer.parseInt(newVal.trim());
                    String productId = selection.substring(selection.lastIndexOf("(") + 1, selection.lastIndexOf(")"));
                    productList.stream().filter(p -> p.getProductId().equals(productId)).findFirst()
                            .ifPresent(p -> {
                                int difference = actualStock - p.getCurrentStock();
                                differenceLabel.setText("差异: " + (difference >= 0 ? "+" : "") + difference + " "
                                        + (p.getUnit() != null ? p.getUnit() : ""));
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
                    String productId = selection.substring(selection.lastIndexOf("(") + 1, selection.lastIndexOf(")"));
                    Product product = productList.stream().filter(p -> p.getProductId().equals(productId)).findFirst()
                            .orElse(null);

                    if (product == null)
                        return;
                    int difference = actualStock - product.getCurrentStock();
                    if (difference == 0) {
                        showAlert("提示", "库存数量无变化，无需调整", Alert.AlertType.INFORMATION);
                        return;
                    }

                    // 构建调整数据包
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
                        Message msg = socketClient
                                .sendAndReceive(new Message(Message.MessageType.STOCK_ADJUSTMENT, adjustmentData));
                        Platform.runLater(() -> {
                            if (msg.isSuccess()) {
                                showAlert("成功", "库存调整成功", Alert.AlertType.INFORMATION);
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

    private String getTextSafe(TextField field) {
        String text = field.getText();
        return text != null ? text.trim() : "";
    }
}