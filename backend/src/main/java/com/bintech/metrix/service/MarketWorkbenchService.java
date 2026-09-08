package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.MarketWorkbenchResponse;
import com.bintech.metrix.dto.response.PortfolioHoldingVO;

import java.util.List;
import java.util.Map;

/** 组合单标的行情工作台所需的数据和技术指标。 */
public interface MarketWorkbenchService {

    MarketWorkbenchResponse load(String thscode, Long userId);

    List<PortfolioHoldingVO> holdings();

    Map<String, Object> anomalies(Long userId);
}
