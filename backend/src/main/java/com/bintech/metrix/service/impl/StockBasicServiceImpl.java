package com.bintech.metrix.service.impl;

import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.response.PageResult;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.StockBasicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockBasicServiceImpl implements StockBasicService {

    private final StockBasicDao stockBasicDao;

    @Override
    public PageResult<StockBasic> pageQuery(String keyword, int page, int size) {
        IPage<StockBasic> p = stockBasicDao.selectStockPage(new Page<>(page, size), keyword);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    @Override
    @Transactional
    public String importCsv(MultipartFile file) {
        try (InputStreamReader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CsvData csvData = CsvUtil.getReader().read(reader);
            List<CsvRow> rows = csvData.getRows();
            if (rows.size() <= 1) {
                return "CSV文件为空或无数据行";
            }

            Set<String> incomingTsCodes = new HashSet<>();
            List<CsvRow> dataRows = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                CsvRow row = rows.get(i);
                String tsCode = row.get(0);
                if (tsCode == null || tsCode.isBlank()) continue;
                incomingTsCodes.add(tsCode.trim());
                dataRows.add(row);
            }

            Map<String, StockBasic> existingMap = new HashMap<>();
            if (!incomingTsCodes.isEmpty()) {
                List<StockBasic> existing = stockBasicDao.selectByTsCodeIn(new ArrayList<>(incomingTsCodes));
                for (StockBasic s : existing) {
                    existingMap.put(s.getTsCode(), s);
                }
            }

            List<StockBasic> insertList = new ArrayList<>();
            List<StockBasic> updateList = new ArrayList<>();

            for (CsvRow row : dataRows) {
                StockBasic entity = rowToEntity(row);
                if (entity == null) continue;

                StockBasic exist = existingMap.get(entity.getTsCode());
                if (exist != null) {
                    entity.setId(exist.getId());
                    updateList.add(entity);
                } else {
                    insertList.add(entity);
                }
            }

            for (StockBasic e : insertList) {
                stockBasicDao.insert(e);
            }
            for (StockBasic e : updateList) {
                stockBasicDao.updateById(e);
            }

            return String.format("导入完成：新增 %d 条，更新 %d 条，共 %d 条",
                    insertList.size(), updateList.size(), dataRows.size());
        } catch (Exception e) {
            log.error("CSV import failed", e);
            throw new RuntimeException("导入失败: " + e.getMessage());
        }
    }

    @Override
    public StockBasic getByTsCode(String stockCode) {
        return stockBasicDao.selectByTsCode(stockCode);
    }

    private StockBasic rowToEntity(CsvRow row) {
        try {
            StockBasic entity = new StockBasic();
            entity.setTsCode(row.get(0));
            entity.setSymbol(row.get(1));
            entity.setName(row.get(2));
            entity.setArea(row.get(3));
            entity.setIndustry(row.get(4));
            entity.setCnspell(row.get(5));
            entity.setMarket(row.get(6));
            String listDateStr = row.get(7);
            if (listDateStr != null && !listDateStr.isBlank()) {
                entity.setListDate(LocalDate.parse(listDateStr.trim(), DateTimeFormatter.BASIC_ISO_DATE));
            }
            entity.setActName(row.get(8));
            entity.setActEntType(row.get(9));
            return entity;
        } catch (Exception e) {
            log.warn("Skip invalid row: {}", row.getRawList(), e);
            return null;
        }
    }
}
