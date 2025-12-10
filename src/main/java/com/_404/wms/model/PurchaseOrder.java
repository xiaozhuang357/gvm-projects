package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采购订单实体类 - 采购业务流程的核心数据模型
 * <p>
 * 功能说明:
 * 1. 表示从采购申请到货物到达的完整采购流程
 * 2. 支持分级审批:部门经理审批<50000,总经理审批>=50000
 * 3. 包含订单明细(OrderItem),支持多商品采购
 * 4. 记录完整的订单生命周期(创建->审批->到货->完成)
 * <p>
 * 审批流程:
 * 1. 采购员创建订单(status=PENDING_SUBMIT或PENDING_DEPT_APPROVAL)
 * 2. 根据金额路由审批人:
 * - totalAmount < 50000 → 部门经理审批
 * - totalAmount >= 50000 → 总经理审批
 * 3. 审批通过(status=APPROVED)或退回(status=REJECTED)
 * 4. 确认到货(status=ARRIVED)后可生成入库记录
 * <p>
 * 数据库映射:
 * - 对应数据库表:purchase_orders(主表) + purchase_order_items(明细表)
 * - 主键:orderId
 * - 外键:purchaserId引用users表
 * <p>
 * 使用示例:
 * 
 * <pre>
 * // 创建采购订单
 * PurchaseOrder order = new PurchaseOrder();
 * order.setOrderId("PO20251206001");
 * order.setPurchaserId("U004");
 * order.setSupplier("联想集团");
 * 
 * // 添加订单明细
 * order.addItem(new OrderItem("P001", "笔记本电脑", 10, 5500.0));
 * order.calculateTotalAmount(); // 计算总金额55000
 * 
 * // 提交审批
 * order.setStatus(OrderStatus.PENDING_GENERAL_APPROVAL); // 因金额>=50000
 * </pre>
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class PurchaseOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 订单唯一标识,主键 */
    private String orderId;

    /** 创建订单的采购员ID */
    private String purchaserId;

    /** 采购员姓名(冗余字段,便于显示) */
    private String purchaserName;

    /** 供应商名称 */
    private String supplier;

    /** 订单总金额,决定审批流程 */
    private double totalAmount;

    /** 订单当前状态 */
    private OrderStatus status;

    /** 审批人ID(经理或总经理) */
    private String approverId;

    /** 审批人姓名 */
    private String approverName;

    /** 审批意见/退回原因 */
    private String approveComment;

    /** 订单创建时间 */
    private LocalDateTime createTime;

    /** 审批完成时间 */
    private LocalDateTime approveTime;

    /** 货物到达时间 */
    private LocalDateTime arrivalTime;

    /** 预计交货日期 */
    private LocalDate expectedDeliveryDate;

    /** 订单明细列表(包含商品、数量、单价等) */
    private List<OrderItem> items;

    /** 订单备注信息 */
    private String remark;

    /**
     * 订单状态枚举 - 定义采购订单的完整生命周期状态
     * <p>
     * 状态流转:
     * DRAFT → PENDING_SUBMIT → PENDING_DEPT/GENERAL_APPROVAL → APPROVED/REJECTED
     * → IN_TRANSIT → ARRIVED → COMPLETED
     * <p>
     * 特殊状态:
     * - CANCELLED:订单被取消,终止状态
     * - REJECTED:审批被退回,可修改后重新提交
     */
    public enum OrderStatus {
        /** 待提交 - 订单已保存但未提交审批 */
        PENDING_SUBMIT("待提交"),

        /** 待部门经理审批 - 金额<50000的订单 */
        PENDING_DEPT_APPROVAL("待部门经理审批"),

        /** 待总经理审批 - 金额>=50000的订单 */
        PENDING_GENERAL_APPROVAL("待总经理审批"),

        /** 已批准 - 审批通过,等待供应商发货 */
        APPROVED("已批准"),

        /** 已退回 - 审批未通过,需修改 */
        REJECTED("已退回"),

        /** 已到货 - 货物已送达,可生成入库单 */
        ARRIVED("已到货"),

        /** 已完成 - 入库完成,订单结束 */
        COMPLETED("已完成");

        /** 状态中文显示名称 */
        private String displayName;

        /**
         * 构造函数
         * 
         * @param displayName 状态的中文显示名称
         */
        OrderStatus(String displayName) {
            this.displayName = displayName;
        }

        /**
         * 获取状态的中文显示名称
         * 
         * @return 中文名称(如"待部门经理审批")
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 订单明细项内部类 - 表示订单中的单个商品信息
     * <p>
     * 功能说明:
     * 1. 包含商品ID、名称、规格、数量、单价
     * 2. 自动计算小计金额(quantity * unitPrice)
     * 3. 支持序列化,可通过网络传输
     * <p>
     * 使用示例:
     * 
     * <pre>
     * OrderItem item = new OrderItem("P001", "笔记本电脑", 10, 5500.0);
     * System.out.println(item.getSubtotal()); // 输出55000.0
     * </pre>
     */
    public static class OrderItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 商品ID */
        private String productId;

        /** 商品名称 */
        private String productName;

        /** 商品规格 */
        private String specification;

        /** 采购数量 */
        private int quantity;

        /** 单价 */
        private double unitPrice;

        /** 小计金额(quantity * unitPrice) */
        private double subtotal;

        /**
         * 默认构造函数
         */
        public OrderItem() {
        }

        /**
         * 带参数的构造函数
         * <p>
         * 自动计算小计金额
         * 
         * @param productId   商品ID
         * @param productName 商品名称
         * @param quantity    采购数量
         * @param unitPrice   单价
         */
        public OrderItem(String productId, String productName, int quantity, double unitPrice) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.subtotal = quantity * unitPrice;
        }

        // ==================== Getters and Setters ====================
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
