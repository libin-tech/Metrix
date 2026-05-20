package com.bintech.metrix.service;

import com.bintech.metrix.dto.request.BrokerAccountRequest;
import com.bintech.metrix.repository.entity.BrokerAccount;

import java.util.List;

/**
 * 券商账户管理服务接口
 */
public interface BrokerAccountService {

    /**
     * 获取所有券商账户
     *
     * @return 券商账户列表
     */
    List<BrokerAccount> getAllAccounts();

    /**
     * 创建券商账户
     *
     * @param request 券商账户请求（券商名必填，账号/备注选填）
     * @return 创建成功的账户
     */
    BrokerAccount createAccount(BrokerAccountRequest request);

    /**
     * 更新券商账户
     *
     * @param id      账户ID
     * @param request 券商账户请求
     * @return 更新后的账户
     */
    BrokerAccount updateAccount(Long id, BrokerAccountRequest request);

    /**
     * 删除券商账户（级联删除关联持仓）
     *
     * @param id 账户ID
     */
    void deleteAccount(Long id);
}
