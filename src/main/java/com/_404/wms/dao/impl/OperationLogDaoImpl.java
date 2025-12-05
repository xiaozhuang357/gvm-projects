package com._404.wms.dao.impl;

import com._404.wms.dao.OperationLogDao;
import com._404.wms.db.util.DbUtils;
import com._404.wms.model.OperationLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * 操作日志DAO实现类
 */
public class OperationLogDaoImpl extends AbstractDao<OperationLog, String> implements OperationLogDao {

    @Override
    protected String getTableName() {
        return "operation_logs";
    }

    @Override
    protected String getIdColumn() {
        return "log_id";
    }

    @Override
    protected OperationLog mapResultSetToEntity(ResultSet rs) throws SQLException {
        OperationLog log = new OperationLog();
        log.setLogId(rs.getString("log_id"));
        log.setUserId(rs.getString("user_id"));
        log.setUsername(rs.getString("username"));
        log.setOperation(rs.getString("operation"));
        log.setModule(rs.getString("module"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));

        Timestamp operationTime = rs.getTimestamp("operation_time");
        if (operationTime != null) {
            log.setOperationTime(operationTime.toLocalDateTime());
        }

        log.setSuccess(rs.getBoolean("success"));
        log.setErrorMessage(rs.getString("error_message"));
        return log;
    }

    @Override
    public boolean save(OperationLog log) {
        String sql = """
                INSERT INTO operation_logs (log_id, user_id, username, operation, module, details,
                                           ip_address, operation_time, success, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return executeUpdate(sql,
                log.getLogId(),
                log.getUserId(),
                log.getUsername(),
                log.getOperation(),
                log.getModule(),
                log.getDetails(),
                log.getIpAddress(),
                log.getOperationTime() != null ? Timestamp.valueOf(log.getOperationTime())
                        : Timestamp.valueOf(LocalDateTime.now()),
                log.isSuccess(),
                log.getErrorMessage());
    }

    @Override
    public boolean update(OperationLog log) {
        String sql = """
                UPDATE operation_logs SET user_id = ?, username = ?, operation = ?, module = ?,
                                         details = ?, ip_address = ?, success = ?, error_message = ?
                WHERE log_id = ?
                """;

        return executeUpdate(sql,
                log.getUserId(),
                log.getUsername(),
                log.getOperation(),
                log.getModule(),
                log.getDetails(),
                log.getIpAddress(),
                log.isSuccess(),
                log.getErrorMessage(),
                log.getLogId());
    }

    @Override
    public List<OperationLog> findAll() {
        String sql = "SELECT * FROM operation_logs ORDER BY operation_time DESC";
        return queryForList(sql);
    }

    @Override
    public List<OperationLog> findByUserId(String userId) {
        String sql = "SELECT * FROM operation_logs WHERE user_id = ? ORDER BY operation_time DESC";
        return queryForList(sql, userId);
    }

    @Override
    public List<OperationLog> findByModule(String module) {
        String sql = "SELECT * FROM operation_logs WHERE module = ? ORDER BY operation_time DESC";
        return queryForList(sql, module);
    }

    @Override
    public List<OperationLog> findByOperation(String operation) {
        String sql = "SELECT * FROM operation_logs WHERE operation = ? ORDER BY operation_time DESC";
        return queryForList(sql, operation);
    }

    @Override
    public List<OperationLog> findByTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM operation_logs WHERE operation_time BETWEEN ? AND ? ORDER BY operation_time DESC";
        return queryForList(sql, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    @Override
    public List<OperationLog> findFailed() {
        String sql = "SELECT * FROM operation_logs WHERE success = false ORDER BY operation_time DESC";
        return queryForList(sql);
    }

    @Override
    public List<OperationLog> findByUserIdAndTimeBetween(String userId, LocalDateTime startTime,
            LocalDateTime endTime) {
        String sql = "SELECT * FROM operation_logs WHERE user_id = ? AND operation_time BETWEEN ? AND ? ORDER BY operation_time DESC";
        return queryForList(sql, userId, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    @Override
    public List<OperationLog> findRecent(int limit) {
        String sql = "SELECT * FROM operation_logs ORDER BY operation_time DESC LIMIT ?";
        return queryForList(sql, limit);
    }

    @Override
    public int deleteBeforeTime(LocalDateTime beforeTime) {
        String sql = "DELETE FROM operation_logs WHERE operation_time < ?";
        return executeUpdateAndCount(sql, Timestamp.valueOf(beforeTime));
    }

    @Override
    public Map<String, Long> countByOperation() {
        Map<String, Long> result = new HashMap<>();
        String sql = "SELECT operation, COUNT(*) as cnt FROM operation_logs GROUP BY operation";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String operation = rs.getString("operation");
                long count = rs.getLong("cnt");
                if (operation != null) {
                    result.put(operation, count);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count by operation", e);
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }

        return result;
    }

    @Override
    public Map<String, Long> countByModule() {
        Map<String, Long> result = new HashMap<>();
        String sql = "SELECT module, COUNT(*) as cnt FROM operation_logs GROUP BY module";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String module = rs.getString("module");
                long count = rs.getLong("cnt");
                if (module != null) {
                    result.put(module, count);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count by module", e);
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }

        return result;
    }
}
