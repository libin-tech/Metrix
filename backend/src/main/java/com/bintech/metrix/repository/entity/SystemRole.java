package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bintech.metrix.enums.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 角色实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_role")
public class SystemRole extends BaseEntity {

    /** 角色编码 */
    @TableField(value = "role_code")
    private String roleCode;

    /** 角色名称 */
    @TableField(value = "role_name")
    private String roleName;

    /** 角色描述 */
    @TableField(value = "description")
    private String description;

    /** 是否系统内置角色 */
    @TableField(value = "is_system")
    private Boolean isSystem;

    /** 状态：ACTIVE-启用 DISABLED-禁用 */
    @TableField(value = "status")
    private CommonStatus status;

    /** 排序号 */
    @TableField(value = "sort_order")
    private Integer sortOrder;

}
