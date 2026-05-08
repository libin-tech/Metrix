package com.bin.stockanalysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAnalysisResponse {

    private String stockCode;
    private String stockName;
    private String analysisType;
    private String analysisResult;
    private BigDecimal confidenceScore;
    private Map<String, Object> marketData;
    private Map<String, Object> newsSummary;
    private LocalDateTime createdAt;
}
