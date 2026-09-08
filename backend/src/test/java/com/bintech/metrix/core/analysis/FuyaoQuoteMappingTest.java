package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONUtil;
import com.bintech.metrix.model.RealTimeMarket;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FuyaoQuoteMappingTest {
    @Test
    void shouldPreservePercentUnitsAndMissingMetrics() {
        AnalysisOverviewBuilder builder = new AnalysisOverviewBuilder(null, null);
        for (String percentage : new String[]{"0.5", "-1.5"}) {
            var payload = JSONUtil.parseObj("{\"data\":[{\"last_price\":10,\"ext\":{\"change_pct\":" + percentage + "}}]}");
            RealTimeMarket quote = ReflectionTestUtils.invokeMethod(builder, "buildRealTimeMarket", payload);
            assertNotNull(quote);
            assertEquals(0, new BigDecimal(percentage).compareTo(quote.getChangePercent()));
            assertNull(quote.getTurnoverRate());
            assertNull(quote.getUpdateTime());
        }
    }
}
