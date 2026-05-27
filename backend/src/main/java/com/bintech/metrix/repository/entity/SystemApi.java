package com.bintech.metrix.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bintech.metrix.enums.CommonStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 接口实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("system_api")
public class SystemApi extends BaseEntity {

    /** 接口名称 */
    @TableField(value = "api_name")
    private String apiName;

    /** 接口路径 */
    @TableField(value = "api_path")
    private String apiPath;

    /** HTTP方法（GET/POST/PUT/DELETE） */
    @TableField(value = "http_method")
    private String httpMethod;

    /** 权限标识 */
    @TableField(value = "permission_code")
    private String permissionCode;

    /** 接口描述 */
    @TableField(value = "description")
    private String description;

    /** 状态：ACTIVE-启用 DISABLED-禁用 */
    @TableField(value = "status")
    private CommonStatus status;

}
