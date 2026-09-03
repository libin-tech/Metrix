package com.bintech.metrix.service;

import cn.hutool.json.JSONUtil;

import java.time.Duration;

public interface RedisCacheService {

    void set(String key, String value);

    void set(String key, String value, Duration timeout);

    boolean setIfAbsent(String key, String value, Duration timeout);

    String get(String key);

    String getAndDelete(String key);

    void delete(String key);

    boolean hasKey(String key);

    default void setJson(String key, Object value, Duration timeout) {
        set(key, JSONUtil.toJsonStr(value), timeout);
    }

    default void setJson(String key, Object value) {
        set(key, JSONUtil.toJsonStr(value));
    }

    default <T> T getJson(String key, Class<T> type) {
        String value = get(key);
        return value == null ? null : JSONUtil.toBean(value, type);
    }

    default <T> T getAndDeleteJson(String key, Class<T> type) {
        String value = getAndDelete(key);
        return value == null ? null : JSONUtil.toBean(value, type);
    }
}
