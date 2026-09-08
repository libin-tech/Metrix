package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.CacheConstants;
import com.bintech.metrix.service.RedisCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketActivityServiceImplTest {
    @Test
    void shouldKeepLastSuccessAndThrottleAfterProcessFailure() {
        RedisCacheService cache = mock(RedisCacheService.class);
        when(cache.get(CacheConstants.MARKET_DASHBOARD_ACTIVITY_LAST_SUCCESS_KEY))
                .thenReturn("{\"up\":100,\"down\":200,\"limitUp\":30,\"limitDown\":2,\"statDate\":\"20260904\"}");
        MarketActivityServiceImpl service = new MarketActivityServiceImpl(cache);
        ReflectionTestUtils.setField(service, "pythonExecutable", "/nonexistent/metrix-test-python");
        ReflectionTestUtils.setField(service, "akshareScriptPath", "python-service/akshare.py");
        var data = JSONUtil.parseObj(service.getMarketActivity()).getJSONObject("data");
        assertEquals(100, data.getInt("up"));
        assertEquals("20260904", data.getStr("statDate"));
        assertTrue(data.getBool("stale"));
        long attempt = (long) ReflectionTestUtils.getField(service, "lastAttemptNanos");
        service.getMarketActivity();
        assertEquals(attempt, ReflectionTestUtils.getField(service, "lastAttemptNanos"));
        verify(cache, never()).setJson(anyString(), any());
    }

    @Test
    void shouldNotInventZeroCountsWithoutCache() {
        MarketActivityServiceImpl service = new MarketActivityServiceImpl(mock(RedisCacheService.class));
        ReflectionTestUtils.setField(service, "pythonExecutable", "/nonexistent/metrix-test-python");
        ReflectionTestUtils.setField(service, "akshareScriptPath", "python-service/akshare.py");
        assertThrows(RuntimeException.class, service::getMarketActivity);
    }
}
