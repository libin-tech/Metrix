package com.bintech.metrix.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.repository.entity.ChatMessage;
import com.bintech.metrix.repository.entity.ChatSession;
import com.bintech.metrix.repository.mapper.ChatMessageMapper;
import com.bintech.metrix.repository.mapper.ChatSessionMapper;
import com.bintech.metrix.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    @Override
    @Transactional
    public ChatSession createSession(Long userId, String sessionName) {
        Long count = sessionMapper.selectCount(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId));
        if (count >= BusinessConstants.MAX_CHAT_SESSIONS_PER_USER) {
            ChatSession oldest = sessionMapper.selectOne(
                    new LambdaQueryWrapper<ChatSession>()
                            .eq(ChatSession::getUserId, userId)
                            .orderByAsc(ChatSession::getUpdateTime)
                            .last("LIMIT 1"));
            if (oldest != null) {
                deleteSession(oldest.getId());
            }
        }

        ChatSession session = new ChatSession();
        session.setSessionName(sessionName);
        session.setUserId(userId);
        session.setTotalTokens(0);
        session.setMessageCount(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        log.info("创建对话会话: id={}, name={}, userId={}", session.getId(), sessionName, userId);
        return session;
    }

    @Override
    public ChatSession getSessionById(Long id) {
        ChatSession session = sessionMapper.selectById(id);
        if (session == null) {
            throw new RuntimeException("对话会话不存在");
        }
        return session;
    }

    @Override
    public List<ChatSessionVO> getUserSessions(Long userId) {
        List<ChatSession> sessions = sessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime));
        return sessions.stream()
                .map(s -> ChatSessionVO.builder()
                        .id(s.getId())
                        .sessionName(s.getSessionName())
                        .userId(s.getUserId())
                        .totalTokens(s.getTotalTokens())
                        .messageCount(s.getMessageCount())
                        .createTime(s.getCreateTime())
                        .updateTime(s.getUpdateTime())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void deleteSession(Long id) {
        messageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, id));
        sessionMapper.deleteById(id);
        log.info("删除对话会话及其消息: sessionId={}", id);
    }

    @Override
    @Transactional
    public void updateSessionTokenAndCount(Long sessionId, int tokens) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setTotalTokens(session.getTotalTokens() + tokens);
            session.setMessageCount(session.getMessageCount() + 1);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    @Override
    @Transactional
    public void updateSessionName(Long sessionId, String sessionName) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setSessionName(sessionName);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }
}
