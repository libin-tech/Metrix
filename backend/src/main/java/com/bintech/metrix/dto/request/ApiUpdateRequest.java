package com.bintech.metrix.dto.request;

import com.bintech.metrix.enums.CommonStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApiUpdateRequest {

    @NotBlank(message = "接口名称不能为空")
    private String apiName;

    @NotBlank(message = "接口路径不能为空")
    private String apiPath;

    @NotBlank(message = "HTTP方法不能为空")
    private String httpMethod;

    private String permissionCode;

    private String description;

    private CommonStatus status;

}
