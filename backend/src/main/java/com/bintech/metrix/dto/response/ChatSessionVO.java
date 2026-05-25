package com.bintech.metrix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionVO {

    private Long id;
    private String sessionName;
    private Long userId;
    private Integer totalTokens;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
