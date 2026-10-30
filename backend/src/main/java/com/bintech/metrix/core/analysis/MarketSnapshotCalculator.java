package com.bintech.metrix.core.analysis;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bintech.metrix.dto.response.DailyMarketIntelligence.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.bintech.metrix.constants.MarketIntelligenceConstants.*;
import static com.bintech.metrix.core.analysis.IntelligenceNumbers.*;

/** 同口径、同交易日计算市场指标；盘中成交额不与完整交易日比较。 */
@Component
public class MarketSnapshotCalculator {

    public MarketSnapshot calculate(JSONObject snapshot, List<JSONObject> history, Map<String, Object> indexData) {
        String date = date(snapshot.getStr("statDate"));
        if (date == null) throw new IllegalArgumentException("Missing snapshot trade date");
        List<HistoryPoint> points = history.stream().filter(this::sameScopeClose).map(this::point).filter(item -> item.date() != null)
                .sorted(Comparator.comparing(HistoryPoint::date)).toList();
        List<HistoryPoint> previous = continuousHistory(snapshot, history);
        boolean closed = "CLOSED".equals(snapshot.getStr("sessionPhase"));
        HistoryPoint today = point(snapshot);
        HistoryPoint yesterday = previous.stream().filter(item -> item.date().equals(date(snapshot.getStr("previousTradeDate")))).findFirst().orElse(null);
        Double turnover = positive(snapshot.get("totalTurnover"));
        Double changePct = closed && yesterday != null ? change(turnover, yesterday.turnover()) : null;
        Double versus5 = closed ? change(turnover, average(previous, SHORT_PERIOD, HistoryPoint::turnover)) : null;
        Double versus20 = closed ? change(turnover, average(previous, MEDIUM_PERIOD, HistoryPoint::turnover)) : null;
        Double upPct = today.upPct();
        Double total = today.upCount() == null || today.downCount() == null || today.flatCount() == null ? null
                : (double) today.upCount() + today.downCount() + today.flatCount();
        List<IndexView> indices = INDICES.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .map(entry -> index(entry.getValue(), entry.getKey(), indexData.get(entry.getKey()), date, date(snapshot.getStr("previousTradeDate")), closed)).toList();
        List<HistoryPoint> displayHistory = new ArrayList<>(points.stream().filter(item -> item.date().compareTo(date) < 0).toList());
        displayHistory.add(today);
        MarketSnapshot result = new MarketSnapshot(date, snapshot.getStr("sessionPhase", "UNKNOWN"),
                snapshot.getStr("scope", "UNKNOWN"), snapshot.getStr("source", "UNKNOWN"), snapshot.getStr("fetchedAt"),
                turnover, changePct, versus5, versus20, today.upCount(), today.downCount(), today.flatCount(),
                ratio(today.upCount(), today.downCount()), upPct,
                percent(today.downCount() == null ? null : today.downCount().doubleValue(), total),
                today.upCount() == null || today.downCount() == null ? null : today.upCount() - today.downCount(),
                difference(upPct, average(previous, SHORT_PERIOD, HistoryPoint::upPct)),
                difference(upPct, average(previous, MEDIUM_PERIOD, HistoryPoint::upPct)),
                count(snapshot.get("limitUp")), count(snapshot.get("limitDown")), count(snapshot.get("streakHeight")),
                number(snapshot.get("brokenRate")), MarketResearchRules.breadth(upPct), "UNKNOWN",
                volumePattern(indices, date, changePct), indices, displayHistory);
        return new MarketSnapshot(result.date(), result.phase(), result.scope(), result.source(), result.updatedAt(),
                result.totalTurnover(), result.turnoverChangePct(), result.turnoverVs5Pct(), result.turnoverVs20Pct(),
                result.upCount(), result.downCount(), result.flatCount(), result.upDownRatio(), result.upPct(), result.downPct(),
                result.breadthNet(), result.upPctVs5(), result.upPctVs20(), result.limitUp(), result.limitDown(),
                result.streakHeight(), result.brokenRate(), result.marketBreadth(), MarketResearchRules.sentiment(result),
                result.volumePattern(), result.indices(), result.history());
    }

    /** 只沿真实交易日历的前一日链取样，缺档不能冒充连续 5 / 20 个交易日。 */
    private List<HistoryPoint> continuousHistory(JSONObject snapshot, List<JSONObject> history) {
        Map<String, JSONObject> byDate = new TreeMap<>();
        history.stream().filter(row -> date(row.getStr("statDate")) != null && sameScopeClose(row))
                .forEach(row -> byDate.put(date(row.getStr("statDate")), row));
        List<HistoryPoint> result = new ArrayList<>();
        String cursor = date(snapshot.getStr("previousTradeDate"));
        while (cursor != null && byDate.containsKey(cursor) && result.size() < HISTORY_LIMIT) {
            JSONObject row = byDate.remove(cursor);
            result.addFirst(point(row));
            cursor = date(row.getStr("previousTradeDate"));
        }
        return result;
    }

    private boolean sameScopeClose(JSONObject row) {
        return SCOPE.equals(row.getStr("scope")) && "CLOSED".equals(row.getStr("sessionPhase"));
    }

    private Double ratio(Integer up, Integer down) {
        return up == null || down == null || down == 0 ? null : round(up.doubleValue() / down);
    }

    private Double difference(Double left, Double right) {
        return left == null || right == null ? null : round(left - right);
    }

