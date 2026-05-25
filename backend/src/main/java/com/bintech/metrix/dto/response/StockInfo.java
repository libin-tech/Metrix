package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockInfo {

    private String stockCode;
    private String stockName;
    private String market;
}
