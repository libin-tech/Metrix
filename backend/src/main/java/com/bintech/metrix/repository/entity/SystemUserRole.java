package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户-角色关联实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_user_role")
public class SystemUserRole extends BaseEntity {

    /** 用户ID */
    @TableField(value = "user_id")
    private Long userId;

    /** 角色ID */
    @TableField(value = "role_id")
    private Long roleId;

}
