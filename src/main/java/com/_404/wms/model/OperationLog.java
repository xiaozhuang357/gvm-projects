package com._404.wms.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
public class OperationLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private String logId; // 日志ID
    private String userId; // 操作用户ID
    private String username; // 操作用户名
    private String operation; // 操作类型
    private String module; // 操作模块
    private String details; // 操作详情
    private String ipAddress; // IP地址
    private LocalDateTime operationTime; // 操作时间
    private boolean success; // 操作是否成功
    private String errorMessage; // 错误信息

    public enum OperationType {
        LOGIN("登录"),
        LOGOUT("登出"),
        CREATE("创建"),
        UPDATE("更新"),
        DELETE("删除"),
        QUERY("查询"),
        APPROVE("审批"),
        REJECT("退回"),
        EXPORT("导出"),
        IMPORT("导入"),
        BACKUP("备份"),
        RESTORE("恢复");

        private String displayName;

        OperationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public OperationLog() {
        this.operationTime = LocalDateTime.now();
        this.success = true;
    }

    public OperationLog(String userId, String username, String operation,
            String module, String details) {
        this();
        this.userId = userId;
        this.username = username;
        this.operation = operation;
        this.module = module;
        this.details = details;
    }

    /**
     * 构造函数重载 - 支持OperationType枚举
     */
    public OperationLog(String userId, String username, OperationType operationType,
            String details, boolean success, String errorMessage) {
        this();
        this.userId = userId;
        this.username = username;
        this.operation = operationType.getDisplayName();
        this.details = details;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

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

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "OperationLog{" +
                "username='" + username + '\'' +
                ", operation='" + operation + '\'' +
                ", module='" + module + '\'' +
                ", operationTime=" + operationTime +
                ", success=" + success +
                '}';
    }
}
