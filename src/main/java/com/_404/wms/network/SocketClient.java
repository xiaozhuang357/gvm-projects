package com._404.wms.network;

import com._404.wms.model.User;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Socket客户端工具类 - 封装与服务器的通信
 */
public class SocketClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8888;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User currentUser;
    private volatile boolean connected;

    public SocketClient() {
        this.connected = false;
    }

    /**
     * 连接到服务器
     */
    public boolean connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            socket.setSoTimeout(10000); // 设置10秒超时，避免无限阻塞
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // 立即刷新，发送流头部信息
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            System.out.println("已连接到服务器: " + SERVER_HOST + ":" + SERVER_PORT);
            return true;
        } catch (IOException e) {
            System.err.println("连接服务器失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 断开连接（会发送LOGOUT消息）
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
     * 用于已经发送过LOGOUT消息后的清理工作
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
     * 登录
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
     * 登出
     */
    public void logout() {
        if (connected && currentUser != null) {
            Message request = new Message(Message.MessageType.LOGOUT);
            sendAndReceive(request);
            currentUser = null;
        }
    }

    /**
     * 获取用户列表
     */
    public Message getUserList() {
        Message request = new Message(Message.MessageType.USER_LIST);
        return sendAndReceive(request);
    }

    /**
     * 获取采购订单列表
     */
    public Message getPurchaseOrders() {
        Message request = new Message(Message.MessageType.PURCHASE_ORDER_LIST);
        return sendAndReceive(request);
    }

    /**
     * 审批采购订单
     */
    public Message approvePurchaseOrder(String orderId) {
        Message request = new Message(Message.MessageType.PURCHASE_ORDER_APPROVE, orderId);
        return sendAndReceive(request);
    }

    /**
     * 拒绝采购订单
     */
    public Message rejectPurchaseOrder(String orderId, String reason) {
        Map<String, String> data = new HashMap<>();
        data.put("orderId", orderId);
        data.put("reason", reason);

        Message request = new Message(Message.MessageType.PURCHASE_ORDER_REJECT, data);
        return sendAndReceive(request);
    }

    // Getters
    public boolean isConnected() {
        return connected;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
