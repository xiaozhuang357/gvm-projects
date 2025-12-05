package com._404.wms.dao;

import com._404.wms.model.StockInRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库记录数据访问接口
 */
public interface StockInRecordDao extends BaseDao<StockInRecord, String> {

    /**
     * 根据订单ID查询入库记录
     * 
     * @param orderId 订单ID
     * @return 入库记录列表
     */
    List<StockInRecord> findByOrderId(String orderId);

    /**
     * 根据商品ID查询入库记录
     * 
     * @param productId 商品ID
     * @return 入库记录列表
     */
    List<StockInRecord> findByProductId(String productId);

    /**
     * 根据操作员ID查询入库记录
     * 
     * @param operatorId 操作员ID
     * @return 入库记录列表
     */
    List<StockInRecord> findByOperatorId(String operatorId);

    /**
     * 根据时间范围查询入库记录
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 入库记录列表
     */
    List<StockInRecord> findByTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据仓库查询入库记录
     * 
     * @param warehouse 仓库名称
     * @return 入库记录列表
     */
    List<StockInRecord> findByWarehouse(String warehouse);

    /**
     * 根据批次号查询入库记录
     * 
     * @param batchNumber 批次号
     * @return 入库记录列表
     */
    List<StockInRecord> findByBatchNumber(String batchNumber);

    /**
     * 统计指定商品的入库总数量
     * 
     * @param productId 商品ID
     * @return 入库总数量
     */
    int sumQuantityByProductId(String productId);

    /**
     * 统计指定时间范围内的入库总数量
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 入库总数量
     */
    int sumQuantityByTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取最近的入库记录
     * 
     * @param limit 数量限制
     * @return 入库记录列表
     */
    List<StockInRecord> findRecent(int limit);
}
