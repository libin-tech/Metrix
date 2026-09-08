package com.bintech.metrix.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 同花顺金融数据脚本配置。 */
@Data
@Component
@ConfigurationProperties(prefix = "financial-data")
public class FinancialDataProperties {
    private String scriptPath = "python-service/fuyao.py";
}
