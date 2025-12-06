package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品实体类 - 仓库管理系统的商品信息模型
 * <p>
 * 功能说明:
 * 1. 表示WMS系统中的商品/物料信息
 * 2. 支持库存预警功能(currentStock <= minStock时触发)
 * 3. 支持库存上限检查(currentStock >= maxStock时提示)
 * 4. 记录商品的采购价和销售价(支持成本核算)
 * <p>
 * 库存管理规则:
 * - minStock:安全库存/最小库存,低于此值触发采购预警
 * - maxStock:最大库存,超过此值提示库存积压
 * - currentStock:实时库存,通过入库/出库操作更新
 * <p>
 * 数据库映射:
 * - 对应数据库表:products
 * - 主键:productId
 * <p>
 * 使用示例:
 * 
 * <pre>
 * // 创建商品
 * Product product = new Product("P001", "笔记本电脑", "电子产品", 5500.0, 5, 50);
 * product.setCurrentStock(25);
 * product.setUnit("台");
 * 
 * // 检查库存预警
 * if (product.needsStockAlert()) {
 *     System.out.println("库存不足,需要补货!");
 * }
 * </pre>
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 商品唯一标识,主键 */
    private String productId;

    /** 商品名称 */
    private String productName;

    /** 商品类别(如"电子产品"、"办公用品") */
    private String category;

    /** 规格型号 */
    private String specification;

    /** 计量单位(如"台"、"箱"、"包") */
    private String unit;

    /** 采购价格(成本价) */
    private double purchasePrice;

    /** 销售价格 */
    private double sellingPrice;

    /** 当前实时库存数量 */
    private int currentStock;

    /** 最小库存/安全库存阈值,低于此值触发预警 */
    private int minStock;

    /** 最大库存阈值,超过此值提示积压 */
    private int maxStock;

    /** 供应商名称 */
    private String supplier;

    /** 商品描述/备注 */
    private String description;

    /** 是否启用:true=正常,false=停用 */
    private boolean active;

    /** 记录创建时间 */
    private LocalDateTime createTime;

    /** 最后更新时间 */
    private LocalDateTime updateTime;

    /**
     * 默认构造函数
     * <p>
     * 自动设置:
     * - createTime和updateTime为当前时间
     * - active为true(商品启用)
     */
    public Product() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.active = true;
    }

    /**
     * 带参数的构造函数
     * <p>
     * 用于快速创建商品对象,常用于初始化示例数据
     * 
     * @param productId     商品ID(唯一标识)
     * @param productName   商品名称
     * @param category      商品类别
     * @param purchasePrice 采购价格
     * @param minStock      最小库存阈值
     * @param maxStock      最大库存阈值
     */
    public Product(String productId, String productName, String category,
            double purchasePrice, int minStock, int maxStock) {
        this();
        this.productId = productId;
        this.productName = productName;
        this.category = category;
        this.purchasePrice = purchasePrice;
        this.minStock = minStock;
        this.maxStock = maxStock;
    }

    /**
     * 检查是否需要库存预警
     * <p>
     * 当实时库存低于或等于安全库存时返回true
     * 用于触发采购订单创建或库存补货提醒
     * 
     * @return true=需要预警,false=库存充足
     */
    public boolean needsStockAlert() {
        return currentStock <= minStock;
    }

    /**
     * 检查库存是否超出上限
     * <p>
     * 当实时库存达到或超过最大库存时返回true
     * 用于提示库存积压或暂停采购
     * 
     * @return true=库存过多,false=库存正常
     */
    public boolean isOverStock() {
        return currentStock >= maxStock;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(double sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
    }

    public int getMinStock() {
        return minStock;
    }

    public void setMinStock(int minStock) {
        this.minStock = minStock;
    }

    public int getMaxStock() {
        return maxStock;
    }

    public void setMaxStock(int maxStock) {
        this.maxStock = maxStock;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取价格（返回采购价格）
     */
    public double getPrice() {
        return purchasePrice;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", category='" + category + '\'' +
                ", currentStock=" + currentStock +
                ", minStock=" + minStock +
                '}';
    }
}
