package com._404.wms.service;

import com._404.wms.dao.*;
import com._404.wms.db.DatabaseManager;
import com._404.wms.model.*;
import com._404.wms.model.PurchaseOrder.OrderStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * 数据服务类 - 统一的业务数据访问入口
 * 使用新的DAO层进行数据访问
 */
public class DataService {
    private static final Logger logger = Logger.getLogger(DataService.class.getName());

    // DAO实例
    private final UserDao userDao;
    private final ProductDao productDao;
    private final PurchaseOrderDao purchaseOrderDao;
    private final StockInRecordDao stockInRecordDao;
    private final StockOutRecordDao stockOutRecordDao;
    private final OperationLogDao operationLogDao;

    // 数据库管理器
    private final DatabaseManager dbManager;

    public DataService() {
        // 初始化数据库管理器（确保表结构已创建）
        this.dbManager = DatabaseManager.getInstance();

        // 获取DAO实例
        DaoFactory daoFactory = DaoFactory.getInstance();
        this.userDao = daoFactory.getUserDao();
        this.productDao = daoFactory.getProductDao();
        this.purchaseOrderDao = daoFactory.getPurchaseOrderDao();
        this.stockInRecordDao = daoFactory.getStockInRecordDao();
        this.stockOutRecordDao = daoFactory.getStockOutRecordDao();
        this.operationLogDao = daoFactory.getOperationLogDao();

        logger.info("DataService initialized successfully");
    }

    // ==================== 用户管理 ====================

    /**
     * 用户认证
     */
    public User authenticate(String username, String password) {
        Optional<User> user = userDao.authenticate(username, password);
        if (user.isPresent()) {
            // 更新最后登录时间
            userDao.updateLastLoginTime(user.get().getUserId());
        }
        return user.orElse(null);
    }

