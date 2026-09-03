package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图形验证码响应。
 */
@Data
@AllArgsConstructor
public class CaptchaResponse {

    private String captchaId;
    private String image;
}
