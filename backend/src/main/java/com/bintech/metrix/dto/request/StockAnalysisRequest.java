package com.bintech.metrix.dto.request;

import com.bintech.metrix.constants.BusinessConstants;
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

    private String analysisType = BusinessConstants.DEFAULT_ANALYSIS_TYPE;

    private Boolean pushToFeishu = false;
}
