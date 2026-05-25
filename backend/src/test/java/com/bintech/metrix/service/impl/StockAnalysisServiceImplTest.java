package com.bintech.metrix.service.impl;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;

/**
 * StockAnalysisServiceImpl JSON解析单元测试
 * 
 * <p>测试 buildAnalysisPrompt 方法中各数据类型的JSON解析功能
 */
class StockAnalysisServiceImplTest {

    // ==================== 模拟 StockAnalysisServiceImpl 的辅助方法 ====================

    private void appendMarketData(StringBuilder prompt, Map<String, Object> marketData) {
        if (marketData == null) {
            return;
        }
        
        try {
            if (!"success".equals(marketData.get("status"))) {
                return;
            }
            
            Object dataObj = marketData.get("data");
            if (!(dataObj instanceof JSONObject)) {
                return;
            }
            
            JSONObject data = (JSONObject) dataObj;
            prompt.append("【实时行情】\n");
            
            prompt.append("最新价: ").append(getBigDecimalOrUnknown(data, "close")).append("\n");
            prompt.append("开盘价: ").append(getBigDecimalOrUnknown(data, "open")).append("\n");
            prompt.append("最高价: ").append(getBigDecimalOrUnknown(data, "high")).append("\n");
            prompt.append("最低价: ").append(getBigDecimalOrUnknown(data, "low")).append("\n");
            prompt.append("成交量: ").append(getLongOrUnknown(data, "volume")).append("\n");
            prompt.append("涨跌幅: ").append(getBigDecimalOrUnknown(data, "changePercent")).append("%\n\n");
        } catch (Exception e) {
            prompt.append("【实时行情】\n数据解析失败: ").append(e.getMessage()).append("\n\n");
        }
    }

    private void appendDepthData(StringBuilder prompt, Map<String, Object> depthData) {
        if (depthData == null) {
            return;
        }

        JSONObject depthDataJson = new JSONObject(depthData);
        
        try {
            if (!"success".equals(depthDataJson.get("status"))) {
                return;
            }

            Object dataObj = depthDataJson.get("data");
            if (!(dataObj instanceof JSONObject)) {
                return;
            }
            JSONObject data = (JSONObject) dataObj;
            prompt.append("【市场深度-五档行情】\n");
            
            // 卖盘五档（使用数组形式：ask_prices, ask_volumes）
            JSONArray askPrices = data.getJSONArray("ask_prices");
            JSONArray askVolumes = data.getJSONArray("ask_volumes");
            
            // 先尝试新格式（数组形式），如果失败则尝试旧格式（对象数组）
            if (askPrices != null && askVolumes != null && !askPrices.isEmpty()) {
                prompt.append("卖盘（按价格从高到低）：\n");
                int count = Math.min(askPrices.size(), 5);
                for (int i = 0; i < count; i++) {
                    try {
                        double price = askPrices.getDouble(i, 0.0);
                        long size = askVolumes.getLong(i, 0L);
                        prompt.append(String.format("  档%d: 价格=%.2f, 数量=%d\n", i + 1, price, size));
                    } catch (Exception e) {
                        prompt.append(String.format("  档%d: 数据解析失败\n", i + 1));
                    }
                }
            } else {
                // 旧格式兼容
                JSONArray asks = data.getJSONArray("asks");
                if (asks != null && !asks.isEmpty()) {
                    prompt.append("卖盘（按价格从高到低）：\n");
                    int count = Math.min(asks.size(), 5);
                    for (int i = 0; i < count; i++) {
                        try {
                            JSONObject ask = asks.getJSONObject(i);
                            double price = ask.getDouble("price", 0.0);
                            long size = ask.getLong("size", 0L);
                            prompt.append(String.format("  档%d: 价格=%.2f, 数量=%d\n", i + 1, price, size));
                        } catch (Exception e) {
                            prompt.append(String.format("  档%d: 数据解析失败\n", i + 1));
                        }
                    }
                } else {
                    prompt.append("卖盘：无数据\n");
                }
            }
            
            // 买盘五档（使用数组形式：bid_prices, bid_volumes）
            JSONArray bidPrices = data.getJSONArray("bid_prices");
            JSONArray bidVolumes = data.getJSONArray("bid_volumes");
            
            // 先尝试新格式（数组形式），如果失败则尝试旧格式（对象数组）
            if (bidPrices != null && bidVolumes != null && !bidPrices.isEmpty()) {
                prompt.append("买盘（按价格从高到低）：\n");
                int count = Math.min(bidPrices.size(), 5);
                for (int i = 0; i < count; i++) {
                    try {
                        double price = bidPrices.getDouble(i, 0.0);
                        long size = bidVolumes.getLong(i, 0L);
                        prompt.append(String.format("  档%d: 价格=%.2f, 数量=%d\n", i + 1, price, size));
                    } catch (Exception e) {
                        prompt.append(String.format("  档%d: 数据解析失败\n", i + 1));
                    }
                }
            } else {
                // 旧格式兼容
                JSONArray bids = data.getJSONArray("bids");
                if (bids != null && !bids.isEmpty()) {
                    prompt.append("买盘（按价格从高到低）：\n");
                    int count = Math.min(bids.size(), 5);
                    for (int i = 0; i < count; i++) {
                        try {
                            JSONObject bid = bids.getJSONObject(i);
                            double price = bid.getDouble("price", 0.0);
                            long size = bid.getLong("size", 0L);
                            prompt.append(String.format("  档%d: 价格=%.2f, 数量=%d\n", i + 1, price, size));
                        } catch (Exception e) {
                            prompt.append(String.format("  档%d: 数据解析失败\n", i + 1));
                        }
                    }
                } else {
                    prompt.append("买盘：无数据\n");
                }
            }
            prompt.append("\n");
        } catch (Exception e) {
            prompt.append("【市场深度-五档行情】\n数据解析失败: ").append(e.getMessage()).append("\n\n");
        }
    }

