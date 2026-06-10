package com.bintech.metrix.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.dto.request.BrokerAccountRequest;
import com.bintech.metrix.repository.entity.BrokerAccount;
import com.bintech.metrix.repository.entity.PortfolioHolding;
import com.bintech.metrix.repository.mapper.BrokerAccountMapper;
import com.bintech.metrix.repository.mapper.PortfolioHoldingMapper;
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

    private final BrokerAccountMapper brokerAccountMapper;
    private final PortfolioHoldingMapper portfolioHoldingMapper;

    @Override
    public List<BrokerAccount> getAllAccounts() {
        Long userId = StpUtil.getLoginIdAsLong();
        LambdaQueryWrapper<BrokerAccount> wrapper = new LambdaQueryWrapper<BrokerAccount>()
                .eq(BrokerAccount::getUserId, userId);
        return brokerAccountMapper.selectList(wrapper);
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
        brokerAccountMapper.insert(account);
        return account;
    }

    @Override
    @Transactional
    public BrokerAccount updateAccount(Long id, BrokerAccountRequest request) {
        Long userId = StpUtil.getLoginIdAsLong();
        BrokerAccount account = brokerAccountMapper.selectOne(
                new LambdaQueryWrapper<BrokerAccount>()
                        .eq(BrokerAccount::getId, id)
                        .eq(BrokerAccount::getUserId, userId));
        if (account == null) {
            throw new RuntimeException("券商账户不存在");
        }
        account.setBrokerName(request.getBrokerName());
        account.setAccountNumber(request.getAccountNumber());
        account.setRemark(request.getRemark());
        account.setUpdateTime(LocalDateTime.now());
        brokerAccountMapper.updateById(account);
        return account;
    }

    @Override
    @Transactional
    public void deleteAccount(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        BrokerAccount account = brokerAccountMapper.selectOne(
                new LambdaQueryWrapper<BrokerAccount>()
                        .eq(BrokerAccount::getId, id)
                        .eq(BrokerAccount::getUserId, userId));
        if (account == null) {
            throw new RuntimeException("券商账户不存在");
        }
        LambdaQueryWrapper<PortfolioHolding> wrapper = new LambdaQueryWrapper<PortfolioHolding>()
                .eq(PortfolioHolding::getAccountId, id)
                .eq(PortfolioHolding::getUserId, userId);
        List<PortfolioHolding> holdings = portfolioHoldingMapper.selectList(wrapper);
        if (!holdings.isEmpty()) {
            portfolioHoldingMapper.delete(wrapper);
            log.info("删除账户 {} 时级联删除了 {} 条持仓记录", id, holdings.size());
        }
        brokerAccountMapper.deleteById(id);
    }
}
