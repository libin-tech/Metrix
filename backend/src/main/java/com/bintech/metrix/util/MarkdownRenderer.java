package com.bintech.metrix.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.constants.SystemConstants;

import java.util.List;
import java.util.Map;

/**
 * Markdown渲染工具类
 * 
 * <p>提供Markdown格式转换和渲染功能，支持标题、列表、链接、代码块等标准Markdown元素。
 */
public class MarkdownRenderer {

    /**
     * 将分析结果渲染为Markdown格式
     * 
     * @param content 原始分析内容
     * @return Markdown格式的内容
     */
    public static String renderAnalysisResult(String content) {
        if (content == null || content.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        
        // 确保内容以标题开头
        if (!content.startsWith(SystemConstants.MARKDOWN_H1_PREFIX)) {
            result.append("# 股票分析报告\n\n");
        }
        
        result.append(content);
        
        // 清理多余的换行
        result = new StringBuilder(result.toString().replace(SystemConstants.TRIPLE_NEWLINE, "\n\n"));
        
        return result.toString();
    }

    /**
     * 渲染新闻列表为Markdown格式
     * 
     * @param newsList 新闻列表
     * @return Markdown格式的新闻列表
     */
    public static String renderNewsList(List<Map<String, Object>> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return "暂无相关新闻";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("## 相关新闻\n\n");
        
        int index = 1;
        for (Map<String, Object> news : newsList) {
            String title = (String) news.getOrDefault("title", "未知标题");
            String summary = (String) news.getOrDefault("summary", "");
            String source = (String) news.getOrDefault("source", "未知来源");
            String publishTime = (String) news.getOrDefault("publishTime", "未知时间");
            String url = (String) news.getOrDefault("url", "");
            
            result.append(index).append(". **").append(escapeMarkdown(title)).append("**  \n");
            result.append("   > ").append(escapeMarkdown(summary)).append("  \n");
            
            StringBuilder meta = new StringBuilder();
            meta.append("   *来源：").append(escapeMarkdown(source)).append("*");
            if (!publishTime.isEmpty()) {
                meta.append(" | *发布时间：").append(publishTime).append("*");
            }
            result.append(meta).append("\n");
            
            if (url != null && !url.isEmpty()) {
                result.append("   [查看详情](").append(url).append(")\n");
            }
            
            result.append("\n");
            index++;
        }
        
        return result.toString();
    }

    /**
     * 渲染新闻列表项为HTML格式（用于前端展示）
     * 
     * @param newsList 新闻列表
     * @return HTML格式的新闻列表
     */
    public static String renderNewsListAsHtml(List<Map<String, Object>> newsList) {
        if (newsList == null || newsList.isEmpty()) {
            return "<div class=\"news-empty\">暂无相关新闻</div>";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("<div class=\"news-list\">\n");
        
        for (Map<String, Object> news : newsList) {
            String title = (String) news.getOrDefault("title", "未知标题");
            String summary = (String) news.getOrDefault("summary", "");
            String source = (String) news.getOrDefault("source", "未知来源");
            String publishTime = (String) news.getOrDefault("publishTime", "未知时间");
            String url = (String) news.getOrDefault("url", "");
            
            result.append("  <article class=\"news-item\">\n");
            result.append("    <h3 class=\"news-title\">").append(escapeHtml(title)).append("</h3>\n");
            result.append("    <p class=\"news-summary\">").append(escapeHtml(summary)).append("</p>\n");
            result.append("    <div class=\"news-meta\">\n");
            result.append("      <span class=\"news-source\">来源：").append(escapeHtml(source)).append("</span>\n");
            result.append("      <span class=\"news-time\">").append(publishTime).append("</span>\n");
            result.append("    </div>\n");
            if (url != null && !url.isEmpty()) {
                result.append("    <a class=\"news-link\" href=\"").append(escapeHtml(url)).append("\" target=\"_blank\" rel=\"noopener noreferrer\">\n");
                result.append("      阅读全文 →\n");
                result.append("    </a>\n");
            }
            result.append("  </article>\n");
        }
        
        result.append("</div>");
        return result.toString();
    }

    /**
     * 渲染市场数据为Markdown格式
     * 
     * @param marketDataJson 市场数据JSON字符串
     * @return Markdown格式的市场数据
     */
    public static String renderMarketData(String marketDataJson) {
        if (marketDataJson == null || marketDataJson.isEmpty()) {
            return "暂无市场数据";
        }
        
        try {
            JSONObject marketData = JSONUtil.parseObj(marketDataJson);
            String status = marketData.getStr("status", "");
            
            if (!"success".equals(status)) {
                return "市场数据获取失败";
            }
            
            JSONObject data = marketData.getJSONObject("data");
            if (data == null) {
                return "市场数据为空";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("## 实时行情\n\n");
            result.append("| 指标 | 值 |\n");
            result.append("|------|----|\n");
            result.append("| 最新价 | ").append(data.getStr("close", "未知")).append(" |\n");
            result.append("| 开盘价 | ").append(data.getStr("open", "未知")).append(" |\n");
            result.append("| 最高价 | ").append(data.getStr("high", "未知")).append(" |\n");
            result.append("| 最低价 | ").append(data.getStr("low", "未知")).append(" |\n");
            result.append("| 成交量 | ").append(data.getStr("volume", "未知")).append(" |\n");
            result.append("| 涨跌幅 | ").append(data.getStr("changePercent", "未知")).append("% |\n");
            
            return result.toString();
        } catch (Exception e) {
            return "市场数据解析失败: " + e.getMessage();
        }
    }

    /**
     * 渲染深度数据为Markdown格式
     * 
     * @param depthDataJson 深度数据JSON字符串
     * @return Markdown格式的深度数据
     */
    public static String renderDepthData(String depthDataJson) {
        if (depthDataJson == null || depthDataJson.isEmpty()) {
            return "暂无深度数据";
        }
        
        try {
            JSONObject depthData = JSONUtil.parseObj(depthDataJson);
            String status = depthData.getStr("status", "");
            
            if (!"success".equals(status)) {
                return "深度数据获取失败";
            }
            
            JSONObject data = depthData.getJSONObject("data");
            if (data == null) {
                return "深度数据为空";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("## 五档行情\n\n");
            
            // 卖盘
            result.append("### 卖盘（按价格从高到低）\n\n");
            result.append("| 档位 | 价格 | 数量 |\n");
            result.append("|------|------|------|\n");
            
            JSONArray askPrices = data.getJSONArray("ask_prices");
            JSONArray askVolumes = data.getJSONArray("ask_volumes");
            
            if (askPrices != null && askVolumes != null) {
                for (int i = 0; i < Math.min(askPrices.size(), SystemConstants.DEPTH_MAX_LEVELS); i++) {
                    double price = askPrices.getDouble(i, 0.0);
                    long volume = askVolumes.getLong(i, 0L);
                    result.append("| ").append(i + 1).append(" | ").append(String.format("%.2f", price))
                          .append(" | ").append(volume).append(" |\n");
                }
            }
            
            // 买盘
            result.append("\n### 买盘（按价格从高到低）\n\n");
            result.append("| 档位 | 价格 | 数量 |\n");
            result.append("|------|------|------|\n");
            
            JSONArray bidPrices = data.getJSONArray("bid_prices");
            JSONArray bidVolumes = data.getJSONArray("bid_volumes");
            
            if (bidPrices != null && bidVolumes != null) {
                for (int i = 0; i < Math.min(bidPrices.size(), SystemConstants.DEPTH_MAX_LEVELS); i++) {
                    double price = bidPrices.getDouble(i, 0.0);
                    long volume = bidVolumes.getLong(i, 0L);
                    result.append("| ").append(i + 1).append(" | ").append(String.format("%.2f", price))
                          .append(" | ").append(volume).append(" |\n");
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            return "深度数据解析失败: " + e.getMessage();
        }
    }

    /**
     * 转义Markdown特殊字符
     * 
     * @param text 原始文本
     * @return 转义后的文本
     */
    public static String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("*", "\\*")
                   .replace("_", "\\_")
                   .replace("{", "\\{")
                   .replace("}", "\\}")
                   .replace("[", "\\[")
                   .replace("]", "\\]")
                   .replace("(", "\\(")
                   .replace(")", "\\)")
                   .replace("#", "\\#")
                   .replace("+", "\\+")
                   .replace("-", "\\-")
                   .replace(".", "\\.")
                   .replace("!", "\\!")
                   .replace("`", "\\`");
    }

    /**
     * 转义HTML特殊字符
     * 
     * @param text 原始文本
     * @return 转义后的文本
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * 添加Markdown标题
     * 
     * @param title 标题内容
     * @param level 标题级别（1-6）
     * @return Markdown标题
     */
    public static String heading(String title, int level) {
        if (level < SystemConstants.HEADING_MIN_LEVEL) level = SystemConstants.HEADING_MIN_LEVEL;
        if (level > SystemConstants.HEADING_MAX_LEVEL) level = SystemConstants.HEADING_MAX_LEVEL;
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < level; i++) {
            result.append("#");
        }
        result.append(" ").append(title).append("\n\n");
        
        return result.toString();
    }

    /**
     * 创建Markdown链接
     * 
     * @param text 链接文本
     * @param url 链接地址
     * @return Markdown链接
     */
    public static String link(String text, String url) {
        return "[" + escapeMarkdown(text) + "](" + url + ")";
    }

    /**
     * 创建Markdown加粗文本
     * 
     * @param text 文本内容
     * @return 加粗文本
     */
    public static String bold(String text) {
        return "**" + escapeMarkdown(text) + "**";
    }

    /**
     * 创建Markdown斜体文本
     * 
     * @param text 文本内容
     * @return 斜体文本
     */
    public static String italic(String text) {
        return "*" + escapeMarkdown(text) + "*";
    }

    /**
     * 创建Markdown代码块
     * 
     * @param code 代码内容
     * @param language 代码语言
     * @return 代码块
     */
    public static String codeBlock(String code, String language) {
        return "```" + (language != null ? language : "") + "\n" + code + "\n```\n";
    }

    /**
     * 创建Markdown无序列表项
     * 
     * @param items 列表项数组
     * @return 无序列表
     */
    public static String unorderedList(String[] items) {
        StringBuilder result = new StringBuilder();
        for (String item : items) {
            result.append("- ").append(escapeMarkdown(item)).append("\n");
        }
        result.append("\n");
        return result.toString();
    }

    /**
     * 创建Markdown有序列表
     * 
     * @param items 列表项数组
     * @return 有序列表
     */
    public static String orderedList(String[] items) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            result.append(i + 1).append(". ").append(escapeMarkdown(items[i])).append("\n");
        }
        result.append("\n");
        return result.toString();
    }
}