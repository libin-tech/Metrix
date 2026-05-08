package com.bin.stockanalysis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationConfigRequest {

    @NotBlank(message = "Channel type is required")
    private String channelType;

    @NotBlank(message = "Webhook URL is required")
    private String webhookUrl;

    private String secret;

    private Boolean isActive = true;
}
