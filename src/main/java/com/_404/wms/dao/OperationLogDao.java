package com._404.wms.dao;

import com._404.wms.model.OperationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问接口
 */
public interface OperationLogDao extends BaseDao<OperationLog, String> {

    /**
     * 根据用户ID查询日志
     * 
     * @param userId 用户ID
     * @return 日志列表
     */
    List<OperationLog> findByUserId(String userId);

    /**
     * 根据模块查询日志
     * 
     * @param module 模块名称
     * @return 日志列表
     */
    List<OperationLog> findByModule(String module);

    /**
     * 根据操作类型查询日志
     * 
     * @param operation 操作类型
     * @return 日志列表
     */
    List<OperationLog> findByOperation(String operation);

    /**
     * 根据时间范围查询日志
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<OperationLog> findByTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询失败的操作日志
     * 
     * @return 失败日志列表
     */
    List<OperationLog> findFailed();

    /**
     * 根据用户ID和时间范围查询日志
     * 
     * @param userId    用户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 日志列表
     */
    List<OperationLog> findByUserIdAndTimeBetween(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取最近的日志记录
     * 
     * @param limit 数量限制
     * @return 日志列表
     */
    List<OperationLog> findRecent(int limit);

    /**
     * 删除指定时间之前的日志
     * 
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    int deleteBeforeTime(LocalDateTime beforeTime);

    /**
     * 统计各操作类型的数量
     * 
     * @return 操作类型-数量映射
     */
    java.util.Map<String, Long> countByOperation();

    /**
     * 统计各模块的日志数量
     * 
     * @return 模块-数量映射
     */
    java.util.Map<String, Long> countByModule();
}
