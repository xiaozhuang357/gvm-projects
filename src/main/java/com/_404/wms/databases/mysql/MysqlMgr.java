package com._404.wms.databases.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com._404.wms.databases.config.ConfigMgr;
import com._404.wms.model.OperationLog;
import com._404.wms.model.Product;
import com._404.wms.model.PurchaseOrder;
import com._404.wms.model.PurchaseOrder.OrderItem;
import com._404.wms.model.StockInRecord;
import com._404.wms.model.StockOutRecord;
import com._404.wms.model.User;
import com._404.wms.model.User.UserRole;

public class MysqlMgr {
    // 单例实例
    private static volatile MysqlMgr instance;

    // 连接池实例
    private MysqlPool mysqlPool;

    // 配置信息
    private String url;
    private String user;
    private String password;
    private String schema;
    private int poolSize;
    private int maxWaitTime = 10000; // 毫秒

    // 私有构造函数
    private MysqlMgr() {
        loadConfig();
        initConnectionPool();
        initTables();
    }

    // 获取单例实例
    public static MysqlMgr getInstance() {
        if (instance == null) {
            synchronized (MysqlMgr.class) {
                if (instance == null) {
                    instance = new MysqlMgr();
                }
            }
        }
        return instance;
    }

    // 加载配置文件
    private void loadConfig() {
        // 读取配置，使用默认值
        ConfigMgr config = ConfigMgr.getInstance();
        this.url = config.getValue("Mysql", "Url");
        this.user = config.getValue("Mysql", "User");
        this.password = config.getValue("Mysql", "Passwd");
        this.schema = config.getValue("Mysql", "Schema");
        this.poolSize = Integer.parseInt(config.getValue("Mysql", "PoolSize"));
    }

    // 初始化连接池
    private void initConnectionPool() {
        try {
            mysqlPool = new MysqlPool(url, user, password, schema, poolSize, maxWaitTime);
            System.out.println("MySQL Manager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize MySQL Manager: " + e.getMessage());
            throw new RuntimeException("Failed to initialize MySQL Manager", e);
        }
    }

