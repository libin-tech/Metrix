package com.bintech.metrix.dto.request;

import com.bintech.metrix.constants.SystemConstants;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BrokerAccountRequest {

    @NotBlank(message = "券商名称不能为空")
    @Size(max = SystemConstants.MAX_BROKER_NAME_LENGTH, message = "券商名称最长10个字")
    private String brokerName;

    @Size(max = SystemConstants.MAX_ACCOUNT_NUMBER_LENGTH, message = "券商账号最长30个字")
    private String accountNumber;

    @PositiveOrZero(message = "总本金不能为负数")
    @Digits(integer = SystemConstants.ACCOUNT_AMOUNT_INTEGER_DIGITS, fraction = SystemConstants.ACCOUNT_AMOUNT_FRACTION_DIGITS,
            message = "总本金最多支持13位整数和2位小数")
    private BigDecimal totalPrincipal;

    @Digits(integer = SystemConstants.ACCOUNT_AMOUNT_INTEGER_DIGITS, fraction = SystemConstants.ACCOUNT_AMOUNT_FRACTION_DIGITS,
            message = "清仓盈亏最多支持13位整数和2位小数")
    private BigDecimal realizedProfitLoss;

    @Size(max = SystemConstants.MAX_REMARK_LENGTH, message = "备注最长50个字")
    private String remark;
}
