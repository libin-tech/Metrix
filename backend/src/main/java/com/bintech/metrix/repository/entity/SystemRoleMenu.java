package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 角色-菜单关联实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_role_menu")
public class SystemRoleMenu extends BaseEntity {

    /** 角色ID */
    @TableField(value = "role_id")
    private Long roleId;

    /** 菜单ID */
    @TableField(value = "menu_id")
    private Long menuId;

}
