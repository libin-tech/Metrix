package com.bintech.metrix.client;

import cn.hutool.cache.CacheUtil;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.config.WechatConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatApiClient {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={appid}&secret={secret}";
    private static final String USER_INFO_URL = "https://api.weixin.qq.com/cgi-bin/user/info?access_token={token}&openid={openid}&lang=zh_CN";
    private static final long TOKEN_CACHE_TTL = 7000_000L;

    private final RestTemplate restTemplate;
    private final WechatConfig wechatConfig;

    private final TimedCache<String, String> tokenCache = CacheUtil.newTimedCache(TOKEN_CACHE_TTL);

    public String getAccessToken() {
        String cached = tokenCache.get("access_token", false);
        if (cached != null) {
            return cached;
        }

        try {
            String response = restTemplate.getForObject(ACCESS_TOKEN_URL, String.class,
                    Map.of("appid", wechatConfig.getAppId(), "secret", wechatConfig.getAppSecret()));
            JSONObject json = JSONUtil.parseObj(response);
            if (json.containsKey("access_token")) {
                String token = json.getStr("access_token");
                tokenCache.put("access_token", token);
                log.info("微信access_token获取成功");
                return token;
            } else {
                log.error("微信access_token获取失败: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("微信access_token获取异常", e);
            return null;
        }
    }

    public JSONObject getUserInfo(String openid) {
        String token = getAccessToken();
        if (token == null) {
            return null;
        }

        try {
            String response = restTemplate.getForObject(USER_INFO_URL, String.class,
                    Map.of("token", token, "openid", openid));
            JSONObject json = JSONUtil.parseObj(response);
            if (json.containsKey("nickname")) {
                return json;
            } else {
                log.warn("获取微信用户信息失败: {}", response);
                return null;
            }
        } catch (Exception e) {
            log.error("获取微信用户信息异常", e);
            return null;
        }
    }
}
