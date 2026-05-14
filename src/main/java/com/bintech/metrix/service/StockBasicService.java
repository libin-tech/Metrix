package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.PageResult;
import com.bintech.metrix.repository.entity.StockBasic;
import org.springframework.web.multipart.MultipartFile;

public interface StockBasicService {

    PageResult<StockBasic> pageQuery(String keyword, int page, int size);

    String importCsv(MultipartFile file);

    StockBasic getByTsCode(String stockCode);
}
