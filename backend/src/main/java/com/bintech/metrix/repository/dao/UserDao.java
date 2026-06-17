package com.bintech.metrix.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.repository.entity.User;

import java.util.List;

public interface UserDao {
    int insert(User entity);
    int updateById(User entity);
    int deleteById(Long id);
    User selectById(Long id);
    User selectByUsername(String username);
    User selectByOpenid(String openid);
    long countByUsername(String username);
    long countByUsernameAndNotId(String username, Long excludeId);
    IPage<User> selectUserPage(Page<User> page, String keyword, List<Long> excludeUserIds);
}
