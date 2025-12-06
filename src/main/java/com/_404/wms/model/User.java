package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类 - 系统用户信息的数据模型
 * <p>
 * 功能说明:
 * 1. 表示WMS系统中的用户账户信息
 * 2. 实现Serializable接口,支持通过Socket进行序列化传输
 * 3. 包含用户基本信息、角色权限、状态管理等字段
 * 4. 支持四种角色:仓库管理员、采购员、部门经理、总经理
 * <p>
 * 角色权限说明:
 * - WAREHOUSE_ADMIN(仓库管理员):管理用户、商品、库存、查看所有订单
 * - PURCHASER(采购员):创建采购订单、查看自己创建的订单
 * - DEPARTMENT_MANAGER(部门经理):审批金额<50000的采购订单
 * - GENERAL_MANAGER(总经理):审批金额>=50000的采购订单
 * <p>
 * 数据库映射:
 * - 对应数据库表:users
 * - 主键:userId
 * - 唯一索引:username
 * <p>
 * 使用示例:
 * 
 * <pre>
 * // 创建新用户
 * User user = new User("U001", "admin", "123456", "张三", UserRole.WAREHOUSE_ADMIN);
 * user.setDepartment("信息部");
 * user.setEmail("admin@wms.com");
 * 
 * // 检查用户权限
 * if (user.getRole() == UserRole.GENERAL_MANAGER) {
 *     // 只有总经理可以审批大额订单
 * }
 * </pre>
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 用户唯一标识,主键 */
    private String userId;

    /** 登录用户名,唯一不可重复 */
    private String username;

    /** 登录密码,建议加密存储(当前未加密) */
    private String password;

    /** 用户真实姓名 */
    private String realName;

    /** 用户角色,决定权限范围 */
    private UserRole role;

    /** 所属部门 */
    private String department;

    /** 邮箱地址 */
    private String email;

    /** 联系电话 */
    private String phone;

    /** 账户状态:true=激活,false=禁用 */
    private boolean active;

    /** 账户创建时间 */
    private LocalDateTime createTime;

    /** 最后一次登录时间 */
    private LocalDateTime lastLoginTime;

    /**
     * 用户角色枚举 - 定义系统中所有可用的角色类型
     * <p>
     * 角色权限分级:
     * - WAREHOUSE_ADMIN:最高权限,可管理所有功能模块
     * - PURCHASER:只能创建采购订单
     * - DEPARTMENT_MANAGER:审批小额订单(<50000)
     * - GENERAL_MANAGER:审批大额订单(>=50000)
     */
    public enum UserRole {
        /** 仓库管理员 - 系统最高权限 */
        WAREHOUSE_ADMIN("仓库管理员"),

        /** 采购员 - 负责创建采购订单 */
        PURCHASER("采购员"),

        /** 部门经理 - 审批小额采购订单 */
        DEPARTMENT_MANAGER("部门经理"),

        /** 总经理 - 审批大额采购订单 */
        GENERAL_MANAGER("总经理");

        /** 角色中文显示名称 */
        private String displayName;

        /**
         * 构造函数
         * 
         * @param displayName 角色的中文显示名称
         */
        UserRole(String displayName) {
            this.displayName = displayName;
        }

        /**
         * 获取角色的中文显示名称
         * 
         * @return 中文名称(如"仓库管理员")
         */
        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 默认构造函数
     * <p>
     * 自动设置:
     * - createTime为当前时间
     * - active为true(账户激活)
     */
    public User() {
        this.createTime = LocalDateTime.now();
        this.active = true;
    }

    /**
     * 带参数的构造函数
     * <p>
     * 用于快速创建用户对象,常用于初始化示例数据
     * 
     * @param userId   用户ID(唯一标识)
     * @param username 登录用户名
     * @param password 登录密码
     * @param realName 真实姓名
     * @param role     用户角色
     */
    public User(String userId, String username, String password, String realName, UserRole role) {
        this();
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
    }

    // ==================== Getters and Setters ====================

    /**
     * 获取用户ID
     * 
     * @return 用户唯一标识
     */
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    /**
     * 获取用户全名（真实姓名）
     */
    public String getFullName() {
        return realName;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", realName='" + realName + '\'' +
                ", role=" + role +
                ", department='" + department + '\'' +
                '}';
    }
}
