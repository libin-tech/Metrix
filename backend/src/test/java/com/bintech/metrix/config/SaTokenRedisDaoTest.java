package com.bintech.metrix.config;

import org.junit.jupiter.api.Test;
import cn.dev33.satoken.dao.SaTokenDao;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaTokenRedisDaoTest {

    private static final String TOKEN_KEY = "satoken:login:token-1";
    private static final String USER_ID = "42";

    @Test
    void keepsTokenMappingWhenDaoIsRecreated() {
        InMemoryStringRedisTemplate redisTemplate = new InMemoryStringRedisTemplate();
        SaTokenRedisDao loginDao = new SaTokenRedisDao(redisTemplate);
        loginDao.set(TOKEN_KEY, USER_ID, 3600);

        SaTokenRedisDao restartedDao = new SaTokenRedisDao(redisTemplate);

        assertEquals(USER_ID, restartedDao.get(TOKEN_KEY));
        assertTrue(restartedDao.getTimeout(TOKEN_KEY) > 0);
    }

    @Test
    void updatePreservesTokenMappingExpiry() {
        InMemoryStringRedisTemplate redisTemplate = new InMemoryStringRedisTemplate();
        SaTokenRedisDao dao = new SaTokenRedisDao(redisTemplate);
        dao.set(TOKEN_KEY, USER_ID, 3600);

        dao.update(TOKEN_KEY, "43");

        assertEquals("43", dao.get(TOKEN_KEY));
        assertTrue(dao.getTimeout(TOKEN_KEY) > 0);
    }

    @Test
    void configUsesRedisBackedTokenDao() {
        SaTokenDao dao = new SaTokenConfig().saTokenDao(new InMemoryStringRedisTemplate());

        assertInstanceOf(SaTokenRedisDao.class, dao);
    }

    private static final class InMemoryStringRedisTemplate extends StringRedisTemplate {

        private final Map<String, String> values = new ConcurrentHashMap<>();
        private final Map<String, Long> expiresAt = new ConcurrentHashMap<>();
        private final ValueOperations<String, String> valueOperations = createValueOperations();

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOperations;
        }

        @Override
        public Boolean hasKey(String key) {
            removeExpired(key);
            return values.containsKey(key);
        }

        @Override
        public Boolean delete(String key) {
            expiresAt.remove(key);
            return values.remove(key) != null;
        }

        @Override
        public Boolean expire(String key, long timeout, TimeUnit unit) {
            if (!Boolean.TRUE.equals(hasKey(key))) {
                return false;
            }
            expiresAt.put(key, System.currentTimeMillis() + unit.toMillis(timeout));
            return true;
        }

        @Override
        public Boolean persist(String key) {
            if (!Boolean.TRUE.equals(hasKey(key))) {
                return false;
            }
            expiresAt.remove(key);
            return true;
        }

        @Override
        public Long getExpire(String key, TimeUnit unit) {
            removeExpired(key);
            if (!values.containsKey(key)) {
                return -2L;
            }
            Long expireAt = expiresAt.get(key);
            if (expireAt == null) {
                return -1L;
            }
            return unit.convert(Math.max(0L, expireAt - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
        }

        private ValueOperations<String, String> createValueOperations() {
            return (ValueOperations<String, String>) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class[]{ValueOperations.class}, (proxy, method, arguments) -> switch (method.getName()) {
                        case "set" -> {
                            String key = (String) arguments[0];
                            values.put(key, (String) arguments[1]);
                            if (arguments.length == 3 && arguments[2] instanceof Duration timeout) {
                                expiresAt.put(key, System.currentTimeMillis() + timeout.toMillis());
                            } else {
                                expiresAt.remove(key);
                            }
                            yield null;
                        }
                        case "get" -> {
                            String key = String.valueOf(arguments[0]);
                            removeExpired(key);
                            yield values.get(key);
                        }
                        case "getOperations" -> this;
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }

        private void removeExpired(String key) {
            Long expireAt = expiresAt.get(key);
            if (expireAt != null && expireAt <= System.currentTimeMillis()) {
                values.remove(key);
                expiresAt.remove(key);
            }
        }
    }
}
