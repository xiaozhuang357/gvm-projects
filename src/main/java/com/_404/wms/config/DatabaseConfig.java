package com._404.wms.config;

/**
 * 数据库配置类
 * 存储数据库连接所需的所有配置信息
 */
public class DatabaseConfig {
    private String url;
    private String username;
    private String password;
    private String schema;
    private int poolSize;
    private int maxWaitTimeMs;
    private int connectionTimeoutMs;
    private int idleTimeoutMs;
    private int maxLifetimeMs;
    private int validationTimeoutSec;
    private boolean autoCommit;

    private DatabaseConfig(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
        this.schema = builder.schema;
        this.poolSize = builder.poolSize;
        this.maxWaitTimeMs = builder.maxWaitTimeMs;
        this.connectionTimeoutMs = builder.connectionTimeoutMs;
        this.idleTimeoutMs = builder.idleTimeoutMs;
        this.maxLifetimeMs = builder.maxLifetimeMs;
        this.validationTimeoutSec = builder.validationTimeoutSec;
        this.autoCommit = builder.autoCommit;
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSchema() {
        return schema;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getMaxWaitTimeMs() {
        return maxWaitTimeMs;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public int getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public int getMaxLifetimeMs() {
        return maxLifetimeMs;
    }

    public int getValidationTimeoutSec() {
        return validationTimeoutSec;
    }

    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * 获取完整的JDBC URL（包含schema）
     */
    public String getFullUrl() {
        if (url.endsWith("/")) {
            return url + schema;
        }
        return url + "/" + schema;
    }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private String url = "jdbc:mysql://127.0.0.1:3306/";
        private String username = "root";
        private String password = "";
        private String schema = "wms_db";
        private int poolSize = 10;
        private int maxWaitTimeMs = 30000;
        private int connectionTimeoutMs = 30000;
        private int idleTimeoutMs = 600000;
        private int maxLifetimeMs = 1800000;
        private int validationTimeoutSec = 5;
        private boolean autoCommit = true;

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder schema(String schema) {
            this.schema = schema;
            return this;
        }

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder maxWaitTimeMs(int maxWaitTimeMs) {
            this.maxWaitTimeMs = maxWaitTimeMs;
            return this;
        }

        public Builder connectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
            return this;
        }

        public Builder idleTimeoutMs(int idleTimeoutMs) {
            this.idleTimeoutMs = idleTimeoutMs;
            return this;
        }

        public Builder maxLifetimeMs(int maxLifetimeMs) {
            this.maxLifetimeMs = maxLifetimeMs;
            return this;
        }

        public Builder validationTimeoutSec(int validationTimeoutSec) {
            this.validationTimeoutSec = validationTimeoutSec;
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" +
                "url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", schema='" + schema + '\'' +
                ", poolSize=" + poolSize +
                ", maxWaitTimeMs=" + maxWaitTimeMs +
                '}';
    }
}
