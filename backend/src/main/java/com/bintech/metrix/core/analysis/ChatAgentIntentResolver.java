package com.bintech.metrix.core.analysis;

import com.bintech.metrix.constants.ChatAgentConstants;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.ChatMessage;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.AiModelService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 先分类再用本地证券主数据校验，模型不能自行授权工具调用。 */
@Component
@RequiredArgsConstructor
public class ChatAgentIntentResolver {
    private final AiModelService aiModelService;
    private final StockBasicDao stockBasicDao;
    private final ObjectMapper objectMapper;

    @AllArgsConstructor
    public enum Intent { ANALYZE, CLARIFY, REFUSE }
    @AllArgsConstructor
    public enum Kind { STOCK, INDEX, SECTOR }
    public record Plan(Intent intent, Kind kind, List<String> targets, List<StockBasic> stocks,
                       String timeRange, List<String> dimensions) {}

    /** 当前输入始终独立接受范围校验，历史只辅助解析指代和追问。 */
    public Plan resolve(String content, List<ChatMessage> history, Long userId) {
        if (ChatAgentConstants.OUT_OF_SCOPE.matcher(content).find() || ChatAgentConstants.EXCLUDED_CODE.matcher(content).find()) return emptyPlan(Intent.REFUSE);
        List<Map<String, String>> context = history.stream().skip(Math.max(0, history.size() - 4))
                .map(m -> Map.of("role", m.getRole().name(), "content", m.getContent())).toList();
        String prompt = ChatAgentConstants.INTENT_PROMPT + objectMapper.writeValueAsString(Map.of("history", context, "input", content));
        String response = aiModelService.generateAnalysis(prompt, aiModelService.getActiveModelType(userId), userId);
        JsonNode json = objectMapper.readTree(response);
        Intent intent = Intent.valueOf(json.path("intent").asText());
        if (intent != Intent.ANALYZE) return emptyPlan(intent);
        Kind kind = Kind.valueOf(json.path("kind").asText());
        List<String> targets = readList(json.path("targets"));
        if (targets.isEmpty()) return emptyPlan(Intent.CLARIFY);
        if (targets.size() > ChatAgentConstants.MAX_TARGETS) throw new IllegalArgumentException("一次最多分析3个标的，请缩小分析范围。");
        List<StockBasic> candidates = switch (kind) {
            case STOCK -> stockBasicDao.selectByNamesOrCodes(targets);
            case SECTOR -> stockBasicDao.selectByIndustries(targets);
            case INDEX -> List.of();
        };
        List<StockBasic> stocks = new ArrayList<>();
        List<String> verified = new ArrayList<>();
        for (String target : targets) {
            switch (kind) {
                case STOCK -> {
                    StockBasic stock = findStock(target, candidates);
                    if (stock == null) return emptyPlan(Intent.CLARIFY);
                    if (!isAShare(stock)) return emptyPlan(Intent.REFUSE);
                    stocks.add(stock);
                    verified.add(stock.getName() + " " + stock.getTsCode());
                }
                case INDEX -> {
                    String index = ChatAgentConstants.INDEX_ALIASES.get(target);
                    if (index == null) return emptyPlan(Intent.CLARIFY);
                    verified.add(index);
                }
                case SECTOR -> {
                    List<StockBasic> members = candidates.stream().filter(stock -> target.equals(stock.getIndustry()))
                            .filter(ChatAgentIntentResolver::isAShare).toList();
                    if (members.isEmpty()) return emptyPlan(Intent.CLARIFY);
                    stocks.addAll(members.stream().limit(Math.max(1, ChatAgentConstants.MAX_TARGETS / targets.size())).toList());
                    verified.add(target);
                }
            }
        }
        return new Plan(intent, kind, verified, stocks, json.path("timeRange").asText("最近60个交易日"), readList(json.path("dimensions")));
    }

    private StockBasic findStock(String target, List<StockBasic> candidates) {
        List<StockBasic> matches = candidates.stream().filter(stock -> target.equals(stock.getName())
                || target.equals(stock.getSymbol()) || target.equalsIgnoreCase(stock.getTsCode())).toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    public static boolean isAShare(StockBasic stock) {
        return stock.getTsCode() != null && ChatAgentConstants.A_SHARE_CODE.matcher(stock.getTsCode()).matches();
    }

    private List<String> readList(JsonNode node) {
        if (!node.isArray()) throw new IllegalArgumentException("意图识别结果格式错误，请重试。");
        List<String> values = new ArrayList<>();
        node.forEach(value -> values.add(value.asText()));
        return values;
    }

    private Plan emptyPlan(Intent intent) {
        return new Plan(intent, Kind.STOCK, List.of(), List.of(), "", List.of());
    }
}
