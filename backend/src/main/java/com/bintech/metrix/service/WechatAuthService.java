package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.UserLoginResponse;

public interface WechatAuthService {

    void cacheLoginCode(String code, String openid);

    boolean isLoginCodeValid(String code);

    UserLoginResponse loginByCode(String code);
}
