package com.bintech.metrix.repository.dao;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.repository.entity.ChatMessage;
import com.bintech.metrix.repository.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
class ChatMessageDaoImpl implements ChatMessageDao {

    private final ChatMessageMapper baseMapper;

    @Override
    public int insert(ChatMessage entity) {
        return baseMapper.insert(entity);
    }

    @Override
    public int updateById(ChatMessage entity) {
        return baseMapper.updateById(entity);
    }

    @Override
    public int deleteBySessionId(Long sessionId) {
        if (sessionId == null) {
            log.warn("deleteBySessionId: sessionId is null");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
    }

    @Override
    public int deleteBySessionIdIn(List<Long> sessionIds) {
        if (CollUtil.isEmpty(sessionIds)) {
            log.warn("deleteBySessionIdIn: sessionIds is empty");
            return 0;
        }
        return baseMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .in(ChatMessage::getSessionId, sessionIds));
    }

    @Override
    public List<ChatMessage> selectBySessionIdOrderByCreateTimeAsc(Long sessionId) {
        if (sessionId == null) {
            log.warn("selectBySessionIdOrderByCreateTimeAsc: sessionId is null");
            return List.of();
        }
        return baseMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime));
    }

    @Override
    public long countBySessionId(Long sessionId) {
        if (sessionId == null) {
            log.warn("countBySessionId: sessionId is null");
            return 0;
        }
        return baseMapper.selectCount(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
    }
}
