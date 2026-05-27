package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 菜单-接口关联实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_menu_api")
public class SystemMenuApi extends BaseEntity {

    /** 菜单ID */
    @TableField(value = "menu_id")
    private Long menuId;

    /** 接口ID */
    @TableField(value = "api_id")
    private Long apiId;

}
