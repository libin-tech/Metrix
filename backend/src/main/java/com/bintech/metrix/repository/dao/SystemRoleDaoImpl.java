package com.bintech.metrix.repository.dao;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.repository.entity.SystemRole;
import com.bintech.metrix.repository.mapper.SystemRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class SystemRoleDaoImpl implements SystemRoleDao {

    private final SystemRoleMapper baseMapper;

    @Override
    public int insert(SystemRole entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(SystemRole entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public SystemRole selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public SystemRole selectByRoleCode(String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            log.warn("selectByRoleCode: roleCode is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<SystemRole>()
                .eq(SystemRole::getRoleCode, roleCode));
    }

    @Override
    public List<SystemRole> selectBatchIds(Collection<Long> ids) {
        return baseMapper.selectBatchIds(ids);
    }

    @Override
    public List<SystemRole> selectAllActiveOrderBySortOrder() {
        return baseMapper.selectList(new LambdaQueryWrapper<SystemRole>()
                .eq(SystemRole::getStatus, CommonStatus.ACTIVE)
                .orderByAsc(SystemRole::getSortOrder));
    }

    @Override
    public long countByRoleCode(String roleCode) {
        if (StrUtil.isBlank(roleCode)) {
            log.warn("countByRoleCode: roleCode is blank");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<SystemRole>()
                .eq(SystemRole::getRoleCode, roleCode));
    }

    @Override
    public IPage<SystemRole> selectRolePage(Page<SystemRole> page, String keyword) {
        LambdaQueryWrapper<SystemRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(SystemRole::getRoleCode, "ADMIN");
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SystemRole::getRoleName, keyword)
                    .or().like(SystemRole::getRoleCode, keyword));
        }
        wrapper.orderByAsc(SystemRole::getSortOrder);
        return baseMapper.selectPage(page, wrapper);
    }
}
