package com._404.wms.db.connection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 数据库连接池接口
 * 定义连接池的基本操作
 */
public interface ConnectionPool {

    /**
     * 获取数据库连接
     * 
     * @return Connection 数据库连接
     * @throws SQLException 获取连接失败时抛出
     */
    Connection getConnection() throws SQLException;

    /**
     * 归还数据库连接到连接池
     * 
     * @param connection 要归还的连接
     */
    void releaseConnection(Connection connection);

    /**
     * 获取当前可用连接数
     * 
     * @return 可用连接数量
     */
    int getAvailableConnections();

    /**
     * 获取正在使用的连接数
     * 
     * @return 使用中的连接数量
     */
    int getActiveConnections();

    /**
     * 获取连接池总大小
     * 
     * @return 连接池大小
     */
    int getPoolSize();

    /**
     * 关闭连接池，释放所有资源
     */
    void shutdown();

    /**
     * 检查连接池是否已关闭
     * 
     * @return true表示已关闭
     */
    boolean isClosed();
}
