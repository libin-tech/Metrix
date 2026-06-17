package com.bintech.metrix.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MarkdownRenderer 单元测试
 */
class MarkdownRendererTest {

    @Test
    @DisplayName("测试渲染分析结果")
    void testRenderAnalysisResult() {
        String content = "这是一段分析内容。\n\n## 分析摘要\n\n更多内容。";
        String result = MarkdownRenderer.renderAnalysisResult(content);
        
        assertTrue(result.contains("# 股票分析报告"), "应包含报告标题");
        assertTrue(result.contains("## 分析摘要"), "应包含二级标题");
    }

    @Test
    @DisplayName("测试渲染空分析结果")
    void testRenderAnalysisResultEmpty() {
        String result = MarkdownRenderer.renderAnalysisResult(null);
        assertEquals("", result);
        
        result = MarkdownRenderer.renderAnalysisResult("");
        assertEquals("", result);
    }

    @Test
    @DisplayName("测试渲染新闻列表")
    void testRenderNewsList() {
        List<Map<String, Object>> newsList = new ArrayList<>();
        
        Map<String, Object> news1 = new HashMap<>();
        news1.put("title", "测试新闻标题");
        news1.put("summary", "这是新闻摘要内容");
        news1.put("source", "测试来源");
        news1.put("publishTime", "2024-01-01");
        news1.put("url", "https://example.com/news/1");
        newsList.add(news1);
        
        String result = MarkdownRenderer.renderNewsList(newsList);
        
        assertTrue(result.contains("## 相关新闻"), "应包含新闻标题");
        assertTrue(result.contains("**测试新闻标题**"), "应包含加粗的新闻标题");
        assertTrue(result.contains("> 这是新闻摘要内容"), "应包含引用格式的摘要");
        assertTrue(result.contains("[查看详情](https://example.com/news/1)"), "应包含跳转链接");
    }

    @Test
    @DisplayName("测试渲染空新闻列表")
    void testRenderNewsListEmpty() {
        String result = MarkdownRenderer.renderNewsList(null);
        assertEquals("暂无相关新闻", result);
        
        result = MarkdownRenderer.renderNewsList(new ArrayList<>());
        assertEquals("暂无相关新闻", result);
    }

    @Test
    @DisplayName("测试渲染新闻列表为HTML格式")
    void testRenderNewsListAsHtml() {
        List<Map<String, Object>> newsList = new ArrayList<>();
        
        Map<String, Object> news1 = new HashMap<>();
        news1.put("title", "HTML测试新闻");
        news1.put("summary", "HTML新闻摘要");
        news1.put("source", "HTML来源");
        news1.put("publishTime", "2024-01-02");
        news1.put("url", "https://example.com/html");
        newsList.add(news1);
        
        String result = MarkdownRenderer.renderNewsListAsHtml(newsList);
        
        assertTrue(result.contains("<div class=\"news-list\">"), "应包含新闻列表容器");
        assertTrue(result.contains("<h3 class=\"news-title\">HTML测试新闻</h3>"), "应包含标题");
        assertTrue(result.contains("<p class=\"news-summary\">HTML新闻摘要</p>"), "应包含摘要");
        assertTrue(result.contains("<a class=\"news-link\""), "应包含链接");
    }

    @Test
    @DisplayName("测试转义Markdown特殊字符")
    void testEscapeMarkdown() {
        String text = "测试 *文本* [链接](url)";
        String result = MarkdownRenderer.escapeMarkdown(text);
        
        assertTrue(result.contains("\\*文本\\*"), "星号应被转义");
        assertTrue(result.contains("\\[链接\\]\\(url\\)"), "方括号和圆括号应被转义");
    }

    @Test
    @DisplayName("测试转义HTML特殊字符")
    void testEscapeHtml() {
        String text = "<script>alert('test')</script>";
        String result = MarkdownRenderer.escapeHtml(text);
        
        assertTrue(result.contains("&lt;script&gt;"), "尖括号应被转义");
        assertTrue(result.contains("&#x27;"), "单引号应被转义");
    }

    @Test
    @DisplayName("测试创建Markdown标题")
    void testHeading() {
        String result = MarkdownRenderer.heading("测试标题", 2);
        assertEquals("## 测试标题\n\n", result);
    }

    @Test
    @DisplayName("测试创建Markdown链接")
    void testLink() {
        String result = MarkdownRenderer.link("链接文本", "https://example.com");
        assertEquals("[链接文本](https://example.com)", result);
    }

    @Test
    @DisplayName("测试创建Markdown加粗文本")
    void testBold() {
        String result = MarkdownRenderer.bold("加粗文本");
        assertEquals("**加粗文本**", result);
    }

    @Test
    @DisplayName("测试创建Markdown斜体文本")
    void testItalic() {
        String result = MarkdownRenderer.italic("斜体文本");
        assertEquals("*斜体文本*", result);
    }

    @Test
    @DisplayName("测试创建Markdown代码块")
    void testCodeBlock() {
        String result = MarkdownRenderer.codeBlock("System.out.println(\"Hello\");", "java");
        assertTrue(result.contains("```java"), "应包含语言标识");
        assertTrue(result.contains("System.out.println(\"Hello\");"), "应包含代码内容");
    }

    @Test
    @DisplayName("测试创建无序列表")
    void testUnorderedList() {
        String[] items = {"项目1", "项目2", "项目3"};
        String result = MarkdownRenderer.unorderedList(items);
        
        assertTrue(result.contains("- 项目1"), "应包含第一项");
        assertTrue(result.contains("- 项目2"), "应包含第二项");
        assertTrue(result.contains("- 项目3"), "应包含第三项");
    }

    @Test
    @DisplayName("测试创建有序列表")
    void testOrderedList() {
        String[] items = {"步骤1", "步骤2", "步骤3"};
        String result = MarkdownRenderer.orderedList(items);
        
        assertTrue(result.contains("1. 步骤1"), "应包含第一项");
        assertTrue(result.contains("2. 步骤2"), "应包含第二项");
        assertTrue(result.contains("3. 步骤3"), "应包含第三项");
    }

    @Test
    @DisplayName("测试渲染市场数据")
    void testRenderMarketData() {
        String marketDataJson = "{\"status\":\"success\",\"data\":{\"close\":10.50,\"open\":10.00,\"high\":11.00,\"low\":9.80,\"volume\":1000000,\"changePercent\":5.00}}";
        String result = MarkdownRenderer.renderMarketData(marketDataJson);
        
        assertTrue(result.contains("## 实时行情"), "应包含实时行情标题");
        assertTrue(result.contains("| 最新价 | 10.50 |"), "应包含最新价");
        assertTrue(result.contains("| 涨跌幅 | 5.00% |"), "应包含涨跌幅");
    }

    @Test
    @DisplayName("测试渲染深度数据")
    void testRenderDepthData() {
        String depthDataJson = "{\"status\":\"success\",\"data\":{\"ask_prices\":[15.74,15.75],\"ask_volumes\":[1282,842],\"bid_prices\":[15.73,15.72],\"bid_volumes\":[892,4358]}}";
        String result = MarkdownRenderer.renderDepthData(depthDataJson);
        
        assertTrue(result.contains("## 五档行情"), "应包含五档行情标题");
        assertTrue(result.contains("### 卖盘"), "应包含卖盘标题");
        assertTrue(result.contains("### 买盘"), "应包含买盘标题");
        assertTrue(result.contains("| 1 | 15.74 | 1282 |"), "应包含卖盘数据");
        assertTrue(result.contains("| 1 | 15.73 | 892 |"), "应包含买盘数据");
    }
}