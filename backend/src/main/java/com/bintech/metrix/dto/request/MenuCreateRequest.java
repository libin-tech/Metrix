package com.bintech.metrix.dto.request;

import com.bintech.metrix.enums.MenuType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MenuCreateRequest {

    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    private String permissionCode;

    @NotNull(message = "菜单类型不能为空")
    private MenuType menuType;

    private String path;

    private String component;

    private String icon;

    private Integer sortOrder;

    private Boolean visible;

}
