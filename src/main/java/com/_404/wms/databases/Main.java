package com._404.wms.databases;

import com._404.wms.databases.mysql.MysqlMgr;
import com._404.wms.model.User;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        Main app = new Main();
        app.testRegisterUser("test_1", "11@qq.com", "pwd_123456");
        app.testLoginUser("test_1", "pwd_123456");
        app.getUsers(5);

        /*
         * try {
         * // 启动LogicMgr
         * LogicMgr logicMgr = LogicMgr.getInstance();
         * logicMgr.start();
         * 
         * // 启动TCP服务器
         * TcpMgr tcpMgr = TcpMgr.getInstance();
         * tcpMgr.start();
         * 
         * System.out.println("Server started successfully!");
         * System.out.println("LogicMgr running: " + logicMgr.isRunning());
         * System.out.println("LogicMgr queue size: " + logicMgr.getQueueSize());
         * System.out.println("LogicMgr handlers: " + logicMgr.getHandlerCount());
         * 
         * // 添加关闭回调
         * Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         * System.out.println("\nShutting down server...");
         * tcpMgr.stop();
         * logicMgr.stop();
         * System.out.println("Server stopped.");
         * }));
         * 
         * // 保持主线程运行
         * System.out.println("\nPress Ctrl+C to stop the server...");
         * while (true) {
         * Thread.sleep(5000);
         * System.out.println("Server status - Active sessions: " +
         * tcpMgr.getActiveSessionCount() +
         * ", Message queue size: " + logicMgr.getQueueSize());
         * }
         * 
         * } catch (Exception e) {
         * System.err.println("Failed to start server: " + e.getMessage());
         * e.printStackTrace();
         * }
         */
    }

    public void testRegisterUser(String name, String email, String password) {
        try {
            User user = new User();
            user.setUserId(UUID.randomUUID().toString());
            user.setUsername(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setRole(User.UserRole.WAREHOUSE_ADMIN); // Default role

            boolean success = MysqlMgr.getInstance().addUser(user);
            System.out.println("Registered user success: " + success);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testLoginUser(String username, String password) {
        try {
            User user = MysqlMgr.getInstance().authenticate(username, password);
            if (user != null) {
                System.out.println("Login successful: " + user);
            } else {
                System.out.println("Login failed for username: " + username);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getUsers(int limit) {
        try {
            for (User user : MysqlMgr.getInstance().getAllUsers()) {
                System.out.println(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
