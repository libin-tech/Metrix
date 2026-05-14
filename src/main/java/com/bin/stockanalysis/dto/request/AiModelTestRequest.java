package com.bin.stockanalysis.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModelTestRequest {

    private String modelType;
    private String modelName;
    private String apiBaseUrl;
    private String apiKey;
    private Double temperature;
    private Integer timeout;
}