    private String volumePattern(List<IndexView> indices, String date, Double turnoverChange) {
        Double benchmark = indices.stream().filter(index -> "sh000300".equals(index.symbol()) && date.equals(index.date()))
                .map(IndexView::changePct).filter(value -> value != null).findFirst().orElse(null);
        if (benchmark == null || turnoverChange == null) return "INSUFFICIENT";
        if (Math.abs(turnoverChange) < VOLUME_CHANGE || benchmark == 0) return "STABLE";
        return (benchmark > 0 ? "UP_" : "DOWN_") + (turnoverChange > 0 ? "EXPANDING" : "SHRINKING");
    }

    public HistoryPoint point(JSONObject data) {
        Integer up = count(data.get("up"));
        Integer down = count(data.get("down"));
        Integer flat = count(data.get("flat"));
        Double total = up == null || down == null || flat == null ? null : (double) up + down + flat;
        return new HistoryPoint(date(data.getStr("statDate")), positive(data.get("totalTurnover")),
                percent(up == null ? null : up.doubleValue(), total), up, down, flat, count(data.get("limitDown")));
    }

    private IndexView index(String symbol, String name, Object raw, String asOf, String previousDate, boolean closed) {
        JSONObject data = raw == null ? new JSONObject() : JSONUtil.parseObj(raw);
        JSONArray rows = data.getJSONArray("records");
        TreeMap<String, JSONObject> sorted = new TreeMap<>();
        if (rows != null) rows.toList(JSONObject.class).forEach(row -> addIndexRow(sorted, row, asOf));
        List<JSONObject> records = new ArrayList<>(sorted.values());
        JSONObject latest = records.isEmpty() ? new JSONObject() : records.getLast();
        String indexDate = date(latest.getStr("date"));
        Double close = positive(latest.get("close"));
        JSONObject prior = records.size() < 2 ? new JSONObject() : records.get(records.size() - 2);
        boolean previousAligned = !asOf.equals(indexDate) || previousDate != null && previousDate.equals(date(prior.getStr("date")));
        Double previous = previousAligned ? positive(prior.get("close")) : null;
        List<JSONObject> completed = records.stream().filter(row -> closed || !asOf.equals(date(row.getStr("date")))).toList();
        Double ma20 = ma(completed, MEDIUM_PERIOD);
        Double ma60 = ma(completed, LONG_PERIOD);
        Double ma120 = ma(completed, CYCLE_PERIOD);
        Double slope20 = slope(completed, MEDIUM_PERIOD);
        Double slope60 = slope(completed, LONG_PERIOD);
        Double slope120 = slope(completed, CYCLE_PERIOD);
        String structure = highLowStructure(completed);
        Double completedClose = completed.isEmpty() ? null : positive(completed.getLast().get("close"));
        String trend = trend(completedClose, ma20, ma60, ma120, slope20, slope60, slope120, structure);
        return new IndexView(symbol, name, indexDate, close, change(close, previous), ma20, ma60, ma120,
                slope20, slope60, slope120, structure, trend, completed.size(), completedClose);
    }

    private void addIndexRow(Map<String, JSONObject> sorted, JSONObject row, String asOf) {
        String date = date(row.getStr("date"));
        if (date == null || date.compareTo(asOf) > 0) return;
        sorted.put(date, row);
    }

    private Double ma(List<JSONObject> records, int period) {
        return average(records, period, row -> positive(row.get("close")));
    }

    private Double slope(List<JSONObject> records, int period) {
        if (records.size() < period + SHORT_PERIOD) return null;
        return change(ma(records, period), ma(records.subList(0, records.size() - SHORT_PERIOD), period));
    }

    private String highLowStructure(List<JSONObject> records) {
        if (records.size() < MEDIUM_PERIOD * 2) return "INSUFFICIENT";
        List<JSONObject> recent = records.subList(records.size() - MEDIUM_PERIOD, records.size());
        List<JSONObject> prior = records.subList(records.size() - MEDIUM_PERIOD * 2, records.size() - MEDIUM_PERIOD);
        if (records.subList(records.size() - MEDIUM_PERIOD * 2, records.size()).stream()
                .anyMatch(row -> positive(row.get("high")) == null || positive(row.get("low")) == null)) return "INSUFFICIENT";
        double high = recent.stream().mapToDouble(row -> positive(row.get("high"))).max().orElseThrow();
        double low = recent.stream().mapToDouble(row -> positive(row.get("low"))).min().orElseThrow();
        double priorHigh = prior.stream().mapToDouble(row -> positive(row.get("high"))).max().orElseThrow();
        double priorLow = prior.stream().mapToDouble(row -> positive(row.get("low"))).min().orElseThrow();
        if (high > priorHigh && low > priorLow) return "RISING";
        if (high < priorHigh && low < priorLow) return "FALLING";
        return "MIXED";
    }

    private String trend(Double close, Double ma20, Double ma60, Double ma120, Double slope20, Double slope60, Double slope120, String structure) {
        if (close == null || ma20 == null || ma60 == null || ma120 == null || slope20 == null || slope60 == null || slope120 == null) return "INSUFFICIENT";
        return MarketResearchRules.classify(close, List.of(
                new MarketResearchRules.Rule<>("UP", value -> value > ma20 && ma20 > ma60 && ma60 > ma120 && slope20 > 0 && slope60 > 0 && slope120 > 0 && "RISING".equals(structure)),
                new MarketResearchRules.Rule<>("DOWN", value -> value < ma20 && ma20 < ma60 && ma60 < ma120 && slope20 < 0 && slope60 < 0 && slope120 < 0 && "FALLING".equals(structure))
        ), "MIXED");
    }

    public static String date(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value, value.length() == 8 ? DateTimeFormatter.BASIC_ISO_DATE : DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeParseException error) {
            return null;
        }
    }
}
