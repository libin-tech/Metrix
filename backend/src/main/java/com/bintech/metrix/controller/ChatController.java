package com.bintech.metrix.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.bintech.metrix.dto.request.ChatSendRequest;
import com.bintech.metrix.dto.request.ChatSessionCreateRequest;
import com.bintech.metrix.dto.response.ApiResponse;
import com.bintech.metrix.dto.response.ChatMessageVO;
import com.bintech.metrix.dto.response.ChatSessionVO;
import com.bintech.metrix.annotation.CheckConfig;
import com.bintech.metrix.enums.ConfigType;
import com.bintech.metrix.service.ChatService;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@SaCheckLogin
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/session")
    @SaCheckPermission("chat:session:create")
    public ApiResponse<ChatSessionVO> createSession(@Valid @RequestBody ChatSessionCreateRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        ChatSessionVO session = chatService.createSession(userId, request.getSessionName());
        return ApiResponse.success("创建成功", session);
    }

    @GetMapping("/sessions")
    @SaCheckPermission("chat:session:list")
    public ApiResponse<List<ChatSessionVO>> listSessions() {
        long userId = StpUtil.getLoginIdAsLong();
        List<ChatSessionVO> sessions = chatService.listSessions(userId);
        return ApiResponse.success(sessions);
    }

    @DeleteMapping("/session/{id}")
    @SaCheckPermission("chat:session:delete")
    public ApiResponse<Void> deleteSession(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        chatService.deleteSession(id, userId);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping("/sessions/delete")
    @SaCheckPermission("chat:session:batch-delete")
    public ApiResponse<Void> deleteSessions(@RequestBody List<Long> ids) {
        long userId = StpUtil.getLoginIdAsLong();
        chatService.deleteSessions(ids, userId);
        return ApiResponse.success("批量删除成功", null);
    }

    @GetMapping("/session/{id}/messages")
    @SaCheckPermission("chat:message:list")
    public ApiResponse<List<ChatMessageVO>> getSessionMessages(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        List<ChatMessageVO> messages = chatService.getSessionMessages(id, userId);
        return ApiResponse.success(messages);
    }

    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckPermission("chat:message:send")
    @CheckConfig(required = {ConfigType.AI_MODEL, ConfigType.MARKET_DATA, ConfigType.NEWS_SOURCE})
    public SseEmitter sendMessage(@Valid @RequestBody ChatSendRequest request) {
        long userId = StpUtil.getLoginIdAsLong();
        return chatService.sendMessage(request.getSessionId(), userId, request.getContent());
    }
}
