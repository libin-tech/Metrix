package com.bin.stockanalysis.service;

import com.bin.stockanalysis.dto.response.PageResult;
import com.bin.stockanalysis.repository.entity.StockBasic;
import org.springframework.web.multipart.MultipartFile;

public interface StockBasicService {

    PageResult<StockBasic> pageQuery(String keyword, int page, int size);

    String importCsv(MultipartFile file);
}
