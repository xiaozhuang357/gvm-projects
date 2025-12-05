package com._404.wms.dao;

import com._404.wms.dao.impl.*;

/**
 * DAO工厂类
 * 提供统一的DAO实例获取入口
 */
public class DaoFactory {

    private static volatile DaoFactory instance;

    // DAO实例缓存
    private UserDao userDao;
    private ProductDao productDao;
    private PurchaseOrderDao purchaseOrderDao;
    private StockInRecordDao stockInRecordDao;
    private StockOutRecordDao stockOutRecordDao;
    private OperationLogDao operationLogDao;

    private DaoFactory() {
        // 私有构造函数
    }

    /**
     * 获取工厂实例
     */
    public static DaoFactory getInstance() {
        if (instance == null) {
            synchronized (DaoFactory.class) {
                if (instance == null) {
                    instance = new DaoFactory();
                }
            }
        }
        return instance;
    }

    /**
     * 获取用户DAO
     */
    public UserDao getUserDao() {
        if (userDao == null) {
            synchronized (this) {
                if (userDao == null) {
                    userDao = new UserDaoImpl();
                }
            }
        }
        return userDao;
    }

    /**
     * 获取商品DAO
     */
    public ProductDao getProductDao() {
        if (productDao == null) {
            synchronized (this) {
                if (productDao == null) {
                    productDao = new ProductDaoImpl();
                }
            }
        }
        return productDao;
    }

    /**
     * 获取采购订单DAO
     */
    public PurchaseOrderDao getPurchaseOrderDao() {
        if (purchaseOrderDao == null) {
            synchronized (this) {
                if (purchaseOrderDao == null) {
                    purchaseOrderDao = new PurchaseOrderDaoImpl();
                }
            }
        }
        return purchaseOrderDao;
    }

    /**
     * 获取入库记录DAO
     */
    public StockInRecordDao getStockInRecordDao() {
        if (stockInRecordDao == null) {
            synchronized (this) {
                if (stockInRecordDao == null) {
                    stockInRecordDao = new StockInRecordDaoImpl();
                }
            }
        }
        return stockInRecordDao;
    }

    /**
     * 获取出库记录DAO
     */
    public StockOutRecordDao getStockOutRecordDao() {
        if (stockOutRecordDao == null) {
            synchronized (this) {
                if (stockOutRecordDao == null) {
                    stockOutRecordDao = new StockOutRecordDaoImpl();
                }
            }
        }
        return stockOutRecordDao;
    }

    /**
     * 获取操作日志DAO
     */
    public OperationLogDao getOperationLogDao() {
        if (operationLogDao == null) {
            synchronized (this) {
                if (operationLogDao == null) {
                    operationLogDao = new OperationLogDaoImpl();
                }
            }
        }
        return operationLogDao;
    }
}
