package com.easywiki.websocket;

import com.easywiki.dto.websocket.ChatMessageWsPayload;
import com.easywiki.dto.websocket.WsMessage;
import com.easywiki.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class WsMessageHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WsMessageHandler.class);

    private final ObjectMapper objectMapper;
    private final WsSessionManager wsSessionManager;
    private final ChatService chatService;

    public WsMessageHandler(ObjectMapper objectMapper,
                            WsSessionManager wsSessionManager,
                            ChatService chatService) {
        this.objectMapper = objectMapper;
        this.wsSessionManager = wsSessionManager;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            wsSessionManager.register(userId, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            wsSessionManager.unregister(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            wsSessionManager.send(session, new WsMessage("ERROR", "未认证"));
            return;
        }

        try {
            WsMessage incoming = objectMapper.readValue(message.getPayload(), WsMessage.class);
            if (incoming.getType() == null) {
                wsSessionManager.send(session, new WsMessage("ERROR", "缺少消息类型"));
                return;
            }

            switch (incoming.getType()) {
                case "PING" -> wsSessionManager.send(session, new WsMessage("PONG", null));
                case "CHAT_MESSAGE" -> handleChatMessage(userId, incoming);
                default -> wsSessionManager.send(session, new WsMessage("ERROR", "未知消息类型: " + incoming.getType()));
            }
        } catch (Exception ex) {
            log.warn("Failed to handle WS message", ex);
            wsSessionManager.send(session, new WsMessage("ERROR", "消息处理失败"));
        }
    }

    private void handleChatMessage(Long userId, WsMessage incoming) {
        ChatMessageWsPayload payload = ChatMessageWsPayload.fromMap(incoming.getPayload());
        chatService.sendMessage(payload.getGroupId(), userId, payload.getContent());
    }
}
