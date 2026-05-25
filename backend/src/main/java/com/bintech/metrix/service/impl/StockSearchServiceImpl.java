package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.response.StockInfo;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.repository.mapper.StockBasicMapper;
import com.bintech.metrix.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockSearchServiceImpl implements StockSearchService {

    private final StockBasicMapper stockBasicMapper;

    @Override
    public List<StockInfo> searchStocks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String kw = keyword.trim().toUpperCase();

        LambdaQueryWrapper<StockBasic> wrapper = new LambdaQueryWrapper<StockBasic>();
        wrapper.and(w -> w
                .like(StockBasic::getTsCode, kw)
                .or()
                .like(StockBasic::getSymbol, kw)
                .or()
                .like(StockBasic::getName, kw)
                .or()
                .like(StockBasic::getCnspell, kw));

        List<StockBasic> list = stockBasicMapper.selectList(wrapper);

        return list.stream()
                .map(s -> new StockInfo(s.getTsCode(), s.getName(), s.getMarket()))
                .collect(Collectors.toList());
    }
}
