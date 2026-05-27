package com.bintech.metrix.dto.request;

import com.bintech.metrix.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private String description;

    private CommonStatus status;

    private Integer sortOrder;

}
