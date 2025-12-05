package com._404.wms.databases.mysql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com._404.wms.databases.config.ConfigMgr;
import com._404.wms.model.User;
import com._404.wms.model.User.UserRole;

public class MysqlMgr {
    // 单例实例
    private static volatile MysqlMgr instance;

    // 连接池实例
    private MysqlPool mysqlPool;

    // 配置信息
    private String url;
    private String user;
    private String password;
    private String schema;
    private int poolSize;
    private int maxWaitTime = 10000; // 毫秒

    // 私有构造函数
    private MysqlMgr() {
        loadConfig();
        initConnectionPool();
        initTable();
    }

    // 获取单例实例
    public static MysqlMgr getInstance() {
        if (instance == null) {
            synchronized (MysqlMgr.class) {
                if (instance == null) {
                    instance = new MysqlMgr();
                }
            }
        }
        return instance;
    }

    // 加载配置文件
    private void loadConfig() {
        // 读取配置，使用默认值
        ConfigMgr config = ConfigMgr.getInstance();
        this.url = config.getValue("Mysql", "Url");
        this.user = config.getValue("Mysql", "User");
        this.password = config.getValue("Mysql", "Passwd");
        this.schema = config.getValue("Mysql", "Schema");
        this.poolSize = Integer.parseInt(config.getValue("Mysql", "PoolSize"));
    }

    // 初始化连接池
    private void initConnectionPool() {
        try {
            mysqlPool = new MysqlPool(url, user, password, schema, poolSize, maxWaitTime);
            System.out.println("MySQL Manager initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize MySQL Manager: " + e.getMessage());
            throw new RuntimeException("Failed to initialize MySQL Manager", e);
        }
    }

    // 初始化表结构
    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id VARCHAR(50) PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, " +
                "real_name VARCHAR(50), " +
                "role VARCHAR(50), " +
                "department VARCHAR(50), " +
                "email VARCHAR(100), " +
                "phone VARCHAR(20), " +
                "active BOOLEAN DEFAULT TRUE, " +
                "create_time DATETIME, " +
                "last_login_time DATETIME" +
                ")";

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = mysqlPool.getConnection();
            stmt = conn.createStatement();
            stmt.execute(sql);
            System.out.println("Table 'users' initialized successfully");
        } catch (SQLException e) {
            System.err.println("Failed to initialize table 'users': " + e.getMessage());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                mysqlPool.returnConnection(conn);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        return mysqlPool.getConnection();
    }

    /**
     * 归还数据库连接
     * 
     * @param connection Connection对象
     */
    public void returnConnection(Connection connection) {
        mysqlPool.returnConnection(connection);
    }

    /**
     * 添加用户
     */
    public boolean addUser(User user) {
        String sql = "INSERT INTO users (user_id, username, password, real_name, role, department, email, phone, active, create_time, last_login_time) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getRealName());
            pstmt.setString(5, user.getRole() != null ? user.getRole().name() : null);
            pstmt.setString(6, user.getDepartment());
            pstmt.setString(7, user.getEmail());
            pstmt.setString(8, user.getPhone());
            pstmt.setBoolean(9, user.isActive());
            pstmt.setTimestamp(10, user.getCreateTime() != null ? Timestamp.valueOf(user.getCreateTime()) : null);
            pstmt.setTimestamp(11, user.getLastLoginTime() != null ? Timestamp.valueOf(user.getLastLoginTime()) : null);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to add user: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 更新用户
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET username=?, password=?, real_name=?, role=?, department=?, email=?, phone=?, active=?, last_login_time=? WHERE user_id=?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getRealName());
            pstmt.setString(4, user.getRole() != null ? user.getRole().name() : null);
            pstmt.setString(5, user.getDepartment());
            pstmt.setString(6, user.getEmail());
            pstmt.setString(7, user.getPhone());
            pstmt.setBoolean(8, user.isActive());
            pstmt.setTimestamp(9, user.getLastLoginTime() != null ? Timestamp.valueOf(user.getLastLoginTime()) : null);
            pstmt.setString(10, user.getUserId());

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to update user: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 删除用户
     */
    public boolean deleteUser(String userId) {
        String sql = "DELETE FROM users WHERE user_id=?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);

            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Failed to delete user: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(String userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Failed to get user by id: " + e.getMessage());
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Failed to get all users: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return users;
    }

    /**
     * 用户认证
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ? AND active = true";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = mysqlPool.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Failed to authenticate user: " + e.getMessage());
            return null;
        } finally {
            closeResources(conn, pstmt, rs);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setRealName(rs.getString("real_name"));

        String roleStr = rs.getString("role");
        if (roleStr != null) {
            try {
                user.setRole(UserRole.valueOf(roleStr));
            } catch (IllegalArgumentException e) {
                // Ignore invalid role
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

    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            mysqlPool.returnConnection(conn);
        }
    }

    /**
     * 关闭连接池
     */
    public void shutdown() {
        if (mysqlPool != null) {
            mysqlPool.close();
        }
    }
}