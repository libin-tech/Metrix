package com.bintech.metrix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bintech.metrix.dto.response.AuditLogVO;
import com.bintech.metrix.repository.entity.AuditLog;

public interface AuditLogService {

    void log(Long userId, String username, String action, String resourceType, String resourceId, String detail, String ipAddress, String userAgent);

    IPage<AuditLogVO> pageQuery(Integer page, Integer size, Long userId, String action, String startTime, String endTime);

    void cleanupExpiredLogs();
}
