package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 商品实体类
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productId; // 商品ID
    private String productName; // 商品名称
    private String category; // 商品类别
    private String specification; // 规格型号
    private String unit; // 单位
    private double purchasePrice; // 采购价格
    private double sellingPrice; // 销售价格
    private int currentStock; // 当前库存
    private int minStock; // 最小库存（安全库存）
    private int maxStock; // 最大库存
    private String supplier; // 供应商
    private String description; // 商品描述
    private boolean active; // 是否启用
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间

    public Product() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.active = true;
    }

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
     */
    public boolean needsStockAlert() {
        return currentStock <= minStock;
    }

    /**
     * 检查库存是否超出上限
     */
    public boolean isOverStock() {
        return currentStock >= maxStock;
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
