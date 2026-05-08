package com.bin.stockanalysis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModelTestResponse {

    private String modelName;
    private Long elapsedMs;
}
