package com.bintech.metrix.repository.dao;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.SystemMenu;
import com.bintech.metrix.repository.mapper.SystemMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class SystemMenuDaoImpl implements SystemMenuDao {

    private final SystemMenuMapper baseMapper;

    @Override
    public int insert(SystemMenu entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(SystemMenu entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int deleteByParentId(Long parentId) {
        if (parentId == null) {
            log.warn("deleteByParentId: parentId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getParentId, parentId));
    }

    @Override
    public SystemMenu selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public List<SystemMenu> selectAll() {
        return baseMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .orderByAsc(SystemMenu::getSortOrder));
    }

    @Override
    public List<SystemMenu> selectBatchIds(Collection<Long> ids) {
        return baseMapper.selectBatchIds(ids);
    }

    @Override
    public List<SystemMenu> selectByParentId(Long parentId) {
        if (parentId == null) {
            log.warn("selectByParentId: parentId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getParentId, parentId));
    }

    @Override
    public long countByParentId(Long parentId) {
        if (parentId == null) {
            log.warn("countByParentId: parentId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getParentId, parentId));
    }

    @Override
    public long countByPermissionCode(String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            log.warn("countByPermissionCode: permissionCode is blank");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getPermissionCode, permissionCode));
    }

    @Override
    public long countByPermissionCodeExcludeId(String permissionCode, Long excludeId) {
        if (StrUtil.isBlank(permissionCode) || excludeId == null) {
            log.warn("countByPermissionCodeExcludeId: params invalid");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<SystemMenu>()
                .eq(SystemMenu::getPermissionCode, permissionCode)
                .ne(SystemMenu::getId, excludeId));
    }
}
