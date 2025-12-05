package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId; // 用户ID
    private String username; // 用户名
    private String password; // 密码
    private String realName; // 真实姓名
    private UserRole role; // 用户角色
    private String department; // 所属部门
    private String email; // 邮箱
    private String phone; // 联系电话
    private boolean active; // 是否激活
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime lastLoginTime; // 最后登录时间

    public enum UserRole {
        WAREHOUSE_ADMIN("仓库管理员"),
        PURCHASER("采购员"),
        DEPARTMENT_MANAGER("部门经理"),
        GENERAL_MANAGER("总经理");

        private String displayName;

        UserRole(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public User() {
        this.createTime = LocalDateTime.now();
        this.active = true;
    }

    public User(String userId, String username, String password, String realName, UserRole role) {
        this();
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
    }

    // Getters and Setters
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
