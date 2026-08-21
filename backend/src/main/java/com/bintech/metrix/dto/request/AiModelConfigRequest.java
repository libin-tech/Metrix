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
public class AiModelConfigRequest {

    @NotBlank(message = "Model type is required")
    @Size(max = SystemConstants.MAX_MODEL_TYPE_LENGTH, message = "Model type must be less than 50 characters")
    private String modelType;

    @NotBlank(message = "Model name is required")
    @Size(max = SystemConstants.MAX_MODEL_NAME_LENGTH, message = "Model name must be less than 100 characters")
    private String modelName;

    private String apiBaseUrl;

    private String apiKey;

    private Double temperature = SystemConstants.DEFAULT_TEMPERATURE;

    private Boolean isActive = true;

    private Integer timeout = SystemConstants.AI_MODEL_TIMEOUT_SECONDS;
}
