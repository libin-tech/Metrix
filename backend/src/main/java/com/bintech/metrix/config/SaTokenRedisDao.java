package com.bintech.metrix.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.auto.SaTokenDaoByObjectFollowString;
import cn.dev33.satoken.util.SaFoxUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Sa-Token Redis 持久层，实现服务重启后的 token 登录态续存。
 */
public class SaTokenRedisDao implements SaTokenDaoByObjectFollowString {

    private final StringRedisTemplate stringRedisTemplate;

    public SaTokenRedisDao(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void set(String key, String value, long timeout) {
        writeValue(key, value, timeout);
    }

    @Override
    public void update(String key, String value) {
        writeValue(key, value, getTimeout(key));
    }

    @Override
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    public long getTimeout(String key) {
        Long timeout = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
        return timeout == null ? SaTokenDao.NOT_VALUE_EXPIRE : timeout;
    }

    @Override
    public void updateTimeout(String key, long timeout) {
        if (timeout > 0) {
            stringRedisTemplate.expire(key, timeout, TimeUnit.SECONDS);
            return;
        }
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            stringRedisTemplate.persist(key);
            return;
        }
        delete(key);
    }

    @Override
    public List<String> searchData(String prefix, String keyword, int start, int size, boolean sortType) {
        Set<String> keys = stringRedisTemplate.keys(prefix + "*" + keyword + "*");
        List<String> values = keys == null ? List.of() : new ArrayList<>(keys);
        return SaFoxUtil.searchList(values, prefix, keyword, start, size, sortType);
    }

    private void writeValue(String key, String value, long timeout) {
        if (timeout > 0) {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(timeout));
            return;
        }
        if (timeout == SaTokenDao.NEVER_EXPIRE) {
            stringRedisTemplate.opsForValue().set(key, value);
            return;
        }
        delete(key);
    }
}
