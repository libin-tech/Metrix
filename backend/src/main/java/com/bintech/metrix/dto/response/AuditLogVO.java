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
public class AuditLogVO {

    private Long id;

    private Long userId;

    private String username;

    private String action;

    private String resourceType;

    private String resourceId;

    private String detail;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime createTime;
}
