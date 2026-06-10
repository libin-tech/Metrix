package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.repository.entity.ChatSession;

import java.util.List;

public interface ChatSessionService {

    ChatSession createSession(Long userId, String sessionName);

    ChatSession getSessionById(Long id);

    ChatSession getSessionById(Long id, Long userId);

    List<ChatSessionVO> getUserSessions(Long userId);

    void deleteSession(Long id, Long userId);

    void deleteSessions(List<Long> ids, Long userId);

    void updateSessionTokenAndCount(Long sessionId, int tokens);

    void updateSessionName(Long sessionId, String sessionName);

}