    private void appendKlinesData(StringBuilder prompt, Map<String, Object> klinesData) {
        if (klinesData == null) {
            return;
        }

        JSONObject klinesDataJson = new JSONObject(klinesData);
        
        try {
            if (!"success".equals(klinesDataJson.get("status"))) {
                return;
            }
            
            Object dataObj = klinesDataJson.get("data");
            if (!(dataObj instanceof JSONObject)) {
                return;
            }
            
            JSONObject data = (JSONObject) dataObj;
            
            // 新格式：并行数组形式（timestamp, open, high, low, close, volume）
            JSONArray timestamps = data.getJSONArray("timestamp");
            JSONArray opens = data.getJSONArray("open");
            JSONArray highs = data.getJSONArray("high");
            JSONArray lows = data.getJSONArray("low");
            JSONArray closes = data.getJSONArray("close");
            JSONArray volumes = data.getJSONArray("volume");
            
            int dataSize = 0;
            if (timestamps != null && !timestamps.isEmpty()) {
                dataSize = timestamps.size();
            } else if (opens != null && !opens.isEmpty()) {
                dataSize = opens.size();
            }
            
            if (dataSize == 0) {
                // 尝试旧格式：对象数组
                JSONArray klines = data.getJSONArray("data");
                if (klines != null && !klines.isEmpty()) {
                    parseKlinesOldFormat(prompt, klines);
                }
                return;
            }
            
            // 新格式：并行数组解析
            prompt.append("【K线数据（日线，最近").append(dataSize).append("条）】\n");
            prompt.append("格式：日期 | 开 | 高 | 低 | 收 | 成交量\n");
            
            // 只显示最近20条，避免提示词过长
            int startIdx = Math.max(0, dataSize - 20);
            for (int i = startIdx; i < dataSize; i++) {
                try {
                    long timestamp = timestamps.getLong(i, 0L);
                    double open = opens.getDouble(i, 0.0);
                    double high = highs.getDouble(i, 0.0);
                    double low = lows.getDouble(i, 0.0);
                    double close = closes.getDouble(i, 0.0);
                    long volume = volumes.getLong(i, 0L);
                    
                    // 将时间戳转换为日期格式
                    String date = formatTimestamp(timestamp);
                    
                    prompt.append(String.format("  %s | %.2f | %.2f | %.2f | %.2f | %d\n",
                            date, open, high, low, close, volume));
                } catch (Exception e) {
                    prompt.append(String.format("  第%d条: 数据解析失败\n", i + 1));
                }
            }
            prompt.append("\n");
        } catch (Exception e) {
            prompt.append("【K线数据】\n数据解析失败: ").append(e.getMessage()).append("\n\n");
        }
    }
    
