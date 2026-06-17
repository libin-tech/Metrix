package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.dto.request.ApiCreateRequest;
import com.bintech.metrix.dto.request.ApiUpdateRequest;
import com.bintech.metrix.enums.CommonStatus;
import com.bintech.metrix.repository.dao.SystemApiDao;
import com.bintech.metrix.repository.dao.SystemMenuApiDao;
import com.bintech.metrix.repository.entity.SystemApi;
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

    private final SystemApiDao apiDao;
    private final SystemMenuApiDao menuApiDao;

    @Override
    public IPage<SystemApi> page(Integer page, Integer size, String keyword) {
        Page<SystemApi> pageParam = new Page<>(page, size);
        return apiDao.selectApiPage(pageParam, keyword);
    }

    @Override
    public SystemApi getById(Long id) {
        SystemApi api = apiDao.selectById(id);
        if (api == null) {
            throw new RuntimeException("接口不存在");
        }
        return api;
    }

    @Override
    @Transactional
    public SystemApi create(ApiCreateRequest request) {
        if (request.getPermissionCode() != null && apiDao.countByPermissionCode(request.getPermissionCode()) > 0) {
            throw new RuntimeException("权限标识已存在");
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
        apiDao.insert(api);
        return api;
    }

    @Override
    @Transactional
    public SystemApi update(Long id, ApiUpdateRequest request) {
        if (request.getPermissionCode() != null
                && apiDao.countByPermissionCodeExcludeId(request.getPermissionCode(), id) > 0) {
            throw new RuntimeException("权限标识已存在");
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
        apiDao.updateById(api);
        return api;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        menuApiDao.deleteByApiId(id);
        apiDao.deleteById(id);
    }

    @Override
    public List<SystemApi> listAll() {
        return apiDao.selectAll();
    }

}
