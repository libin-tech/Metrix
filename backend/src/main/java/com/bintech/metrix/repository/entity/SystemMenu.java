package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.enums.MenuType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 菜单实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_menu")
public class SystemMenu extends BaseEntity {

    /** 父菜单ID */
    @TableField(value = "parent_id")
    private Long parentId;

    /** 菜单名称 */
    @TableField(value = "menu_name")
    private String menuName;

    /** 权限标识 */
    @TableField(value = "permission_code")
    private String permissionCode;

    /** 菜单类型：DIRECTORY-目录 MENU-菜单 BUTTON-按钮 */
    @TableField(value = "menu_type")
    private MenuType menuType;

    /** 路由路径 */
    @TableField(value = "path")
    private String path;

    /** 组件路径 */
    @TableField(value = "component")
    private String component;

    /** 图标 */
    @TableField(value = "icon")
    private String icon;

    /** 排序号 */
    @TableField(value = "sort_order")
    private Integer sortOrder;

    /** 状态：ACTIVE-启用 DISABLED-禁用 */
    @TableField(value = "status")
    private CommonStatus status;

    /** 是否可见 */
    @TableField(value = "visible")
    private Boolean visible;

}
