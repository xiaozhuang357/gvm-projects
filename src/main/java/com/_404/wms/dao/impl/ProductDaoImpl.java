package com._404.wms.dao.impl;

import com._404.wms.dao.ProductDao;
import com._404.wms.model.Product;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品DAO实现类
 */
public class ProductDaoImpl extends AbstractDao<Product, String> implements ProductDao {

    @Override
    protected String getTableName() {
        return "products";
    }

    @Override
    protected String getIdColumn() {
        return "product_id";
    }

    @Override
    protected Product mapResultSetToEntity(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getString("product_id"));
        product.setProductName(rs.getString("product_name"));
        product.setCategory(rs.getString("category"));
        product.setSpecification(rs.getString("specification"));
        product.setUnit(rs.getString("unit"));
        product.setPurchasePrice(rs.getDouble("purchase_price"));
        product.setSellingPrice(rs.getDouble("selling_price"));
        product.setCurrentStock(rs.getInt("current_stock"));
        product.setMinStock(rs.getInt("min_stock"));
        product.setMaxStock(rs.getInt("max_stock"));
        product.setSupplier(rs.getString("supplier"));
        product.setDescription(rs.getString("description"));
        product.setActive(rs.getBoolean("active"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            product.setCreateTime(createTime.toLocalDateTime());
        }

        Timestamp updateTime = rs.getTimestamp("update_time");
        if (updateTime != null) {
            product.setUpdateTime(updateTime.toLocalDateTime());
        }

        return product;
    }

    @Override
    public boolean save(Product product) {
        String sql = """
                INSERT INTO products (product_id, product_name, category, specification, unit,
                                     purchase_price, selling_price, current_stock, min_stock, max_stock,
                                     supplier, description, active, create_time, update_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        LocalDateTime now = LocalDateTime.now();
        return executeUpdate(sql,
                product.getProductId(),
                product.getProductName(),
                product.getCategory(),
                product.getSpecification(),
                product.getUnit(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getCurrentStock(),
                product.getMinStock(),
                product.getMaxStock(),
                product.getSupplier(),
                product.getDescription(),
                product.isActive(),
                product.getCreateTime() != null ? Timestamp.valueOf(product.getCreateTime()) : Timestamp.valueOf(now),
                product.getUpdateTime() != null ? Timestamp.valueOf(product.getUpdateTime()) : Timestamp.valueOf(now));
    }

    @Override
    public boolean update(Product product) {
        String sql = """
                UPDATE products SET product_name = ?, category = ?, specification = ?, unit = ?,
                                   purchase_price = ?, selling_price = ?, current_stock = ?,
                                   min_stock = ?, max_stock = ?, supplier = ?, description = ?,
                                   active = ?, update_time = ?
                WHERE product_id = ?
                """;

        return executeUpdate(sql,
                product.getProductName(),
                product.getCategory(),
                product.getSpecification(),
                product.getUnit(),
                product.getPurchasePrice(),
                product.getSellingPrice(),
                product.getCurrentStock(),
                product.getMinStock(),
                product.getMaxStock(),
                product.getSupplier(),
                product.getDescription(),
                product.isActive(),
                Timestamp.valueOf(LocalDateTime.now()),
                product.getProductId());
    }

    @Override
    public List<Product> findByProductNameLike(String productName) {
        String sql = "SELECT * FROM products WHERE product_name LIKE ?";
        return queryForList(sql, "%" + productName + "%");
    }

    @Override
    public List<Product> findByCategory(String category) {
        String sql = "SELECT * FROM products WHERE category = ?";
        return queryForList(sql, category);
    }

    @Override
    public List<Product> findAllActive() {
        String sql = "SELECT * FROM products WHERE active = true";
        return queryForList(sql);
    }

    @Override
    public List<Product> findLowStock() {
        String sql = "SELECT * FROM products WHERE current_stock <= min_stock AND active = true";
        return queryForList(sql);
    }

    @Override
    public List<Product> findOverStock() {
        String sql = "SELECT * FROM products WHERE current_stock >= max_stock AND active = true";
        return queryForList(sql);
    }

    @Override
    public List<Product> findBySupplier(String supplier) {
        String sql = "SELECT * FROM products WHERE supplier = ?";
        return queryForList(sql, supplier);
    }

    @Override
    public boolean updateStock(String productId, int quantity) {
        String sql = "UPDATE products SET current_stock = current_stock + ?, update_time = ? WHERE product_id = ?";
        return executeUpdate(sql, quantity, Timestamp.valueOf(LocalDateTime.now()), productId);
    }

    @Override
    public boolean setStock(String productId, int stock) {
        String sql = "UPDATE products SET current_stock = ?, update_time = ? WHERE product_id = ?";
        return executeUpdate(sql, stock, Timestamp.valueOf(LocalDateTime.now()), productId);
    }

    @Override
    public boolean setActive(String productId, boolean active) {
        String sql = "UPDATE products SET active = ?, update_time = ? WHERE product_id = ?";
        return executeUpdate(sql, active, Timestamp.valueOf(LocalDateTime.now()), productId);
    }

    @Override
    public List<String> findAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM products WHERE category IS NOT NULL ORDER BY category";

        java.sql.Connection conn = null;
        java.sql.PreparedStatement pstmt = null;
        java.sql.ResultSet rs = null;

        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                if (category != null && !category.isEmpty()) {
                    categories.add(category);
                }
            }
        } catch (SQLException e) {
            logger.severe("Failed to get categories: " + e.getMessage());
        } finally {
            com._404.wms.db.util.DbUtils.closeAll(rs, pstmt);
            releaseConnection(conn);
        }

        return categories;
    }
}
