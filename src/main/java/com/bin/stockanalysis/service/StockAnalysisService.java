package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.request.StockAnalysisRequest;
import com.bin.stockanalysis.dto.response.StockAnalysisResponse;
import com.bin.stockanalysis.repository.entity.StockAnalysisRecord;

import java.util.List;

public interface StockAnalysisService {
    StockAnalysisResponse analyzeStock(StockAnalysisRequest request);
    StockAnalysisRecord getAnalysisById(Long id);
    List<StockAnalysisRecord> getAllAnalysisRecords();
    void cleanupExcessRecords();

}