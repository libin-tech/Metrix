package com.bintech.metrix.service;

import com.bintech.metrix.dto.response.ChatMessageVO;
import com.bintech.metrix.dto.response.ChatSessionVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {

    SseEmitter sendMessage(Long sessionId, Long userId, String content);

    List<ChatSessionVO> listSessions(Long userId);

    ChatSessionVO createSession(Long userId, String sessionName);

    void deleteSession(Long id);

    List<ChatMessageVO> getSessionMessages(Long sessionId);

}
