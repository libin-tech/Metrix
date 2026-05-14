package com.bin.stockanalysis.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bin.stockanalysis.repository.entity.AiModelConfig;
import com.bin.stockanalysis.repository.mapper.AiModelConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AiModelServiceImpl 单元测试类
 * 
 * <p>测试动态获取激活模型类型的功能，包括：
 * <ul>
 *   <li>当数据库存在激活配置时，正确返回模型类型</li>
 *   <li>当数据库不存在激活配置时，返回默认值OPENAI</li>
 *   <li>当数据库查询发生异常时，返回默认值OPENAI</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AiModelServiceImplTest {

    private static final Logger log = LoggerFactory.getLogger(AiModelServiceImplTest.class);

    @Mock
    private AiModelConfigMapper configMapper;

    private AiModelServiceImpl aiModelService;

    @BeforeEach
    void setUp() {
        aiModelService = new AiModelServiceImpl(configMapper);
        log.info("测试环境初始化完成");
    }

    /**
     * 测试当数据库存在激活的OLLAMA配置时，正确返回OLLAMA类型
     */
    @Test
    @DisplayName("获取激活模型类型 - 返回OLLAMA")
    void getActiveModelType_ShouldReturnOllama_WhenActiveConfigExists() {
        // 准备测试数据
        AiModelConfig activeConfig = new AiModelConfig();
        activeConfig.setId(1L);
        activeConfig.setModelType("OLLAMA");
        activeConfig.setModelName("llama3");
        activeConfig.setIsActive(true);
        activeConfig.setUpdateTime(LocalDateTime.now());

        // 模拟数据库查询
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activeConfig);

        // 执行测试
        String result = aiModelService.getActiveModelType();

        // 验证结果
        assertEquals("OLLAMA", result);
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：成功获取到激活模型类型: {}", result);
    }

    /**
     * 测试当数据库存在激活的OPENAI配置时，正确返回OPENAI类型
     */
    @Test
    @DisplayName("获取激活模型类型 - 返回OPENAI")
    void getActiveModelType_ShouldReturnOpenai_WhenActiveConfigExists() {
        // 准备测试数据
        AiModelConfig activeConfig = new AiModelConfig();
        activeConfig.setId(1L);
        activeConfig.setModelType("OPENAI");
        activeConfig.setModelName("gpt-4");
        activeConfig.setIsActive(true);
        activeConfig.setUpdateTime(LocalDateTime.now());

        // 模拟数据库查询
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activeConfig);

        // 执行测试
        String result = aiModelService.getActiveModelType();

        // 验证结果
        assertEquals("OPENAI", result);
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：成功获取到激活模型类型: {}", result);
    }

    /**
     * 测试当数据库不存在激活配置时，返回默认值OPENAI
     */
    @Test
    @DisplayName("获取激活模型类型 - 无激活配置返回默认OPENAI")
    void getActiveModelType_ShouldReturnDefaultOpenai_WhenNoActiveConfig() {
        // 模拟数据库查询返回空
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // 执行测试
        String result = aiModelService.getActiveModelType();

        // 验证结果
        assertEquals("OPENAI", result);
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：无激活配置时返回默认值: {}", result);
    }

    /**
     * 测试当数据库查询发生异常时，返回默认值OPENAI
     */
    @Test
    @DisplayName("获取激活模型类型 - 异常时返回默认OPENAI")
    void getActiveModelType_ShouldReturnDefaultOpenai_WhenExceptionOccurs() {
        // 模拟数据库查询抛出异常
        when(configMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // 执行测试
        String result = aiModelService.getActiveModelType();

        // 验证结果
        assertEquals("OPENAI", result);
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：异常情况下返回默认值: {}", result);
    }

    /**
     * 测试当激活配置的模型类型为空时，返回默认值OPENAI
     */
    @Test
    @DisplayName("获取激活模型类型 - 模型类型为空返回默认OPENAI")
    void getActiveModelType_ShouldReturnDefaultOpenai_WhenModelTypeIsNull() {
        // 准备测试数据（模型类型为空）
        AiModelConfig activeConfig = new AiModelConfig();
        activeConfig.setId(1L);
        activeConfig.setModelType(null);
        activeConfig.setIsActive(true);
        activeConfig.setUpdateTime(LocalDateTime.now());

        // 模拟数据库查询
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activeConfig);

        // 执行测试
        String result = aiModelService.getActiveModelType();

        // 验证结果
        assertEquals("OPENAI", result);
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：模型类型为空时返回默认值: {}", result);
    }

    /**
     * 测试当激活配置的模型类型为空字符串时，返回默认值OPENAI
     */
    @Test
    @DisplayName("获取激活模型类型 - 模型类型为空字符串返回默认OPENAI")
    void getActiveModelType_ShouldReturnDefaultOpenai_WhenModelTypeIsEmpty() {
        // 准备测试数据（模型类型为空字符串）
        AiModelConfig activeConfig = new AiModelConfig();
        activeConfig.setId(1L);
        activeConfig.setModelType("");
        activeConfig.setIsActive(true);
        activeConfig.setUpdateTime(LocalDateTime.now());

        // 模拟数据库查询
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activeConfig);

        // 执行测试
        String result = aiModelService.getActiveModelType();

        // 验证结果
        assertEquals("OPENAI", result);
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：模型类型为空字符串时返回默认值: {}", result);
    }

    /**
     * 测试获取激活配置方法
     */
    @Test
    @DisplayName("获取激活配置 - 返回配置对象")
    void getActiveConfig_ShouldReturnConfig_WhenActiveConfigExists() {
        // 准备测试数据
        AiModelConfig activeConfig = new AiModelConfig();
        activeConfig.setId(1L);
        activeConfig.setModelType("OLLAMA");
        activeConfig.setModelName("llama3");
        activeConfig.setApiBaseUrl("http://localhost:11434");
        activeConfig.setIsActive(true);
        activeConfig.setUpdateTime(LocalDateTime.now());

        // 模拟数据库查询
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(activeConfig);

        // 执行测试
        AiModelConfig result = aiModelService.getActiveConfig();

        // 验证结果
        assertNotNull(result);
        assertEquals("OLLAMA", result.getModelType());
        assertEquals("llama3", result.getModelName());
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：成功获取激活配置");
    }

    /**
     * 测试获取激活配置方法（无激活配置）
     */
    @Test
    @DisplayName("获取激活配置 - 无配置返回null")
    void getActiveConfig_ShouldReturnNull_WhenNoActiveConfig() {
        // 模拟数据库查询返回空
        when(configMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // 执行测试
        AiModelConfig result = aiModelService.getActiveConfig();

        // 验证结果
        assertNull(result);
        verify(configMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        log.info("测试通过：无激活配置时返回null");
    }

    /**
     * 测试获取所有激活配置方法
     */
    @Test
    @DisplayName("获取所有激活配置")
    void getActiveConfigs_ShouldReturnAllActiveConfigs() {
        // 准备测试数据
        AiModelConfig config1 = new AiModelConfig();
        config1.setId(1L);
        config1.setModelType("OPENAI");
        config1.setIsActive(true);

        AiModelConfig config2 = new AiModelConfig();
        config2.setId(2L);
        config2.setModelType("OLLAMA");
        config2.setIsActive(true);

        List<AiModelConfig> activeConfigs = List.of(config1, config2);

        // 模拟数据库查询
        when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(activeConfigs);

        // 执行测试
        List<AiModelConfig> result = aiModelService.getActiveConfigs();

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(configMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        log.info("测试通过：成功获取所有激活配置，数量: {}", result.size());
    }

    /**
     * 测试获取所有激活配置方法（无激活配置）
     */
    @Test
    @DisplayName("获取所有激活配置 - 无配置返回空列表")
    void getActiveConfigs_ShouldReturnEmptyList_WhenNoActiveConfig() {
        // 模拟数据库查询返回空列表
        when(configMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // 执行测试
        List<AiModelConfig> result = aiModelService.getActiveConfigs();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(configMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        log.info("测试通过：无激活配置时返回空列表");
    }

    /**
     * 测试计算置信度 - 高质量提示词和响应
     */
    @Test
    @DisplayName("计算置信度 - 高质量提示词和响应")
    void calculateConfidenceScore_ShouldReturnHighScore_ForQualityInput() {
        // 准备测试数据
        AiModelConfig config = new AiModelConfig();
        config.setTemperature(0.7); // 中等温度
        config.setModelName("test-model");

        // 高质量提示词（详细且丰富，超过500字符）
        String prompt = "请对股票 600036 进行综合分析。市场数据：开盘价: 100.50, 最高价: 105.80, 最低价: 98.20, 收盘价: 103.50, 成交量: 12560000, 涨跌幅: 2.3%\n\n最新新闻摘要：公司发布季度财报，净利润同比增长15.6%，市场反应积极。公司主营业务保持稳定增长，毛利率同比提升2个百分点。\n\n请提供以下分析内容：1. 基本面分析（包括财务指标、盈利能力、成长能力）2. 技术面分析（包括均线系统、MACD指标、RSI指标、布林带）3. 市场情绪分析（包括资金流向、投资者情绪）4. 投资建议（包括买入时机、目标价位、止损建议）5. 风险提示（包括市场风险、行业风险、公司特定风险）";

        // 高质量响应（结构化内容，超过500字符）
        String response = "## 基本面分析\n1. 市盈率：25.5，处于行业合理区间，低于行业平均水平\n2. 净利润增长率：15.6%，连续三个季度保持两位数增长\n3. ROE：12.8%，盈利能力稳健\n4. 资产负债率：45.2%，财务结构健康\n\n## 技术面分析\n1. 均线系统：MA5、MA10、MA20多头排列，形成上升趋势\n2. MACD指标：DIFF线上穿DEA线，形成金叉信号\n3. RSI指标：当前值58，处于正常区间，未出现超买超卖\n4. 布林带：价格运行于布林带中上轨之间，走势偏强\n\n## 市场情绪分析\n1. 资金流向：主力资金持续净流入\n2. 成交量：近期成交量温和放大\n3. 投资者情绪：看多情绪占比65%\n\n## 投资建议\n建议关注，短期目标价110元，止损价95元";

        // 执行测试（需要通过反射调用私有方法）
        try {
            var method = AiModelServiceImpl.class.getDeclaredMethod(
                    "calculateConfidenceScore", String.class, String.class, AiModelConfig.class);
            method.setAccessible(true);
            BigDecimal result = (BigDecimal) method.invoke(aiModelService, prompt, response, config);

            // 验证结果（高质量输入应获得较高置信度）
            assertNotNull(result);
            assertTrue(result.compareTo(new BigDecimal("0.75")) >= 0, 
                    "高质量输入应获得较高置信度，实际值: " + result);
            assertTrue(result.compareTo(new BigDecimal("0.95")) <= 0, 
                    "置信度不应超过0.95，实际值: " + result);
            log.info("测试通过：高质量输入置信度: {}", result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试计算置信度 - 低质量提示词和响应
     */
    @Test
    @DisplayName("计算置信度 - 低质量提示词和响应")
    void calculateConfidenceScore_ShouldReturnLowScore_ForPoorInput() {
        // 准备测试数据
        AiModelConfig config = new AiModelConfig();
        config.setTemperature(1.5); // 高温度（更随机）
        config.setModelName("test-model");

        // 低质量提示词（简短）
        String prompt = "分析股票";

        // 低质量响应（简短且无结构）
        String response = "这只股票还可以。";

        // 执行测试
        try {
            var method = AiModelServiceImpl.class.getDeclaredMethod(
                    "calculateConfidenceScore", String.class, String.class, AiModelConfig.class);
            method.setAccessible(true);
            BigDecimal result = (BigDecimal) method.invoke(aiModelService, prompt, response, config);

            // 验证结果（低质量输入应获得较低置信度）
            assertNotNull(result);
            assertTrue(result.compareTo(new BigDecimal("0.5")) >= 0, 
                    "置信度不应低于0.5，实际值: " + result);
            assertTrue(result.compareTo(new BigDecimal("0.7")) <= 0, 
                    "低质量输入应获得较低置信度，实际值: " + result);
            log.info("测试通过：低质量输入置信度: {}", result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试计算置信度 - 空提示词
     */
    @Test
    @DisplayName("计算置信度 - 空提示词")
    void calculateConfidenceScore_ShouldReturnLowScore_ForEmptyPrompt() {
        // 准备测试数据
        AiModelConfig config = new AiModelConfig();
        config.setTemperature(0.5);
        config.setModelName("test-model");

        String prompt = "";
        String response = "分析结果";

        // 执行测试
        try {
            var method = AiModelServiceImpl.class.getDeclaredMethod(
                    "calculateConfidenceScore", String.class, String.class, AiModelConfig.class);
            method.setAccessible(true);
            BigDecimal result = (BigDecimal) method.invoke(aiModelService, prompt, response, config);

            assertNotNull(result);
            assertTrue(result.compareTo(new BigDecimal("0.5")) >= 0);
            log.info("测试通过：空提示词置信度: {}", result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试计算置信度 - 空响应
     */
    @Test
    @DisplayName("计算置信度 - 空响应")
    void calculateConfidenceScore_ShouldReturnLowScore_ForEmptyResponse() {
        // 准备测试数据
        AiModelConfig config = new AiModelConfig();
        config.setTemperature(0.5);
        config.setModelName("test-model");

        String prompt = "详细分析股票";
        String response = "";

        // 执行测试
        try {
            var method = AiModelServiceImpl.class.getDeclaredMethod(
                    "calculateConfidenceScore", String.class, String.class, AiModelConfig.class);
            method.setAccessible(true);
            BigDecimal result = (BigDecimal) method.invoke(aiModelService, prompt, response, config);

            assertNotNull(result);
            assertTrue(result.compareTo(new BigDecimal("0.5")) >= 0);
            assertTrue(result.compareTo(new BigDecimal("0.6")) <= 0);
            log.info("测试通过：空响应置信度: {}", result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }

    /**
     * 测试置信度范围限制
     */
    @Test
    @DisplayName("计算置信度 - 范围限制在0.5-0.95之间")
    void calculateConfidenceScore_ShouldBeInRange() {
        // 测试边界情况：极高质量输入
        AiModelConfig config = new AiModelConfig();
        config.setTemperature(0.1); // 极低温度（非常确定）
        config.setModelName("test-model");

        String prompt = "非常详细的提示词".repeat(100); // 超长提示词
        String response = "1. 分析点一\n2. 分析点二\n3. 分析点三\n## 总结\n详细的分析结果".repeat(10); // 超长篇结构化响应

        try {
            var method = AiModelServiceImpl.class.getDeclaredMethod(
                    "calculateConfidenceScore", String.class, String.class, AiModelConfig.class);
            method.setAccessible(true);
            BigDecimal result = (BigDecimal) method.invoke(aiModelService, prompt, response, config);

            // 验证置信度不超过0.95
            assertTrue(result.compareTo(new BigDecimal("0.95")) <= 0, 
                    "置信度不应超过0.95，实际值: " + result);
            log.info("测试通过：极高质量输入置信度上限: {}", result);
        } catch (Exception e) {
            fail("反射调用失败: " + e.getMessage());
        }
    }
}