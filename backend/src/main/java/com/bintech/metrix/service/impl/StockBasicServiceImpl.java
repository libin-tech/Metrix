package com.bintech.metrix.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.constants.ApiConstants;
import com.bintech.metrix.dto.response.PageResult;
import com.bintech.metrix.dto.response.StockSyncResult;
import com.bintech.metrix.repository.dao.StockBasicDao;
import com.bintech.metrix.repository.entity.StockBasic;
import com.bintech.metrix.service.MarketDataService;
import com.bintech.metrix.service.StockBasicService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.stereotype.Service;

/** 股票资料查询及按完整代码增量同步；远程取数完成后才进入写库事务。 */
@Service
@RequiredArgsConstructor
public class StockBasicServiceImpl implements StockBasicService {
    private final StockBasicDao stockBasicDao;
    private final MarketDataService marketDataService;
    private final StockBasicSyncWriter stockBasicSyncWriter;

    @Override
    public PageResult<StockBasic> pageQuery(String keyword, int page, int size) {
        IPage<StockBasic> result = stockBasicDao.selectStockPage(new Page<>(page, size), keyword);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    /** 同一进程串行同步，写库事务在释放同步锁前提交，防止重复点击交错插入。 */
    @Override
    public synchronized StockSyncResult sync(Long userId) {
        if (!marketDataService.hasActiveConfig(userId)) {
            return new StockSyncResult(true, 0, 0, 0, 0);
        }
        var response = JSONUtil.parseObj(marketDataService.fetchTickerData(userId));
        if (!ApiConstants.STATUS_SUCCESS.equals(response.getStr(ApiConstants.KEY_STATUS))) {
            throw new IllegalStateException("标的列表获取失败，本次同步未写入数据库");
        }
        var rows = response.getJSONArray(ApiConstants.KEY_DATA);
        return stockBasicSyncWriter.apply(rows == null ? List.of() : rows.toList(StockBasic.class));
    }

    @Override
    public StockBasic getByTsCode(String stockCode) {
        return stockBasicDao.selectByTsCode(stockCode);
    }
}
