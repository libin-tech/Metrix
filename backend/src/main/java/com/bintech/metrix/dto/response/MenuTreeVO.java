package com.bintech.metrix.dto.response;

import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.enums.MenuType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MenuTreeVO {

    private Long id;

    private Long parentId;

    private String menuName;

    private String permissionCode;

    private MenuType menuType;

    private String path;

    private String component;

    private String icon;

    private Integer sortOrder;

    private Boolean visible;

    private CommonStatus status;

    private List<MenuTreeVO> children = new ArrayList<>();

    private List<Long> apiIds = new ArrayList<>();

}
