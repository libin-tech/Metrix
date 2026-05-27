package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 角色-接口关联实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_role_api")
public class SystemRoleApi extends BaseEntity {

    /** 角色ID */
    @TableField(value = "role_id")
    private Long roleId;

    /** 接口ID */
    @TableField(value = "api_id")
    private Long apiId;

}
