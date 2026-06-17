package com.bintech.metrix.repository.dao;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.ChatSession;
import com.bintech.metrix.repository.mapper.ChatSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class ChatSessionDaoImpl implements ChatSessionDao {

    private final ChatSessionMapper baseMapper;

    @Override
    public int insert(ChatSession entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(ChatSession entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteById(Long id) {
        return baseMapper.deleteById(id);
    }

    @Override
    public int deleteByIdInAndUserId(List<Long> ids, Long userId) {
        if (CollUtil.isEmpty(ids) || userId == null) {
            log.warn("deleteByIdInAndUserId: ids is empty or userId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<ChatSession>()
                .in(ChatSession::getId, ids)
                .eq(ChatSession::getUserId, userId));
    }

    @Override
    public ChatSession selectById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public ChatSession selectByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            log.warn("selectByIdAndUserId: id or userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getId, id)
                .eq(ChatSession::getUserId, userId));
    }

    @Override
    public List<ChatSession> selectByUserIdOrderByUpdateTimeDesc(Long userId) {
        if (userId == null) {
            log.warn("selectByUserIdOrderByUpdateTimeDesc: userId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime));
    }

    @Override
    public ChatSession selectOldestByUserId(Long userId) {
        if (userId == null) {
            log.warn("selectOldestByUserId: userId is null");
            return null;
        }
        return baseMapper.selectOne(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .orderByAsc(ChatSession::getUpdateTime)
                .last("LIMIT 1"));
    }

    @Override
    public long countByUserId(Long userId) {
        if (userId == null) {
            log.warn("countByUserId: userId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId));
    }
}
