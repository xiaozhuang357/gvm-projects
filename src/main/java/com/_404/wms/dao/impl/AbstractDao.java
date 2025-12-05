package com._404.wms.dao.impl;

import com._404.wms.db.DatabaseManager;
import com._404.wms.db.util.DbUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO抽象基类
 * 提供通用的数据库操作方法
 * 
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public abstract class AbstractDao<T, ID> {
    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final DatabaseManager dbManager;

    protected AbstractDao() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * 获取表名
     */
    protected abstract String getTableName();

    /**
     * 获取主键列名
     */
    protected abstract String getIdColumn();

    /**
     * 将ResultSet映射为实体对象
     */
    protected abstract T mapResultSetToEntity(ResultSet rs) throws SQLException;

    /**
     * 获取数据库连接
     */
    protected Connection getConnection() throws SQLException {
        return dbManager.getConnection();
    }

    /**
     * 释放数据库连接
     */
    protected void releaseConnection(Connection conn) {
        dbManager.releaseConnection(conn);
    }

    /**
     * 执行查询并返回单个结果
     */
    protected Optional<T> queryForObject(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToEntity(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Query error: " + sql, e);
            return Optional.empty();
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * 执行查询并返回列表结果
     */
    protected List<T> queryForList(String sql, Object... params) {
        List<T> result = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                result.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Query error: " + sql, e);
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }
        return result;
    }

    /**
     * 执行更新操作（INSERT/UPDATE/DELETE）
     */
    protected boolean executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Update error: " + sql, e);
            return false;
        } finally {
            DbUtils.closeQuietly(pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * 执行更新操作并返回影响的行数
     */
    protected int executeUpdateAndCount(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Update error: " + sql, e);
            return 0;
        } finally {
            DbUtils.closeQuietly(pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * 查询单个值（如COUNT、SUM等）
     */
    protected <V> V queryForScalar(String sql, Class<V> type, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                Object value = rs.getObject(1);
                if (value == null)
                    return null;

                if (type == Long.class) {
                    return type.cast(((Number) value).longValue());
                } else if (type == Integer.class) {
                    return type.cast(((Number) value).intValue());
                } else if (type == Double.class) {
                    return type.cast(((Number) value).doubleValue());
                } else if (type == String.class) {
                    return type.cast(value.toString());
                }
                return type.cast(value);
            }
            return null;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Scalar query error: " + sql, e);
            return null;
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * 设置PreparedStatement参数
     */
    protected void setParameters(PreparedStatement pstmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            int index = i + 1;

            if (param == null) {
                pstmt.setObject(index, null);
            } else if (param instanceof String) {
                pstmt.setString(index, (String) param);
            } else if (param instanceof Integer) {
                pstmt.setInt(index, (Integer) param);
            } else if (param instanceof Long) {
                pstmt.setLong(index, (Long) param);
            } else if (param instanceof Double) {
                pstmt.setDouble(index, (Double) param);
            } else if (param instanceof Boolean) {
                pstmt.setBoolean(index, (Boolean) param);
            } else if (param instanceof java.sql.Timestamp) {
                pstmt.setTimestamp(index, (java.sql.Timestamp) param);
            } else if (param instanceof java.sql.Date) {
                pstmt.setDate(index, (java.sql.Date) param);
            } else if (param instanceof java.time.LocalDateTime) {
                pstmt.setTimestamp(index, java.sql.Timestamp.valueOf((java.time.LocalDateTime) param));
            } else if (param instanceof java.time.LocalDate) {
                pstmt.setDate(index, java.sql.Date.valueOf((java.time.LocalDate) param));
            } else if (param instanceof Enum) {
                pstmt.setString(index, ((Enum<?>) param).name());
            } else {
                pstmt.setObject(index, param);
            }
        }
    }

    /**
     * 根据ID查询
     */
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getIdColumn() + " = ?";
        return queryForObject(sql, id);
    }

    /**
     * 查询所有
     */
    public List<T> findAll() {
        String sql = "SELECT * FROM " + getTableName();
        return queryForList(sql);
    }

    /**
     * 根据ID删除
     */
    public boolean deleteById(ID id) {
        String sql = "DELETE FROM " + getTableName() + " WHERE " + getIdColumn() + " = ?";
        return executeUpdate(sql, id);
    }

    /**
     * 统计数量
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + getTableName();
        Long result = queryForScalar(sql, Long.class);
        return result != null ? result : 0;
    }

    /**
     * 检查ID是否存在
     */
    public boolean existsById(ID id) {
        String sql = "SELECT COUNT(*) FROM " + getTableName() + " WHERE " + getIdColumn() + " = ?";
        Long result = queryForScalar(sql, Long.class, id);
        return result != null && result > 0;
    }
}
