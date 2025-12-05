package com._404.wms.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 配置管理器 - 单例模式
 * 支持INI格式配置文件解析
 */
public class ConfigManager {
    private static volatile ConfigManager instance;

    private final Map<String, Map<String, String>> configMap = new HashMap<>();
    private final String configFilePath;
    private DatabaseConfig databaseConfig;

    private ConfigManager() {
        this("config.ini");
    }

    private ConfigManager(String configFilePath) {
        this.configFilePath = configFilePath;
        loadConfig();
        initDatabaseConfig();
    }

    /**
     * 获取单例实例
     */
    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        System.out.println("Loading config from: " + configFilePath);

        try {
            Path path = Paths.get(configFilePath);
            if (Files.exists(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    parseConfig(reader);
                }
            } else {
                loadFromClasspath();
            }
            System.out.println("Config loaded successfully");
        } catch (IOException e) {
            System.err.println("Failed to load config file: " + e.getMessage());
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     * 从classpath加载配置文件
     */
    private void loadFromClasspath() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
        if (inputStream == null) {
            throw new IOException("Config file not found: " + configFilePath);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            parseConfig(reader);
        }
    }

    /**
     * 解析INI格式配置文件
     */
    private void parseConfig(BufferedReader reader) throws IOException {
        String currentSection = "";
        Map<String, String> currentSectionData = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }

            // 处理section [sectionName]
            if (line.startsWith("[") && line.endsWith("]")) {
                if (!currentSection.isEmpty()) {
                    configMap.put(currentSection, new HashMap<>(currentSectionData));
                }
                currentSection = line.substring(1, line.length() - 1).trim();
                currentSectionData = new HashMap<>();
                continue;
            }

            // 处理键值对 key = value
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                // 移除行尾注释
                int commentIndex = value.indexOf(';');
                if (commentIndex != -1) {
                    value = value.substring(0, commentIndex).trim();
                }
                commentIndex = value.indexOf('#');
                if (commentIndex != -1) {
                    value = value.substring(0, commentIndex).trim();
                }

                currentSectionData.put(key, value);
            }
        }

        // 保存最后一个section
        if (!currentSection.isEmpty()) {
            configMap.put(currentSection, new HashMap<>(currentSectionData));
        }
    }

    /**
     * 初始化数据库配置
     */
    private void initDatabaseConfig() {
        Map<String, String> mysqlConfig = configMap.getOrDefault("Mysql", new HashMap<>());

        databaseConfig = new DatabaseConfig.Builder()
                .url(mysqlConfig.getOrDefault("Url", "jdbc:mysql://127.0.0.1:3306/"))
                .username(mysqlConfig.getOrDefault("User", "root"))
                .password(mysqlConfig.getOrDefault("Passwd", ""))
                .schema(mysqlConfig.getOrDefault("Schema", "wms_db"))
                .poolSize(parseIntOrDefault(mysqlConfig.get("PoolSize"), 10))
                .maxWaitTimeMs(parseIntOrDefault(mysqlConfig.get("MaxWaitTime"), 30000))
                .connectionTimeoutMs(parseIntOrDefault(mysqlConfig.get("ConnectionTimeout"), 30000))
                .idleTimeoutMs(parseIntOrDefault(mysqlConfig.get("IdleTimeout"), 600000))
                .build();

        System.out.println("Database config initialized: " + databaseConfig);
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取数据库配置
     */
    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }

    /**
     * 获取指定section的值
     */
    public Optional<String> getValue(String section, String key) {
        Map<String, String> sectionMap = configMap.get(section);
        if (sectionMap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sectionMap.get(key));
    }

    /**
     * 获取值，带默认值
     */
    public String getValue(String section, String key, String defaultValue) {
        return getValue(section, key).orElse(defaultValue);
    }

    /**
     * 获取整数值
     */
    public int getIntValue(String section, String key, int defaultValue) {
        return getValue(section, key)
                .map(v -> {
                    try {
                        return Integer.parseInt(v);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        configMap.clear();
        loadConfig();
        initDatabaseConfig();
        System.out.println("Config reloaded successfully");
    }
}
