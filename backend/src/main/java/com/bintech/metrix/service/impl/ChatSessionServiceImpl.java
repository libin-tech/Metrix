package com.bintech.metrix.service.impl;

import com.bintech.metrix.constants.BusinessConstants;
import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.repository.dao.ChatMessageDao;
import com.bintech.metrix.repository.dao.ChatSessionDao;
import com.bintech.metrix.repository.entity.ChatSession;
import com.bintech.metrix.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionDao sessionDao;
    private final ChatMessageDao messageDao;

    @Override
    @Transactional
    public ChatSession createSession(Long userId, String sessionName) {
        Long count = sessionDao.countByUserId(userId);
        if (count >= BusinessConstants.MAX_CHAT_SESSIONS_PER_USER) {
            ChatSession oldest = sessionDao.selectOldestByUserId(userId);
            if (oldest != null) {
                deleteSession(oldest.getId(), userId);
            }
        }

        ChatSession session = new ChatSession();
        session.setSessionName(sessionName);
        session.setUserId(userId);
        session.setTotalTokens(0);
        session.setMessageCount(0);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionDao.insert(session);
        log.info("创建对话会话: id={}, name={}, userId={}", session.getId(), sessionName, userId);
        return session;
    }

    @Override
    public ChatSession getSessionById(Long id) {
        ChatSession session = sessionDao.selectById(id);
        if (session == null) {
            throw new RuntimeException("对话会话不存在");
        }
        return session;
    }

    @Override
    public ChatSession getSessionById(Long id, Long userId) {
        ChatSession session = sessionDao.selectByIdAndUserId(id, userId);
        if (session == null) {
            throw new RuntimeException("对话会话不存在");
        }
        return session;
    }

    @Override
    public List<ChatSessionVO> getUserSessions(Long userId) {
        List<ChatSession> sessions = sessionDao.selectByUserIdOrderByUpdateTimeDesc(userId);
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
    public void deleteSession(Long id, Long userId) {
        ChatSession session = sessionDao.selectByIdAndUserId(id, userId);
        if (session == null) {
            throw new RuntimeException("对话会话不存在");
        }
        messageDao.deleteBySessionId(id);
        sessionDao.deleteById(id);
        log.info("删除对话会话及其消息: sessionId={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public void deleteSessions(List<Long> ids, Long userId) {
        List<ChatSession> sessions = sessionDao.selectByUserIdOrderByUpdateTimeDesc(userId);
        if (sessions.isEmpty()) return;
        Set<Long> validIds = sessions.stream().map(ChatSession::getId).filter(ids::contains).collect(Collectors.toSet());
        if (validIds.isEmpty()) return;
        messageDao.deleteBySessionIdIn(validIds.stream().toList());
        sessionDao.deleteByIdInAndUserId(validIds.stream().toList(), userId);
        log.info("批量删除对话会话及其消息: ids={}, userId={}", validIds, userId);
    }

    @Override
    @Transactional
    public void updateSessionTokenAndCount(Long sessionId, int tokens) {
        ChatSession session = sessionDao.selectById(sessionId);
        if (session != null) {
            session.setTotalTokens(session.getTotalTokens() + tokens);
            session.setMessageCount(session.getMessageCount() + 1);
            session.setUpdateTime(LocalDateTime.now());
            sessionDao.updateById(session);
        }
    }

    @Override
    @Transactional
    public void updateSessionName(Long sessionId, String sessionName) {
        ChatSession session = sessionDao.selectById(sessionId);
        if (session != null) {
            session.setSessionName(sessionName);
            session.setUpdateTime(LocalDateTime.now());
            sessionDao.updateById(session);
        }
    }
}
