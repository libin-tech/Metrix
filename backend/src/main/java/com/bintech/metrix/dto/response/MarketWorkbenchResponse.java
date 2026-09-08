package com.bintech.metrix.dto.response;

import java.util.List;
import java.util.Map;

/** 行情工作台单标的快照、日线序列和计算指标。 */
public record MarketWorkbenchResponse(
        Map<String, Object> instrument,
        Map<String, Object> quote,
        Map<String, Object> metrics,
        List<Map<String, Object>> bars,
        List<PortfolioHoldingVO> holdings,
        String source,
        String adjustment,
        String dataTime
) {
}
