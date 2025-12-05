package com._404.wms.network;

import java.io.Serializable;

/**
 * 网络消息封装类
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type; // 消息类型
    private String sender; // 发送者ID
    private Object data; // 消息数据
    private boolean success; // 操作是否成功
    private String message; // 提示信息
    private long timestamp; // 时间戳

    public enum MessageType {
        // 认证相关
        LOGIN_REQUEST,
        LOGIN_RESPONSE,
        LOGOUT,

        // 用户管理
        USER_ADD,
        USER_UPDATE,
        USER_DELETE,
        USER_QUERY,
        USER_LIST,

        // 商品管理
        PRODUCT_ADD,
        PRODUCT_UPDATE,
        PRODUCT_DELETE,
        PRODUCT_QUERY,
        PRODUCT_LIST,
        PRODUCT_STOCK_ALERT,

        // 采购管理
        PURCHASE_ORDER_ADD,
        PURCHASE_ORDER_CREATE,
        PURCHASE_ORDER_UPDATE,
        PURCHASE_ORDER_APPROVE,
        PURCHASE_ORDER_REJECT,
        PURCHASE_ORDER_DELETE,
        PURCHASE_ORDER_QUERY,
        PURCHASE_ORDER_LIST,

        // 出入库管理
        STOCK_IN,
        STOCK_IN_ADD,
        STOCK_OUT,
        STOCK_OUT_ADD,
        STOCK_ADJUSTMENT, // 库存调整/盘点
        STOCK_QUERY,
        STOCK_IN_LIST,
        STOCK_OUT_LIST,
        STOCK_RECORD_LIST,

        // 报表
        REPORT_INVENTORY,
        REPORT_PURCHASE,
        REPORT_EXPORT,

        // 采购到货确认
        PURCHASE_ORDER_ARRIVAL_CONFIRM,

        // 日志
        LOG_QUERY,
        LOG_LIST,

        // 系统
        BACKUP,
        RESTORE,
        HEARTBEAT,
        ERROR
    }

    public Message() {
        this.timestamp = System.currentTimeMillis();
        this.success = true;
    }

    public Message(MessageType type) {
        this();
        this.type = type;
    }

    public Message(MessageType type, Object data) {
        this(type);
        this.data = data;
    }

    public static Message success(MessageType type, Object data, String message) {
        Message msg = new Message(type, data);
        msg.setSuccess(true);
        msg.setMessage(message);
        return msg;
    }

    public static Message error(MessageType type, String message) {
        Message msg = new Message(type);
        msg.setSuccess(false);
        msg.setMessage(message);
        return msg;
    }

    // Getters and Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

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
