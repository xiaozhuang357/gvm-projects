package com._404.wms.dao;

import java.util.List;
import java.util.Optional;

/**
 * 通用DAO接口
 * 定义基本的CRUD操作
 * 
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface BaseDao<T, ID> {

    /**
     * 保存实体
     * 
     * @param entity 要保存的实体
     * @return 保存成功返回true
     */
    boolean save(T entity);

    /**
     * 更新实体
     * 
     * @param entity 要更新的实体
     * @return 更新成功返回true
     */
    boolean update(T entity);

    /**
     * 根据ID删除实体
     * 
     * @param id 实体ID
     * @return 删除成功返回true
     */
    boolean deleteById(ID id);

    /**
     * 根据ID查询实体
     * 
     * @param id 实体ID
     * @return 实体对象的Optional包装
     */
    Optional<T> findById(ID id);

    /**
     * 查询所有实体
     * 
     * @return 实体列表
     */
    List<T> findAll();

    /**
     * 统计实体数量
     * 
     * @return 实体数量
     */
    long count();

    /**
     * 检查ID是否存在
     * 
     * @param id 实体ID
     * @return 存在返回true
     */
    boolean existsById(ID id);
}
