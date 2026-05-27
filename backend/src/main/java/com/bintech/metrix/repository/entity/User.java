package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    @TableField(value = "username")
    private String username;

    /**
     * 密码（MD5加密）
     */
    @TableField(value = "password")
    private String password;

    /**
     * 邮箱
     */
    @TableField(value = "email")
    private String email;

    /**
     * 角色（ADMIN/USER）
     */
    @TableField(value = "role")
    private UserRole role;

    /**
     * 是否激活
     */
    @TableField(value = "is_active")
    private Boolean isActive;

    /**
     * 用户状态（NORMAL/FROZEN）
     */
    @TableField(value = "status")
    private UserStatus status;

    /**
     * 冻结备注
     */
    @TableField(value = "freeze_reason")
    private String freezeReason;

    /**
     * 微信昵称
     */
    @TableField(value = "nickname")
    private String nickname;

    /**
     * 微信头像URL
     */
    @TableField(value = "avatar")
    private String avatar;

    /**
     * 微信OpenID
     */
    @TableField(value = "openid")
    private String openid;

    /**
     * 是否同意隐私政策
     */
    @TableField(value = "privacy_agreed")
    private Boolean privacyAgreed;

    /**
     * 同意隐私政策时间
     */
    @TableField(value = "privacy_agreed_time")
    private LocalDateTime privacyAgreedTime;
}
