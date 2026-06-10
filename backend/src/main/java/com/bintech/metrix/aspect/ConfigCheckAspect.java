package com.bintech.metrix.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.annotation.CheckConfig;
import com.bintech.metrix.enums.ConfigType;
import com.bintech.metrix.service.AiModelService;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ConfigCheckAspect {

    private final AiModelService aiModelService;
    private final MarketDataService marketDataService;
    private final NewsService newsService;

    private static final Map<ConfigType, String> MESSAGES = Map.of(
            ConfigType.AI_MODEL, "请先在【系统设置 → AI模型配置】中配置并启用AI模型",
            ConfigType.MARKET_DATA, "请先在【系统设置 → 数据源配置】中配置并启用行情数据源",
            ConfigType.NEWS_SOURCE, "请先在【系统设置 → 数据源配置】中配置并启用新闻源"
    );

    @Before("@annotation(checkConfig)")
    public void checkConfig(CheckConfig checkConfig) {
        long userId = StpUtil.getLoginIdAsLong();
        for (ConfigType type : checkConfig.required()) {
            boolean configured = switch (type) {
                case AI_MODEL -> aiModelService.hasActiveConfig(userId);
                case MARKET_DATA -> marketDataService.hasActiveConfig(userId);
                case NEWS_SOURCE -> newsService.hasActiveNewsSource(userId);
            };
            if (!configured) {
                String msg = MESSAGES.getOrDefault(type, "请先完成相关系统设置");
                log.warn("配置检查未通过: type={}, userId={}", type, userId);
                throw new RuntimeException(msg);
            }
        }
    }

}
