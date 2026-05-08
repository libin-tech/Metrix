package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.response.StockInfo;

import java.util.List;

public interface StockSearchService {

    List<StockInfo> searchStocks(String keyword);
}
