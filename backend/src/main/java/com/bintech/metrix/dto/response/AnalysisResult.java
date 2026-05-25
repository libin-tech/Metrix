package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalysisResult {
    private String content;
    private Integer totalTokens;
}
