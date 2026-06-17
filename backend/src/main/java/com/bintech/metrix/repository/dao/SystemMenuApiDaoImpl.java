package com.bintech.metrix.repository.dao;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.SystemMenuApi;
import com.bintech.metrix.repository.mapper.SystemMenuApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class SystemMenuApiDaoImpl implements SystemMenuApiDao {

    private final SystemMenuApiMapper baseMapper;

    @Override
    public int insert(SystemMenuApi entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int deleteByMenuId(Long menuId) {
        if (menuId == null) {
            log.warn("deleteByMenuId: menuId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<SystemMenuApi>()
                .eq(SystemMenuApi::getMenuId, menuId));
    }

    @Override
    public int deleteByApiId(Long apiId) {
        if (apiId == null) {
            log.warn("deleteByApiId: apiId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<SystemMenuApi>()
                .eq(SystemMenuApi::getApiId, apiId));
    }

    @Override
    public List<SystemMenuApi> selectAll() {
        return baseMapper.selectList(null);
    }

    @Override
    public List<SystemMenuApi> selectByMenuIdIn(List<Long> menuIds) {
        if (CollUtil.isEmpty(menuIds)) {
            log.warn("selectByMenuIdIn: menuIds is empty");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<SystemMenuApi>()
                .in(SystemMenuApi::getMenuId, menuIds));
    }
}
