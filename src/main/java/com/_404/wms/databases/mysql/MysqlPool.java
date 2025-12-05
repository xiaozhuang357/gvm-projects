package com._404.wms.databases.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MysqlPool {
    // 数据库配置
    private final String url;
    private final String user;
    private final String password;
    private final String schema;

    // 连接池参数
    private final int poolSize;
    private final int maxWaitTime; // 最大等待时间（秒）

    // 连接存储
    private final BlockingQueue<Connection> pool;
    private final BlockingQueue<Timestamp> connectionTimes; // 记录每个连接的最后使用时间

    // 线程安全控制
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    // 状态控制
    private volatile boolean isClosed = false;
    private final AtomicInteger failCount = new AtomicInteger(0);

    // 线程池
    private final ScheduledExecutorService checkExecutor; // 用于检测连接健康
    private final ExecutorService connectionCreator; // 用于创建新连接

    // 构造函数
    public MysqlPool(String url, String user, String password, String schema,
            int poolSize, int maxWaitTime) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.schema = schema;
        this.poolSize = poolSize;
        this.maxWaitTime = maxWaitTime;
        this.pool = new ArrayBlockingQueue<>(poolSize);
        this.connectionTimes = new ArrayBlockingQueue<>(poolSize);

        // 初始化线程池
        this.checkExecutor = Executors.newScheduledThreadPool(1);
        this.connectionCreator = Executors.newFixedThreadPool(2);

        // 初始化连接池
        initializePool();

        // 启动连接检查任务
        startConnectionCheck();
    }

    // 初始化连接池
    private void initializePool() {
        try {
            // 加载MySQL驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            for (int i = 0; i < poolSize; i++) {
                createAndAddConnection();
            }
            System.out.println("MySQL connection pool initialized successfully. Pool size: " + pool.size());
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Failed to initialize MySQL connection pool: " + e.getMessage());
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    // 创建并添加连接
    private void createAndAddConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url, user, password);
        conn.setCatalog(schema);
        conn.setAutoCommit(true);

        // 设置连接参数
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        if (!pool.offer(conn)) {
            conn.close();
            throw new SQLException("Connection pool is full");
        }

        // 记录连接创建时间
        connectionTimes.offer(new Timestamp(System.currentTimeMillis()));
    }

    // 获取连接
    public Connection getConnection() throws SQLException {
        if (isClosed) {
            throw new SQLException("Connection pool is closed");
        }

        try {
            lock.lock();
            // 等待可用的连接
            long startTime = System.currentTimeMillis();
            while (pool.isEmpty()) {
                if (!notEmpty.await(maxWaitTime, TimeUnit.SECONDS)) {
                    throw new SQLException("Timeout waiting for connection");
                }
                if (System.currentTimeMillis() - startTime > maxWaitTime * 1000) {
                    throw new SQLException("Timeout waiting for connection");
                }
            }

            Connection conn = pool.poll();// 取出一个待复用的数据库连接
            connectionTimes.poll(); // 移除对应的时间记录

            if (conn != null && !conn.isClosed() && conn.isValid(5)) {// 验证连接是否有效
                return conn;
            } else {
                // 连接无效，创建新的
                closeConnection(conn);
                return createNewConnection();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for connection", e);
        } finally {
            lock.unlock();
        }
    }

    // 归还连接
    public void returnConnection(Connection conn) {
        if (conn == null || isClosed) {
            closeConnection(conn);
            return;
        }

        try {
            // 检查连接是否仍然有效
            if (!conn.isClosed() && conn.isValid(5)) {
                try {
                    lock.lock();
                    if (!pool.offer(conn)) {
                        // 池已满，关闭连接
                        closeConnection(conn);
                    } else {
                        // 记录当前时间
                        connectionTimes.offer(new Timestamp(System.currentTimeMillis()));
                        notEmpty.signal();
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                // 连接无效，关闭并创建新的
                closeConnection(conn);
                recreateConnectionAsync();
            }
        } catch (SQLException e) {
            System.err.println("Error returning connection: " + e.getMessage());
            closeConnection(conn);
        }
    }

    // 启动连接检查
    private void startConnectionCheck() {
        checkExecutor.scheduleAtFixedRate(() -> {
            try {
                checkConnection();
            } catch (Exception e) {
                System.err.println("Error during connection check: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    // 检查连接健康状态
    private void checkConnection() {
        int targetCount = pool.size();
        int processed = 0;
        Timestamp now = new Timestamp(System.currentTimeMillis());

        while (processed < targetCount) {
            Connection conn = null;
            Timestamp lastTime = null;

            try {
                lock.lock();
                if (pool.isEmpty()) {
                    break;
                }
                conn = pool.poll();
                lastTime = connectionTimes.poll();
            } finally {
                lock.unlock();
            }

            if (conn != null && lastTime != null) {
                try {
                    // 检查连接是否超过5秒没有使用
                    long idleTime = (now.getTime() - lastTime.getTime()) / 1000;
                    if (idleTime >= 5) {
                        // 执行健康检查查询
                        try (java.sql.Statement stmt = conn.createStatement()) {
                            stmt.executeQuery("SELECT 1");
                            System.out.println("Connection health check passed");
                        }
                    }

                    // 返回连接池
                    returnConnection(conn);
                } catch (SQLException e) {
                    System.err.println("Error checking connection health: " + e.getMessage());
                    failCount.incrementAndGet();
                    // 关闭损坏的连接
                    closeConnection(conn);
                    // 创建新连接
                    recreateConnectionAsync();
                }

                processed++;
            }
        }

        // 处理失败的重连
        processFailedReconnections();
    }

    // 关闭连接
    private void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    // 异步重建连接
    private void recreateConnectionAsync() {
        connectionCreator.submit(() -> {
            try {
                recreateConnection();
                failCount.decrementAndGet();
            } catch (SQLException e) {
                System.err.println("Failed to recreate connection: " + e.getMessage());
            }
        });
    }

    // 重建连接
    private void recreateConnection() throws SQLException {
        createAndAddConnection();
        System.out.println("MySQL connection recreated successfully");
    }

    // 处理失败的重连
    private void processFailedReconnections() {
        int currentFailCount = failCount.get();
        while (currentFailCount > 0) {
            try {
                recreateConnection();
                failCount.decrementAndGet();
            } catch (SQLException e) {
                System.err.println("Failed to process reconnection: " + e.getMessage());
                break;
            }
            currentFailCount = failCount.get();
        }
    }

    // 创建新连接（当池中没有有效连接时）
    private Connection createNewConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url, user, password);
        conn.setCatalog(schema);
        return conn;
    }

    // 关闭连接池
    public void close() {
        isClosed = true;

        // 关闭所有连接
        try {
            lock.lock();
            while (!pool.isEmpty()) {
                Connection conn = pool.poll();
                closeConnection(conn);
            }
            connectionTimes.clear();
        } finally {
            lock.unlock();
        }

        // 关闭线程池
        checkExecutor.shutdown();
        connectionCreator.shutdown();

        try {
            if (!checkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                checkExecutor.shutdownNow();
            }
            if (!connectionCreator.awaitTermination(5, TimeUnit.SECONDS)) {
                connectionCreator.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            checkExecutor.shutdownNow();
            connectionCreator.shutdownNow();
        }

        System.out.println("MySQL connection pool closed");
    }

    // 获取当前连接池大小
    public int getPoolSize() {
        return pool.size();
    }

    // 获取失败计数
    public int getFailCount() {
        return failCount.get();
    }

    // 获取可用连接数
    public int getAvailableConnections() {
        try {
            lock.lock();
            return pool.size();
        } finally {
            lock.unlock();
        }
    }

    // 检查连接池是否已关闭
    public boolean isClosed() {
        return isClosed;
    }
}