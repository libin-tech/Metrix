package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.BrokerAccount;

import java.util.List;

public interface BrokerAccountDao {
    int insert(BrokerAccount entity);
    int updateById(BrokerAccount entity);
    int deleteById(Long id);
    BrokerAccount selectByIdAndUserId(Long id, Long userId);
    List<BrokerAccount> selectByUserId(Long userId);
}
