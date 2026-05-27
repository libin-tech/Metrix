package com.bintech.metrix.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bintech.metrix.dto.request.ApiCreateRequest;
import com.bintech.metrix.dto.request.ApiUpdateRequest;
import com.bintech.metrix.repository.entity.SystemApi;

import java.util.List;

public interface SystemApiService {

    IPage<SystemApi> page(Integer page, Integer size, String keyword);

    SystemApi getById(Long id);

    SystemApi create(ApiCreateRequest request);

    SystemApi update(Long id, ApiUpdateRequest request);

    void delete(Long id);

    List<SystemApi> listAll();

}