    /**
     * 添加用户
     */
    public void addUser(User user) {
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId(generateId("USR"));
        }
        userDao.save(user);
    }

    /**
     * 更新用户
     */
    public void updateUser(User user) {
        userDao.update(user);
    }

    /**
     * 删除用户
     */
    public void deleteUser(String userId) {
        userDao.deleteById(userId);
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(String userId) {
        return userDao.findById(userId).orElse(null);
    }

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        return userDao.findByUsername(username).orElse(null);
    }

    // ==================== 商品管理 ====================

    /**
     * 添加商品
     */
    public void addProduct(Product product) {
        if (product.getProductId() == null || product.getProductId().isEmpty()) {
            product.setProductId(generateId("PRD"));
        }
        productDao.save(product);
    }

    /**
     * 更新商品
     */
    public void updateProduct(Product product) {
        productDao.update(product);
    }

    /**
     * 删除商品
     */
    public void deleteProduct(String productId) {
        productDao.deleteById(productId);
    }

    /**
     * 根据ID获取商品
     */
    public Product getProductById(String productId) {
        return productDao.findById(productId).orElse(null);
    }

    /**
     * 获取所有商品
     */
    public List<Product> getAllProducts() {
        return productDao.findAll();
    }

    /**
     * 按类别查询商品
     */
    public List<Product> getProductsByCategory(String category) {
        return productDao.findByCategory(category);
    }

    /**
     * 查询需要补货的商品
     */
    public List<Product> getLowStockProducts() {
        return productDao.findLowStock();
    }

    /**
     * 更新商品库存
     */
    public boolean updateProductStock(String productId, int quantity) {
        return productDao.updateStock(productId, quantity);
    }

    /**
     * 获取所有商品类别
     */
    public List<String> getAllCategories() {
        return productDao.findAllCategories();
    }

    // ==================== 采购订单管理 ====================

    /**
     * 添加采购订单
     */
    public void addPurchaseOrder(PurchaseOrder order) {
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId(generateId("ORD"));
        }
        purchaseOrderDao.save(order);
    }

    /**
     * 更新采购订单
     */
    public void updatePurchaseOrder(PurchaseOrder order) {
        purchaseOrderDao.update(order);
    }

    /**
     * 根据ID获取采购订单
     */
    public PurchaseOrder getPurchaseOrderById(String orderId) {
        return purchaseOrderDao.findById(orderId).orElse(null);
    }

    /**
     * 获取所有采购订单
     */
    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderDao.findAll();
    }

    /**
     * 根据状态查询订单
     */
    public List<PurchaseOrder> getOrdersByStatus(OrderStatus status) {
        return purchaseOrderDao.findByStatus(status);
    }

    /**
     * 根据采购员查询订单
     */
    public List<PurchaseOrder> getOrdersByPurchaser(String purchaserId) {
        return purchaseOrderDao.findByPurchaserId(purchaserId);
    }

    /**
     * 查询待审批订单
     */
    public List<PurchaseOrder> getPendingApprovalOrders() {
        return purchaseOrderDao.findPendingApproval();
    }

    /**
     * 审批订单
     */
    public boolean approveOrder(String orderId, String approverId, String approverName,
            OrderStatus status, String comment) {
        return purchaseOrderDao.approve(orderId, approverId, approverName, status, comment);
    }

    /**
     * 更新订单状态
     */
    public boolean updateOrderStatus(String orderId, OrderStatus status) {
        return purchaseOrderDao.updateStatus(orderId, status);
    }

    // ==================== 出入库管理 ====================

    /**
     * 添加入库记录
     */
    public void addStockInRecord(StockInRecord record) {
        if (record.getRecordId() == null || record.getRecordId().isEmpty()) {
            record.setRecordId(generateId("SIN"));
        }

        // 更新商品库存
        boolean stockUpdated = productDao.updateStock(record.getProductId(), record.getQuantity());
        if (!stockUpdated) {
            logger.warning("Failed to update product stock for: " + record.getProductId());
        }

        stockInRecordDao.save(record);
    }

    /**
     * 获取所有入库记录
     */
    public List<StockInRecord> getAllStockInRecords() {
        return stockInRecordDao.findAll();
    }

    /**
     * 根据订单ID获取入库记录
     */
    public List<StockInRecord> getStockInRecordsByOrderId(String orderId) {
        return stockInRecordDao.findByOrderId(orderId);
    }

    /**
     * 添加出库记录
     */
    public void addStockOutRecord(StockOutRecord record) {
        if (record.getRecordId() == null || record.getRecordId().isEmpty()) {
            record.setRecordId(generateId("SOT"));
        }

        // 更新商品库存（减少）
        boolean stockUpdated = productDao.updateStock(record.getProductId(), -record.getQuantity());
        if (!stockUpdated) {
            logger.warning("Failed to update product stock for: " + record.getProductId());
        }

        stockOutRecordDao.save(record);
    }

    /**
     * 获取所有出库记录
     */
    public List<StockOutRecord> getAllStockOutRecords() {
        return stockOutRecordDao.findAll();
    }

    /**
     * 根据商品ID获取出库记录
     */
    public List<StockOutRecord> getStockOutRecordsByProductId(String productId) {
        return stockOutRecordDao.findByProductId(productId);
    }

    // ==================== 日志管理 ====================

    /**
     * 添加操作日志
     */
    public void addLog(OperationLog log) {
        if (log.getLogId() == null || log.getLogId().isEmpty()) {
            log.setLogId(generateId("LOG"));
        }
        operationLogDao.save(log);
    }

    /**
     * 获取所有日志
     */
    public List<OperationLog> getAllLogs() {
        return operationLogDao.findAll();
    }

    /**
     * 根据用户查询日志
     */
    public List<OperationLog> getLogsByUser(String userId) {
        return operationLogDao.findByUserId(userId);
    }

    /**
     * 获取最近的日志
     */
    public List<OperationLog> getRecentLogs(int limit) {
        return operationLogDao.findRecent(limit);
    }

    // ==================== 统计查询 ====================

    /**
     * 统计各状态订单数量
     */
    public Map<OrderStatus, Long> countOrdersByStatus() {
        return purchaseOrderDao.countByStatus();
    }

    /**
     * 统计各部门出库数量
     */
    public Map<String, Integer> sumStockOutByDept() {
        return stockOutRecordDao.sumQuantityByDept();
    }

    /**
     * 根据时间范围查询入库记录
     */
    public List<StockInRecord> getStockInRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return stockInRecordDao.findByTimeBetween(startTime, endTime);
    }

    /**
     * 根据时间范围查询出库记录
     */
    public List<StockOutRecord> getStockOutRecordsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return stockOutRecordDao.findByTimeBetween(startTime, endTime);
    }

    // ==================== 数据备份恢复 ====================

    /**
     * 数据备份（保留接口，暂时返回true）
     */
    public boolean backup(String backupPath) {
        // TODO: 实现数据库备份逻辑
        logger.info("Backup requested to: " + backupPath);
        return true;
    }

    /**
     * 数据恢复（保留接口，暂时返回true）
     */
    public boolean restore(String backupPath) {
        // TODO: 实现数据库恢复逻辑
        logger.info("Restore requested from: " + backupPath);
        return true;
    }

    // ==================== 辅助方法 ====================

    /**
     * 生成唯一ID
     */
    private String generateId(String prefix) {
        return prefix + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
    }

    /**
     * 获取数据库连接池状态
     */
    public String getPoolStats() {
        return dbManager.getPoolStats();
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        dbManager.shutdown();
        logger.info("DataService shutdown complete");
    }

    /**
     * 保存数据（兼容旧接口，新实现中数据直接写入数据库，无需手动保存）
     */
    public void saveData() {
        // 新实现中数据直接写入数据库，此方法保留用于兼容性
        logger.fine("saveData called - data is automatically persisted to database");
    }

    /**
     * 加载数据（兼容旧接口，新实现中数据从数据库实时读取，无需预加载）
     */
    public void loadData() {
        // 新实现中数据从数据库实时读取，此方法保留用于兼容性
        logger.fine("loadData called - data is loaded from database on demand");
    }
}
