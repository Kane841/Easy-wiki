package com.easywiki.websocket;

import com.easywiki.dto.websocket.WsMessage;
import com.easywiki.entity.Notification;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WsSessionManager {

    private static final Logger log = LoggerFactory.getLogger(WsSessionManager.class);

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();

    public WsSessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByUser.remove(userId);
        }
    }

    public boolean hasActiveSession(Long userId) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    public void sendToUser(Long userId, WsMessage message) {
        Set<WebSocketSession> sessions = sessionsByUser.getOrDefault(userId, Collections.emptySet());
        for (WebSocketSession session : sessions) {
            send(session, message);
        }
    }

    public void broadcastToUsers(Iterable<Long> userIds, WsMessage message) {
        for (Long userId : userIds) {
            sendToUser(userId, message);
        }
    }

    public void broadcastToGroupMembers(List<Long> memberUserIds, WsMessage message) {
        broadcastToUsers(memberUserIds, message);
    }

    public void pushNotification(Notification notification, String targetUrl) {
        var payload = new com.easywiki.dto.websocket.NotificationWsPayload(
                notification.getId(),
                notification.getType(),
                notification.getBody(),
                targetUrl,
                notification.getGroupId()
        );
        sendToUser(notification.getUserId(), new WsMessage("NOTIFICATION", payload));
    }

    public void send(WebSocketSession session, WsMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize WS message", e);
        } catch (IOException e) {
            log.warn("Failed to send WS message to session {}", session.getId(), e);
        }
    }

    public int sessionCount(Long userId) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        return sessions == null ? 0 : sessions.size();
    }
}
