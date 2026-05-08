package com.bin.stockanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsSourceConfigRequest {

    @NotBlank(message = "Source name is required")
    private String sourceName;

    @NotBlank(message = "API URL is required")
    private String apiUrl;

    private String apiKey;

    private Integer requestInterval = 60;

    private Boolean isActive = true;
}
