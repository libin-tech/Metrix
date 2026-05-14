package com.bin.stockanalysis.core.queue;

import com.bin.stockanalysis.dto.request.StockAnalysisRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 分析任务体，封装待执行的股票分析请求
 */
@Data
@AllArgsConstructor
public class AnalysisTask {
    /** 分析记录ID */
    private Long recordId;
    /** 分析请求参数 */
    private StockAnalysisRequest request;
    /** 股票名称 */
    private String stockName;
}
