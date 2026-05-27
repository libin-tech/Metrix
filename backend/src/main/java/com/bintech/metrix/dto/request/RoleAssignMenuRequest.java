package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RoleAssignMenuRequest {

    @NotEmpty(message = "菜单ID列表不能为空")
    private List<Long> menuIds;

}
