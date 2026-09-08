package com.bintech.metrix.service.impl;

import com.bintech.metrix.dto.response.StockSyncResult;
import com.bintech.metrix.constants.StockSyncConstants;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.StockBasic;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 将完整有效的上游列表作为一次事务增量合并，只写上游提供的字段。 */
@Service
@RequiredArgsConstructor
public class StockBasicSyncWriter {
    private final StockBasicDao stockBasicDao;

    @Transactional
    public StockSyncResult apply(List<StockBasic> incoming) {
        Map<String, StockBasic> unique = validateAndDeduplicate(incoming);
        Map<String, StockBasic> existing = stockBasicDao.selectByTsCodeIn(new ArrayList<>(unique.keySet())).stream()
                .collect(Collectors.toMap(StockBasic::getTsCode, Function.identity()));
        List<StockBasic> inserts = new ArrayList<>();
        List<StockBasic> updates = new ArrayList<>();
        for (StockBasic row : unique.values()) {
            StockBasic current = existing.get(row.getTsCode());
            if (current == null) {
                inserts.add(row);
                continue;
            }
            if (!Objects.equals(row.getName(), current.getName()) || !Objects.equals(row.getSymbol(), current.getSymbol())) {
                row.setId(current.getId());
                row.setVersion(current.getVersion());
                updates.add(row);
            }
        }
        stockBasicDao.insertBatch(inserts);
        stockBasicDao.updateBatch(updates);
        return new StockSyncResult(false, inserts.size(), updates.size(), unique.size() - inserts.size() - updates.size(), unique.size());
    }

    private Map<String, StockBasic> validateAndDeduplicate(List<StockBasic> incoming) {
        if (incoming == null || incoming.isEmpty()) {
            throw new IllegalArgumentException("上游标的列表为空，同步已取消");
        }
        Map<String, StockBasic> result = new LinkedHashMap<>();
        for (StockBasic source : incoming) {
            validateRow(source);
            StockBasic row = new StockBasic();
            row.setTsCode(source.getTsCode());
            row.setSymbol(source.getSymbol());
            row.setName(source.getName().trim());
            result.put(row.getTsCode(), row);
        }
        return result;
    }

    private void validateRow(StockBasic row) {
        if (row == null || row.getTsCode() == null || !row.getTsCode().matches(StockSyncConstants.CODE_PATTERN)) {
            throw new IllegalArgumentException("上游标的代码无效，同步已取消");
        }
        if (!row.getTsCode().substring(0, StockSyncConstants.SYMBOL_LENGTH).equals(row.getSymbol()) || row.getName() == null || row.getName().isBlank()) {
            throw new IllegalArgumentException("上游标的名称或代码缺失，同步已取消");
        }
    }
}
