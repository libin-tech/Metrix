package com.bintech.metrix.repository.dao;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.repository.entity.SystemApi;
import com.bintech.metrix.repository.mapper.SystemApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class SystemApiDaoImpl implements SystemApiDao {

    private final SystemApiMapper baseMapper;

    @Override
    public int insert(SystemApi entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(SystemApi entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public SystemApi selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public SystemApi selectByPathAndMethod(String path, String method) {
        if (StrUtil.isBlank(path) || StrUtil.isBlank(method)) {
            log.warn("selectByPathAndMethod: path or method is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<SystemApi>()
                .eq(SystemApi::getApiPath, path)
                .eq(SystemApi::getHttpMethod, method));
    }

    @Override
    public List<SystemApi> selectAll() {
        return baseMapper.selectList(null);
    }

    @Override
    public IPage<SystemApi> selectApiPage(Page<SystemApi> page, String keyword) {
        LambdaQueryWrapper<SystemApi> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(SystemApi::getApiPath, keyword)
                    .or().like(SystemApi::getDescription, keyword)
                    .or().like(SystemApi::getApiName, keyword);
        }
        return baseMapper.selectPage(page, wrapper);
    }

    @Override
    public long countByPermissionCode(String permissionCode) {
        if (StrUtil.isBlank(permissionCode)) {
            log.warn("countByPermissionCode: permissionCode is blank");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<SystemApi>()
                .eq(SystemApi::getPermissionCode, permissionCode));
    }

    @Override
    public long countByPermissionCodeExcludeId(String permissionCode, Long excludeId) {
        if (StrUtil.isBlank(permissionCode) || excludeId == null) {
            log.warn("countByPermissionCodeExcludeId: params invalid");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<SystemApi>()
                .eq(SystemApi::getPermissionCode, permissionCode)
                .ne(SystemApi::getId, excludeId));
    }
}
