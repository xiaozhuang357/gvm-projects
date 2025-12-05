package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 出库记录实体类
 */
public class StockOutRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String recordId; // 记录ID
    private String productId; // 商品ID
    private String productName; // 商品名称
    private int quantity; // 出库数量
    private String recipient; // 领用人
    private String recipientDept; // 领用部门
    private String purpose; // 出库原因/用途
    private String operatorId; // 操作员ID
    private String operatorName; // 操作员姓名
    private LocalDateTime outTime; // 出库时间
    private String remark; // 备注

    public StockOutRecord() {
        this.outTime = LocalDateTime.now();
    }

    public StockOutRecord(String productId, String productName, int quantity,
            String recipient, String purpose, String operatorId, String operatorName) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.recipient = recipient;
        this.purpose = purpose;
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

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getRecipientDept() {
        return recipientDept;
    }

    public void setRecipientDept(String recipientDept) {
        this.recipientDept = recipientDept;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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

    public LocalDateTime getOutTime() {
        return outTime;
    }

    public void setOutTime(LocalDateTime outTime) {
        this.outTime = outTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 获取时间戳（返回出库时间）
     */
    public LocalDateTime getTimestamp() {
        return outTime;
    }

    /**
     * 设置时间戳（设置出库时间）
     */
    public void setTimestamp(LocalDateTime timestamp) {
        this.outTime = timestamp;
    }

    @Override
    public String toString() {
        return "StockOutRecord{" +
                "recordId='" + recordId + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", recipient='" + recipient + '\'' +
                ", outTime=" + outTime +
                '}';
    }
}
