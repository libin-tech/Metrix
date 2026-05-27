package com.bintech.metrix.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bintech.metrix.repository.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {

}
