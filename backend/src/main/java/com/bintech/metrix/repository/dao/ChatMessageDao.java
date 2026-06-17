package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.ChatMessage;

import java.util.List;

public interface ChatMessageDao {
    int insert(ChatMessage entity);
    int updateById(ChatMessage entity);
    int deleteBySessionId(Long sessionId);
    int deleteBySessionIdIn(List<Long> sessionIds);
    List<ChatMessage> selectBySessionIdOrderByCreateTimeAsc(Long sessionId);
    long countBySessionId(Long sessionId);
}
