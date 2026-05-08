package com.bin.stockanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataConfigRequest {

    @NotBlank(message = "Source name is required")
    private String sourceName;

    @NotBlank(message = "API URL is required")
    private String apiUrl;

    private String apiKey;

    private String dataType;

    private Integer requestInterval = 30;

    private Boolean isActive = true;
}
