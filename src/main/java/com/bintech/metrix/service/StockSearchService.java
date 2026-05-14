package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.StockInfo;

import java.util.List;

public interface StockSearchService {

    List<StockInfo> searchStocks(String keyword);
}
