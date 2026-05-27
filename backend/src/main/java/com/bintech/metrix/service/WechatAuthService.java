package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.UserLoginResponse;

public interface WechatAuthService {

    UserLoginResponse loginByCode(String code);
}
