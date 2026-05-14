package com.bin.stockanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    private Integer timeout = 60;

    @Size(max = 100, message = "备注不能超过100字")
    private String remark;
}
