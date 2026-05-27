package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("audit_log")
public class AuditLog {

    /** 主键ID */
    @TableId(value = "id")
    private Long id;

    /** 用户ID */
    @TableField(value = "user_id")
    private Long userId;

    /** 用户名 */
    @TableField(value = "username")
    private String username;

    /** 操作动作 */
    @TableField(value = "action")
    private String action;

    /** 资源类型 */
    @TableField(value = "resource_type")
    private String resourceType;

    /** 资源ID */
    @TableField(value = "resource_id")
    private String resourceId;

    /** 操作详情 */
    @TableField(value = "detail")
    private String detail;

    /** IP地址 */
    @TableField(value = "ip_address")
    private String ipAddress;

    /** 用户代理 */
    @TableField(value = "user_agent")
    private String userAgent;

    /** 创建时间 */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

}
