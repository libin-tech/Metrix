package com.bin.stockanalysis.service.impl;

import com.bin.stockanalysis.repository.entity.StockAnalysisRecord;
import com.bin.stockanalysis.repository.entity.StockBasic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PdfExportServiceImplTest {

    @Test
    @DisplayName("测试PDF生成")
    void testGeneratePdf() {
        PdfExportServiceImpl service = new PdfExportServiceImpl();

        StockBasic stockBasic = new StockBasic();
        stockBasic.setTsCode("601138.SH");
        stockBasic.setName("工业富联");

        StockAnalysisRecord record = new StockAnalysisRecord();
        record.setAnalysisType("深度分析");
        record.setAnalysisResult("# 测试报告\n\n这是一段测试内容。\n\n## 基本面分析\n\n- 营收增长\n- 利润增长");
        record.setAnalysisOverview("{\"coreInsight\":\"测试核心洞察\",\"realTimeMarket\":{\"currentPrice\":\"70.84\",\"changePercent\":\"5.00\",\"openPrice\":\"65.00\",\"highPrice\":\"71.00\",\"lowPrice\":\"64.00\",\"prevClosePrice\":\"67.50\",\"volume\":\"407356986\",\"turnover\":\"28.05亿\",\"turnoverRate\":\"2.05\",\"volumeRatio\":\"1.25\"},\"dataPivot\":{\"currentPrice\":\"70.84\",\"ma5\":\"68.50\",\"ma20\":\"65.20\",\"ma60\":\"60.10\",\"supportLevel\":\"64.00\",\"resistanceLevel\":\"71.00\",\"volume\":\"407356986\",\"chipConcentration\":\"0.14\",\"profitRatio\":\"99.93\",\"lossRatio\":\"0.07\",\"avgCostPrice\":\"60.62\",\"cost90Low\":\"51.00\",\"cost90High\":\"67.58\",\"cost70Low\":\"53.82\",\"cost70High\":\"65.43\",\"concentration70\":\"0.0974\",\"chipSummary\":\"当前价格远高于平均成本，筹码获利盘比例很高。\"},\"battlePlan\":{\"idealEntryPrice\":\"65.00\",\"suboptimalEntryPrice\":\"66.50\",\"stopLossPrice\":\"62.00\",\"targetPrice\":\"75.00\",\"riskRewardRatio\":\"3.5\"}}");

        assertDoesNotThrow(() -> {
            byte[] pdfBytes = service.generatePdf(record, stockBasic);
            assertNotNull(pdfBytes);
            assertTrue(pdfBytes.length > 0);
            System.out.println("PDF生成成功，大小: " + pdfBytes.length + " bytes");
        });
    }
}
