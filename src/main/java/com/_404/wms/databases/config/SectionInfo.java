package com._404.wms.databases.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置节信息类
 * 对应INI文件中的[Section]
 */
public class SectionInfo {
    // 空SectionInfo实例
    public static final SectionInfo EMPTY = new SectionInfo(Collections.emptyMap());

    // 存储该section下的所有键值对
    private final Map<String, String> sectionData;

    /**
     * 构造函数
     */
    public SectionInfo(Map<String, String> sectionData) {
        if (sectionData == null) {
            this.sectionData = new HashMap<>();
        } else {
            this.sectionData = new HashMap<>(sectionData);
        }
    }

    /**
     * 获取指定key的值
     */
    public String getValue(String key) {
        return sectionData.getOrDefault(key, "");
    }

    /**
     * 获取指定key的值，带默认值
     */
    public String getValue(String key, String defaultValue) {
        String value = sectionData.get(key);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取整数值
     */
    public int getIntValue(String key, int defaultValue) {
        String value = getValue(key);
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
    public long getLongValue(String key, long defaultValue) {
        String value = getValue(key);
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
    public boolean getBooleanValue(String key, boolean defaultValue) {
        String value = getValue(key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 获取所有键值对（只读）
     */
    public Map<String, String> getSectionData() {
        return Collections.unmodifiableMap(sectionData);
    }

    /**
     * 检查是否包含指定key
     */
    public boolean containsKey(String key) {
        return sectionData.containsKey(key);
    }

    /**
     * 获取该section下的所有key
     */
    public java.util.Set<String> keySet() {
        return sectionData.keySet();
    }

    /**
     * 获取该section下的所有值
     */
    public java.util.Collection<String> values() {
        return sectionData.values();
    }

    /**
     * 获取键值对数量
     */
    public int size() {
        return sectionData.size();
    }

    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return sectionData.isEmpty();
    }

    @Override
    public String toString() {
        return "SectionInfo{" +
                "sectionData=" + sectionData +
                '}';
    }
}