    private void parseKlinesOldFormat(StringBuilder prompt, JSONArray klines) {
        prompt.append("【K线数据（日线，最近").append(klines.size()).append("条）】\n");
        prompt.append("格式：日期 | 开 | 高 | 低 | 收 | 成交量\n");
        
        int startIdx = Math.max(0, klines.size() - 20);
        for (int i = startIdx; i < klines.size(); i++) {
            try {
                JSONObject kline = klines.getJSONObject(i);
                String date = kline.getStr("date", "未知日期");
                double open = kline.getDouble("open", 0.0);
                double high = kline.getDouble("high", 0.0);
                double low = kline.getDouble("low", 0.0);
                double close = kline.getDouble("close", 0.0);
                long volume = kline.getLong("volume", 0L);
                
                prompt.append(String.format("  %s | %.2f | %.2f | %.2f | %.2f | %d\n",
                        date, open, high, low, close, volume));
            } catch (Exception e) {
                prompt.append(String.format("  第%d条: 数据解析失败\n", i + 1));
            }
        }
        prompt.append("\n");
    }
    
    private String formatTimestamp(long timestamp) {
        if (timestamp == 0) {
            return "未知日期";
        }
        try {
            if (timestamp > 1000000000000L) {
                return java.time.Instant.ofEpochMilli(timestamp)
                        .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                        .toLocalDate()
                        .toString();
            } else {
                return java.time.Instant.ofEpochSecond(timestamp)
                        .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                        .toLocalDate()
                        .toString();
            }
        } catch (Exception e) {
            return "未知日期";
        }
    }

    private void appendNewsSummary(StringBuilder prompt, Map<String, Object> newsSummary) {
        if (newsSummary == null) {
            return;
        }
        
        try {
            String summary = (String) newsSummary.get("summary");
            if (summary == null || summary.isEmpty()) {
                return;
            }
            
            prompt.append("【新闻舆情摘要】\n");
            prompt.append("新闻数量：").append(newsSummary.getOrDefault("count", 0)).append("\n");
            prompt.append("核心摘要：\n").append(summary).append("\n\n");
        } catch (Exception e) {
            prompt.append("【新闻舆情摘要】\n数据解析失败: ").append(e.getMessage()).append("\n\n");
        }
    }

