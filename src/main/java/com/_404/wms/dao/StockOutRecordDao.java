package com._404.wms.dao;

import com._404.wms.model.StockOutRecord;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 出库记录数据访问接口
 */
public interface StockOutRecordDao extends BaseDao<StockOutRecord, String> {

    /**
     * 根据商品ID查询出库记录
     * 
     * @param productId 商品ID
     * @return 出库记录列表
     */
    List<StockOutRecord> findByProductId(String productId);

    /**
     * 根据操作员ID查询出库记录
     * 
     * @param operatorId 操作员ID
     * @return 出库记录列表
     */
    List<StockOutRecord> findByOperatorId(String operatorId);

    /**
     * 根据领用人查询出库记录
     * 
     * @param recipient 领用人
     * @return 出库记录列表
     */
    List<StockOutRecord> findByRecipient(String recipient);

    /**
     * 根据领用部门查询出库记录
     * 
     * @param recipientDept 领用部门
     * @return 出库记录列表
     */
    List<StockOutRecord> findByRecipientDept(String recipientDept);

    /**
     * 根据时间范围查询出库记录
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 出库记录列表
     */
    List<StockOutRecord> findByTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用途查询出库记录
     * 
     * @param purpose 出库用途
     * @return 出库记录列表
     */
    List<StockOutRecord> findByPurposeLike(String purpose);

    /**
     * 统计指定商品的出库总数量
     * 
     * @param productId 商品ID
     * @return 出库总数量
     */
    int sumQuantityByProductId(String productId);

    /**
     * 统计指定时间范围内的出库总数量
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 出库总数量
     */
    int sumQuantityByTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取最近的出库记录
     * 
     * @param limit 数量限制
     * @return 出库记录列表
     */
    List<StockOutRecord> findRecent(int limit);

    /**
     * 统计各部门出库数量
     * 
     * @return 部门-数量映射
     */
    java.util.Map<String, Integer> sumQuantityByDept();
}
