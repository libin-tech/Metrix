package com.bin.stockanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockAnalysisRequest {

    @NotBlank(message = "Stock code is required")
    private String stockCode;

    private String analysisType = "COMPREHENSIVE";

    private Boolean includeNews = true;

    private Boolean includeMarketData = true;
}
