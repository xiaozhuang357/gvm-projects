package com._404.wms.network;

import com._404.wms.config.ConfigManager;
import com._404.wms.model.*;
import com._404.wms.service.DataService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * WMS服务器端 - 处理所有客户端连接和业务逻辑
 */
public class WMSServer {
    private static final int DEFAULT_PORT = 8888;
    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private Map<String, ClientHandler> connectedClients;
    private DataService dataService;
    private volatile boolean running;

    public WMSServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.connectedClients = new ConcurrentHashMap<>();
        this.dataService = new DataService();
        this.running = false;
        // 从配置文件读取端口
        this.port = ConfigManager.getInstance().getIntValue("Server", "Port", DEFAULT_PORT);
    }

    /**
     * 使用指定端口创建服务器
     * 
     * @param port 服务器端口
     */
    public WMSServer(int port) {
        this.threadPool = Executors.newCachedThreadPool();
        this.connectedClients = new ConcurrentHashMap<>();
        this.dataService = new DataService();
        this.running = false;
        this.port = port;
    }

    /**
     * 启动服务器
     */
    public void start() {
        try {
            System.out.println("创建ServerSocket，端口: " + port);
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("WMS服务器已启动，监听端口: " + port);
            System.out.println("等待客户端连接...");

            // 初始化示例数据
            System.out.println("开始初始化示例数据...");
            initializeData();
            System.out.println("示例数据初始化完成");

            System.out.println("进入主循环，等待客户端连接...");
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("新客户端连接: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
            System.out.println("主循环退出");
        } catch (IOException e) {
            System.err.println("服务器IO错误: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("服务器未知错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 停止服务器
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdownNow();
            System.out.println("服务器已停止");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化示例数据
     * 仅在数据不存在时插入，避免重复插入导致主键冲突
     */
    private void initializeData() {

        // User.UserRole.PURCHASER));
        if (dataService.getUserById("U001") == null) {
            // 系统管理员 (对应 WAREHOUSE_ADMIN)
            User u1 = new User("U001", "admin1", "admin123", "系统管理员", User.UserRole.WAREHOUSE_ADMIN);
            // 默认 active 字段可能需要在 User 构造函数中设置为 true，或使用 setter 设置
            u1.setActive(true);
            dataService.addUser(u1);
        }

        if (dataService.getUserById("U002") == null) {
            // 经理 (对应 DEPARTMENT_MAN)
            User u2 = new User("U002", "manager1", "123", "张经理", User.UserRole.DEPARTMENT_MANAGER);
            u2.setActive(true);
            dataService.addUser(u2);
        }

        if (dataService.getUserById("U003") == null) {
            // 总经理 (对应 GENERAL_MANAGER)
            User u3 = new User("U003", "general", "123", "李总", User.UserRole.GENERAL_MANAGER);
            u3.setActive(true);
            dataService.addUser(u3);
        }

        if (dataService.getUserById("U004") == null) {
            // 采购员 (对应 PURCHASER)
            User u4 = new User("U004", "purchaser", "123", "王采购", User.UserRole.PURCHASER);
            u4.setActive(true);
            dataService.addUser(u4);
        }

        System.out.println("示例用户初始化完成");
        // 初始化商品 - 仅在不存在时插入
        if (dataService.getProductById("P001") == null) {
            Product p1 = new Product("P001", "联想笔记本电脑", "电子产品", 5500.0, 5, 50);
            p1.setCurrentStock(25);
            p1.setUnit("台");
            p1.setSupplier("联想集团");
            dataService.addProduct(p1);
        }

        if (dataService.getProductById("P002") == null) {
            Product p2 = new Product("P002", "办公打印纸", "办公用品", 25.0, 100, 1000);
            p2.setCurrentStock(500);
            p2.setUnit("包");
            p2.setSupplier("晨光文具");
            dataService.addProduct(p2);
        }

        if (dataService.getProductById("P003") == null) {
            Product p3 = new Product("P003", "矿泉水", "食品饮料", 2.5, 200, 2000);
            p3.setCurrentStock(150);
            p3.setUnit("箱");
            p3.setSupplier("农夫山泉");
            dataService.addProduct(p3);
        }

        System.out.println("示例数据初始化完成");
    }

    /**
     * 客户端处理器线程
     * 负责维护与单个客户端的长连接，处理请求并发送响应
     */
    private class ClientHandler implements Runnable {
        private Socket socket;
        private ObjectInputStream in;
        private ObjectOutputStream out;
        private String userId;
        private User currentUser;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // 初始化对象输出流（必须先于输入流创建，以写入流头）
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush(); // 立即刷新，发送流头部信息
                // 初始化对象输入流
                in = new ObjectInputStream(socket.getInputStream());

                Message message;
                // 循环读取客户端发送的消息对象
                while ((message = (Message) in.readObject()) != null) {
                    try {
                        System.out.println("收到消息类型: " + message.getType() + ", 用户: " + userId);
                        handleMessage(message);
                    } catch (Exception innerEx) {
                        System.err.println("处理消息内部异常 [" + message.getType() + "]: " + innerEx.getMessage());
                        innerEx.printStackTrace();
                        // 尝试发送错误响应
                        try {
                            Message errorResponse = Message.error(message.getType(),
                                    "服务器处理错误: " + innerEx.getMessage());
                            sendMessage(errorResponse);
                        } catch (Exception sendEx) {
                            System.err.println("发送错误响应失败: " + sendEx.getMessage());
                        }
                    }
                }
            } catch (EOFException e) {
                System.out.println("客户端断开连接: " + userId);
            } catch (Exception e) {
                System.err.println("处理客户端消息错误 (外层): " + e.getMessage());
                e.printStackTrace();
            } finally {
                cleanup();
            }
        }

        /**
         * 处理客户端消息
         */
        private void handleMessage(Message message) throws IOException {
            Message response = null;

            try {
                switch (message.getType()) {
                    case LOGIN_REQUEST:
                        response = handleLogin(message);
                        break;
                    case LOGOUT:
                        response = handleLogout(message);
                        break;
                    case USER_LIST:
                        response = handleUserList(message);
                        break;
                    case USER_ADD:
                        response = handleUserAdd(message);
                        break;
                    case USER_DELETE:
                        response = handleUserDelete(message);
                        break;
                    case PRODUCT_LIST:
                        response = handleProductList(message);
                        break;
                    case PRODUCT_ADD:
                        response = handleProductAdd(message);
                        break;
                    case PRODUCT_UPDATE:
                        response = handleProductUpdate(message);
                        break;
                    case PRODUCT_DELETE:
                        response = handleProductDelete(message);
                        break;
                    case PURCHASE_ORDER_ADD:
                    case PURCHASE_ORDER_CREATE:
                        response = handlePurchaseOrderCreate(message);
                        break;
                    case PURCHASE_ORDER_LIST:
                        response = handlePurchaseOrderList(message);
                        break;
                    case PURCHASE_ORDER_APPROVE:
                        response = handlePurchaseOrderApprove(message);
                        break;
                    case PURCHASE_ORDER_ARRIVAL_CONFIRM:
                        response = handlePurchaseOrderArrivalConfirm(message);
                        break;
                    case PRODUCT_STOCK_ALERT:
                        response = handleProductStockAlert(message);
                        break;
                    case PURCHASE_ORDER_REJECT:
                        response = handlePurchaseOrderReject(message);
                        break;
                    case PURCHASE_ORDER_DELETE:
                        response = handlePurchaseOrderDelete(message);
                        break;
                    case STOCK_IN:
                    case STOCK_IN_ADD:
                        response = handleStockIn(message);
                        break;
                    case STOCK_OUT:
                    case STOCK_OUT_ADD:
                        response = handleStockOut(message);
                        break;
                    case STOCK_ADJUSTMENT:
                        response = handleStockAdjustment(message);
                        break;
                    case STOCK_IN_LIST:
                        response = handleStockInList(message);
                        break;
                    case STOCK_OUT_LIST:
                        response = handleStockOutList(message);
                        break;
                    case STOCK_RECORD_LIST:
                        response = handleStockRecordList(message);
                        break;
                    case LOG_LIST:
                        response = handleLogList(message);
                        break;
                    case HEARTBEAT:
                        response = Message.success(Message.MessageType.HEARTBEAT, null, "OK");
                        break;
                    default:
                        response = Message.error(Message.MessageType.ERROR, "未知的消息类型");
                }
            } catch (Exception e) {
                System.err.println("处理消息异常 [" + message.getType() + "]: " + e.getMessage());
                e.printStackTrace();
                response = Message.error(message.getType(), "服务器处理错误: " + e.getMessage());
            }

            if (response != null) {
                sendMessage(response);
            }
        }

        /**
         * 处理登录
         */
        // @SuppressWarnings("unchecked")
        private Message handleLogin(Message message) {
            Map<String, String> credentials = (Map<String, String>) message.getData();
            String username = credentials.get("username");
            String password = credentials.get("password");

            User user = dataService.authenticate(username, password);
            if (user != null) {
                this.userId = user.getUserId();
                this.currentUser = user;
                connectedClients.put(userId, this);
                user.setLastLoginTime(LocalDateTime.now());

                // 调试输出
                System.out.println("用户登录成功: " + username);
                System.out.println("用户角色: " + user.getRole());
                System.out.println("用户ID: " + user.getUserId());

                // 记录日志
                dataService.addLog(new OperationLog(user.getUserId(), user.getUsername(),
                        "登录", "系统", "用户登录成功"));

                return Message.success(Message.MessageType.LOGIN_RESPONSE, user, "登录成功");
            } else {
                return Message.error(Message.MessageType.LOGIN_RESPONSE, "用户名或密码错误");
            }
        }

        /**
         * 处理登出
         */
        private Message handleLogout(Message message) {
            if (currentUser != null) {
                dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                        "登出", "系统", "用户登出"));
                connectedClients.remove(userId);
            }
            return Message.success(Message.MessageType.LOGOUT, null, "登出成功");
        }

        /**
         * 处理用户列表查询
         */
        private Message handleUserList(Message message) {
            List<User> users = dataService.getAllUsers();
            return Message.success(Message.MessageType.USER_LIST, users, "查询成功");
        }

        /**
         * 处理用户添加
         */
        private Message handleUserAdd(Message message) {
            User user = (User) message.getData();
            dataService.addUser(user);
            dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                    "创建", "用户管理", "添加用户: " + user.getUsername()));
            return Message.success(Message.MessageType.USER_ADD, user, "用户添加成功");
        }

        /**
         * 处理用户删除
         */
        private Message handleUserDelete(Message message) {
            String userIdToDelete = (String) message.getData();
            List<User> users = dataService.getAllUsers();
            User userToDelete = users.stream()
                    .filter(u -> u.getUserId().equals(userIdToDelete))
                    .findFirst()
                    .orElse(null);

            if (userToDelete != null) {
                users.remove(userToDelete);
                dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                        "删除", "用户管理", "删除用户: " + userToDelete.getUsername()));
                return Message.success(Message.MessageType.USER_DELETE, null, "用户删除成功");
            }
            return Message.error(Message.MessageType.USER_DELETE, "用户不存在");
        }

        /**
         * 处理商品列表查询
         */
        private Message handleProductList(Message message) {
            List<Product> products = dataService.getAllProducts();
            return Message.success(Message.MessageType.PRODUCT_LIST, products, "查询成功");
        }

        /**
         * 处理添加商品
         */
        private Message handleProductAdd(Message message) {
            Product product = (Product) message.getData();
            dataService.addProduct(product);
            dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                    "创建", "商品管理", "添加商品: " + product.getProductName()));
            return Message.success(Message.MessageType.PRODUCT_ADD, product, "商品添加成功");
        }

        /**
         * 处理更新商品
         */
        private Message handleProductUpdate(Message message) {
            Product product = (Product) message.getData();
            dataService.updateProduct(product);
            dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                    "更新", "商品管理", "更新商品: " + product.getProductName()));
            return Message.success(Message.MessageType.PRODUCT_UPDATE, product, "商品更新成功");
        }

        /**
         * 处理删除商品
         */
        private Message handleProductDelete(Message message) {
            String productId = (String) message.getData();
            Product product = dataService.getProductById(productId);
            if (product != null) {
                dataService.deleteProduct(productId);
                dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                        "删除", "商品管理", "删除商品: " + product.getProductName()));
                return Message.success(Message.MessageType.PRODUCT_DELETE, null, "商品删除成功");
            }
            return Message.error(Message.MessageType.PRODUCT_DELETE, "商品不存在");
        }

        /**
         * 处理创建采购订单
         */
        private Message handlePurchaseOrderCreate(Message message) {
            PurchaseOrder order = (PurchaseOrder) message.getData();
            order.calculateTotalAmount();

            // 根据金额设置审批状态
            if (order.needsGeneralManagerApproval()) {
                order.setStatus(PurchaseOrder.OrderStatus.PENDING_GENERAL_APPROVAL);
            } else {
                order.setStatus(PurchaseOrder.OrderStatus.PENDING_DEPT_APPROVAL);
            }

            dataService.addPurchaseOrder(order);
            dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                    "创建", "采购管理", "创建采购订单: " + order.getOrderId() + ", 金额: " + order.getTotalAmount()));
            return Message.success(message.getType(), order, "采购订单创建成功");
        }

        /**
         * 处理采购订单列表查询
         */
        private Message handlePurchaseOrderList(Message message) {
            List<PurchaseOrder> orders = dataService.getAllPurchaseOrders();
            return Message.success(Message.MessageType.PURCHASE_ORDER_LIST, orders, "查询成功");
        }

        /**
         * 处理采购订单审批
         */
        @SuppressWarnings("unchecked")
        private Message handlePurchaseOrderApprove(Message message) {
            Map<String, Object> data = (Map<String, Object>) message.getData();
            String orderId = (String) data.get("orderId");
            String comment = (String) data.get("comment");

            PurchaseOrder order = dataService.getPurchaseOrderById(orderId);
            if (order != null) {
                order.setStatus(PurchaseOrder.OrderStatus.APPROVED);
                order.setApproverId(currentUser.getUserId());
                order.setApproverName(currentUser.getRealName());
                order.setApproveComment(comment);
                order.setApproveTime(LocalDateTime.now());

                dataService.updatePurchaseOrder(order);
                dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                        "审批", "采购管理", "批准采购订单: " + orderId));
                // 注意：不做主动推送，仓库可通过轮询或刷新获取已审批订单
                return Message.success(Message.MessageType.PURCHASE_ORDER_APPROVE, order, "审批通过");
            }
            return Message.error(Message.MessageType.PURCHASE_ORDER_APPROVE, "订单不存在");
        }

        /**
         * 处理采购订单到货确认（由仓库管理员确认）
         */
        private Message handlePurchaseOrderArrivalConfirm(Message message) {
            String orderId = (String) message.getData();
            PurchaseOrder order = dataService.getPurchaseOrderById(orderId);
            if (order == null) {
                return Message.error(Message.MessageType.PURCHASE_ORDER_ARRIVAL_CONFIRM, "订单不存在");
            }

            order.setStatus(PurchaseOrder.OrderStatus.ARRIVED);
            order.setArrivalTime(java.time.LocalDateTime.now());
            dataService.updatePurchaseOrder(order);
            dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                    "入库", "采购管理", "确认采购订单到货: " + orderId));

            return Message.success(Message.MessageType.PURCHASE_ORDER_ARRIVAL_CONFIRM, order, "确认到货成功");
        }

        /**
         * 返回需要补货的商品列表（库存预警）
         */
        private Message handleProductStockAlert(Message message) {
            List<Product> lows = dataService.getLowStockProducts();
            return Message.success(Message.MessageType.PRODUCT_STOCK_ALERT, lows, "低库存查询成功");
        }

        /**
         * 处理采购订单拒绝
         */
        @SuppressWarnings("unchecked")
        private Message handlePurchaseOrderReject(Message message) {
            Map<String, Object> data = (Map<String, Object>) message.getData();
            String orderId = (String) data.get("orderId");
            String comment = (String) data.get("comment");

            PurchaseOrder order = dataService.getPurchaseOrderById(orderId);
            if (order != null) {
                order.setStatus(PurchaseOrder.OrderStatus.REJECTED);
                order.setApproverId(currentUser.getUserId());
                order.setApproverName(currentUser.getRealName());
                order.setApproveComment(comment);
                order.setApproveTime(LocalDateTime.now());

                dataService.updatePurchaseOrder(order);
                dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                        "退回", "采购管理", "退回采购订单: " + orderId));
                return Message.success(Message.MessageType.PURCHASE_ORDER_REJECT, order, "订单已退回");
            }
            return Message.success(Message.MessageType.PURCHASE_ORDER_REJECT, order, "采购订单已拒绝");
        }

        /**
         * 处理采购订单删除
         */
        private Message handlePurchaseOrderDelete(Message message) {
            String orderId = (String) message.getData();
            PurchaseOrder order = dataService.getPurchaseOrderById(orderId);

            if (order == null) {
                return Message.error(Message.MessageType.PURCHASE_ORDER_DELETE, "订单不存在");
            }

            // 只有待提交状态的订单可以删除
            if (order.getStatus() != PurchaseOrder.OrderStatus.PENDING_SUBMIT) {
                return Message.error(Message.MessageType.PURCHASE_ORDER_DELETE, "只有待提交状态的订单可以删除");
            }

            dataService.getAllPurchaseOrders().remove(order);
            dataService.addLog(new OperationLog(userId, currentUser.getUsername(),
                    OperationLog.OperationType.DELETE, "删除采购订单: " + orderId, true, ""));

            return Message.success(Message.MessageType.PURCHASE_ORDER_DELETE, null, "订单删除成功");
        }

        /**
         * 处理入库
         */
        private Message handleStockIn(Message message) {
            StockInRecord record = (StockInRecord) message.getData();
            Product product = dataService.getProductById(record.getProductId());

            if (product != null) {
                // 注意：addStockInRecord 内部已经会更新库存，这里不需要重复更新
                dataService.addStockInRecord(record);

                dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                        "入库", "库存管理", "商品入库: " + product.getProductName() + ", 数量: " + record.getQuantity()));
                return Message.success(Message.MessageType.STOCK_IN, record, "入库成功");
            }
            return Message.error(Message.MessageType.STOCK_IN, "商品不存在");
        }

        /**
         * 处理出库
         */
        private Message handleStockOut(Message message) {
            StockOutRecord record = (StockOutRecord) message.getData();
            Product product = dataService.getProductById(record.getProductId());

            if (product != null) {
                if (product.getCurrentStock() >= record.getQuantity()) {
                    // 注意：addStockOutRecord 内部已经会更新库存，这里不需要重复更新
                    dataService.addStockOutRecord(record);

                    dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                            "出库", "库存管理", "商品出库: " + product.getProductName() + ", 数量: " + record.getQuantity()));

                    // 重新获取更新后的商品信息检查库存预警
                    product = dataService.getProductById(record.getProductId());
                    if (product != null && product.needsStockAlert()) {
                        dataService.addLog(new OperationLog(currentUser.getUserId(), currentUser.getUsername(),
                                "库存预警", "库存管理", "库存低于预警值: " + product.getProductName() +
                                        ", 当前库存: " + product.getCurrentStock()));
                    }
                    return Message.success(Message.MessageType.STOCK_OUT, record, "出库成功");
                } else {
                    return Message.error(Message.MessageType.STOCK_OUT, "库存不足");
                }
            }
            return Message.error(Message.MessageType.STOCK_OUT, "商品不存在");
        }

        /**
         * 处理入库记录查询
         */
        private Message handleStockInList(Message message) {
            List<StockInRecord> records = dataService.getAllStockInRecords();
            return Message.success(Message.MessageType.STOCK_IN_LIST, records, "查询成功");
        }

        /**
         * 处理出库记录查询
         */
        private Message handleStockOutList(Message message) {
            return Message.success(Message.MessageType.STOCK_OUT_LIST, dataService.getAllStockOutRecords(), "获取出库记录成功");
        }

        /**
         * 处理库存调整
         */
        @SuppressWarnings("unchecked")
        private Message handleStockAdjustment(Message message) {
            try {
                Map<String, Object> adjustmentData = (Map<String, Object>) message.getData();
                String productId = (String) adjustmentData.get("productId");
                int oldStock = (Integer) adjustmentData.get("oldStock");
                int newStock = (Integer) adjustmentData.get("newStock");
                int difference = (Integer) adjustmentData.get("difference");
                String reason = (String) adjustmentData.get("reason");
                String remark = (String) adjustmentData.get("remark");
                String operatorId = (String) adjustmentData.get("operatorId");
                String operatorName = (String) adjustmentData.get("operatorName");

                // 获取商品
                Product product = dataService.getProductById(productId);
                if (product == null) {
                    return Message.error(Message.MessageType.STOCK_ADJUSTMENT, "商品不存在");
                }

                // 更新商品库存
                product.setCurrentStock(newStock);
                dataService.updateProduct(product);

                // 记录调整日志
                String logDetails = String.format(
                        "库存调整 - 商品: %s, 原库存: %d, 新库存: %d, 差异: %+d, 原因: %s, 说明: %s",
                        product.getProductName(), oldStock, newStock, difference, reason, remark);
                dataService.addLog(new OperationLog(
                        operatorId, operatorName, "库存调整", "库存管理", logDetails));

                // 如果是增加库存，创建入库记录
                if (difference > 0) {
                    StockInRecord inRecord = new StockInRecord();
                    inRecord.setRecordId("ADJ-IN-" + System.currentTimeMillis());
                    inRecord.setProductId(productId);
                    inRecord.setQuantity(difference);
                    inRecord.setOperatorId(operatorId);
                    inRecord.setOperatorName(operatorName);
                    inRecord.setTimestamp(LocalDateTime.now());
                    inRecord.setRemark("库存调整: " + reason + " - " + remark);
                    dataService.addStockInRecord(inRecord);
                }
                // 如果是减少库存，创建出库记录
                else if (difference < 0) {
                    StockOutRecord outRecord = new StockOutRecord();
                    outRecord.setRecordId("ADJ-OUT-" + System.currentTimeMillis());
                    outRecord.setProductId(productId);
                    outRecord.setQuantity(Math.abs(difference));
                    outRecord.setOperatorId(operatorId);
                    outRecord.setOperatorName(operatorName);
                    outRecord.setTimestamp(LocalDateTime.now());
                    outRecord.setRemark("库存调整: " + reason + " - " + remark);
                    dataService.addStockOutRecord(outRecord);
                }

                return Message.success(Message.MessageType.STOCK_ADJUSTMENT, product, "库存调整成功");

            } catch (Exception e) {
                e.printStackTrace();
                return Message.error(Message.MessageType.STOCK_ADJUSTMENT, "库存调整失败: " + e.getMessage());
            }
        }

        /**
         * 处理库存记录列表查询
         */
        private Message handleStockRecordList(Message message) {
            List<StockRecordDTO> records = new ArrayList<>();

            // 添加入库记录
            for (StockInRecord record : dataService.getAllStockInRecords()) {
                StockRecordDTO dto = new StockRecordDTO();
                dto.setRecordId(record.getRecordId());
                dto.setType("入库");
                dto.setProductName(getProductNameById(record.getProductId()));
                dto.setQuantity(record.getQuantity());
                dto.setOperator(record.getOperatorName());
                dto.setTime(record.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                dto.setRemark(record.getRemark());
                records.add(dto);
            }

            // 添加出库记录
            for (StockOutRecord record : dataService.getAllStockOutRecords()) {
                StockRecordDTO dto = new StockRecordDTO();
                dto.setRecordId(record.getRecordId());
                dto.setType("出库");
                dto.setProductName(getProductNameById(record.getProductId()));
                dto.setQuantity(record.getQuantity());
                dto.setOperator(record.getOperatorName());
                dto.setTime(record.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                dto.setRemark(record.getRemark());
                records.add(dto);
            }

            return Message.success(Message.MessageType.STOCK_RECORD_LIST, records, "获取库存记录成功");
        }

        /**
         * 辅助方法：根据产品ID获取产品名称
         */
        private String getProductNameById(String productId) {
            Product product = dataService.getProductById(productId);
            return product != null ? product.getProductName() : "未知产品";
        }

        /**
         * 处理日志列表查询
         */
        private Message handleLogList(Message message) {
            List<OperationLog> logs = dataService.getAllLogs();
            return Message.success(Message.MessageType.LOG_LIST, logs, "查询成功");
        }

        /**
         * 发送消息
         */
        private void sendMessage(Message message) throws IOException {
            out.writeObject(message);
            out.flush();
        }

        /**
         * 清理资源
         */
        private void cleanup() {
            try {
                if (userId != null) {
                    connectedClients.remove(userId);
                }
                if (in != null)
                    in.close();
                if (out != null)
                    out.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 库存记录DTO - 用于统一入库和出库记录的显示
     */
    public static class StockRecordDTO implements Serializable {
        private static final long serialVersionUID = 1L;

        private String recordId;
        private String type; // "入库" 或 "出库"
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

    /**
     * 主函数 - 启动服务器
     */
    public static void main(String[] args) {
        WMSServer server = new WMSServer();

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n正在关闭服务器...");
            server.stop();
        }));

        server.start();
    }
}
