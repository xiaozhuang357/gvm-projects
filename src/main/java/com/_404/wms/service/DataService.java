package com._404.wms.service;

import com._404.wms.dao.*;
import com._404.wms.db.DatabaseManager;
import com._404.wms.model.*;
import com._404.wms.model.PurchaseOrder.OrderStatus;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

/**
 * 数据服务类 - 业务逻辑层核心组件
 * <p>
 * 功能说明:
 * 1. 提供统一的业务数据访问接口,封装DAO层
 * 2. 实现业务逻辑处理和数据验证
 * 3. 管理跨DAO的事务操作(如采购订单+库存更新)
 * 4. 提供ID生成、数据统计等公共服务
 * 5. 记录操作日志,支持审计追踪
 * <p>
 * 架构设计:
 * - Service层:业务逻辑封装
 * - DAO层:数据持久化
 * - Model层:数据模型
 * <p>
 * 核心功能模块:
 * 1. 用户管理:认证、增删改查、角色管理
 * 2. 商品管理:CRUD、库存预警查询
 * 3. 采购订单:创建、审批、到货确认、取消
 * 4. 库存管理:入库、出库、库存调整
 * 5. 操作日志:记录所有关键业务操作
 * <p>
 * 使用示例:
 * 
 * <pre>
 * // 创建服务实例
 * DataService service = new DataService();
 * 
 * // 用户认证
 * User user = service.authenticate("admin", "123456");
 * 
 * // 创建采购订单
 * PurchaseOrder order = new PurchaseOrder();
 * // ... 设置订单信息
 * service.addPurchaseOrder(order);
 * 
 * // 审批订单
 * service.approvePurchaseOrder(orderId, approverId, "同意");
 * </pre>
 * <p>
 * 注意事项:
 * - 所有数据操作都通过DAO层进行,不直接操作数据库
 * - 关键操作会记录操作日志
 * - ID生成使用时间戳+随机数确保唯一性
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class DataService {
    /** 日志记录器 */
    private static final Logger logger = Logger.getLogger(DataService.class.getName());

    // ==================== DAO实例 ====================
    /** 用户数据访问对象 */
    private final UserDao userDao;

    /** 商品数据访问对象 */
    private final ProductDao productDao;

    /** 采购订单数据访问对象 */
    private final PurchaseOrderDao purchaseOrderDao;

    /** 入库记录数据访问对象 */
    private final StockInRecordDao stockInRecordDao;

    /** 出库记录数据访问对象 */
    private final StockOutRecordDao stockOutRecordDao;

    /** 操作日志数据访问对象 */
    private final OperationLogDao operationLogDao;

    /** 数据库管理器 */
    private final DatabaseManager dbManager;

    /**
     * 构造函数
     * <p>
     * 初始化所有DAO实例和数据库管理器
     * 确保数据库表结构已创建
     */
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
     * 用户认证 - 验证用户名和密码
     * <p>
     * 认证成功后会自动更新用户的最后登录时间
     * 
     * @param username 用户名
     * @param password 密码(明文)
     * @return 认证成功返回User对象,失败返回null
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
     * 添加新用户
     * <p>
     * 如果用户ID为空,会自动生成唯一ID(格式:USR+时间戳)
     * 
     * @param user 用户对象
     */
    public void addUser(User user) {
        if (user.getUserId() == null || user.getUserId().isEmpty()) {
            user.setUserId(generateId("USR"));
        }
        userDao.save(user);
    }

    /**
     * 更新用户信息
     * <p>
     * 根据用户ID更新用户信息
     * 
     * @param user 包含更新信息的用户对象
     */
    public void updateUser(User user) {
        userDao.update(user);
    }

    /**
     * 删除用户
     * <p>
     * 物理删除用户记录
     * 注意:删除前应检查用户是否有关联数据(如采购订单)
     * 
     * @param userId 用户ID
     */
    public void deleteUser(String userId) {
        userDao.deleteById(userId);
    }

    /**
     * 根据ID获取用户
     * 
     * @param userId 用户ID
     * @return 用户对象,不存在返回null
     */
    public User getUserById(String userId) {
        return userDao.findById(userId).orElse(null);
    }

    /**
     * 获取所有用户列表
     * 
     * @return 所有用户的列表
     */
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    /**
     * 根据用户名获取用户
     * <p>
     * 用于检查用户名是否已存在
     * 
     * @param username 用户名
     * @return 用户对象,不存在返回null
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
