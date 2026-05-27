package com.bintech.metrix.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.request.ApiCreateRequest;
import com.bintech.metrix.dto.request.ApiUpdateRequest;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.repository.entity.SystemApi;
import com.bintech.metrix.repository.entity.SystemMenuApi;
import com.bintech.metrix.repository.mapper.SystemApiMapper;
import com.bintech.metrix.repository.mapper.SystemMenuApiMapper;
import com.bintech.metrix.service.SystemApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemApiServiceImpl implements SystemApiService {

    private final SystemApiMapper apiMapper;
    private final SystemMenuApiMapper menuApiMapper;

    @Override
    public IPage<SystemApi> page(Integer page, Integer size, String keyword) {
        Page<SystemApi> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SystemApi> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(SystemApi::getApiName, keyword)
                    .or().like(SystemApi::getApiPath, keyword)
                    .or().like(SystemApi::getPermissionCode, keyword);
        }
        wrapper.orderByAsc(SystemApi::getId);
        return apiMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public SystemApi getById(Long id) {
        SystemApi api = apiMapper.selectById(id);
        if (api == null) {
            throw new RuntimeException("接口不存在");
        }
        return api;
    }

    @Override
    @Transactional
    public SystemApi create(ApiCreateRequest request) {
        if (request.getPermissionCode() != null) {
            LambdaQueryWrapper<SystemApi> existsWrapper = new LambdaQueryWrapper<SystemApi>()
                    .eq(SystemApi::getPermissionCode, request.getPermissionCode());
            if (apiMapper.selectCount(existsWrapper) > 0) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        SystemApi api = new SystemApi();
        api.setApiName(request.getApiName());
        api.setApiPath(request.getApiPath());
        api.setHttpMethod(request.getHttpMethod());
        api.setPermissionCode(request.getPermissionCode());
        api.setDescription(request.getDescription());
        api.setStatus(CommonStatus.ACTIVE);
        api.setCreateTime(LocalDateTime.now());
        api.setUpdateTime(LocalDateTime.now());
        apiMapper.insert(api);
        return api;
    }

    @Override
    @Transactional
    public SystemApi update(Long id, ApiUpdateRequest request) {
        if (request.getPermissionCode() != null) {
            LambdaQueryWrapper<SystemApi> existsWrapper = new LambdaQueryWrapper<SystemApi>()
                    .eq(SystemApi::getPermissionCode, request.getPermissionCode())
                    .ne(SystemApi::getId, id);
            if (apiMapper.selectCount(existsWrapper) > 0) {
                throw new RuntimeException("权限标识已存在");
            }
        }

        SystemApi api = getById(id);
        api.setApiName(request.getApiName());
        api.setApiPath(request.getApiPath());
        api.setHttpMethod(request.getHttpMethod());
        api.setPermissionCode(request.getPermissionCode());
        api.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            api.setStatus(request.getStatus());
        }
        api.setUpdateTime(LocalDateTime.now());
        apiMapper.updateById(api);
        return api;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        menuApiMapper.delete(new LambdaQueryWrapper<SystemMenuApi>().eq(SystemMenuApi::getApiId, id));
        apiMapper.deleteById(id);
    }

    @Override
    public List<SystemApi> listAll() {
        LambdaQueryWrapper<SystemApi> wrapper = new LambdaQueryWrapper<SystemApi>()
                .eq(SystemApi::getStatus, CommonStatus.ACTIVE)
                .orderByAsc(SystemApi::getId);
        return apiMapper.selectList(wrapper);
    }

}
