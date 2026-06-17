package com.bintech.metrix.repository.dao;

import com.bintech.metrix.repository.entity.ChatSession;

import java.util.List;

public interface ChatSessionDao {
    int insert(ChatSession entity);
    int updateById(ChatSession entity);
    int deleteById(Long id);
    int deleteByIdInAndUserId(List<Long> ids, Long userId);
    ChatSession selectById(Long id);
    ChatSession selectByIdAndUserId(Long id, Long userId);
    List<ChatSession> selectByUserIdOrderByUpdateTimeDesc(Long userId);
    ChatSession selectOldestByUserId(Long userId);
    long countByUserId(Long userId);
}
