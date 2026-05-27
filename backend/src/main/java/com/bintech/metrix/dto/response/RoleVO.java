package com.bintech.metrix.dto.response;

import com.bintech.metrix.enums.CommonStatus;
import lombok.Data;

import java.util.List;

@Data
public class RoleVO {

    private Long id;

    private String roleCode;

    private String roleName;

    private String description;

    private Boolean isSystem;

    private CommonStatus status;

    private Integer sortOrder;

    private List<Long> menuIds;

    private List<Long> apiIds;

}
