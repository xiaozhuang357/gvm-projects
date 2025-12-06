package com._404.wms.dao;

import java.util.List;
import java.util.Optional;

/**
 * 通用DAO接口 - 定义标准的数据访问操作
 * <p>
 * 功能说明:
 * 1. 定义所有实体类的通用CRUD操作
 * 2. 使用泛型支持不同的实体类型和主键类型
 * 3. 使用Optional包装返回值,避免空指针异常
 * 4. 遵循DAO设计模式,分离业务逻辑和数据访问
 * <p>
 * 设计模式:
 * - DAO模式:数据访问对象模式
 * - 泛型编程:提高代码复用性
 * - Optional模式:安全的空值处理
 * <p>
 * 实现类:
 * - UserDaoImpl:用户数据访问实现
 * - ProductDaoImpl:商品数据访问实现
 * - PurchaseOrderDaoImpl:采购订单数据访问实现
 * - StockInRecordDaoImpl:入库记录数据访问实现
 * - StockOutRecordDaoImpl:出库记录数据访问实现
 * - OperationLogDaoImpl:操作日志数据访问实现
 * <p>
 * 使用示例:
 * 
 * <pre>
 * // 通过工厂获取DAO实例
 * UserDao userDao = DaoFactory.getInstance().getUserDao();
 * 
 * // 保存用户
 * User user = new User(...);
 * userDao.save(user);
 * 
 * // 查询用户
 * Optional<User> result = userDao.findById("U001");
 * result.ifPresent(u -> System.out.println(u.getUsername()));
 * </pre>
 *
 * @param <T>  实体类型(如User、Product等)
 * @param <ID> 主键类型(通常为String或Long)
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public interface BaseDao<T, ID> {

    /**
     * 保存实体到数据库
     * <p>
     * 执行INSERT操作,插入新记录
     * 
     * @param entity 要保存的实体对象(不能为null)
     * @return 保存成功返回true,失败返回false
     */
    boolean save(T entity);

    /**
     * 更新实体信息
     * <p>
     * 执行UPDATE操作,根据主键更新记录
     * 
     * @param entity 要更新的实体对象(必须包含主键)
     * @return 更新成功返回true,失败返回false
     */
    boolean update(T entity);

    /**
     * 根据主键删除实体
     * <p>
     * 执行DELETE操作,物理删除记录
     * 
     * @param id 实体主键(不能为null)
     * @return 删除成功返回true,失败或记录不存在返回false
     */
    boolean deleteById(ID id);

    /**
     * 根据主键查询实体
     * <p>
     * 执行SELECT操作,查询单条记录
     * 使用Optional包装返回值,避免空指针
     * 
     * @param id 实体主键(不能为null)
     * @return Optional包装的实体对象,不存在时为Optional.empty()
     */
    Optional<T> findById(ID id);

    /**
     * 查询所有实体
     * <p>
     * 执行SELECT操作,查询表中所有记录
     * 注意:大数据量时应使用分页查询
     * 
     * @return 实体列表,无记录时返回空列表(非null)
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
