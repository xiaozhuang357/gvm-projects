package com._404.wms.dao.impl;

import com._404.wms.dao.PurchaseOrderDao;
import com._404.wms.db.util.DbUtils;
import com._404.wms.model.PurchaseOrder;
import com._404.wms.model.PurchaseOrder.OrderItem;
import com._404.wms.model.PurchaseOrder.OrderStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * 采购订单DAO实现类
 */
public class PurchaseOrderDaoImpl extends AbstractDao<PurchaseOrder, String> implements PurchaseOrderDao {

    @Override
    protected String getTableName() {
        return "purchase_orders";
    }

    @Override
    protected String getIdColumn() {
        return "order_id";
    }

    @Override
    protected PurchaseOrder mapResultSetToEntity(ResultSet rs) throws SQLException {
        PurchaseOrder order = new PurchaseOrder();
        order.setOrderId(rs.getString("order_id"));
        order.setPurchaserId(rs.getString("purchaser_id"));
        order.setPurchaserName(rs.getString("purchaser_name"));
        order.setSupplier(rs.getString("supplier"));
        order.setTotalAmount(rs.getDouble("total_amount"));

        String statusStr = rs.getString("status");
        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                order.setStatus(OrderStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                // 忽略无效状态
            }
        }

        order.setApproverId(rs.getString("approver_id"));
        order.setApproverName(rs.getString("approver_name"));
        order.setApproveComment(rs.getString("approve_comment"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            order.setCreateTime(createTime.toLocalDateTime());
        }

        Timestamp approveTime = rs.getTimestamp("approve_time");
        if (approveTime != null) {
            order.setApproveTime(approveTime.toLocalDateTime());
        }

        Timestamp arrivalTime = rs.getTimestamp("arrival_time");
        if (arrivalTime != null) {
            order.setArrivalTime(arrivalTime.toLocalDateTime());
        }

        java.sql.Date expectedDate = rs.getDate("expected_delivery_date");
        if (expectedDate != null) {
            order.setExpectedDeliveryDate(expectedDate.toLocalDate());
        }

        order.setRemark(rs.getString("remark"));

        return order;
    }

    /**
     * 映射订单项
     */
    private OrderItem mapResultSetToOrderItem(ResultSet rs) throws SQLException {
        OrderItem item = new OrderItem();
        item.setProductId(rs.getString("product_id"));
        item.setProductName(rs.getString("product_name"));
        item.setSpecification(rs.getString("specification"));
        item.setQuantity(rs.getInt("quantity"));
        item.setUnitPrice(rs.getDouble("unit_price"));
        item.setSubtotal(rs.getDouble("subtotal"));
        return item;
    }

