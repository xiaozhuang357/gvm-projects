package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 入库记录实体类
 */
public class StockInRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String recordId; // 记录ID
    private String orderId; // 关联采购订单ID
    private String productId; // 商品ID
    private String productName; // 商品名称
    private int quantity; // 入库数量
    private String batchNumber; // 批次号
    private String warehouse; // 仓库位置
    private String operatorId; // 操作员ID
    private String operatorName; // 操作员姓名
    private LocalDateTime inTime; // 入库时间
    private String remark; // 备注

    public StockInRecord() {
        this.inTime = LocalDateTime.now();
    }

    public StockInRecord(String productId, String productName, int quantity,
            String operatorId, String operatorName) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.operatorId = operatorId;
        this.operatorName = operatorName;
    }

    // Getters and Setters
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public void setWarehouse(String warehouse) {
        this.warehouse = warehouse;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public LocalDateTime getInTime() {
        return inTime;
    }

    public void setInTime(LocalDateTime inTime) {
        this.inTime = inTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 设置仓库位置（warehouse的别名）
     */
    public void setWarehouseLocation(String location) {
        this.warehouse = location;
    }

    /**
     * 获取时间戳（返回入库时间）
     */
    public LocalDateTime getTimestamp() {
        return inTime;
    }

    /**
     * 设置时间戳（设置入库时间）
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.inTime = timestamp;
    }

    @Override
    public String toString() {
        return "StockInRecord{" +
                "recordId='" + recordId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", inTime=" + inTime +
                '}';
    }
}
