package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.bintech.metrix.dto.request.BrokerAccountRequest;
import com.bintech.metrix.repository.dao.BrokerAccountDao;
import com.bintech.metrix.repository.dao.PortfolioHoldingDao;
import com.bintech.metrix.repository.entity.BrokerAccount;
import com.bintech.metrix.service.BrokerAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrokerAccountServiceImpl implements BrokerAccountService {

    private final BrokerAccountDao brokerAccountDao;
    private final PortfolioHoldingDao portfolioHoldingDao;

    @Override
    public List<BrokerAccount> getAllAccounts() {
        Long userId = StpUtil.getLoginIdAsLong();
        return brokerAccountDao.selectByUserId(userId);
    }

    @Override
    @Transactional
    public BrokerAccount createAccount(BrokerAccountRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        BrokerAccount account = new BrokerAccount();
        account.setBrokerName(request.getBrokerName());
        account.setAccountNumber(request.getAccountNumber());
        account.setRemark(request.getRemark());
        account.setUserId(userId);
        account.setCreateTime(LocalDateTime.now());
        account.setUpdateTime(LocalDateTime.now());
        brokerAccountDao.insert(account);
        return account;
    }

    @Override
    @Transactional
    public BrokerAccount updateAccount(Long id, BrokerAccountRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        BrokerAccount account = brokerAccountDao.selectByIdAndUserId(id, userId);
        if (account == null) {
            throw new RuntimeException("券商账户不存在");
        }
        account.setBrokerName(request.getBrokerName());
        account.setAccountNumber(request.getAccountNumber());
        account.setRemark(request.getRemark());
        account.setUpdateTime(LocalDateTime.now());
        brokerAccountDao.updateById(account);
        return account;
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        BrokerAccount account = brokerAccountDao.selectByIdAndUserId(id, userId);
        if (account == null) {
            throw new RuntimeException("券商账户不存在");
        }
        List<com.bintech.metrix.repository.entity.PortfolioHolding> holdings = portfolioHoldingDao.selectByAccountId(id);
        if (!holdings.isEmpty()) {
            portfolioHoldingDao.deleteByAccountId(id);
            log.info("删除账户 {} 时级联删除了 {} 条持仓记录", id, holdings.size());
        }
        brokerAccountDao.deleteById(id);
    }
}
