package com.bintech.metrix.dto.request;

import com.bintech.metrix.constants.SystemConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrokerAccountRequest {

    @NotBlank(message = "券商名称不能为空")
    @Size(max = SystemConstants.MAX_BROKER_NAME_LENGTH, message = "券商名称最长10个字")
    private String brokerName;

    @Size(max = SystemConstants.MAX_ACCOUNT_NUMBER_LENGTH, message = "券商账号最长30个字")
    private String accountNumber;

    @Size(max = SystemConstants.MAX_REMARK_LENGTH, message = "备注最长50个字")
    private String remark;
}
