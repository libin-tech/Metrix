package com.bintech.metrix.core.analysis;

import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatAgentIntentResolverTest {
    private final AiModelService ai = mock(AiModelService.class);
    private final StockBasicDao stocks = mock(StockBasicDao.class);
    private final ChatAgentIntentResolver resolver = new ChatAgentIntentResolver(ai, stocks, new ObjectMapper());

    @ParameterizedTest
    @ValueSource(strings = {"分析美股苹果", "分析600519和港股腾讯", "北交所走势", "看看920001", "看看830799", "分析430047", "看看900901", "分析200001", "600519写代码", "Bitcoin走势", "分析600519.BJ", "分析期货"})
    void rejectsExcludedRequestsBeforeCallingAnyModelOrDataTool(String input) throws Exception {
        assertEquals(ChatAgentIntentResolver.Intent.REFUSE, resolver.resolve(input, List.of(), 1L).intent());
        verifyNoInteractions(ai, stocks);
    }

    @Test
    void validatesExchangeAgainstDatabaseInsteadOfTrustingTheModel() throws Exception {
        modelResult("STOCK", "宁德时代");
        StockBasic stock = stock("宁德时代", "300750.SZ");
        when(stocks.selectByNamesOrCodes(List.of("宁德时代"))).thenReturn(List.of(stock));
        var plan = resolver.resolve("看看宁德时代最近走势", List.of(), 1L);
        assertEquals(ChatAgentIntentResolver.Intent.ANALYZE, plan.intent());
        assertEquals(List.of(stock), plan.stocks());
        assertEquals("近一个月", plan.timeRange());
        assertEquals(List.of("技术面", "风险"), plan.dimensions());
    }

    @Test
    void neverAcceptsBeijingStockEvenIfModelClassifiesItAsAllowed() throws Exception {
        modelResult("STOCK", "北方企业");
        when(stocks.selectByNamesOrCodes(List.of("北方企业"))).thenReturn(List.of(stock("北方企业", "920001.BJ")));
        assertEquals(ChatAgentIntentResolver.Intent.REFUSE, resolver.resolve("分析北方企业", List.of(), 1L).intent());
    }

    @Test
    void asksForConfirmationWhenNameCannotBeVerified() throws Exception {
        modelResult("STOCK", "虚构公司");
        assertEquals(ChatAgentIntentResolver.Intent.CLARIFY, resolver.resolve("看看这家公司", List.of(), 1L).intent());
        verify(stocks, never()).selectByNameLike(anyString());
    }

    @Test
    void doesNotSilentlyDropUnresolvedComparisonTargets() throws Exception {
        when(ai.generateAnalysis(anyString(), any(), eq(1L))).thenReturn("{\"intent\":\"ANALYZE\",\"kind\":\"STOCK\",\"targets\":[\"贵州茅台\",\"未知公司\"],\"dimensions\":[\"风险\"]}");
        when(stocks.selectByNamesOrCodes(List.of("贵州茅台", "未知公司"))).thenReturn(List.of(stock("贵州茅台", "600519.SH")));
        assertEquals(ChatAgentIntentResolver.Intent.CLARIFY, resolver.resolve("对比茅台与未知公司", List.of(), 1L).intent());
    }

    @Test
    void distinguishesShanghaiIndexFromShenzhenStockCode() throws Exception {
        modelResult("STOCK", "000001");
        when(stocks.selectByNamesOrCodes(List.of("000001"))).thenReturn(List.of(stock("平安银行", "000001.SZ")));
        assertEquals("000001.SZ", resolver.resolve("看看000001", List.of(), 1L).stocks().getFirst().getTsCode());
        modelResult("INDEX", "000001.SH");
        assertEquals(List.of("上证指数"), resolver.resolve("看看上证指数000001.SH", List.of(), 1L).targets());
    }

    @Test
    void malformedModelResultFailsClosed() {
        when(ai.generateAnalysis(anyString(), any(), eq(1L))).thenReturn("当然可以帮你写代码");
        assertThrows(Exception.class, () -> resolver.resolve("分析茅台", List.of(), 1L));
        verifyNoInteractions(stocks);
    }

    @Test
    void industrySamplesAreVerifiedAndExcludeBeijing() throws Exception {
        modelResult("SECTOR", "银行");
        var sh = stock("银行一", "600000.SH");
        var bj = stock("银行二", "920001.BJ");
        sh.setIndustry("银行"); bj.setIndustry("银行");
        when(stocks.selectByIndustries(List.of("银行"))).thenReturn(List.of(sh, bj));
        assertEquals(List.of(sh), resolver.resolve("银行板块怎么样", List.of(), 1L).stocks());
    }

    private void modelResult(String kind, String target) {
        when(ai.generateAnalysis(anyString(), any(), eq(1L))).thenReturn("{\"intent\":\"ANALYZE\",\"kind\":\"" + kind + "\",\"targets\":[\"" + target + "\"],\"timeRange\":\"近一个月\",\"dimensions\":[\"技术面\",\"风险\"]}");
    }
    private StockBasic stock(String name, String code) {
        StockBasic stock = new StockBasic(); stock.setName(name); stock.setTsCode(code); stock.setSymbol(code.substring(0, 6)); return stock;
    }
}
