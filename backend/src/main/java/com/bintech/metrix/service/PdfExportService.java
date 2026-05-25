package com.bintech.metrix.service;

import com.bintech.metrix.repository.entity.StockAnalysisRecord;
import com.bintech.metrix.repository.entity.StockBasic;

/**
 * PDF导出服务
 */
public interface PdfExportService {

    /**
     * 生成分析报告PDF
     *
     * @param record     分析记录
     * @param stockBasic 股票基本信息
     * @return PDF字节数组
     */
    byte[] generatePdf(StockAnalysisRecord record, StockBasic stockBasic);
}
