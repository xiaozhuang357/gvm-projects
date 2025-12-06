package com._404.wms.network;

import com._404.wms.config.ConfigManager;
import com._404.wms.model.User;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Socket客户端工具类 - 客户端与服务端通信的核心组件
 * <p>
 * 功能说明：
 * 1. 建立和维护与WMS服务器的Socket连接
 * 2. 提供同步的请求-响应通信机制（sendAndReceive）
 * 3. 自动处理对象流的创建和清理
 * 4. 实现超时控制，避免长时间阻塞
 * 5. 封装常用业务操作（登录、登出、获取列表等）
 * 6. 线程安全的消息发送（synchronized）
 * <p>
 * 连接流程：
 * 1. 从config.ini读取服务器地址和端口（支持远程部署）
 * 2. 调用connect()建立Socket连接
 * 3. 创建ObjectOutputStream/ObjectInputStream进行序列化通信
 * 4. 使用sendAndReceive()发送Message对象并等待响应
 * 5. 调用disconnect()或closeConnection()关闭连接
 * <p>
 * 使用示例：
 * 
 * <pre>
 * // 创建客户端并连接服务器
 * SocketClient client = new SocketClient();
 * if (client.connect()) {
 *     // 登录
 *     Message response = client.login("admin", "123456");
 *     if (response.isSuccess()) {
 *         User user = client.getCurrentUser();
 *         System.out.println("登录成功: " + user.getUsername());
 *     }
 * 
 *     // 获取采购订单列表
 *     Message orders = client.getPurchaseOrders();
 * 
 *     // 断开连接
 *     client.disconnect();
 * }
 * </pre>
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class SocketClient {
    /** 默认服务器地址（当配置文件读取失败时使用） */
    private static final String DEFAULT_HOST = "localhost";

    /** 默认服务器端口 */
    private static final int DEFAULT_PORT = 8888;

    /** 服务器地址（从配置文件读取） */
    private final String serverHost;

    /** 服务器端口（从配置文件读取） */
    private final int serverPort;

    /** Socket连接对象 */
    private Socket socket;

    /** 对象输出流，用于向服务器发送Message对象 */
    private ObjectOutputStream out;

    /** 对象输入流，用于从服务器接收Message对象 */
    private ObjectInputStream in;

    /** 当前登录的用户对象 */
    private User currentUser;

    /** 连接状态标志，volatile保证多线程可见性 */
    private volatile boolean connected;

    /**
     * 默认构造函数
     * <p>
     * 从config.ini配置文件读取服务器地址和端口
     * 支持部署到云服务器（如华为云）的场景
     */
    public SocketClient() {
        this.connected = false;
        // 从配置文件读取服务器地址和端口
        ConfigManager config = ConfigManager.getInstance();
        this.serverHost = config.getValue("Server", "Host", DEFAULT_HOST);
        this.serverPort = config.getIntValue("Server", "Port", DEFAULT_PORT);
    }

    /**
     * 使用指定的服务器地址和端口创建客户端
     * <p>
     * 使用场景：测试或特殊环境需要动态指定服务器地址
     * 
     * @param host 服务器IP地址或域名
     * @param port 服务器端口号（1-65535）
     */
    public SocketClient(String host, int port) {
        this.connected = false;
        this.serverHost = host;
        this.serverPort = port;
    }

    /**
     * 连接到服务器
     * <p>
     * 执行步骤：
     * 1. 创建Socket连接到指定的服务器地址和端口
     * 2. 设置10秒超时，防止无限阻塞
     * 3. 创建ObjectOutputStream并立即flush（发送流头部）
     * 4. 创建ObjectInputStream（接收流头部）
     * 5. 设置连接状态为true
     * <p>
     * 注意事项：
     * - 必须先创建ObjectOutputStream再创建ObjectInputStream（Java序列化协议要求）
     * - flush()操作确保流头部信息立即发送，否则可能导致客户端和服务端死锁
     * - 如果连接失败，会打印错误信息并返回false
     * 
     * @return 连接成功返回true，失败返回false
     */
    public boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            socket.setSoTimeout(10000); // 设置10秒超时，避免无限阻塞
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // 立即刷新，发送流头部信息
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            System.out.println("已连接到服务器: " + serverHost + ":" + serverPort);
            return true;
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取服务器地址
     * 
     * @return 服务器IP地址或域名
     */
    public String getServerHost() {
        return serverHost;
    }

    /**
     * 获取服务器端口
     * 
     * @return 服务器端口号
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * 断开连接（会向服务器发送LOGOUT消息）
     * <p>
     * 使用场景：正常退出应用时调用
     * <p>
     * 执行步骤：
     * 1. 设置连接状态为false
     * 2. 如果用户已登录，发送LOGOUT消息通知服务器
     * 3. 调用closeConnection()关闭所有资源
     */
    public void disconnect() {
        connected = false;
        try {
            if (currentUser != null) {
                sendAndReceive(new Message(Message.MessageType.LOGOUT));
            }
            closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 仅关闭连接（不发送任何消息）
     * <p>
     * 使用场景：
     * - 连接失败后的清理工作
     * - 已经发送过LOGOUT消息后的资源释放
     * - 异常情况下的强制关闭
     * <p>
     * 执行步骤：
     * 1. 按顺序关闭输入流、输出流、Socket
     * 2. 将所有引用设置为null（帮助GC）
     * 3. 清空当前用户信息
     */
    public void closeConnection() {
        connected = false;
        currentUser = null;
        try {
            if (in != null) {
                in.close();
                in = null;
            }
            if (out != null) {
                out.close();
                out = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
            System.out.println("已断开与服务器的连接");
        } catch (IOException e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }

    /**
     * 发送消息并等待响应（同步方式）
     * <p>
     * 线程安全的核心通信方法，使用synchronized确保同一时间只有一个请求
     * <p>
     * 执行流程：
     * 1. 检查连接状态，未连接则直接返回错误消息
     * 2. 自动设置发送者ID（当前用户ID）
     * 3. 将Message对象序列化发送到服务器
     * 4. flush()确保消息立即发送
     * 5. 阻塞等待服务器响应（最长10秒）
     * 6. 反序列化响应Message对象并返回
     * <p>
     * 注意事项：
     * - 该方法会阻塞直到收到响应或超时
     * - 通信失败会自动设置connected=false
     * - 失败后需要重新调用connect()重连
     * 
     * @param message 要发送的消息对象
     * @return 服务器返回的响应消息，失败返回错误消息
     */
    public synchronized Message sendAndReceive(Message message) {
        if (!connected) {
            return Message.error(Message.MessageType.ERROR, "未连接到服务器");
        }

        try {
            if (currentUser != null) {
                message.setSender(currentUser.getUserId());
            }
            out.writeObject(message);
            out.flush();

            // 等待响应
            Message response = (Message) in.readObject();
            return response;
        } catch (Exception e) {
            System.err.println("通信错误: " + e.getMessage());
            connected = false;
            return Message.error(Message.MessageType.ERROR, "通信失败: " + e.getMessage());
        }
    }

    /**
     * 用户登录
     * <p>
     * 发送LOGIN_REQUEST消息到服务器进行身份验证
     * 登录成功后会自动设置当前用户对象
     * 
     * @param username 用户名
     * @param password 密码
     * @return 登录响应消息，成功时data字段包含User对象
     */
    public Message login(String username, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        Message request = new Message(Message.MessageType.LOGIN_REQUEST, credentials);
        Message response = sendAndReceive(request);

        if (response.isSuccess()) {
            this.currentUser = (User) response.getData();
        }

        return response;
    }

    /**
     * 用户登出
     * <p>
     * 发送LOGOUT消息通知服务器，清空当前用户
     * 该方法不会关闭连接，只是退出登录状态
     */
    public void logout() {
        if (connected && currentUser != null) {
            Message request = new Message(Message.MessageType.LOGOUT);
            sendAndReceive(request);
            currentUser = null;
        }
    }

    /**
     * 获取系统用户列表
     * <p>
     * 发送USER_LIST请求到服务器
     * 
     * @return 响应消息，成功时data字段包含List<User>
     */
    public Message getUserList() {
        Message request = new Message(Message.MessageType.USER_LIST);
        return sendAndReceive(request);
    }

    /**
     * 获取采购订单列表
     * <p>
     * 发送PURCHASE_ORDER_LIST请求到服务器
     * 服务器会根据当前用户角色过滤订单：
     * - 部门经理：只显示金额<50000的订单
     * - 总经理：只显示金额>=50000的订单
     * 
     * @return 响应消息，成功时data字段包含List<PurchaseOrder>
     */
    public Message getPurchaseOrders() {
        Message request = new Message(Message.MessageType.PURCHASE_ORDER_LIST);
        return sendAndReceive(request);
    }

    /**
     * 审批通过采购订单
     * <p>
     * 发送PURCHASE_ORDER_APPROVE请求到服务器
     * 只有经理角色可以审批订单
     * 
     * @param orderId 订单ID
     * @return 响应消息，成功或失败信息
     */
    public Message approvePurchaseOrder(String orderId) {
        Message request = new Message(Message.MessageType.PURCHASE_ORDER_APPROVE, orderId);
        return sendAndReceive(request);
    }

    /**
     * 退回采购订单
     * <p>
     * 发送PURCHASE_ORDER_REJECT请求到服务器
     * 只有经理角色可以退回订单
     * 
     * @param orderId 订单ID
     * @param reason  退回原因
     * @return 响应消息，成功或失败信息
     */
    public Message rejectPurchaseOrder(String orderId, String reason) {
        Map<String, String> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("reason", reason);

        Message request = new Message(Message.MessageType.PURCHASE_ORDER_REJECT, data);
        return sendAndReceive(request);
    }

    // ==================== Getters and Setters ====================

    /**
     * 检查是否已连接到服务器
     * 
     * @return 连接中返回true，未连接返回false
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * 获取当前登录用户
     * 
     * @return 当前用户对象，未登录时为null
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * 设置当前用户（通常在登录成功后调用）
     * 
     * @param user 用户对象
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
