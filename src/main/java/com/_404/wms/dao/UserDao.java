package com._404.wms.dao;

import com._404.wms.model.User;
import com._404.wms.model.User.UserRole;

import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 */
public interface UserDao extends BaseDao<User, String> {

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户对象的Optional包装
     */
    Optional<User> findByUsername(String username);

    /**
     * 用户认证
     * 
     * @param username 用户名
     * @param password 密码
     * @return 认证成功返回用户对象，否则返回空
     */
    Optional<User> authenticate(String username, String password);

    /**
     * 根据角色查询用户列表
     * 
     * @param role 用户角色
     * @return 用户列表
     */
    List<User> findByRole(UserRole role);

    /**
     * 根据部门查询用户列表
     * 
     * @param department 部门名称
     * @return 用户列表
     */
    List<User> findByDepartment(String department);

    /**
     * 查询所有活跃用户
     * 
     * @return 活跃用户列表
     */
    List<User> findAllActive();

    /**
     * 更新用户密码
     * 
     * @param userId      用户ID
     * @param newPassword 新密码
     * @return 更新成功返回true
     */
    boolean updatePassword(String userId, String newPassword);

    /**
     * 更新最后登录时间
     * 
     * @param userId 用户ID
     * @return 更新成功返回true
     */
    boolean updateLastLoginTime(String userId);

    /**
     * 设置用户激活状态
     * 
     * @param userId 用户ID
     * @param active 激活状态
     * @return 更新成功返回true
     */
    boolean setActive(String userId, boolean active);
}
