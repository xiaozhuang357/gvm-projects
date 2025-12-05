package com._404.wms.dao.impl;

import com._404.wms.dao.UserDao;
import com._404.wms.model.User;
import com._404.wms.model.User.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户DAO实现类
 */
public class UserDaoImpl extends AbstractDao<User, String> implements UserDao {

    @Override
    protected String getTableName() {
        return "users";
    }

    @Override
    protected String getIdColumn() {
        return "user_id";
    }

    @Override
    protected User mapResultSetToEntity(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRealName(rs.getString("real_name"));

        String roleStr = rs.getString("role");
        if (roleStr != null && !roleStr.isEmpty()) {
            try {
                user.setRole(UserRole.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                // 忽略无效的角色值
            }
        }

        user.setDepartment(rs.getString("department"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setActive(rs.getBoolean("active"));

        Timestamp createTime = rs.getTimestamp("create_time");
        if (createTime != null) {
            user.setCreateTime(createTime.toLocalDateTime());
        }

        Timestamp lastLoginTime = rs.getTimestamp("last_login_time");
        if (lastLoginTime != null) {
            user.setLastLoginTime(lastLoginTime.toLocalDateTime());
        }

        return user;
    }

    @Override
    public boolean save(User user) {
        String sql = """
                INSERT INTO users (user_id, username, password, real_name, role, department,
                                  email, phone, active, create_time, last_login_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        return executeUpdate(sql,
                user.getUserId(),
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getDepartment(),
                user.getEmail(),
                user.getPhone(),
                user.isActive(),
                user.getCreateTime() != null ? Timestamp.valueOf(user.getCreateTime())
                        : Timestamp.valueOf(LocalDateTime.now()),
                user.getLastLoginTime() != null ? Timestamp.valueOf(user.getLastLoginTime()) : null);
    }

    @Override
    public boolean update(User user) {
        String sql = """
                UPDATE users SET username = ?, password = ?, real_name = ?, role = ?,
                                department = ?, email = ?, phone = ?, active = ?, last_login_time = ?
                WHERE user_id = ?
                """;

        return executeUpdate(sql,
                user.getUsername(),
                user.getPassword(),
                user.getRealName(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getDepartment(),
                user.getEmail(),
                user.getPhone(),
                user.isActive(),
                user.getLastLoginTime() != null ? Timestamp.valueOf(user.getLastLoginTime()) : null,
                user.getUserId());
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return queryForObject(sql, username);
    }

    @Override
    public Optional<User> authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND active = true";
        return queryForObject(sql, username, password);
    }

    @Override
    public List<User> findByRole(UserRole role) {
        String sql = "SELECT * FROM users WHERE role = ?";
        return queryForList(sql, role.name());
    }

    @Override
    public List<User> findByDepartment(String department) {
        String sql = "SELECT * FROM users WHERE department = ?";
        return queryForList(sql, department);
    }

    @Override
    public List<User> findAllActive() {
        String sql = "SELECT * FROM users WHERE active = true";
        return queryForList(sql);
    }

    @Override
    public boolean updatePassword(String userId, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        return executeUpdate(sql, newPassword, userId);
    }

    @Override
    public boolean updateLastLoginTime(String userId) {
        String sql = "UPDATE users SET last_login_time = ? WHERE user_id = ?";
        return executeUpdate(sql, Timestamp.valueOf(LocalDateTime.now()), userId);
    }

    @Override
    public boolean setActive(String userId, boolean active) {
        String sql = "UPDATE users SET active = ? WHERE user_id = ?";
        return executeUpdate(sql, active, userId);
    }
}
