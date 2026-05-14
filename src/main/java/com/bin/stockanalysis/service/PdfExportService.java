package com.bin.stockanalysis.service;

import com.bin.stockanalysis.repository.entity.StockAnalysisRecord;
import com.bin.stockanalysis.repository.entity.StockBasic;

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
