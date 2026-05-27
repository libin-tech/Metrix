package com.bintech.metrix.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RoleAssignApiRequest {

    @NotEmpty(message = "接口ID列表不能为空")
    private List<Long> apiIds;

}
