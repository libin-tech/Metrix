package com.bin.stockanalysis.service.impl;

import com.bin.stockanalysis.repository.entity.StockBasic;
import com.bin.stockanalysis.repository.mapper.MarketDataConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceImplTest {

    private static final Logger log = LoggerFactory.getLogger(MarketDataServiceImplTest.class);

    @Mock
    private MarketDataConfigMapper configMapper;

    private MarketDataServiceImpl marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService = new MarketDataServiceImpl(configMapper);
        ReflectionTestUtils.setField(marketDataService, "pythonExecutable", "python");
        log.info("测试环境初始化完成");
    }

    @Test
    @DisplayName("测试筹码分布 - 正常获取（调用真实 akshare.py）")
    void testFetchChipDataSuccess() {
        ReflectionTestUtils.setField(marketDataService, "akshareScriptPath",
                "python-service/akshare.py");

        StockBasic stockBasic = new StockBasic();
        stockBasic.setTsCode("002028.SZ");
        stockBasic.setSymbol("002028");

        Map<String, Object> result = marketDataService.fetchChipData(stockBasic);

        assertNotNull(result);
        assertEquals("success", result.get("status"));

        Object dataObj = result.get("data");
        assertInstanceOf(Map.class, dataObj);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) dataObj;

        assertNotNull(data.get("date"));
        assertNotNull(data.get("profit_ratio"));
        assertNotNull(data.get("loss_ratio"));
        assertTrue(((Number) data.get("avg_cost")).doubleValue() > 0);
        assertNotNull(data.get("cost_90_low"));
        assertNotNull(data.get("cost_90_high"));

        log.info("测试通过：成功获取筹码分布数据，日期={}", data.get("date"));
    }

    @Test
    @DisplayName("测试筹码分布 - 脚本执行异常（脚本路径不存在）")
    void testFetchChipDataScriptNotFound() {
        ReflectionTestUtils.setField(marketDataService, "akshareScriptPath",
                "python-service/nonexistent.py");

        StockBasic stockBasic = new StockBasic();
        stockBasic.setTsCode("601138.SH");
        stockBasic.setSymbol("601138");

        Exception exception = assertThrows(RuntimeException.class,
                () -> marketDataService.fetchChipData(stockBasic));

        assertNotNull(exception.getMessage());
        assertFalse(exception.getMessage().isEmpty(),
                "错误信息不应为空");

        log.info("测试通过：脚本异常正确处理，错误信息: {}", exception.getMessage());

        log.info("测试通过：脚本异常正确处理，错误信息: {}", exception.getMessage());
    }
}
