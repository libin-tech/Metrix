package com.bintech.metrix.service.impl;

import com.bintech.metrix.repository.dao.MarketDataConfigDao;
import com.bintech.metrix.config.FinancialDataProperties;
import com.bintech.metrix.service.RedisCacheService;
import com.bintech.metrix.constants.CacheConstants;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.entity.MarketDataConfig;
import com.bintech.metrix.constants.FinancialDataConstants;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MarketDataServiceImplTest {

    @Test
    void shouldFetchAkShareChipWithoutFinancialConfig() throws Exception {
        verifyChipProcess("{\"status\":\"success\",\"data\":{\"profit_ratio\":25.0}}", false, false);
    }

    @Test
    void shouldPropagateAkShareFailure() throws Exception {
        verifyChipProcess("{\"status\":\"error\",\"message\":\"筹码数据不可用\"}", true, false);
    }

    @Test
    void shouldReturnCachedDateOnDisconnect() throws Exception {
        verifyChipProcess("{\"status\":\"error\",\"message\":\"RemoteDisconnected\"}", true, true);
    }

    @Test
    void shouldNotUseLegacyActiveConfig() {
        MarketDataConfigDao configDao = mock(MarketDataConfigDao.class);
        MarketDataConfig legacy = new MarketDataConfig();
        legacy.setSourceName("LEGACY");
        legacy.setApiKey("old-key");
        when(configDao.selectActiveByUserId(1L)).thenReturn(List.of(legacy));
        MarketDataServiceImpl service = new MarketDataServiceImpl(configDao, mock(RedisCacheService.class), new FinancialDataProperties());
        assertFalse(service.hasActiveConfig(1L));
        assertThrows(RuntimeException.class, () -> service.fetchMarketTurnoverData(1L));
    }

    @Test
    void shouldRouteTurnoverToFuyaoWithSecretInEnvironment() throws Exception {
        MarketDataConfigDao configDao = mock(MarketDataConfigDao.class);
        MarketDataConfig config = new MarketDataConfig();
        config.setSourceName(FinancialDataConstants.SOURCE_NAME);
        config.setApiKey("test-key");
        config.setApiUrl("https://fuyao.aicubes.cn");
        when(configDao.selectActiveByUserId(1L)).thenReturn(List.of(config));
        MarketDataServiceImpl service = new MarketDataServiceImpl(configDao, mock(RedisCacheService.class), new FinancialDataProperties());
        ReflectionTestUtils.setField(service, "pythonExecutable", "python");
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream("{\"status\":\"success\",\"data\":{}}".getBytes(StandardCharsets.UTF_8)));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        var environment = new HashMap<String, String>();
        try (MockedConstruction<ProcessBuilder> construction = mockConstruction(ProcessBuilder.class, (builder, context) -> {
            assertEquals(List.of("python", "python-service/fuyao.py", "--operation", "market-turnover",
                    "--api-url", "https://fuyao.aicubes.cn", "--symbols", "000001.SH,399001.SZ"), context.arguments().get(0));
            when(builder.environment()).thenReturn(environment);
            when(builder.start()).thenReturn(process);
        })) {
            assertEquals("success", service.fetchMarketTurnoverData(1L).get("status"));
            assertEquals("test-key", environment.get(FinancialDataConstants.API_KEY_ENV));
            assertEquals("utf-8", environment.get("PYTHONIOENCODING"));
            assertEquals(1, construction.constructed().size());
        }
    }

    /** 验证真实服务入口的进程参数、无 Key 依赖和上游错误传播。 */
    private void verifyChipProcess(String output, boolean failure, boolean cached) throws Exception {
        MarketDataConfigDao configDao = mock(MarketDataConfigDao.class);
        RedisCacheService cache = mock(RedisCacheService.class);
        String key = CacheConstants.CHIP_LAST_SUCCESS_KEY_PREFIX + "601138.SH";
        if (cached) {
            when(cache.get(key)).thenReturn("{\"status\":\"success\",\"data\":{\"date\":\"2026-09-04\",\"profit_ratio\":25}}");
        }
        MarketDataServiceImpl service = new MarketDataServiceImpl(configDao, cache, new FinancialDataProperties());
        ReflectionTestUtils.setField(service, "pythonExecutable", "python");
        ReflectionTestUtils.setField(service, "akshareScriptPath", "python-service/akshare.py");
        StockBasic stock = new StockBasic();
        stock.setTsCode("601138.SH");
        stock.setSymbol("601138");
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        when(process.waitFor(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);

        try (MockedConstruction<ProcessBuilder> construction = mockConstruction(ProcessBuilder.class,
                (builder, context) -> {
                    assertEquals(List.of("python", "python-service/akshare_chip.py", "--symbol", "601138"),
                            context.arguments().get(0));
                    when(builder.environment()).thenReturn(new HashMap<>());
                    when(builder.start()).thenReturn(process);
                })) {
            var response = service.fetchChipData(stock, null);
            if (failure) {
                var result = JSONUtil.parseObj(response);
                assertEquals(cached ? "success" : "error", result.getStr("status"));
                assertTrue(result.getStr("message").contains("筹码上游"));
                verify(cache, never()).setJson(any(), any());
            } else {
                assertEquals("success", response.get("status"));
                verify(cache).setJson(eq(key), any());
            }
            if (cached) {
                var data = JSONUtil.parseObj(response).getJSONObject("data");
                assertEquals("2026-09-04", data.getStr("date"));
                assertTrue(data.getBool("stale"));
            }
            assertEquals(1, construction.constructed().size());
            verifyNoInteractions(configDao);
        }
    }
}
