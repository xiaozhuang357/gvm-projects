package com._404.wms.network;

import java.io.Serializable;

/**
 * 网络消息封装类 - 客户端与服务端通信的数据载体
 * <p>
 * 功能说明：
 * 1. 统一封装所有客户端-服务端之间的通信消息
 * 2. 实现Serializable接口，支持通过Socket进行对象序列化传输
 * 3. 包含消息类型、发送者、数据负载、操作结果、提示信息等字段
 * 4. 提供便捷的静态工厂方法创建成功/失败消息
 * <p>
 * 消息类型分类：
 * - 认证相关：LOGIN_REQUEST, LOGIN_RESPONSE, LOGOUT
 * - 用户管理：USER_ADD, USER_UPDATE, USER_DELETE, USER_LIST等
 * - 商品管理：PRODUCT_ADD, PRODUCT_UPDATE, PRODUCT_DELETE等
 * - 采购管理：PURCHASE_ORDER_*系列
 * - 库存管理：STOCK_IN, STOCK_OUT, STOCK_ADJUSTMENT等
 * - 系统功能：HEARTBEAT, ERROR, BACKUP等
 * <p>
 * 使用示例：
 * 
 * <pre>
 * // 客户端发送登录请求
 * Map<String, String> credentials = new HashMap<>();
 * credentials.put("username", "admin");
 * credentials.put("password", "123456");
 * Message request = new Message(MessageType.LOGIN_REQUEST, credentials);
 * Message response = socketClient.sendAndReceive(request);
 * 
 * // 服务端返回成功消息
 * User user = dataService.authenticate(username, password);
 * return Message.success(MessageType.LOGIN_RESPONSE, user, "登录成功");
 * 
 * // 服务端返回失败消息
 * return Message.error(MessageType.LOGIN_RESPONSE, "用户名或密码错误");
 * </pre>
 * <p>
 * 通信协议：
 * 1. 客户端发送请求消息（Request）
 * 2. 服务端处理并返回响应消息（Response）
 * 3. 通过success字段判断操作是否成功
 * 4. 通过message字段获取提示信息
 * 5. 通过data字段传输业务数据（如User对象、订单列表等）
 *
 * @author WMS开发团队
 * @version 1.0
 * @since 2025-12-06
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 消息类型，决定服务端如何处理该消息 */
    private MessageType type;

    /** 发送者ID（用户ID），用于日志记录和权限验证 */
    private String sender;

    /** 消息数据负载，可以是任意可序列化对象（User、Product、List等） */
    private Object data;

    /** 操作是否成功标志，true表示成功，false表示失败 */
    private boolean success;

    /** 提示信息，用于向用户显示操作结果（成功提示或错误信息） */
    private String message;

    /** 消息时间戳（毫秒），用于日志记录和调试 */
    private long timestamp;

    /**
     * 消息类型枚举 - 定义所有支持的业务操作类型
     * <p>
     * 命名规范：
     * - REQUEST/RESPONSE后缀：表示请求-响应对
     * - ADD/UPDATE/DELETE：标准CRUD操作
     * - LIST：批量查询操作
     * - APPROVE/REJECT：审批相关操作
     */
    public enum MessageType {
        // ==================== 认证相关 ====================
        /** 客户端发起登录请求 */
        LOGIN_REQUEST,
        /** 服务端返回登录结果 */
        LOGIN_RESPONSE,
        /** 客户端登出请求 */
        LOGOUT,

        // ==================== 用户管理 ====================
        /** 添加新用户 */
        USER_ADD,
        /** 更新用户信息 */
        USER_UPDATE,
        /** 删除用户 */
        USER_DELETE,
        /** 查询单个用户 */
        USER_QUERY,
        /** 获取用户列表 */
        USER_LIST,

        // ==================== 商品管理 ====================
        /** 添加新商品 */
        PRODUCT_ADD,
        /** 更新商品信息 */
        PRODUCT_UPDATE,
        /** 删除商品 */
        PRODUCT_DELETE,
        /** 查询单个商品 */
        PRODUCT_QUERY,
        /** 获取商品列表 */
        PRODUCT_LIST,
        /** 库存预警查询 */
        PRODUCT_STOCK_ALERT,

        // ==================== 采购管理 ====================
        /** 添加采购订单 */
        PURCHASE_ORDER_ADD,
        /** 创建采购订单（别名） */
        PURCHASE_ORDER_CREATE,
        /** 更新采购订单 */
        PURCHASE_ORDER_UPDATE,
        /** 审批通过采购订单 */
        PURCHASE_ORDER_APPROVE,
        /** 退回采购订单 */
        PURCHASE_ORDER_REJECT,
        /** 删除采购订单 */
        PURCHASE_ORDER_DELETE,
        /** 查询单个采购订单 */
        PURCHASE_ORDER_QUERY,
        /** 获取采购订单列表 */
        PURCHASE_ORDER_LIST,

        // ==================== 出入库管理 ====================
        /** 入库操作 */
        STOCK_IN,
        /** 添加入库记录（别名） */
        STOCK_IN_ADD,
        /** 出库操作 */
        STOCK_OUT,
        /** 添加出库记录（别名） */
        STOCK_OUT_ADD,
        /** 库存调整/盘点 */
        STOCK_ADJUSTMENT,
        /** 查询库存 */
        STOCK_QUERY,
        /** 获取入库记录列表 */
        STOCK_IN_LIST,
        /** 获取出库记录列表 */
        STOCK_OUT_LIST,
        /** 获取出入库记录列表 */
        STOCK_RECORD_LIST,

        // ==================== 报表 ====================
        /** 库存报表 */
        REPORT_INVENTORY,
        /** 采购报表 */
        REPORT_PURCHASE,
        /** 导出报表 */
        REPORT_EXPORT,

        // ==================== 采购到货确认 ====================
        /** 确认采购订单到货 */
        PURCHASE_ORDER_ARRIVAL_CONFIRM,

        // ==================== 日志 ====================
        /** 查询操作日志 */
        LOG_QUERY,
        /** 获取日志列表 */
        LOG_LIST,

        // ==================== 系统 ====================
        /** 数据备份 */
        BACKUP,
        /** 数据恢复 */
        RESTORE,
        /** 心跳检测，保持连接活跃 */
        HEARTBEAT,
        /** 错误消息 */
        ERROR
    }

    /**
     * 默认构造函数
     * <p>
     * 初始化时间戳为当前系统时间，成功标志默认为true
     */
    public Message() {
        this.timestamp = System.currentTimeMillis();
        this.success = true;
    }

    /**
     * 带消息类型的构造函数
     * 
     * @param type 消息类型枚举（如LOGIN_REQUEST、PRODUCT_ADD等）
     */
    public Message(MessageType type) {
        this();
        this.type = type;
    }

    /**
     * 带消息类型和数据的构造函数
     * <p>
     * 使用场景：客户端发送请求消息
     * 
     * @param type 消息类型枚举
     * @param data 消息数据负载（可以是Map、实体对象、List等）
     */
    public Message(MessageType type, Object data) {
        this(type);
        this.data = data;
    }

    /**
     * 创建成功响应消息的静态工厂方法
     * <p>
     * 使用场景：服务端返回操作成功的响应
     * 
     * @param type    消息类型枚举（通常与请求类型对应）
     * @param data    返回的业务数据（如用户对象、订单列表等）
     * @param message 成功提示信息（如"登录成功"、"添加成功"）
     * @return 设置了success=true的消息对象
     */
    public static Message success(MessageType type, Object data, String message) {
        Message msg = new Message(type, data);
        msg.setSuccess(true);
        msg.setMessage(message);
        return msg;
    }

    /**
     * 创建错误响应消息的静态工厂方法
     * <p>
     * 使用场景：服务端返回操作失败的响应
     * 
     * @param type    消息类型枚举
     * @param message 错误信息（如"用户名或密码错误"、"库存不足"）
     * @return 设置了success=false的消息对象
     */
    public static Message error(MessageType type, String message) {
        Message msg = new Message(type);
        msg.setSuccess(false);
        msg.setMessage(message);
        return msg;
    }

    // ==================== Getters and Setters ====================

    /**
     * 获取消息类型
     * 
     * @return 消息类型枚举（LOGIN_REQUEST、PRODUCT_ADD等）
     */
    public MessageType getType() {
        return type;
    }

    /**
     * 设置消息类型
     * 
     * @param type 消息类型枚举
     */
    public void setType(MessageType type) {
        this.type = type;
    }

    /**
     * 获取发送者ID
     * <p>
     * 通常为用户ID，用于日志记录和权限验证
     * 
     * @return 发送者ID（可能为null）
     */
    public String getSender() {
        return sender;
    }

    /**
     * 设置发送者ID
     * 
     * @param sender 发送者ID（用户ID）
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * 获取消息数据负载
     * <p>
     * 返回的对象类型取决于消息类型：
     * - LOGIN_REQUEST: Map<String, String>（用户名密码）
     * - USER_ADD: User对象
     * - PRODUCT_LIST: List<Product>
     * 
     * @return 消息数据对象（可能为null）
     */
    public Object getData() {
        return data;
    }

    /**
     * 设置消息数据负载
     * 
     * @param data 消息数据对象（必须可序列化）
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     * 获取操作成功标志
     * 
     * @return true表示成功，false表示失败
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 设置操作成功标志
     * 
     * @param success 成功标志（true/false）
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * 获取提示信息
     * <p>
     * 成功消息示例："登录成功"、"添加成功"
     * 失败消息示例："用户名或密码错误"、"库存不足"
     * 
     * @return 提示信息字符串（可能为null）
     */
    public String getMessage() {
        return message;
    }

    /**
     * 设置提示信息
     * 
     * @param message 提示信息字符串
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * 获取消息时间戳
     * 
     * @return 毫秒级时间戳（从1970-01-01 00:00:00 UTC开始）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 设置消息时间戳
     * 
     * @param timestamp 毫秒级时间戳
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 将消息对象转换为字符串（用于日志记录和调试）
     * 
     * @return 格式化的消息字符串，包含类型、成功标志、提示信息和时间戳
     */
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
