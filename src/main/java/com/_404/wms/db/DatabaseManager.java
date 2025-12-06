package com._404.wms.db;

import com._404.wms.config.ConfigManager;
import com._404.wms.config.DatabaseConfig;
import com._404.wms.db.connection.ConnectionPool;
import com._404.wms.db.connection.MysqlConnectionPool;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库管理器 - 数据库连接和表结构管理的核心组件
 * <p>
 * 功能说明:
 * 1. 管理数据库连接池的生命周期
 * 2. 自动创建和维护数据库表结构
 * 3. 提供统一的连接获取和释放接口
 * 4. 单例模式确保全局唯一实例
 * 5. 线程安全的双重检查锁(DCL)实现
 * <p>
 * 管理的数据库表:
 * - users:用户表
 * - products:商品表
 * - purchase_orders:采购订单主表
 * - purchase_order_items:采购订单明细表
 * - stock_in_records:入库记录表
 * - stock_out_records:出库记录表
 * - operation_logs:操作日志表
 * <p>
 * 表结构特点:
 * - 使用IF NOT EXISTS避免重复创建
 * - 设置合适的字段类型和长度
 * - 定义主键和外键约束
 * - 使用UTF-8字符集
 * <p>
 * 使用示例:
 * 
 * <pre>
 * // 获取单例实例
 * DatabaseManager dbManager = DatabaseManager.getInstance();
 * 
 * // 获取数据库连接
 * Connection conn = dbManager.getConnection();
 * try {
 *     // 执行数据库操作
 *     PreparedStatement ps = conn.prepareStatement("SELECT * FROM users");
 *     ResultSet rs = ps.executeQuery();
 *     // ...
 * } finally {
 *     // 释放连接回连接池
 *     dbManager.releaseConnection(conn);
 * }
 * </pre>
 * <p>
 * 注意事项:
 * - 首次调用getInstance()会触发表结构初始化
 * - 连接池使用后必须调用releaseConnection()释放
 * - DDL语句失败只记录日志不抛出异常,允许部分表创建失败
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class DatabaseManager {
    /** 日志记录器 */
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    /** 单例实例,使用volatile保证多线程可见性 */
    private static volatile DatabaseManager instance;

    /** 数据库连接池 */
    private final ConnectionPool connectionPool;

    /** 数据库配置信息 */
    private final DatabaseConfig config;

    /** 初始化完成标志 */
    private boolean initialized = false;

    /**
     * 私有构造函数
     * <p>
     * 执行步骤:
     * 1. 从ConfigManager读取数据库配置
     * 2. 创建MySQL连接池
     * 3. 调用initializeTables()创建表结构
     * 4. 设置initialized标志为true
     */
    private DatabaseManager() {
        this.config = ConfigManager.getInstance().getDatabaseConfig();
        this.connectionPool = new MysqlConnectionPool(config);
        initializeTables();
        this.initialized = true;
    }

    /**
     * 获取单例实例(线程安全的双重检查锁)
     * <p>
     * 使用DCL模式确保:
     * - 延迟初始化:只在首次调用时创建实例
     * - 线程安全:synchronized保证并发安全
     * - 性能优化:避免每次调用都加锁
     * 
     * @return DatabaseManager唯一实例
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * 从连接池获取数据库连接
     * <p>
     * 注意:使用完毕后必须调用releaseConnection()释放连接
     * 
     * @return 数据库连接对象
     * @throws SQLException 连接池无可用连接时抛出
     */
    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    /**
     * 释放数据库连接回连接池
     * <p>
     * 将连接归还到连接池供其他线程使用
     * 不会真正关闭物理连接
     * 
     * @param connection 要释放的连接对象
     */
    public void releaseConnection(Connection connection) {
        connectionPool.releaseConnection(connection);
    }

    /**
     * 获取连接池实例
     * <p>
     * 用于高级操作,如关闭连接池
     * 
     * @return ConnectionPool实例
     */
    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    /**
     * 检查数据库管理器是否已初始化
     * <p>
     * 初始化包括连接池创建和表结构创建
     * 
     * @return true=已初始化,false=初始化中
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 初始化数据库表结构
     * <p>
     * 执行所有DDL语句创建表:
     * - 使用CREATE TABLE IF NOT EXISTS避免重复创建
     * - 失败的DDL只记录日志,不阻止后续表的创建
     * - 所有表使用UTF8字符集
     */
    private void initializeTables() {
        String[] ddlStatements = {
                // 用户表
                """
                        CREATE TABLE IF NOT EXISTS users (
                            user_id VARCHAR(50) PRIMARY KEY,
                            username VARCHAR(50) NOT NULL UNIQUE,
                            password VARCHAR(100) NOT NULL,
                            real_name VARCHAR(50),
                            role VARCHAR(50),
                            department VARCHAR(50),
                            email VARCHAR(100),
                            phone VARCHAR(20),
                            active BOOLEAN DEFAULT TRUE,
                            create_time DATETIME,
                            last_login_time DATETIME,
                            INDEX idx_username (username),
                            INDEX idx_role (role)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """,

                // 商品表
                """
                        CREATE TABLE IF NOT EXISTS products (
                            product_id VARCHAR(50) PRIMARY KEY,
                            product_name VARCHAR(100) NOT NULL,
                            category VARCHAR(100),
                            specification VARCHAR(200),
                            unit VARCHAR(20),
                            purchase_price DOUBLE,
                            selling_price DOUBLE,
                            current_stock INT DEFAULT 0,
                            min_stock INT DEFAULT 0,
                            max_stock INT DEFAULT 0,
                            supplier VARCHAR(200),
                            description TEXT,
                            active BOOLEAN DEFAULT TRUE,
                            create_time DATETIME,
                            update_time DATETIME,
                            INDEX idx_category (category),
                            INDEX idx_product_name (product_name)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """,

                // 采购订单表
                """
                        CREATE TABLE IF NOT EXISTS purchase_orders (
                            order_id VARCHAR(50) PRIMARY KEY,
                            purchaser_id VARCHAR(50),
                            purchaser_name VARCHAR(100),
                            supplier VARCHAR(200),
                            total_amount DOUBLE DEFAULT 0,
                            status VARCHAR(50),
                            approver_id VARCHAR(50),
                            approver_name VARCHAR(100),
                            approve_comment TEXT,
                            create_time DATETIME,
                            approve_time DATETIME,
                            arrival_time DATETIME,
                            expected_delivery_date DATE,
                            remark TEXT,
                            INDEX idx_purchaser (purchaser_id),
                            INDEX idx_status (status),
                            INDEX idx_create_time (create_time)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """,

                // 订单明细表
                """
                        CREATE TABLE IF NOT EXISTS purchase_order_items (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            order_id VARCHAR(50) NOT NULL,
                            product_id VARCHAR(50),
                            product_name VARCHAR(200),
                            specification VARCHAR(200),
                            quantity INT DEFAULT 0,
                            unit_price DOUBLE DEFAULT 0,
                            subtotal DOUBLE DEFAULT 0,
                            INDEX idx_order_id (order_id),
                            FOREIGN KEY (order_id) REFERENCES purchase_orders(order_id) ON DELETE CASCADE
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """,

                // 入库记录表
                """
                        CREATE TABLE IF NOT EXISTS stock_in_records (
                            record_id VARCHAR(50) PRIMARY KEY,
                            order_id VARCHAR(50),
                            product_id VARCHAR(50),
                            product_name VARCHAR(200),
                            quantity INT DEFAULT 0,
                            batch_number VARCHAR(100),
                            warehouse VARCHAR(200),
                            operator_id VARCHAR(50),
                            operator_name VARCHAR(100),
                            in_time DATETIME,
                            remark TEXT,
                            INDEX idx_product (product_id),
                            INDEX idx_in_time (in_time)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """,

                // 出库记录表
                """
                        CREATE TABLE IF NOT EXISTS stock_out_records (
                            record_id VARCHAR(50) PRIMARY KEY,
                            product_id VARCHAR(50),
                            product_name VARCHAR(200),
                            quantity INT DEFAULT 0,
                            recipient VARCHAR(100),
                            recipient_dept VARCHAR(100),
                            purpose VARCHAR(255),
                            operator_id VARCHAR(50),
                            operator_name VARCHAR(100),
                            out_time DATETIME,
                            remark TEXT,
                            INDEX idx_product (product_id),
                            INDEX idx_out_time (out_time)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """,

                // 操作日志表
                """
                        CREATE TABLE IF NOT EXISTS operation_logs (
                            log_id VARCHAR(50) PRIMARY KEY,
                            user_id VARCHAR(50),
                            username VARCHAR(100),
                            operation VARCHAR(100),
                            module VARCHAR(100),
                            details TEXT,
                            ip_address VARCHAR(50),
                            operation_time DATETIME,
                            success BOOLEAN DEFAULT TRUE,
                            error_message TEXT,
                            INDEX idx_user (user_id),
                            INDEX idx_operation_time (operation_time),
                            INDEX idx_module (module)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
                        """
        };

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = connectionPool.getConnection();
            stmt = conn.createStatement();

            for (String ddl : ddlStatements) {
                try {
                    stmt.execute(ddl);
                } catch (SQLException e) {
                    logger.log(Level.WARNING, "Failed to execute DDL: " + e.getMessage());
                }
            }

            logger.info("Database tables initialized successfully");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database tables", e);
            throw new RuntimeException("Failed to initialize database", e);
        } finally {
            closeQuietly(stmt);
            if (conn != null) {
                connectionPool.releaseConnection(conn);
            }
        }
    }

    /**
     * 静默关闭Statement
     */
    private void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    /**
     * 关闭数据库管理器
     */
    public void shutdown() {
        if (connectionPool != null) {
            connectionPool.shutdown();
        }
        logger.info("DatabaseManager shutdown complete");
    }

    /**
     * 获取连接池状态
     */
    public String getPoolStats() {
        if (connectionPool instanceof MysqlConnectionPool) {
            return ((MysqlConnectionPool) connectionPool).getPoolStats();
        }
        return String.format("Pool[available=%d, active=%d]",
                connectionPool.getAvailableConnections(),
                connectionPool.getActiveConnections());
    }
}
