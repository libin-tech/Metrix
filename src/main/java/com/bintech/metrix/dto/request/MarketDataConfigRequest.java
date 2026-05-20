package com.bintech.metrix.dto.request;

import com.bintech.metrix.constants.SystemConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    private Integer requestInterval = SystemConstants.DEFAULT_REQUEST_INTERVAL;

    private Boolean isActive = true;

    private Integer timeout = SystemConstants.DEFAULT_TIMEOUT_SECONDS;

    @Size(max = SystemConstants.MAX_REMARK_LENGTH_100, message = "备注不能超过100字")
    private String remark;
}
