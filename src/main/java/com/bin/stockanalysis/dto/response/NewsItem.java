package com.bin.stockanalysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新闻列表项DTO
 * 
 * <p>提供用户友好的新闻显示格式，包含文章标题、内容摘要和跳转链接。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsItem {

    /**
     * 新闻标题
     */
    private String title;

    /**
     * 内容摘要
     */
    private String summary;

    /**
     * 来源
     */
    private String source;

    /**
     * 发布时间
     */
    private String publishTime;

    /**
     * 跳转链接
     */
    private String url;
}