    @Override
    public boolean save(PurchaseOrder order) {
        String insertOrder = """
                INSERT INTO purchase_orders (order_id, purchaser_id, purchaser_name, supplier, total_amount,
                                            status, approver_id, approver_name, approve_comment, create_time,
                                            approve_time, arrival_time, expected_delivery_date, remark)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        String insertItem = """
                INSERT INTO purchase_order_items (order_id, product_id, product_name, specification,
                                                 quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement orderStmt = null;
        PreparedStatement itemStmt = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 插入订单主表
            orderStmt = conn.prepareStatement(insertOrder);
            setOrderParameters(orderStmt, order);
            orderStmt.executeUpdate();

            // 插入订单明细
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                itemStmt = conn.prepareStatement(insertItem);
                for (OrderItem item : order.getItems()) {
                    setOrderItemParameters(itemStmt, order.getOrderId(), item);
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            DbUtils.rollbackQuietly(conn);
            logger.log(Level.SEVERE, "Failed to save purchase order", e);
            return false;
        } finally {
            DbUtils.closeQuietly(itemStmt);
            DbUtils.closeQuietly(orderStmt);
            DbUtils.resetAutoCommit(conn);
            releaseConnection(conn);
        }
    }

    @Override
    public boolean update(PurchaseOrder order) {
        String updateOrder = """
                UPDATE purchase_orders SET purchaser_id = ?, purchaser_name = ?, supplier = ?,
                                          total_amount = ?, status = ?, approver_id = ?, approver_name = ?,
                                          approve_comment = ?, approve_time = ?, arrival_time = ?,
                                          expected_delivery_date = ?, remark = ?
                WHERE order_id = ?
                """;

        String deleteItems = "DELETE FROM purchase_order_items WHERE order_id = ?";
        String insertItem = """
                INSERT INTO purchase_order_items (order_id, product_id, product_name, specification,
                                                 quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        Connection conn = null;
        PreparedStatement orderStmt = null;
        PreparedStatement deleteStmt = null;
        PreparedStatement itemStmt = null;

        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 更新订单主表
            orderStmt = conn.prepareStatement(updateOrder);
            orderStmt.setString(1, order.getPurchaserId());
            orderStmt.setString(2, order.getPurchaserName());
            orderStmt.setString(3, order.getSupplier());
            orderStmt.setDouble(4, order.getTotalAmount());
            orderStmt.setString(5, order.getStatus() != null ? order.getStatus().name() : null);
            orderStmt.setString(6, order.getApproverId());
            orderStmt.setString(7, order.getApproverName());
            orderStmt.setString(8, order.getApproveComment());
            orderStmt.setTimestamp(9,
                    order.getApproveTime() != null ? Timestamp.valueOf(order.getApproveTime()) : null);
            orderStmt.setTimestamp(10,
                    order.getArrivalTime() != null ? Timestamp.valueOf(order.getArrivalTime()) : null);
            orderStmt.setDate(11,
                    order.getExpectedDeliveryDate() != null ? java.sql.Date.valueOf(order.getExpectedDeliveryDate())
                            : null);
            orderStmt.setString(12, order.getRemark());
            orderStmt.setString(13, order.getOrderId());
            orderStmt.executeUpdate();

            // 删除旧的订单明细
            deleteStmt = conn.prepareStatement(deleteItems);
            deleteStmt.setString(1, order.getOrderId());
            deleteStmt.executeUpdate();

            // 插入新的订单明细
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                itemStmt = conn.prepareStatement(insertItem);
                for (OrderItem item : order.getItems()) {
                    setOrderItemParameters(itemStmt, order.getOrderId(), item);
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            DbUtils.rollbackQuietly(conn);
            logger.log(Level.SEVERE, "Failed to update purchase order", e);
            return false;
        } finally {
            DbUtils.closeQuietly(itemStmt);
            DbUtils.closeQuietly(deleteStmt);
            DbUtils.closeQuietly(orderStmt);
            DbUtils.resetAutoCommit(conn);
            releaseConnection(conn);
        }
    }

    @Override
    public Optional<PurchaseOrder> findById(String orderId) {
        String sql = "SELECT * FROM purchase_orders WHERE order_id = ?";
        Optional<PurchaseOrder> result = queryForObject(sql, orderId);

        result.ifPresent(order -> {
            order.setItems(findOrderItems(orderId));
        });

        return result;
    }

    @Override
    public List<PurchaseOrder> findAll() {
        String sql = "SELECT * FROM purchase_orders ORDER BY create_time DESC";
        List<PurchaseOrder> orders = queryForList(sql);
        loadItemsForOrders(orders);
        return orders;
    }

    @Override
    public List<PurchaseOrder> findByPurchaserId(String purchaserId) {
        String sql = "SELECT * FROM purchase_orders WHERE purchaser_id = ? ORDER BY create_time DESC";
        List<PurchaseOrder> orders = queryForList(sql, purchaserId);
        loadItemsForOrders(orders);
        return orders;
    }

    @Override
    public List<PurchaseOrder> findByStatus(OrderStatus status) {
        String sql = "SELECT * FROM purchase_orders WHERE status = ? ORDER BY create_time DESC";
        List<PurchaseOrder> orders = queryForList(sql, status.name());
        loadItemsForOrders(orders);
        return orders;
    }

    @Override
    public List<PurchaseOrder> findByStatuses(List<OrderStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return new ArrayList<>();
        }

        String placeholders = DbUtils.generatePlaceholders(statuses.size());
        String sql = "SELECT * FROM purchase_orders WHERE status IN (" + placeholders + ") ORDER BY create_time DESC";

        Object[] params = statuses.stream().map(Enum::name).toArray();
        List<PurchaseOrder> orders = queryForList(sql, params);
        loadItemsForOrders(orders);
        return orders;
    }

    @Override
    public List<PurchaseOrder> findBySupplier(String supplier) {
        String sql = "SELECT * FROM purchase_orders WHERE supplier = ? ORDER BY create_time DESC";
        List<PurchaseOrder> orders = queryForList(sql, supplier);
        loadItemsForOrders(orders);
        return orders;
    }

    @Override
    public List<PurchaseOrder> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "SELECT * FROM purchase_orders WHERE create_time BETWEEN ? AND ? ORDER BY create_time DESC";
        List<PurchaseOrder> orders = queryForList(sql,
                Timestamp.valueOf(startTime), Timestamp.valueOf(endTime));
        loadItemsForOrders(orders);
        return orders;
    }

    @Override
    public List<PurchaseOrder> findByExpectedDeliveryDateBetween(LocalDate startDate, LocalDate endDate) {
        String sql = "SELECT * FROM purchase_orders WHERE expected_delivery_date BETWEEN ? AND ? ORDER BY expected_delivery_date";
        List<PurchaseOrder> orders = queryForList(sql,
                java.sql.Date.valueOf(startDate), java.sql.Date.valueOf(endDate));
        loadItemsForOrders(orders);
        return orders;
    }

    @Override
    public List<PurchaseOrder> findPendingApproval() {
        List<OrderStatus> pendingStatuses = Arrays.asList(
                OrderStatus.PENDING_DEPT_APPROVAL,
                OrderStatus.PENDING_GENERAL_APPROVAL);
        return findByStatuses(pendingStatuses);
    }

    @Override
    public boolean updateStatus(String orderId, OrderStatus status) {
        String sql = "UPDATE purchase_orders SET status = ? WHERE order_id = ?";
        return executeUpdate(sql, status.name(), orderId);
    }

    @Override
    public boolean approve(String orderId, String approverId, String approverName,
            OrderStatus status, String comment) {
        String sql = """
                UPDATE purchase_orders SET status = ?, approver_id = ?, approver_name = ?,
                                          approve_comment = ?, approve_time = ?
                WHERE order_id = ?
                """;
        return executeUpdate(sql, status.name(), approverId, approverName, comment,
                Timestamp.valueOf(LocalDateTime.now()), orderId);
    }

    @Override
    public boolean setArrivalTime(String orderId, LocalDateTime arrivalTime) {
        String sql = "UPDATE purchase_orders SET arrival_time = ? WHERE order_id = ?";
        return executeUpdate(sql, Timestamp.valueOf(arrivalTime), orderId);
    }

    @Override
    public Map<OrderStatus, Long> countByStatus() {
        Map<OrderStatus, Long> result = new HashMap<>();
        String sql = "SELECT status, COUNT(*) as cnt FROM purchase_orders GROUP BY status";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String statusStr = rs.getString("status");
                long count = rs.getLong("cnt");

                if (statusStr != null) {
                    try {
                        OrderStatus status = OrderStatus.valueOf(statusStr);
                        result.put(status, count);
                    } catch (IllegalArgumentException e) {
                        // 忽略无效状态
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to count by status", e);
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }

        return result;
    }

    /**
     * 查询订单项
     */
    private List<OrderItem> findOrderItems(String orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM purchase_order_items WHERE order_id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, orderId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                items.add(mapResultSetToOrderItem(rs));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to find order items", e);
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }

        return items;
    }

    /**
     * 批量加载订单明细
     */
    private void loadItemsForOrders(List<PurchaseOrder> orders) {
        if (orders.isEmpty())
            return;

        Map<String, PurchaseOrder> orderMap = new HashMap<>();
        for (PurchaseOrder order : orders) {
            orderMap.put(order.getOrderId(), order);
            order.setItems(new ArrayList<>());
        }

        String placeholders = DbUtils.generatePlaceholders(orders.size());
        String sql = "SELECT * FROM purchase_order_items WHERE order_id IN (" + placeholders + ")";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);

            int index = 1;
            for (PurchaseOrder order : orders) {
                pstmt.setString(index++, order.getOrderId());
            }

            rs = pstmt.executeQuery();
            while (rs.next()) {
                String orderId = rs.getString("order_id");
                PurchaseOrder order = orderMap.get(orderId);
                if (order != null) {
                    order.getItems().add(mapResultSetToOrderItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to load items for orders", e);
        } finally {
            DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }
    }

    /**
     * 设置订单参数
     */
    private void setOrderParameters(PreparedStatement pstmt, PurchaseOrder order) throws SQLException {
        pstmt.setString(1, order.getOrderId());
        pstmt.setString(2, order.getPurchaserId());
        pstmt.setString(3, order.getPurchaserName());
        pstmt.setString(4, order.getSupplier());
        pstmt.setDouble(5, order.getTotalAmount());
        pstmt.setString(6, order.getStatus() != null ? order.getStatus().name() : null);
        pstmt.setString(7, order.getApproverId());
        pstmt.setString(8, order.getApproverName());
        pstmt.setString(9, order.getApproveComment());
        pstmt.setTimestamp(10, order.getCreateTime() != null ? Timestamp.valueOf(order.getCreateTime())
                : Timestamp.valueOf(LocalDateTime.now()));
        pstmt.setTimestamp(11, order.getApproveTime() != null ? Timestamp.valueOf(order.getApproveTime()) : null);
        pstmt.setTimestamp(12, order.getArrivalTime() != null ? Timestamp.valueOf(order.getArrivalTime()) : null);
        pstmt.setDate(13,
                order.getExpectedDeliveryDate() != null ? java.sql.Date.valueOf(order.getExpectedDeliveryDate())
                        : null);
        pstmt.setString(14, order.getRemark());
    }

    /**
     * 设置订单项参数
     */
    private void setOrderItemParameters(PreparedStatement pstmt, String orderId, OrderItem item) throws SQLException {
        pstmt.setString(1, orderId);
        pstmt.setString(2, item.getProductId());
        pstmt.setString(3, item.getProductName());
        pstmt.setString(4, item.getSpecification());
        pstmt.setInt(5, item.getQuantity());
        pstmt.setDouble(6, item.getUnitPrice());
        pstmt.setDouble(7, item.getSubtotal());
    }
}
