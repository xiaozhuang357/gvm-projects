package com._404.wms.dao;

import com._404.wms.model.Product;

import java.util.List;

/**
 * 商品数据访问接口
 */
public interface ProductDao extends BaseDao<Product, String> {

    /**
     * 根据商品名称模糊查询
     * 
     * @param productName 商品名称
     * @return 商品列表
     */
    List<Product> findByProductNameLike(String productName);

    /**
     * 根据类别查询商品
     * 
     * @param category 商品类别
     * @return 商品列表
     */
    List<Product> findByCategory(String category);

    /**
     * 查询所有活跃商品
     * 
     * @return 活跃商品列表
     */
    List<Product> findAllActive();

    /**
     * 查询库存低于安全库存的商品
     * 
     * @return 低库存商品列表
     */
    List<Product> findLowStock();

    /**
     * 查询库存超出上限的商品
     * 
     * @return 超库存商品列表
     */
    List<Product> findOverStock();

    /**
     * 根据供应商查询商品
     * 
     * @param supplier 供应商名称
     * @return 商品列表
     */
    List<Product> findBySupplier(String supplier);

    /**
     * 更新商品库存
     * 
     * @param productId 商品ID
     * @param quantity  变化数量（正数增加，负数减少）
     * @return 更新成功返回true
     */
    boolean updateStock(String productId, int quantity);

    /**
     * 设置商品库存
     * 
     * @param productId 商品ID
     * @param stock     新库存数量
     * @return 更新成功返回true
     */
    boolean setStock(String productId, int stock);

    /**
     * 设置商品激活状态
     * 
     * @param productId 商品ID
     * @param active    激活状态
     * @return 更新成功返回true
     */
    boolean setActive(String productId, boolean active);

    /**
     * 获取所有类别
     * 
     * @return 类别列表
     */
    List<String> findAllCategories();
}
