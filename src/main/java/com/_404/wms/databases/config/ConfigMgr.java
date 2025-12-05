package com._404.wms.databases.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理器 - 单例模式
 * 支持INI格式配置文件解析
 */
public class ConfigMgr {
    // 单例实例
    private static volatile ConfigMgr instance;

    // 存储所有配置，key为section名称，value为SectionInfo对象
    private final Map<String, SectionInfo> configMap = new HashMap<>();

    // 配置文件路径
    private String configFilePath = "config.ini";

    // 私有构造函数
    private ConfigMgr() {
        loadConfig();
    }

    // 获取单例实例
    public static ConfigMgr getInstance() {
        if (instance == null) {
            synchronized (ConfigMgr.class) {
                if (instance == null) {
                    instance = new ConfigMgr();
                }
            }
        }
        return instance;
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        System.out.println("config path: " + configFilePath);

        try {
            Path path = Paths.get(configFilePath);

            // 先尝试从当前目录读取
            if (Files.exists(path)) {
                loadFromFile(path);
            } else {
                // 尝试从classpath读取
                loadFromClasspath();
            }

            printAllConfigs();

        } catch (IOException e) {
            System.err.println("Failed to load config file: " + e.getMessage());
            // 使用默认配置或抛出异常
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    /**
     * 从文件系统加载配置文件
     */
    private void loadFromFile(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            parseConfig(reader);
        }
    }

    /**
     * 从classpath加载配置文件
     */
    private void loadFromClasspath() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFilePath);
        if (inputStream == null) {
            throw new IOException("Config file not found in classpath: " + configFilePath);
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
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();

            // 跳过空行和注释
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }

            // 处理section [sectionName]
            if (line.startsWith("[") && line.endsWith("]")) {
                // 保存上一个section的数据
                if (!currentSection.isEmpty()) {
                    configMap.put(currentSection, new SectionInfo(currentSectionData));
                }

                // 开始新的section
                currentSection = line.substring(1, line.length() - 1).trim();
                currentSectionData = new HashMap<>();
                continue;
            }

            // 处理键值对 key = value
            int equalsIndex = line.indexOf('=');
            if (equalsIndex > 0) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();

                // 处理值中的注释
                int commentIndex = value.indexOf(';');
                if (commentIndex != -1) {
                    value = value.substring(0, commentIndex).trim();
                }

                commentIndex = value.indexOf('#');
                if (commentIndex != -1) {
                    value = value.substring(0, commentIndex).trim();
                }

                currentSectionData.put(key, value);
            } else {
                System.err.println("Warning: Invalid line format at line " + lineNumber + ": " + line);
            }
        }

        // 保存最后一个section的数据
        if (!currentSection.isEmpty()) {
            configMap.put(currentSection, new SectionInfo(currentSectionData));
        }
    }

    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        configMap.clear();
        loadConfig();
        System.out.println("Config reloaded successfully");
    }

    /**
     * 获取指定section的配置信息
     */
    public SectionInfo getSection(String section) {
        return configMap.getOrDefault(section, SectionInfo.EMPTY);
    }

    /**
     * 获取指定section和key的值
     */
    public String getValue(String section, String key) {
        SectionInfo sectionInfo = configMap.get(section);
        if (sectionInfo == null) {
            return "";
        }
        return sectionInfo.getValue(key);
    }

    /**
     * 获取指定section和key的值，带默认值
     */
    public String getValue(String section, String key, String defaultValue) {
        SectionInfo sectionInfo = configMap.get(section);
        if (sectionInfo == null) {
            return defaultValue;
        }
        String value = sectionInfo.getValue(key);
        return value.isEmpty() ? defaultValue : value;
    }

    /**
     * 获取整数值
     */
    public int getIntValue(String section, String key, int defaultValue) {
        String value = getValue(section, key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取长整数值
     */
    public long getLongValue(String section, String key, long defaultValue) {
        String value = getValue(section, key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取布尔值
     */
    public boolean getBooleanValue(String section, String key, boolean defaultValue) {
        String value = getValue(section, key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 打印所有配置信息（用于调试）
     */
    private void printAllConfigs() {
        System.out.println("=== Configuration Loaded ===");
        for (Map.Entry<String, SectionInfo> entry : configMap.entrySet()) {
            String sectionName = entry.getKey();
            SectionInfo sectionInfo = entry.getValue();

            System.out.println("[" + sectionName + "]");
            for (Map.Entry<String, String> kv : sectionInfo.getSectionData().entrySet()) {
                System.out.println(kv.getKey() + " = " + kv.getValue());
            }
            System.out.println();
        }
        System.out.println("===========================");
    }

    /**
     * 获取所有配置信息（只读）
     */
    public Map<String, SectionInfo> getAllConfigs() {
        return new HashMap<>(configMap);
    }

    /**
     * 检查指定section是否存在
     */
    public boolean hasSection(String section) {
        return configMap.containsKey(section);
    }

    /**
     * 设置配置文件路径（会触发重新加载）
     */
    public void setConfigFilePath(String configFilePath) {
        this.configFilePath = configFilePath;
        reloadConfig();
    }

    /**
     * 获取配置文件路径
     */
    public String getConfigFilePath() {
        return configFilePath;
    }

    /**
     * 清除所有配置
     */
    public void clear() {
        configMap.clear();
    }
}