    // 初始化表结构
    private void initTables() {
        String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id VARCHAR(50) PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, " +
                "real_name VARCHAR(50), " +
                "role VARCHAR(50), " +
                "department VARCHAR(50), " +
                "email VARCHAR(100), " +
                "phone VARCHAR(20), " +
                "active BOOLEAN DEFAULT TRUE, " +
                "create_time DATETIME, " +
                "last_login_time DATETIME" +
                ")";

        String createProducts = "CREATE TABLE IF NOT EXISTS products (" +
                "product_id VARCHAR(50) PRIMARY KEY, " +
                "product_name VARCHAR(100) NOT NULL, " +
                "category VARCHAR(100), " +
                "specification VARCHAR(200), " +
                "unit VARCHAR(20), " +
                "purchase_price DOUBLE, " +
                "selling_price DOUBLE, " +
                "current_stock INT, " +
                "min_stock INT, " +
                "max_stock INT, " +
                "supplier VARCHAR(200), " +
                "description TEXT, " +
                "active BOOLEAN DEFAULT TRUE, " +
                "create_time DATETIME, " +
                "update_time DATETIME" +
                ")";

        String createPurchaseOrders = "CREATE TABLE IF NOT EXISTS purchase_orders (" +
                "order_id VARCHAR(50) PRIMARY KEY, " +
                "purchaser_id VARCHAR(50), " +
                "purchaser_name VARCHAR(100), " +
                "supplier VARCHAR(200), " +
                "total_amount DOUBLE, " +
                "status VARCHAR(50), " +
                "approver_id VARCHAR(50), " +
                "approver_name VARCHAR(100), " +
                "approve_comment TEXT, " +
                "create_time DATETIME, " +
                "approve_time DATETIME, " +
                "arrival_time DATETIME, " +
                "expected_delivery_date DATE, " +
                "remark TEXT" +
                ")";

        String createOrderItems = "CREATE TABLE IF NOT EXISTS purchase_order_items (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "order_id VARCHAR(50), " +
                "product_id VARCHAR(50), " +
                "product_name VARCHAR(200), " +
                "specification VARCHAR(200), " +
                "quantity INT, " +
                "unit_price DOUBLE, " +
                "subtotal DOUBLE, " +
                "INDEX idx_order_id(order_id)" +
                ")";

        String createStockIn = "CREATE TABLE IF NOT EXISTS stock_in_records (" +
                "record_id VARCHAR(50) PRIMARY KEY, " +
                "order_id VARCHAR(50), " +
                "product_id VARCHAR(50), " +
                "product_name VARCHAR(200), " +
                "quantity INT, " +
                "batch_number VARCHAR(100), " +
                "warehouse VARCHAR(200), " +
                "operator_id VARCHAR(50), " +
                "operator_name VARCHAR(100), " +
                "in_time DATETIME, " +
                "remark TEXT" +
                ")";

        String createStockOut = "CREATE TABLE IF NOT EXISTS stock_out_records (" +
                "record_id VARCHAR(50) PRIMARY KEY, " +
                "product_id VARCHAR(50), " +
                "product_name VARCHAR(200), " +
                "quantity INT, " +
                "recipient VARCHAR(100), " +
                "recipient_dept VARCHAR(100), " +
                "purpose VARCHAR(255), " +
                "operator_id VARCHAR(50), " +
                "operator_name VARCHAR(100), " +
                "out_time DATETIME, " +
                "remark TEXT" +
                ")";

        String createLogs = "CREATE TABLE IF NOT EXISTS operation_logs (" +
                "log_id VARCHAR(50) PRIMARY KEY, " +
                "user_id VARCHAR(50), " +
                "username VARCHAR(100), " +
                "operation VARCHAR(100), " +
                "module VARCHAR(100), " +
                "details TEXT, " +
                "ip_address VARCHAR(50), " +
                "operation_time DATETIME, " +
                "success BOOLEAN, " +
                "error_message TEXT" +
                ")";

        String[] ddlArray = new String[] { createUsers, createProducts, createPurchaseOrders, createOrderItems,
                createStockIn, createStockOut, createLogs };

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = mysqlPool.getConnection();
            stmt = conn.createStatement();
            for (String ddl : ddlArray) {
                stmt.execute(ddl);
            }
            System.out.println("Database tables initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize tables: " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return mysqlPool.getConnection();
    }

    /**
     * 归还数据库连接
     * 
     * @param connection Connection对象
     */
    public void returnConnection(Connection connection) {
        mysqlPool.returnConnection(connection);
    }

    /**
     * 添加用户
     */
    public boolean addUser(User user) {
        String sql = "INSERT INTO users (user_id, username, password, real_name, role, department, email, phone, active, create_time, last_login_time) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRealName());
            pstmt.setString(5, user.getRole() != null ? user.getRole().name() : null);
            pstmt.setString(6, user.getDepartment());
            pstmt.setString(7, user.getEmail());
            pstmt.setString(8, user.getPhone());
            pstmt.setBoolean(9, user.isActive());
            pstmt.setTimestamp(10, user.getCreateTime() != null ? Timestamp.valueOf(user.getCreateTime()) : null);
            pstmt.setTimestamp(11, user.getLastLoginTime() != null ? Timestamp.valueOf(user.getLastLoginTime()) : null);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add user: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 更新用户
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username=?, password=?, real_name=?, role=?, department=?, email=?, phone=?, active=?, last_login_time=? WHERE user_id=?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRealName());
            pstmt.setString(4, user.getRole() != null ? user.getRole().name() : null);
            pstmt.setString(5, user.getDepartment());
            pstmt.setString(6, user.getEmail());
            pstmt.setString(7, user.getPhone());
            pstmt.setBoolean(8, user.isActive());
            pstmt.setTimestamp(9, user.getLastLoginTime() != null ? Timestamp.valueOf(user.getLastLoginTime()) : null);
            pstmt.setString(10, user.getUserId());

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update user: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 删除用户
     */
    public boolean deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE user_id=?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete user: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(String userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Failed to get user by id: " + e.getMessage());
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get all users: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return users;
    }

    /**
     * 用户认证
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND active = true";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Failed to authenticate user: " + e.getMessage());
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    // ==================== Product Management ====================

    public boolean addProduct(Product product) {
        String sql = "INSERT INTO products (product_id, product_name, category, specification, unit, purchase_price, " +
                "selling_price, current_stock, min_stock, max_stock, supplier, description, active, create_time, update_time) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getProductId());
            pstmt.setString(2, product.getProductName());
            pstmt.setString(3, product.getCategory());
            pstmt.setString(4, product.getSpecification());
            pstmt.setString(5, product.getUnit());
            pstmt.setDouble(6, product.getPurchasePrice());
            pstmt.setDouble(7, product.getSellingPrice());
            pstmt.setInt(8, product.getCurrentStock());
            pstmt.setInt(9, product.getMinStock());
            pstmt.setInt(10, product.getMaxStock());
            pstmt.setString(11, product.getSupplier());
            pstmt.setString(12, product.getDescription());
            pstmt.setBoolean(13, product.isActive());
            pstmt.setTimestamp(14, product.getCreateTime() != null ? Timestamp.valueOf(product.getCreateTime())
                    : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setTimestamp(15, product.getUpdateTime() != null ? Timestamp.valueOf(product.getUpdateTime())
                    : Timestamp.valueOf(LocalDateTime.now()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add product: " + e.getMessage());
            return false;
        }
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET product_name=?, category=?, specification=?, unit=?, purchase_price=?, selling_price=?, "
                +
                "current_stock=?, min_stock=?, max_stock=?, supplier=?, description=?, active=?, update_time=? WHERE product_id=?";

        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.getProductName());
            pstmt.setString(2, product.getCategory());
            pstmt.setString(3, product.getSpecification());
            pstmt.setString(4, product.getUnit());
            pstmt.setDouble(5, product.getPurchasePrice());
            pstmt.setDouble(6, product.getSellingPrice());
            pstmt.setInt(7, product.getCurrentStock());
            pstmt.setInt(8, product.getMinStock());
            pstmt.setInt(9, product.getMaxStock());
            pstmt.setString(10, product.getSupplier());
            pstmt.setString(11, product.getDescription());
            pstmt.setBoolean(12, product.isActive());
            pstmt.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(14, product.getProductId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update product: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteProduct(String productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete product: " + e.getMessage());
            return false;
        }
    }

    public Product getProductById(String productId) {
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToProduct(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get product: " + e.getMessage());
        }
        return null;
    }

    public List<Product> getAllProducts() {
        String sql = "SELECT * FROM products";
        List<Product> list = new ArrayList<>();
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get all products: " + e.getMessage());
        }
        return list;
    }

    public List<Product> getLowStockProducts() {
        String sql = "SELECT * FROM products WHERE current_stock <= min_stock";
        List<Product> list = new ArrayList<>();
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to query low stock products: " + e.getMessage());
        }
        return list;
    }

    // ==================== Purchase Order Management ====================

    public boolean addPurchaseOrder(PurchaseOrder order) {
        String insertOrder = "INSERT INTO purchase_orders (order_id, purchaser_id, purchaser_name, supplier, total_amount, status, "
                +
                "approver_id, approver_name, approve_comment, create_time, approve_time, arrival_time, expected_delivery_date, remark) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String insertItem = "INSERT INTO purchase_order_items (order_id, product_id, product_name, specification, quantity, unit_price, subtotal) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement orderStmt = null;
        PreparedStatement itemStmt = null;
        try {
            conn = mysqlPool.getConnection();
            conn.setAutoCommit(false);

            orderStmt = conn.prepareStatement(insertOrder);
            fillOrderStatement(orderStmt, order);
            orderStmt.executeUpdate();

            itemStmt = conn.prepareStatement(insertItem);
            for (OrderItem item : order.getItems()) {
                fillOrderItemStatement(itemStmt, order.getOrderId(), item);
                itemStmt.addBatch();
            }
            itemStmt.executeBatch();

            conn.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            System.err.println("Failed to add purchase order: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(itemStmt);
            closeQuietly(orderStmt);
            resetAutoCommit(conn);
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
    }

    public boolean updatePurchaseOrder(PurchaseOrder order) {
        String updateOrder = "UPDATE purchase_orders SET purchaser_id=?, purchaser_name=?, supplier=?, total_amount=?, status=?, "
                +
                "approver_id=?, approver_name=?, approve_comment=?, create_time=?, approve_time=?, arrival_time=?, expected_delivery_date=?, remark=? "
                +
                "WHERE order_id=?";

        String deleteItems = "DELETE FROM purchase_order_items WHERE order_id=?";
        String insertItem = "INSERT INTO purchase_order_items (order_id, product_id, product_name, specification, quantity, unit_price, subtotal) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement orderStmt = null;
        PreparedStatement deleteStmt = null;
        PreparedStatement itemStmt = null;
        try {
            conn = mysqlPool.getConnection();
            conn.setAutoCommit(false);

            orderStmt = conn.prepareStatement(updateOrder);
            fillOrderStatement(orderStmt, order);
            orderStmt.setString(14, order.getOrderId());
            orderStmt.executeUpdate();

            deleteStmt = conn.prepareStatement(deleteItems);
            deleteStmt.setString(1, order.getOrderId());
            deleteStmt.executeUpdate();

            itemStmt = conn.prepareStatement(insertItem);
            for (OrderItem item : order.getItems()) {
                fillOrderItemStatement(itemStmt, order.getOrderId(), item);
                itemStmt.addBatch();
            }
            itemStmt.executeBatch();

            conn.commit();
            return true;
        } catch (SQLException e) {
            rollbackQuietly(conn);
            System.err.println("Failed to update purchase order: " + e.getMessage());
            return false;
        } finally {
            closeQuietly(itemStmt);
            closeQuietly(deleteStmt);
            closeQuietly(orderStmt);
            resetAutoCommit(conn);
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
    }

    public PurchaseOrder getPurchaseOrderById(String orderId) {
        String sql = "SELECT * FROM purchase_orders WHERE order_id=?";
        Connection conn = null;
        try {
            conn = mysqlPool.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, orderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        PurchaseOrder order = mapResultSetToOrder(rs);
                        order.setItems(getOrderItems(conn, orderId));
                        return order;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to query purchase order: " + e.getMessage());
        } finally {
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
        return null;
    }

    public List<PurchaseOrder> getAllPurchaseOrders() {
        String sql = "SELECT * FROM purchase_orders";
        List<PurchaseOrder> orders = new ArrayList<>();
        Connection conn = null;
        try {
            conn = mysqlPool.getConnection();
            Map<String, PurchaseOrder> orderMap = new HashMap<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql);
                    ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PurchaseOrder order = mapResultSetToOrder(rs);
                    orderMap.put(order.getOrderId(), order);
                }
            }

            if (!orderMap.isEmpty()) {
                loadItemsForOrders(conn, orderMap);
                orders.addAll(orderMap.values());
            }
        } catch (SQLException e) {
            System.err.println("Failed to get purchase orders: " + e.getMessage());
        } finally {
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
        return orders;
    }

    public List<PurchaseOrder> getOrdersByStatus(PurchaseOrder.OrderStatus status) {
        String sql = "SELECT * FROM purchase_orders WHERE status=?";
        List<PurchaseOrder> orders = new ArrayList<>();
        Connection conn = null;
        try {
            conn = mysqlPool.getConnection();
            Map<String, PurchaseOrder> orderMap = new HashMap<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, status.name());
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        PurchaseOrder order = mapResultSetToOrder(rs);
                        orderMap.put(order.getOrderId(), order);
                    }
                }
            }

            if (!orderMap.isEmpty()) {
                loadItemsForOrders(conn, orderMap);
                orders.addAll(orderMap.values());
            }
        } catch (SQLException e) {
            System.err.println("Failed to query orders by status: " + e.getMessage());
        } finally {
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
        return orders;
    }

    public List<PurchaseOrder> getOrdersByPurchaser(String purchaserId) {
        String sql = "SELECT * FROM purchase_orders WHERE purchaser_id=?";
        List<PurchaseOrder> orders = new ArrayList<>();
        Connection conn = null;
        try {
            conn = mysqlPool.getConnection();
            Map<String, PurchaseOrder> orderMap = new HashMap<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, purchaserId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        PurchaseOrder order = mapResultSetToOrder(rs);
                        orderMap.put(order.getOrderId(), order);
                    }
                }
            }

            if (!orderMap.isEmpty()) {
                loadItemsForOrders(conn, orderMap);
                orders.addAll(orderMap.values());
            }
        } catch (SQLException e) {
            System.err.println("Failed to query orders by purchaser: " + e.getMessage());
        } finally {
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
        return orders;
    }

    // ==================== Stock Record Management ====================

    public boolean addStockInRecord(StockInRecord record) {
        String sql = "INSERT INTO stock_in_records (record_id, order_id, product_id, product_name, quantity, batch_number, warehouse, operator_id, operator_name, in_time, remark) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.getRecordId());
            pstmt.setString(2, record.getOrderId());
            pstmt.setString(3, record.getProductId());
            pstmt.setString(4, record.getProductName());
            pstmt.setInt(5, record.getQuantity());
            pstmt.setString(6, record.getBatchNumber());
            pstmt.setString(7, record.getWarehouse());
            pstmt.setString(8, record.getOperatorId());
            pstmt.setString(9, record.getOperatorName());
            pstmt.setTimestamp(10, record.getTimestamp() != null ? Timestamp.valueOf(record.getTimestamp())
                    : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(11, record.getRemark());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add stock in record: " + e.getMessage());
            return false;
        }
    }

    public List<StockInRecord> getAllStockInRecords() {
        String sql = "SELECT * FROM stock_in_records ORDER BY in_time DESC";
        List<StockInRecord> records = new ArrayList<>();
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                records.add(mapResultSetToStockIn(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get stock in records: " + e.getMessage());
        }
        return records;
    }

    public boolean addStockOutRecord(StockOutRecord record) {
        String sql = "INSERT INTO stock_out_records (record_id, product_id, product_name, quantity, recipient, recipient_dept, purpose, operator_id, operator_name, out_time, remark) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, record.getRecordId());
            pstmt.setString(2, record.getProductId());
            pstmt.setString(3, record.getProductName());
            pstmt.setInt(4, record.getQuantity());
            pstmt.setString(5, record.getRecipient());
            pstmt.setString(6, record.getRecipientDept());
            pstmt.setString(7, record.getPurpose());
            pstmt.setString(8, record.getOperatorId());
            pstmt.setString(9, record.getOperatorName());
            pstmt.setTimestamp(10, record.getTimestamp() != null ? Timestamp.valueOf(record.getTimestamp())
                    : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(11, record.getRemark());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add stock out record: " + e.getMessage());
            return false;
        }
    }

    public List<StockOutRecord> getAllStockOutRecords() {
        String sql = "SELECT * FROM stock_out_records ORDER BY out_time DESC";
        List<StockOutRecord> records = new ArrayList<>();
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                records.add(mapResultSetToStockOut(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get stock out records: " + e.getMessage());
        }
        return records;
    }

    // ==================== Operation Log ====================

    public boolean addLog(OperationLog log) {
        String sql = "INSERT INTO operation_logs (log_id, user_id, username, operation, module, details, ip_address, operation_time, success, error_message) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getLogId());
            pstmt.setString(2, log.getUserId());
            pstmt.setString(3, log.getUsername());
            pstmt.setString(4, log.getOperation());
            pstmt.setString(5, log.getModule());
            pstmt.setString(6, log.getDetails());
            pstmt.setString(7, log.getIpAddress());
            pstmt.setTimestamp(8, log.getOperationTime() != null ? Timestamp.valueOf(log.getOperationTime())
                    : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setBoolean(9, log.isSuccess());
            pstmt.setString(10, log.getErrorMessage());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add log: " + e.getMessage());
            return false;
        }
    }

    public List<OperationLog> getAllLogs() {
        String sql = "SELECT * FROM operation_logs ORDER BY operation_time DESC";
        List<OperationLog> logs = new ArrayList<>();
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                logs.add(mapResultSetToLog(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get logs: " + e.getMessage());
        }
        return logs;
    }

    public List<OperationLog> getLogsByUser(String userId) {
        String sql = "SELECT * FROM operation_logs WHERE user_id=? ORDER BY operation_time DESC";
        List<OperationLog> logs = new ArrayList<>();
        try (Connection conn = mysqlPool.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToLog(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to get logs by user: " + e.getMessage());
        }
        return logs;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRealName(rs.getString("real_name"));

        String roleStr = rs.getString("role");
        if (roleStr != null) {
            try {
                user.setRole(UserRole.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                // Ignore invalid role
            }
        }

        user.setDepartment(rs.getString("department"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setActive(rs.getBoolean("active"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            user.setCreateTime(createTime.toLocalDateTime());
        }

        Timestamp lastLoginTime = rs.getTimestamp("last_login_time");
        if (lastLoginTime != null) {
            user.setLastLoginTime(lastLoginTime.toLocalDateTime());
        }

        return user;
    }

    private void fillOrderStatement(PreparedStatement pstmt, PurchaseOrder order) throws SQLException {
        pstmt.setString(1, order.getOrderId());
        pstmt.setString(2, order.getPurchaserId());
        pstmt.setString(3, order.getPurchaserName());
        pstmt.setString(4, order.getSupplier());
        pstmt.setDouble(5, order.getTotalAmount());
        pstmt.setString(6, order.getStatus() != null ? order.getStatus().name() : null);
        pstmt.setString(7, order.getApproverId());
        pstmt.setString(8, order.getApproverName());
        pstmt.setString(9, order.getApproveComment());
        pstmt.setTimestamp(10, order.getCreateTime() != null ? Timestamp.valueOf(order.getCreateTime())
                : Timestamp.valueOf(LocalDateTime.now()));
        pstmt.setTimestamp(11, order.getApproveTime() != null ? Timestamp.valueOf(order.getApproveTime()) : null);
        pstmt.setTimestamp(12, order.getArrivalTime() != null ? Timestamp.valueOf(order.getArrivalTime()) : null);
        pstmt.setDate(13,
                order.getExpectedDeliveryDate() != null ? java.sql.Date.valueOf(order.getExpectedDeliveryDate())
                        : null);
        pstmt.setString(14, order.getRemark());
    }

    private void fillOrderItemStatement(PreparedStatement pstmt, String orderId, OrderItem item) throws SQLException {
        pstmt.setString(1, orderId);
        pstmt.setString(2, item.getProductId());
        pstmt.setString(3, item.getProductName());
        pstmt.setString(4, item.getSpecification());
        pstmt.setInt(5, item.getQuantity());
        pstmt.setDouble(6, item.getUnitPrice());
        pstmt.setDouble(7, item.getSubtotal());
    }

    private PurchaseOrder mapResultSetToOrder(ResultSet rs) throws SQLException {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(rs.getString("order_id"));
        order.setPurchaserId(rs.getString("purchaser_id"));
        order.setPurchaserName(rs.getString("purchaser_name"));
        order.setSupplier(rs.getString("supplier"));
        order.setTotalAmount(rs.getDouble("total_amount"));

        String status = rs.getString("status");
        if (status != null) {
            try {
                order.setStatus(PurchaseOrder.OrderStatus.valueOf(status));
            } catch (IllegalArgumentException ex) {
                // ignore invalid value
            }
        }

        order.setApproverId(rs.getString("approver_id"));
        order.setApproverName(rs.getString("approver_name"));
        order.setApproveComment(rs.getString("approve_comment"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            order.setCreateTime(createTime.toLocalDateTime());
        }

        Timestamp approveTime = rs.getTimestamp("approve_time");
        if (approveTime != null) {
            order.setApproveTime(approveTime.toLocalDateTime());
        }

        Timestamp arrivalTime = rs.getTimestamp("arrival_time");
        if (arrivalTime != null) {
            order.setArrivalTime(arrivalTime.toLocalDateTime());
        }

        java.sql.Date expectedDate = rs.getDate("expected_delivery_date");
        if (expectedDate != null) {
            order.setExpectedDeliveryDate(expectedDate.toLocalDate());
        }

        order.setRemark(rs.getString("remark"));
        return order;
    }

    private List<OrderItem> getOrderItems(Connection conn, String orderId) throws SQLException {
        String sql = "SELECT * FROM purchase_order_items WHERE order_id=?";
        List<OrderItem> items = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToOrderItem(rs));
                }
            }
        }
        return items;
    }

    private OrderItem mapResultSetToOrderItem(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setProductId(rs.getString("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setSpecification(rs.getString("specification"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        item.setSubtotal(rs.getDouble("subtotal"));
        return item;
    }

    private void loadItemsForOrders(Connection conn, Map<String, PurchaseOrder> orderMap) throws SQLException {
        if (orderMap.isEmpty()) {
            return;
        }
        String sql = "SELECT * FROM purchase_order_items WHERE order_id IN (" +
                String.join(",", java.util.Collections.nCopies(orderMap.size(), "?")) + ")";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (String orderId : orderMap.keySet()) {
                pstmt.setString(index++, orderId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String orderId = rs.getString("order_id");
                    PurchaseOrder order = orderMap.get(orderId);
                    if (order != null) {
                        order.getItems().add(mapResultSetToOrderItem(rs));
                    }
                }
            }
        }
    }

    private StockInRecord mapResultSetToStockIn(ResultSet rs) throws SQLException {
        StockInRecord record = new StockInRecord();
        record.setRecordId(rs.getString("record_id"));
        record.setOrderId(rs.getString("order_id"));
        record.setProductId(rs.getString("product_id"));
        record.setProductName(rs.getString("product_name"));
        record.setQuantity(rs.getInt("quantity"));
        record.setBatchNumber(rs.getString("batch_number"));
        record.setWarehouse(rs.getString("warehouse"));
        record.setOperatorId(rs.getString("operator_id"));
        record.setOperatorName(rs.getString("operator_name"));
        Timestamp time = rs.getTimestamp("in_time");
        if (time != null) {
            record.setTimestamp(time.toLocalDateTime());
        }
        record.setRemark(rs.getString("remark"));
        return record;
    }

    private StockOutRecord mapResultSetToStockOut(ResultSet rs) throws SQLException {
        StockOutRecord record = new StockOutRecord();
        record.setRecordId(rs.getString("record_id"));
        record.setProductId(rs.getString("product_id"));
        record.setProductName(rs.getString("product_name"));
        record.setQuantity(rs.getInt("quantity"));
        record.setRecipient(rs.getString("recipient"));
        record.setRecipientDept(rs.getString("recipient_dept"));
        record.setPurpose(rs.getString("purpose"));
        record.setOperatorId(rs.getString("operator_id"));
        record.setOperatorName(rs.getString("operator_name"));
        Timestamp time = rs.getTimestamp("out_time");
        if (time != null) {
            record.setTimestamp(time.toLocalDateTime());
        }
        record.setRemark(rs.getString("remark"));
        return record;
    }

    private OperationLog mapResultSetToLog(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.setLogId(rs.getString("log_id"));
        log.setUserId(rs.getString("user_id"));
        log.setUsername(rs.getString("username"));
        log.setOperation(rs.getString("operation"));
        log.setModule(rs.getString("module"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        Timestamp ts = rs.getTimestamp("operation_time");
        if (ts != null) {
            log.setOperationTime(ts.toLocalDateTime());
        }
        log.setSuccess(rs.getBoolean("success"));
        log.setErrorMessage(rs.getString("error_message"));
        return log;
    }

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    private void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    private void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getString("product_id"));
        product.setProductName(rs.getString("product_name"));
        product.setCategory(rs.getString("category"));
        product.setSpecification(rs.getString("specification"));
        product.setUnit(rs.getString("unit"));
        product.setPurchasePrice(rs.getDouble("purchase_price"));
        product.setSellingPrice(rs.getDouble("selling_price"));
        product.setCurrentStock(rs.getInt("current_stock"));
        product.setMinStock(rs.getInt("min_stock"));
        product.setMaxStock(rs.getInt("max_stock"));
        product.setSupplier(rs.getString("supplier"));
        product.setDescription(rs.getString("description"));
        product.setActive(rs.getBoolean("active"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            product.setCreateTime(createTime.toLocalDateTime());
        }

        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            product.setUpdateTime(updateTime.toLocalDateTime());
        }
        return product;
    }

    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            mysqlPool.returnConnection(conn);
        }
    }

    /**
     * 关闭连接池
     */
    public void shutdown() {
        if (mysqlPool != null) {
            mysqlPool.close();
        }
    }
}