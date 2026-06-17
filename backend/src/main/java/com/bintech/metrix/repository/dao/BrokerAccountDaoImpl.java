package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.BrokerAccount;
import com.bintech.metrix.repository.mapper.BrokerAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class BrokerAccountDaoImpl implements BrokerAccountDao {

    private final BrokerAccountMapper baseMapper;

    @Override
    public int insert(BrokerAccount entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(BrokerAccount entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public BrokerAccount selectByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("BrokerAccountDaoImpl.selectByIdAndUserId: id or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<BrokerAccount>()
                .eq(BrokerAccount::getId, id)
                .eq(BrokerAccount::getUserId, userId));
    }

    @Override
    public List<BrokerAccount> selectByUserId(Long userId) {
        if (userId == null) {
            log.warn("BrokerAccountDaoImpl.selectByUserId: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<BrokerAccount>()
                .eq(BrokerAccount::getUserId, userId));
    }
}
