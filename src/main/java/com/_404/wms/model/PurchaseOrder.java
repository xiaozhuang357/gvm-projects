package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单实体类
 */
public class PurchaseOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private String orderId; // 订单ID
    private String purchaserId; // 采购员ID
    private String purchaserName; // 采购员姓名
    private String supplier; // 供应商
    private double totalAmount; // 订单总金额
    private OrderStatus status; // 订单状态
    private String approverId; // 审批人ID
    private String approverName; // 审批人姓名
    private String approveComment; // 审批意见
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime approveTime; // 审批时间
    private LocalDateTime arrivalTime; // 到货时间
    private LocalDate expectedDeliveryDate; // 预计交货日期
    private List<OrderItem> items; // 订单明细
    private String remark; // 备注

    public enum OrderStatus {
        DRAFT("草稿"),
        PENDING_SUBMIT("待提交"),
        PENDING_DEPT_APPROVAL("待部门经理审批"),
        PENDING_GENERAL_APPROVAL("待总经理审批"),
        APPROVED("已批准"),
        REJECTED("已退回"),
        IN_TRANSIT("运输中"),
        ARRIVED("已到货"),
        COMPLETED("已完成"),
        CANCELLED("已取消");

        private String displayName;

        OrderStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 订单明细项
     */
    public static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private String productId;
        private String productName;
        private String specification;
        private int quantity;
        private double unitPrice;
        private double subtotal;

        public OrderItem() {
        }

        public OrderItem(String productId, String productName, int quantity, double unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = quantity * unitPrice;
        }

        // Getters and Setters
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

        public String getSpecification() {
            return specification;
        }

        public void setSpecification(String specification) {
            this.specification = specification;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
            this.subtotal = quantity * unitPrice;
        }

        public double getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(double unitPrice) {
            this.unitPrice = unitPrice;
            this.subtotal = quantity * unitPrice;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public void setSubtotal(double subtotal) {
            this.subtotal = subtotal;
        }

        /**
         * 获取单价
         */
        public double getPrice() {
            return unitPrice;
        }
    }

    public PurchaseOrder() {
        this.createTime = LocalDateTime.now();
        this.status = OrderStatus.DRAFT;
        this.items = new ArrayList<>();
    }

    /**
     * 计算订单总金额
     */
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .mapToDouble(OrderItem::getSubtotal)
                .sum();
    }

    /**
     * 判断是否需要总经理审批
     */
    public boolean needsGeneralManagerApproval() {
        return totalAmount >= 50000;
    }

    /**
     * 添加订单明细
     */
    public void addItem(String productId, String productName, double unitPrice, int quantity, String specification) {
        OrderItem item = new OrderItem(productId, productName, quantity, unitPrice);
        item.setSpecification(specification);
        this.items.add(item);
        calculateTotalAmount();
    }

    // Getters and Setters
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPurchaserId() {
        return purchaserId;
    }

    public void setPurchaserId(String purchaserId) {
        this.purchaserId = purchaserId;
    }

    public String getPurchaserName() {
        return purchaserName;
    }

    public void setPurchaserName(String purchaserName) {
        this.purchaserName = purchaserName;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getApproverId() {
        return approverId;
    }

    public void setApproverId(String approverId) {
        this.approverId = approverId;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApproveComment() {
        return approveComment;
    }

    public void setApproveComment(String approveComment) {
        this.approveComment = approveComment;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getApproveTime() {
        return approveTime;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    @Override
    public String toString() {
        return "PurchaseOrder{" +
                "orderId='" + orderId + '\'' +
                ", purchaserName='" + purchaserName + '\'' +
                ", totalAmount=" + totalAmount +
                ", status=" + status +
                ", createTime=" + createTime +
                '}';
    }
}
