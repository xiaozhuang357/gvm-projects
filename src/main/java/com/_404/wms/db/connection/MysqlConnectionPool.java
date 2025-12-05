package com._404.wms.db.connection;

import com._404.wms.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MySQL连接池实现
 * 提供高效的数据库连接管理
 */
public class MysqlConnectionPool implements ConnectionPool {
    private static final Logger logger = Logger.getLogger(MysqlConnectionPool.class.getName());

    // 连接池配置
    private final DatabaseConfig config;

    // 连接存储
    private final BlockingQueue<Connection> availableConnections;
    private final ConcurrentHashMap<Connection, Long> activeConnections;

    // 状态控制
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    // 后台任务执行器
    private final ScheduledExecutorService maintenanceExecutor;

    // 锁
    private final Object poolLock = new Object();

    /**
     * 构造函数
     * 
     * @param config 数据库配置
     */
    public MysqlConnectionPool(DatabaseConfig config) {
        this.config = config;
        this.availableConnections = new LinkedBlockingQueue<>(config.getPoolSize());
        this.activeConnections = new ConcurrentHashMap<>();
        this.maintenanceExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "ConnectionPool-Maintenance");
            t.setDaemon(true);
            return t;
        });

        initializePool();
        startMaintenanceTasks();
    }

    /**
     * 初始化连接池
     */
    private void initializePool() {
        try {
            // 加载驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 创建初始连接
            int initialSize = Math.min(config.getPoolSize() / 2 + 1, config.getPoolSize());
            for (int i = 0; i < initialSize; i++) {
                Connection conn = createConnection();
                if (conn != null) {
                    availableConnections.offer(conn);
                    totalConnections.incrementAndGet();
                }
            }

            logger.info(String.format("Connection pool initialized with %d connections",
                    availableConnections.size()));

        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "MySQL driver not found", e);
            throw new RuntimeException("Failed to load MySQL driver", e);
        }
    }

    /**
     * 创建新的数据库连接
     */
    private Connection createConnection() {
        try {
            Connection conn = DriverManager.getConnection(
                    config.getUrl() + config.getSchema(),
                    config.getUsername(),
                    config.getPassword());
            conn.setAutoCommit(config.isAutoCommit());
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            return conn;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to create connection", e);
            return null;
        }
    }

    /**
     * 启动维护任务
     */
    private void startMaintenanceTasks() {
        // 连接健康检查任务
        maintenanceExecutor.scheduleAtFixedRate(
                this::validateConnections,
                60, 60, TimeUnit.SECONDS);

        // 连接泄漏检测任务
        maintenanceExecutor.scheduleAtFixedRate(
                this::detectLeakedConnections,
                120, 120, TimeUnit.SECONDS);
    }

    /**
     * 验证连接池中的连接
     */
    private void validateConnections() {
        if (isClosed.get())
            return;

        int checked = 0;
        int removed = 0;

        synchronized (poolLock) {
            int size = availableConnections.size();
            for (int i = 0; i < size && !isClosed.get(); i++) {
                Connection conn = availableConnections.poll();
                if (conn == null)
                    break;

                checked++;
                if (isConnectionValid(conn)) {
                    availableConnections.offer(conn);
                } else {
                    closeConnection(conn);
                    removed++;
                    totalConnections.decrementAndGet();
                }
            }
        }

        if (removed > 0) {
            logger.info(String.format("Connection validation: checked=%d, removed=%d", checked, removed));
            ensureMinConnections();
        }
    }

    /**
     * 检测泄漏的连接
     */
    private void detectLeakedConnections() {
        if (isClosed.get())
            return;

        long now = System.currentTimeMillis();
        long leakThreshold = config.getMaxLifetimeMs();

        for (var entry : activeConnections.entrySet()) {
            if (now - entry.getValue() > leakThreshold) {
                logger.warning("Possible connection leak detected, connection in use for " +
                        (now - entry.getValue()) / 1000 + " seconds");
            }
        }
    }

    /**
     * 确保连接池有最少的连接数
     */
    private void ensureMinConnections() {
        int minConnections = config.getPoolSize() / 2;

        while (totalConnections.get() < minConnections && !isClosed.get()) {
            Connection conn = createConnection();
            if (conn != null) {
                if (availableConnections.offer(conn)) {
                    totalConnections.incrementAndGet();
                } else {
                    closeConnection(conn);
                    break;
                }
            } else {
                break;
            }
        }
    }

    /**
     * 检查连接是否有效
     */
    private boolean isConnectionValid(Connection conn) {
        try {
            return conn != null && !conn.isClosed() &&
                    conn.isValid(config.getValidationTimeoutSec());
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 关闭连接
     */
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.log(Level.FINE, "Error closing connection", e);
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (isClosed.get()) {
            throw new SQLException("Connection pool is closed");
        }

        Connection conn = null;
        long startTime = System.currentTimeMillis();

        while (conn == null) {
            // 检查超时
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= config.getMaxWaitTimeMs()) {
                throw new SQLException("Timeout waiting for connection, waited " + elapsed + "ms");
            }

            // 尝试从池中获取
            try {
                conn = availableConnections.poll(
                        Math.min(1000, config.getMaxWaitTimeMs() - elapsed),
                        TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Interrupted while waiting for connection", e);
            }

            if (conn != null) {
                // 验证连接
                if (isConnectionValid(conn)) {
                    activeConnections.put(conn, System.currentTimeMillis());
                    return conn;
                } else {
                    closeConnection(conn);
                    totalConnections.decrementAndGet();
                    conn = null;
                }
            } else {
                // 池为空，尝试创建新连接
                synchronized (poolLock) {
                    if (totalConnections.get() < config.getPoolSize()) {
                        conn = createConnection();
                        if (conn != null) {
                            totalConnections.incrementAndGet();
                            activeConnections.put(conn, System.currentTimeMillis());
                            return conn;
                        }
                    }
                }
            }
        }

        throw new SQLException("Failed to get connection");
    }

    @Override
    public void releaseConnection(Connection connection) {
        if (connection == null)
            return;

        activeConnections.remove(connection);

        if (isClosed.get()) {
            closeConnection(connection);
            return;
        }

        if (isConnectionValid(connection)) {
            try {
                // 重置连接状态
                if (!connection.getAutoCommit()) {
                    connection.setAutoCommit(true);
                }

                if (!availableConnections.offer(connection)) {
                    closeConnection(connection);
                    totalConnections.decrementAndGet();
                }
            } catch (SQLException e) {
                closeConnection(connection);
                totalConnections.decrementAndGet();
            }
        } else {
            closeConnection(connection);
            totalConnections.decrementAndGet();
            ensureMinConnections();
        }
    }

    @Override
    public int getAvailableConnections() {
        return availableConnections.size();
    }

    @Override
    public int getActiveConnections() {
        return activeConnections.size();
    }

    @Override
    public int getPoolSize() {
        return totalConnections.get();
    }

    @Override
    public void shutdown() {
        if (isClosed.compareAndSet(false, true)) {
            logger.info("Shutting down connection pool...");

            // 停止维护任务
            maintenanceExecutor.shutdown();
            try {
                maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 关闭活动连接
            for (Connection conn : activeConnections.keySet()) {
                closeConnection(conn);
            }
            activeConnections.clear();

            // 关闭可用连接
            Connection conn;
            while ((conn = availableConnections.poll()) != null) {
                closeConnection(conn);
            }

            totalConnections.set(0);
            logger.info("Connection pool shutdown complete");
        }
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    /**
     * 获取连接池状态信息
     */
    public String getPoolStats() {
        return String.format(
                "ConnectionPool[total=%d, available=%d, active=%d, closed=%s]",
                totalConnections.get(),
                availableConnections.size(),
                activeConnections.size(),
                isClosed.get());
    }
}
