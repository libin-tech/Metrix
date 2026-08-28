package com.bintech.metrix.config;

import com.bintech.metrix.constants.SystemConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "market-turnover")
public class MarketTurnoverProperties {

    private String tickflowKlinesScriptPath = "python-service/tickflow_klines.py";
    private String baostockMarketTurnoverScriptPath = "python-service/baostock_market_turnover.py";
    private int timeoutSeconds = SystemConstants.DEFAULT_TIMEOUT_SECONDS;
}
