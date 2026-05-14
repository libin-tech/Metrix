package com.bin.stockanalysis.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StockAnalysisStatus {


    ANALYZING("ANALYZING","分析中"),
    COMPLETED("COMPLETED","分析完成"),
    FAILED("FAILED","分析失败");

    @EnumValue
    @JsonValue
    private final String code;
    private final String description;

}
