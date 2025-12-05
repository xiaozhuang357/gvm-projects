package com._404.wms.dao.impl;

import com._404.wms.dao.StockInRecordDao;
import com._404.wms.model.StockInRecord;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库记录DAO实现类
 */
public class StockInRecordDaoImpl extends AbstractDao<StockInRecord, String> implements StockInRecordDao {

    @Override
    protected String getTableName() {
        return "stock_in_records";
    }

    @Override
    protected String getIdColumn() {
        return "record_id";
    }

    @Override
    protected StockInRecord mapResultSetToEntity(ResultSet rs) throws SQLException {
        StockInRecord record = new StockInRecord();
        record.setRecordId(rs.getString("record_id"));
        record.setOrderId(rs.getString("order_id"));
        record.setProductId(rs.getString("product_id"));
        record.setProductName(rs.getString("product_name"));
        record.setQuantity(rs.getInt("quantity"));
        record.setBatchNumber(rs.getString("batch_number"));
        record.setWarehouse(rs.getString("warehouse"));
        record.setOperatorId(rs.getString("operator_id"));
        record.setOperatorName(rs.getString("operator_name"));

        Timestamp inTime = rs.getTimestamp("in_time");
        if (inTime != null) {
            record.setTimestamp(inTime.toLocalDateTime());
        }

        record.setRemark(rs.getString("remark"));
        return record;
    }

    @Override
    public boolean save(StockInRecord record) {
        String sql = """
                INSERT INTO stock_in_records (record_id, order_id, product_id, product_name, quantity,
                                             batch_number, warehouse, operator_id, operator_name, in_time, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return executeUpdate(sql,
                record.getRecordId(),
                record.getOrderId(),
                record.getProductId(),
                record.getProductName(),
                record.getQuantity(),
                record.getBatchNumber(),
                record.getWarehouse(),
                record.getOperatorId(),
                record.getOperatorName(),
                record.getTimestamp() != null ? Timestamp.valueOf(record.getTimestamp())
                        : Timestamp.valueOf(LocalDateTime.now()),
                record.getRemark());
    }

    @Override
    public boolean update(StockInRecord record) {
        String sql = """
                UPDATE stock_in_records SET order_id = ?, product_id = ?, product_name = ?,
                                           quantity = ?, batch_number = ?, warehouse = ?,
                                           operator_id = ?, operator_name = ?, remark = ?
                WHERE record_id = ?
                """;

        return executeUpdate(sql,
                record.getOrderId(),
                record.getProductId(),
                record.getProductName(),
                record.getQuantity(),
                record.getBatchNumber(),
                record.getWarehouse(),
                record.getOperatorId(),
                record.getOperatorName(),
                record.getRemark(),
                record.getRecordId());
    }

    @Override
    public List<StockInRecord> findAll() {
        String sql = "SELECT * FROM stock_in_records ORDER BY in_time DESC";
        return queryForList(sql);
    }

    @Override
    public List<StockInRecord> findByOrderId(String orderId) {
        String sql = "SELECT * FROM stock_in_records WHERE order_id = ? ORDER BY in_time DESC";
        return queryForList(sql, orderId);
    }

    @Override
    public List<StockInRecord> findByProductId(String productId) {
        String sql = "SELECT * FROM stock_in_records WHERE product_id = ? ORDER BY in_time DESC";
        return queryForList(sql, productId);
    }

    @Override
    public List<StockInRecord> findByOperatorId(String operatorId) {
        String sql = "SELECT * FROM stock_in_records WHERE operator_id = ? ORDER BY in_time DESC";
        return queryForList(sql, operatorId);
    }

    @Override
    public List<StockInRecord> findByTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM stock_in_records WHERE in_time BETWEEN ? AND ? ORDER BY in_time DESC";
        return queryForList(sql, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
    }

    @Override
    public List<StockInRecord> findByWarehouse(String warehouse) {
        String sql = "SELECT * FROM stock_in_records WHERE warehouse = ? ORDER BY in_time DESC";
        return queryForList(sql, warehouse);
    }

    @Override
    public List<StockInRecord> findByBatchNumber(String batchNumber) {
        String sql = "SELECT * FROM stock_in_records WHERE batch_number = ? ORDER BY in_time DESC";
        return queryForList(sql, batchNumber);
    }

    @Override
    public int sumQuantityByProductId(String productId) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_in_records WHERE product_id = ?";
        Integer result = queryForScalar(sql, Integer.class, productId);
        return result != null ? result : 0;
    }

    @Override
    public int sumQuantityByTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT COALESCE(SUM(quantity), 0) FROM stock_in_records WHERE in_time BETWEEN ? AND ?";
        Integer result = queryForScalar(sql, Integer.class, Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
        return result != null ? result : 0;
    }

    @Override
    public List<StockInRecord> findRecent(int limit) {
        String sql = "SELECT * FROM stock_in_records ORDER BY in_time DESC LIMIT ?";
        return queryForList(sql, limit);
    }
}
