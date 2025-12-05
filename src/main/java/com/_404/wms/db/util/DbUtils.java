package com._404.wms.db.util;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库工具类
 * 提供通用的数据库操作辅助方法
 */
public class DbUtils {
    private static final Logger logger = Logger.getLogger(DbUtils.class.getName());

    private DbUtils() {
        // 工具类不允许实例化
    }

    /**
     * 安全关闭ResultSet
     */
    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.log(Level.FINE, "Error closing ResultSet", e);
            }
        }
    }

    /**
     * 安全关闭Statement
     */
    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.log(Level.FINE, "Error closing Statement", e);
            }
        }
    }

    /**
     * 安全关闭Connection（注意：如果使用连接池，应该用releaseConnection）
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.log(Level.FINE, "Error closing Connection", e);
            }
        }
    }

    /**
     * 关闭所有资源
     */
    public static void closeAll(ResultSet rs, Statement stmt, Connection conn) {
        closeQuietly(rs);
        closeQuietly(stmt);
        closeQuietly(conn);
    }

    /**
     * 关闭ResultSet和Statement
     */
    public static void closeAll(ResultSet rs, Statement stmt) {
        closeQuietly(rs);
        closeQuietly(stmt);
    }

    /**
     * 安全回滚事务
     */
    public static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Error rolling back transaction", e);
            }
        }
    }

    /**
     * 重置自动提交状态
     */
    public static void resetAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.log(Level.FINE, "Error resetting auto-commit", e);
            }
        }
    }

    /**
     * 设置PreparedStatement的String参数，处理null值
     */
    public static void setStringOrNull(PreparedStatement pstmt, int index, String value) throws SQLException {
        if (value != null) {
            pstmt.setString(index, value);
        } else {
            pstmt.setNull(index, Types.VARCHAR);
        }
    }

    /**
     * 设置PreparedStatement的Integer参数，处理null值
     */
    public static void setIntOrNull(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value != null) {
            pstmt.setInt(index, value);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }

    /**
     * 设置PreparedStatement的Double参数，处理null值
     */
    public static void setDoubleOrNull(PreparedStatement pstmt, int index, Double value) throws SQLException {
        if (value != null) {
            pstmt.setDouble(index, value);
        } else {
            pstmt.setNull(index, Types.DOUBLE);
        }
    }

    /**
     * 设置PreparedStatement的Timestamp参数，处理null值
     */
    public static void setTimestampOrNull(PreparedStatement pstmt, int index, Timestamp value) throws SQLException {
        if (value != null) {
            pstmt.setTimestamp(index, value);
        } else {
            pstmt.setNull(index, Types.TIMESTAMP);
        }
    }

    /**
     * 设置PreparedStatement的Date参数，处理null值
     */
    public static void setDateOrNull(PreparedStatement pstmt, int index, java.sql.Date value) throws SQLException {
        if (value != null) {
            pstmt.setDate(index, value);
        } else {
            pstmt.setNull(index, Types.DATE);
        }
    }

    /**
     * 从ResultSet安全获取String
     */
    public static String getStringOrNull(ResultSet rs, String column) throws SQLException {
        String value = rs.getString(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 从ResultSet安全获取Integer
     */
    public static Integer getIntOrNull(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 从ResultSet安全获取Double
     */
    public static Double getDoubleOrNull(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 从ResultSet安全获取Boolean
     */
    public static Boolean getBooleanOrNull(ResultSet rs, String column) throws SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * 生成IN子句的占位符
     * 
     * @param count 参数数量
     * @return 如 "?, ?, ?"
     */
    public static String generatePlaceholders(int count) {
        if (count <= 0)
            return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("?");
        }
        return sb.toString();
    }

    /**
     * 打印SQL异常详细信息
     */
    public static void logSQLException(SQLException e, String operation) {
        logger.log(Level.SEVERE, String.format(
                "SQL Error during %s: SQLState=%s, ErrorCode=%d, Message=%s",
                operation, e.getSQLState(), e.getErrorCode(), e.getMessage()), e);
    }
}
