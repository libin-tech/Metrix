package com.bintech.metrix.integration;

import com.bintech.metrix.dto.response.DailyMarketIntelligence.Result;
import com.bintech.metrix.repository.dao.UserDao;
import com.bintech.metrix.service.DailyMarketIntelligenceService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.config.TaskManagementConfigUtils;
import org.springframework.test.context.TestConstructor;
import tools.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** 显式启用后读取真实配置与持仓；不修改持仓、不执行交易、不触发其他定时工作。 */
@EnabledIfEnvironmentVariable(named = "METRIX_LIVE_INTELLIGENCE_TEST", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.import=optional:file:.env[.properties]",
        "python.akshare-script-path=../python-service/akshare.py",
        "logging.level.com.baomidou.mybatisplus=WARN",
        "logging.level.com.bintech.metrix=INFO"
})
@Import(DailyMarketIntelligenceLiveTest.Configuration.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class DailyMarketIntelligenceLiveTest {
    private final DailyMarketIntelligenceService service;
    private final UserDao users;
    private final ObjectMapper mapper;

    @Test void realSourcesProduceTraceableReport() throws Exception {
        var user = users.selectByUsername(System.getenv().getOrDefault("METRIX_LIVE_USERNAME", "admin"));
        assertThat(user).isNotNull();
        await().atMost(Duration.ofMinutes(4)).pollInterval(Duration.ofSeconds(2))
                .until(() -> service.getReport(user.getId()).report() != null && !service.getReport(user.getId()).refreshing());
        Result result = service.getReport(user.getId());
        assertThat(result.report().marketSnapshot().scope()).isEqualTo("SH_SZ_BJ_A");
        assertThat(result.report().marketSnapshot().upCount()).isPositive();
        assertThat(result.report().marketSnapshot().date()).isNotBlank();
        assertThat(result.report().todayAction().triggerConditions()).isNotEmpty();
        String output = System.getenv("METRIX_LIVE_INTELLIGENCE_OUTPUT");
        if (output != null) Files.writeString(Path.of(output), mapper.writeValueAsString(result));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean static BeanFactoryPostProcessor disableScheduledWork() {
            return factory -> {
                BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
                String name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME;
                if (registry.containsBeanDefinition(name)) registry.removeBeanDefinition(name);
            };
        }
    }
}
