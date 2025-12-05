package com._404.wms.dao.impl;

import com._404.wms.dao.StockOutRecordDao;
import com._404.wms.db.util.DbUtils;
import com._404.wms.model.StockOutRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * 出库记录DAO实现类
 */
public class StockOutRecordDaoImpl extends AbstractDao<StockOutRecord, String> implements StockOutRecordDao {

    @Override
    protected String getTableName() {
        return "stock_out_records";
    }

    @Override
    protected String getIdColumn() {
        return "record_id";
    }

    @Override
    protected StockOutRecord mapResultSetToEntity(ResultSet rs) throws SQLException {
        StockOutRecord record = new StockOutRecord();
        record.setRecordId(rs.getString("record_id"));
        record.setProductId(rs.getString("product_id"));
        record.setProductName(rs.getString("product_name"));
        record.setQuantity(rs.getInt("quantity"));
        record.setRecipient(rs.getString("recipient"));
        record.setRecipientDept(rs.getString("recipient_dept"));
        record.setPurpose(rs.getString("purpose"));
        record.setOperatorId(rs.getString("operator_id"));
        record.setOperatorName(rs.getString("operator_name"));

        Timestamp outTime = rs.getTimestamp("out_time");
        if (outTime != null) {
            record.setTimestamp(outTime.toLocalDateTime());
        }

        record.setRemark(rs.getString("remark"));
        return record;
    }

    @Override
    public boolean save(StockOutRecord record) {
        String sql = """
                INSERT INTO stock_out_records (record_id, product_id, product_name, quantity, recipient,
                                              recipient_dept, purpose, operator_id, operator_name, out_time, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return executeUpdate(sql,
                record.getRecordId(),
                record.getProductId(),
                record.getProductName(),
                record.getQuantity(),
                record.getRecipient(),
                record.getRecipientDept(),
                record.getPurpose(),
                record.getOperatorId(),
                record.getOperatorName(),
                record.getTimestamp() != null ? Timestamp.valueOf(record.getTimestamp())
                        : Timestamp.valueOf(LocalDateTime.now()),
                record.getRemark());
    }

    @Override
    public boolean update(StockOutRecord record) {
        String sql = """
                UPDATE stock_out_records SET product_id = ?, product_name = ?, quantity = ?,
                                            recipient = ?, recipient_dept = ?, purpose = ?,
                                            operator_id = ?, operator_name = ?, remark = ?
                WHERE record_id = ?
                """;

        return executeUpdate(sql,
                record.getProductId(),
                record.getProductName(),
                record.getQuantity(),
                record.getRecipient(),
                record.getRecipientDept(),
                record.getPurpose(),
                record.getOperatorId(),
                record.getOperatorName(),
                record.getRemark(),
                record.getRecordId());
    }

    @Override
    public List<StockOutRecord> findAll() {
        String sql = "SELECT * FROM stock_out_records ORDER BY out_time DESC";
        return queryForList(sql);
    }

    @Override
    public List<StockOutRecord> findByProductId(String productId) {
        String sql = "SELECT * FROM stock_out_records WHERE product_id = ? ORDER BY out_time DESC";
        return queryForList(sql, productId);
    }

    @Override
    public List<StockOutRecord> findByOperatorId(String operatorId) {
        String sql = "SELECT * FROM stock_out_records WHERE operator_id = ? ORDER BY out_time DESC";
        return queryForList(sql, operatorId);
    }

    @Override
    public List<StockOutRecord> findByRecipient(String recipient) {
        String sql = "SELECT * FROM stock_out_records WHERE recipient = ? ORDER BY out_time DESC";
        return queryForList(sql, recipient);
    }

    @Override
    public List<StockOutRecord> findByRecipientDept(String recipientDept) {
        String sql = "SELECT * FROM stock_out_records WHERE recipient_dept = ? ORDER BY out_time DESC";
        return queryForList(sql, recipientDept);
    }

    @Override
    public List<StockOutRecord> findByTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM stock_out_records WHERE out_time BETWEEN ? AND ? ORDER BY out_time DESC";
        return queryForList(sql, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    @Override
    public List<StockOutRecord> findByPurposeLike(String purpose) {
        String sql = "SELECT * FROM stock_out_records WHERE purpose LIKE ? ORDER BY out_time DESC";
        return queryForList(sql, "%" + purpose + "%");
    }

    @Override
    public int sumQuantityByProductId(String productId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_out_records WHERE product_id = ?";
        Integer result = queryForScalar(sql, Integer.class, productId);
        return result != null ? result : 0;
    }

    @Override
    public int sumQuantityByTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_out_records WHERE out_time BETWEEN ? AND ?";
        Integer result = queryForScalar(sql, Integer.class, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
        return result != null ? result : 0;
    }

    @Override
    public List<StockOutRecord> findRecent(int limit) {
        String sql = "SELECT * FROM stock_out_records ORDER BY out_time DESC LIMIT ?";
        return queryForList(sql, limit);
    }

    @Override
    public Map<String, Integer> sumQuantityByDept() {
        Map<String, Integer> result = new HashMap<>();
        String sql = "SELECT recipient_dept, SUM(quantity) as total FROM stock_out_records GROUP BY recipient_dept";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String dept = rs.getString("recipient_dept");
                int total = rs.getInt("total");
                if (dept != null) {
                    result.put(dept, total);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to sum quantity by dept", e);
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }

        return result;
    }
}
