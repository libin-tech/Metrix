package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.SystemRoleApi;
import com.bintech.metrix.repository.mapper.SystemRoleApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class SystemRoleApiDaoImpl implements SystemRoleApiDao {

    private final SystemRoleApiMapper baseMapper;

    @Override
    public int insert(SystemRoleApi entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int deleteByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("deleteByRoleId: roleId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<SystemRoleApi>()
                .eq(SystemRoleApi::getRoleId, roleId));
    }

    @Override
    public List<SystemRoleApi> selectByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("selectByRoleId: roleId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<SystemRoleApi>()
                .eq(SystemRoleApi::getRoleId, roleId));
    }
}