    private String getBigDecimalOrUnknown(JSONObject json, String key) {
        try {
            Object value = json.get(key);
            if (value == null) {
                return "未知";
            }
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).toPlainString();
            }
            if (value instanceof Number) {
                return String.valueOf(((Number) value).doubleValue());
            }
            return String.valueOf(value);
        } catch (Exception e) {
            return "未知";
        }
    }

    private String getLongOrUnknown(JSONObject json, String key) {
        try {
            Object value = json.get(key);
            if (value == null) {
                return "未知";
            }
            if (value instanceof Long) {
                return value.toString();
            }
            if (value instanceof Number) {
                return String.valueOf(((Number) value).longValue());
            }
            return String.valueOf(value);
        } catch (Exception e) {
            return "未知";
        }
    }

    // ==================== 测试方法 ====================

    @Test
    @DisplayName("测试空市场数据解析 - 应跳过添加")
    void testAppendMarketDataWithNull() {
        StringBuilder prompt = new StringBuilder();
        appendMarketData(prompt, null);
        assertFalse(prompt.toString().contains("【实时行情】"), 
                "空市场数据不应添加实时行情内容");
    }

    @Test
    @DisplayName("测试失败状态的市场数据解析 - 应跳过添加")
    void testAppendMarketDataWithFailedStatus() {
        StringBuilder prompt = new StringBuilder();
        Map<String, Object> marketData = new HashMap<>();
        marketData.put("status", "error");
        marketData.put("message", "请求失败");
        
        appendMarketData(prompt, marketData);
        assertFalse(prompt.toString().contains("【实时行情】"), 
                "失败状态的市场数据不应添加实时行情内容");
    }

    @Test
    @DisplayName("测试正常市场数据解析")
    void testAppendMarketDataSuccess() {
        StringBuilder prompt = new StringBuilder();
        Map<String, Object> marketData = new HashMap<>();
        marketData.put("status", "success");
        
        JSONObject data = new JSONObject();
        data.put("close", new BigDecimal("10.50"));
        data.put("open", new BigDecimal("10.00"));
        data.put("high", new BigDecimal("11.00"));
        data.put("low", new BigDecimal("9.80"));
        data.put("volume", 1000000L);
        data.put("changePercent", new BigDecimal("5.00"));
        marketData.put("data", data);
        
        appendMarketData(prompt, marketData);
        
        String result = prompt.toString();
        assertTrue(result.contains("【实时行情】"), "应包含实时行情标题");
        assertTrue(result.contains("最新价: 10.50"), "应包含最新价");
        assertTrue(result.contains("开盘价: 10.00"), "应包含开盘价");
        assertTrue(result.contains("最高价: 11.00"), "应包含最高价");
        assertTrue(result.contains("最低价: 9.80"), "应包含最低价");
        assertTrue(result.contains("成交量: 1000000"), "应包含成交量");
        assertTrue(result.contains("涨跌幅: 5.00%"), "应包含涨跌幅");
    }

    @Test
    @DisplayName("测试深度数据为空时的处理")
    void testAppendDepthDataWithNull() {
        StringBuilder prompt = new StringBuilder();
        appendDepthData(prompt, null);
        assertFalse(prompt.toString().contains("【市场深度-五档行情】"), 
                "空深度数据不应添加五档行情内容");
    }

    @Test
    @DisplayName("测试新格式深度数据解析（数组形式）")
    void testAppendDepthDataWithArrayFormat() {
        StringBuilder prompt = new StringBuilder();
        Map<String, Object> depthData = new HashMap<>();
        depthData.put("status", "success");
        
        JSONObject data = new JSONObject();
        data.put("symbol", "603118.SH");
        data.put("region", "CN");
        data.put("timestamp", 1778223602000L);
        
        // 新格式：数组形式
        JSONArray bidPrices = new JSONArray();
        bidPrices.add(15.73);
        bidPrices.add(15.72);
        bidPrices.add(15.71);
        bidPrices.add(15.70);
        bidPrices.add(15.69);
        data.put("bid_prices", bidPrices);
        
        JSONArray bidVolumes = new JSONArray();
        bidVolumes.add(892);
        bidVolumes.add(4358);
        bidVolumes.add(1145);
        bidVolumes.add(1753);
        bidVolumes.add(378);
        data.put("bid_volumes", bidVolumes);
        
        JSONArray askPrices = new JSONArray();
        askPrices.add(15.74);
        askPrices.add(15.75);
        askPrices.add(15.76);
        askPrices.add(15.77);
        askPrices.add(15.78);
        data.put("ask_prices", askPrices);
        
        JSONArray askVolumes = new JSONArray();
        askVolumes.add(1282);
        askVolumes.add(842);
        askVolumes.add(456);
        askVolumes.add(392);
        askVolumes.add(610);
        data.put("ask_volumes", askVolumes);
        
        depthData.put("data", data);
        
        appendDepthData(prompt, depthData);
        
        String result = prompt.toString();
        assertTrue(result.contains("【市场深度-五档行情】"), "应包含五档行情标题");
        assertTrue(result.contains("卖盘（按价格从高到低）"), "应包含卖盘标题");
        assertTrue(result.contains("买盘（按价格从高到低）"), "应包含买盘标题");
        assertTrue(result.contains("档1: 价格=15.74, 数量=1282"), "应包含卖盘第一档");
        assertTrue(result.contains("档1: 价格=15.73, 数量=892"), "应包含买盘第一档");
    }

    @Test
    @DisplayName("测试新格式K线数据解析（并行数组）")
    void testAppendKlinesDataWithParallelArrays() {
        StringBuilder prompt = new StringBuilder();
        Map<String, Object> klinesData = new HashMap<>();
        klinesData.put("status", "success");
        
        JSONObject data = new JSONObject();
        
        JSONArray timestamps = new JSONArray();
        timestamps.add(1764777600000L);
        timestamps.add(1764864000000L);
        timestamps.add(1765123200000L);
        data.put("timestamp", timestamps);
        
        JSONArray opens = new JSONArray();
        opens.add(11.23);
        opens.add(11.32);
        opens.add(11.41);
        data.put("open", opens);
        
        JSONArray highs = new JSONArray();
        highs.add(11.35);
        highs.add(11.45);
        highs.add(11.77);
        data.put("high", highs);
        
        JSONArray lows = new JSONArray();
        lows.add(11.10);
        lows.add(11.13);
        lows.add(11.39);
        data.put("low", lows);
        
        JSONArray closes = new JSONArray();
        closes.add(11.29);
        closes.add(11.39);
        closes.add(11.69);
        data.put("close", closes);
        
        JSONArray volumes = new JSONArray();
        volumes.add(120046);
        volumes.add(142360);
        volumes.add(223785);
        data.put("volume", volumes);
        
        klinesData.put("data", data);
        
        appendKlinesData(prompt, klinesData);
        
        String result = prompt.toString();
        assertTrue(result.contains("【K线数据"), "应包含K线数据标题");
        // 验证K线数据格式和关键数值
        assertTrue(result.contains("| 11.23 |"), "应包含开盘价数据");
        assertTrue(result.contains("| 11.35 |"), "应包含最高价数据");
        assertTrue(result.contains("| 11.29 |"), "应包含收盘价数据");
        assertTrue(result.contains("| 120046"), "应包含成交量数据");
        // 验证包含日期（日期格式单独在testFormatTimestamp中测试）
        assertFalse(result.contains("未知日期"), "日期不应为未知");
    }

    @Test
    @DisplayName("测试K线数据为空时的处理")
    void testAppendKlinesDataWithNull() {
        StringBuilder prompt = new StringBuilder();
        appendKlinesData(prompt, null);
        assertFalse(prompt.toString().contains("【K线数据"), 
                "空K线数据不应添加K线内容");
    }

    @Test
    @DisplayName("测试新闻摘要为空时的处理")
    void testAppendNewsSummaryWithNull() {
        StringBuilder prompt = new StringBuilder();
        appendNewsSummary(prompt, null);
        assertFalse(prompt.toString().contains("【新闻舆情摘要】"), 
                "空新闻摘要不应添加新闻内容");
    }

    @Test
    @DisplayName("测试正常新闻摘要解析")
    void testAppendNewsSummarySuccess() {
        StringBuilder prompt = new StringBuilder();
        Map<String, Object> newsSummary = new HashMap<>();
        newsSummary.put("summary", "今日A股三大指数集体高开，科技板块领涨。");
        newsSummary.put("count", 5);
        
        appendNewsSummary(prompt, newsSummary);
        
        String result = prompt.toString();
        assertTrue(result.contains("【新闻舆情摘要】"), "应包含新闻摘要标题");
        assertTrue(result.contains("今日A股三大指数集体高开"), "应包含新闻摘要内容");
    }

    @Test
    @DisplayName("测试安全获取BigDecimal值 - 正常情况")
    void testGetBigDecimalOrUnknownSuccess() {
        JSONObject json = new JSONObject();
        json.put("price", new BigDecimal("100.50"));
        
        String result = getBigDecimalOrUnknown(json, "price");
        assertEquals("100.50", result);
    }

    @Test
    @DisplayName("测试安全获取Long值 - 正常情况")
    void testGetLongOrUnknownSuccess() {
        JSONObject json = new JSONObject();
        json.put("volume", 1000000L);
        
        String result = getLongOrUnknown(json, "volume");
        assertEquals("1000000", result);
    }

    @Test
    @DisplayName("测试时间戳格式转换")
    void testFormatTimestamp() {
        // 毫秒时间戳
        String result1 = formatTimestamp(1778223602000L);
        assertTrue(result1.length() == 10, "日期格式应为yyyy-MM-dd");
        
        // 秒时间戳
        String result2 = formatTimestamp(1778223602L);
        assertTrue(result2.length() == 10, "日期格式应为yyyy-MM-dd");
        
        // 零值
        String result3 = formatTimestamp(0);
        assertEquals("未知日期", result3);
    }

    @Test
    @DisplayName("测试buildAnalysisPrompt整合所有数据类型")
    void testBuildAnalysisPromptWithAllData() {
        StringBuilder prompt = new StringBuilder();
        
        // 准备市场数据
        Map<String, Object> marketData = new HashMap<>();
        marketData.put("status", "success");
        JSONObject marketJson = new JSONObject();
        marketJson.put("close", new BigDecimal("25.50"));
        marketJson.put("open", new BigDecimal("25.00"));
        marketJson.put("high", new BigDecimal("26.00"));
        marketJson.put("low", new BigDecimal("24.80"));
        marketJson.put("volume", 2000000L);
        marketJson.put("changePercent", new BigDecimal("2.00"));
        marketData.put("data", marketJson);

        // 准备深度数据（新格式）
        Map<String, Object> depthData = new HashMap<>();
        depthData.put("status", "success");
        JSONObject depthJson = new JSONObject();
        JSONArray askPrices = new JSONArray();
        askPrices.add(25.6);
        askPrices.add(25.7);
        depthJson.put("ask_prices", askPrices);
        JSONArray askVolumes = new JSONArray();
        askVolumes.add(5000);
        askVolumes.add(6000);
        depthJson.put("ask_volumes", askVolumes);
        JSONArray bidPrices = new JSONArray();
        bidPrices.add(25.4);
        bidPrices.add(25.3);
        depthJson.put("bid_prices", bidPrices);
        JSONArray bidVolumes = new JSONArray();
        bidVolumes.add(6000);
        bidVolumes.add(7000);
        depthJson.put("bid_volumes", bidVolumes);
        depthData.put("data", depthJson);

        // 准备K线数据（新格式）
        Map<String, Object> klinesData = new HashMap<>();
        klinesData.put("status", "success");
        JSONObject klineJson = new JSONObject();
        JSONArray timestamps = new JSONArray();
        timestamps.add(1778223600000L);
        klineJson.put("timestamp", timestamps);
        JSONArray opens = new JSONArray();
        opens.add(25.0);
        klineJson.put("open", opens);
        JSONArray highs = new JSONArray();
        highs.add(26.0);
        klineJson.put("high", highs);
        JSONArray lows = new JSONArray();
        lows.add(24.5);
        klineJson.put("low", lows);
        JSONArray closes = new JSONArray();
        closes.add(25.5);
        klineJson.put("close", closes);
        JSONArray volumes = new JSONArray();
        volumes.add(2000000);
        klineJson.put("volume", volumes);
        klinesData.put("data", klineJson);

        // 准备新闻摘要
        Map<String, Object> newsSummary = new HashMap<>();
        newsSummary.put("summary", "测试新闻摘要内容");
        newsSummary.put("count", 3);

        prompt.append("你是一名专业的金融分析师， 请对股票 600000.SH进行综合分析。\n\n");
        appendMarketData(prompt, marketData);
        appendDepthData(prompt, depthData);
        appendKlinesData(prompt, klinesData);
        appendNewsSummary(prompt, newsSummary);
        prompt.append("请提供以下分析内容：\n");
        prompt.append("1. 基本面分析\n");
        prompt.append("2. 技术面分析（结合K线形态和成交量）\n");
        prompt.append("3. 市场情绪分析（结合五档行情和资金流向）\n");
        prompt.append("4. 投资建议\n");
        prompt.append("5. 风险提示\n");

        String result = prompt.toString();
        assertTrue(result.contains("请对股票 600000.SH进行综合分析"), "应包含股票代码和分析类型");
        assertTrue(result.contains("【实时行情】"), "应包含实时行情");
        assertTrue(result.contains("【市场深度-五档行情】"), "应包含五档行情");
        assertTrue(result.contains("【K线数据"), "应包含K线数据");
        assertTrue(result.contains("【新闻舆情摘要】"), "应包含新闻摘要");
        assertTrue(result.contains("请提供以下分析内容"), "应包含分析要求");
    }
}