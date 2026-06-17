package com.bintech.metrix.service.impl;

import com.bintech.metrix.dto.response.StockInfo;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockSearchServiceImpl implements StockSearchService {

    private final StockBasicDao stockBasicDao;

    @Override
    public List<StockInfo> searchStocks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String kw = keyword.trim().toUpperCase();

        List<StockBasic> list = stockBasicDao.selectLikeNameOrTsCode(kw);

        return list.stream()
                .map(s -> new StockInfo(s.getTsCode(), s.getName(), s.getMarket()))
                .collect(Collectors.toList());
    }
}
