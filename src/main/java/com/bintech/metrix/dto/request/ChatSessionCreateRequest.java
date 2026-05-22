package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionCreateRequest {

    @NotBlank(message = "会话名称不能为空")
    private String sessionName;

}
