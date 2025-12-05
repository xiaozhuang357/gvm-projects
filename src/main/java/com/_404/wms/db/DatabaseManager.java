package com._404.wms.db;

import com._404.wms.config.ConfigManager;
import com._404.wms.config.DatabaseConfig;
import com._404.wms.db.connection.ConnectionPool;
import com._404.wms.db.connection.MysqlConnectionPool;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库管理器 - 单例模式
 * 管理数据库连接池和表结构初始化
 */
public class DatabaseManager {
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    private static volatile DatabaseManager instance;

    private final ConnectionPool connectionPool;
    private final DatabaseConfig config;
    private boolean initialized = false;

    private DatabaseManager() {
        this.config = ConfigManager.getInstance().getDatabaseConfig();
        this.connectionPool = new MysqlConnectionPool(config);
        initializeTables();
        this.initialized = true;
    }

    /**
     * 获取单例实例
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
     * 获取数据库连接
     */
    public Connection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    /**
     * 释放数据库连接
     */
    public void releaseConnection(Connection connection) {
        connectionPool.releaseConnection(connection);
    }

    /**
     * 获取连接池
     */
    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }

    /**
     * 是否已初始化
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 初始化数据库表结构
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
