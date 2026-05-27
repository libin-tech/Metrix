package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.response.AuditLogVO;
import com.bintech.metrix.repository.entity.AuditLog;
import com.bintech.metrix.repository.mapper.AuditLogMapper;
import com.bintech.metrix.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public void log(Long userId, String username, String action, String resourceType, String resourceId, String detail, String ipAddress, String userAgent) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setDetail(detail);
        auditLog.setIpAddress(ipAddress);
        auditLog.setUserAgent(userAgent);
        auditLog.setCreateTime(LocalDateTime.now());
        auditLogMapper.insert(auditLog);
    }

    @Override
    public IPage<AuditLogVO> pageQuery(Integer pageNum, Integer size, Long userId, String action, String startTime, String endTime) {
        Page<AuditLog> page = new Page<>(pageNum, size);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(AuditLog::getUserId, userId);
        }
        if (action != null && !action.isEmpty()) {
            wrapper.eq(AuditLog::getAction, action);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(AuditLog::getCreateTime, LocalDateTime.parse(startTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(AuditLog::getCreateTime, LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        wrapper.orderByDesc(AuditLog::getId);

        IPage<AuditLog> result = auditLogMapper.selectPage(page, wrapper);
        IPage<AuditLogVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredLogs() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(AuditLog::getCreateTime, sevenDaysAgo);
        long deleted = auditLogMapper.delete(wrapper);
        if (deleted > 0) {
            log.info("已清理 {} 条过期审计日志", deleted);
        }
    }

    private AuditLogVO toVO(AuditLog log) {
        return AuditLogVO.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .detail(log.getDetail())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createTime(log.getCreateTime())
                .build();
    }
}
