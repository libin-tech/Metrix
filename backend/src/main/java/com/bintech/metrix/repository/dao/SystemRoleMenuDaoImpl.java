package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.SystemRoleMenu;
import com.bintech.metrix.repository.mapper.SystemRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class SystemRoleMenuDaoImpl implements SystemRoleMenuDao {

    private final SystemRoleMenuMapper baseMapper;

    @Override
    public int insert(SystemRoleMenu entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int deleteByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("deleteByRoleId: roleId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<SystemRoleMenu>()
                .eq(SystemRoleMenu::getRoleId, roleId));
    }

    @Override
    public List<SystemRoleMenu> selectByRoleId(Long roleId) {
        if (roleId == null) {
            log.warn("selectByRoleId: roleId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<SystemRoleMenu>()
                .eq(SystemRoleMenu::getRoleId, roleId));
    }
}
