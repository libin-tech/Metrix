package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModelConfigRequest {

    @NotBlank(message = "Model type is required")
    @Size(max = 50, message = "Model type must be less than 50 characters")
    private String modelType;

    @NotBlank(message = "Model name is required")
    @Size(max = 100, message = "Model name must be less than 100 characters")
    private String modelName;

    private String apiBaseUrl;

    private String apiKey;

    private Double temperature = 0.7;

    private Integer maxTokens = 2048;

    private Boolean isActive = true;

    private Integer timeout = 120;
}
