package com.bintech.metrix.dto.response;

import com.bintech.metrix.enums.ChatRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {

    private Long id;
    private Long sessionId;
    private ChatRole role;
    private String content;
    private Integer tokens;
    private String stockCode;
    private String stockName;
    private LocalDateTime createTime;
    private String steps;

}
