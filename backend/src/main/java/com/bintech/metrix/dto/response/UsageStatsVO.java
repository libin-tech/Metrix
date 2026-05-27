package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatsVO {

    private Long userId;

    private String username;

    private String nickname;

    private LocalDate statDate;

    private Integer analysisCount;

    private Integer reviewCount;
}
