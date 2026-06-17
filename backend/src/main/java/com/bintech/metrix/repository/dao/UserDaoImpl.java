package com.bintech.metrix.repository.dao;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bintech.metrix.enums.UserRole;
import com.bintech.metrix.repository.entity.User;
import com.bintech.metrix.repository.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class UserDaoImpl implements UserDao {

    private final UserMapper baseMapper;

    @Override
    public int insert(User entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(User entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public User selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public User selectByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("selectByUsername: username is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User selectByOpenid(String openid) {
        if (StrUtil.isBlank(openid)) {
            log.warn("selectByOpenid: openid is blank");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
    }

    @Override
    public long countByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            log.warn("countByUsername: username is blank");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public long countByUsernameAndNotId(String username, Long excludeId) {
        if (StrUtil.isBlank(username)) {
            log.warn("countByUsernameAndNotId: username is blank");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .ne(excludeId != null, User::getId, excludeId));
    }

    @Override
    public IPage<User> selectUserPage(Page<User> page, String keyword, List<Long> excludeUserIds) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(User::getRole, UserRole.ADMIN);
        if (CollUtil.isNotEmpty(excludeUserIds)) {
            wrapper.notIn(User::getId, excludeUserIds);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getOpenid, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return baseMapper.selectPage(page, wrapper);
    }
}
