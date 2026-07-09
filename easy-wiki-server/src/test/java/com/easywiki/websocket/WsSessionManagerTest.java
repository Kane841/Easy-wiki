package com.easywiki.websocket;

import com.easywiki.dto.websocket.WsMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

class WsSessionManagerTest {

    private WsSessionManager wsSessionManager;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        wsSessionManager = new WsSessionManager(objectMapper);
    }

    @Test
    void registerAndBroadcastToUser() throws Exception {
        Long userId = 42L;
        FakeWebSocketSession session = new FakeWebSocketSession("s1");

        wsSessionManager.register(userId, session);
        wsSessionManager.sendToUser(userId, new WsMessage("PONG", null));

        assertThat(wsSessionManager.sessionCount(userId)).isEqualTo(1);
        assertThat(session.sentMessages).hasSize(1);
        assertThat(session.sentMessages.get(0)).contains("\"type\":\"PONG\"");
    }

    @Test
    void unregisterRemovesSession() {
        Long userId = 7L;
        FakeWebSocketSession session = new FakeWebSocketSession("s2");
        wsSessionManager.register(userId, session);
        wsSessionManager.unregister(userId, session);
        assertThat(wsSessionManager.sessionCount(userId)).isZero();
    }

    @Test
    void hasActiveSessionReflectsRegistration() {
        Long userId = 99L;
        FakeWebSocketSession session = new FakeWebSocketSession("s3");
        assertThat(wsSessionManager.hasActiveSession(userId)).isFalse();
        wsSessionManager.register(userId, session);
        assertThat(wsSessionManager.hasActiveSession(userId)).isTrue();
    }

    private static class FakeWebSocketSession implements WebSocketSession {

        private final String id;
        private final List<String> sentMessages = new ArrayList<>();
        private final Map<String, Object> attributes = new ConcurrentHashMap<>();
        private boolean open = true;

        FakeWebSocketSession(String id) {
            this.id = id;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return URI.create("ws://localhost/ws");
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public void sendMessage(org.springframework.web.socket.WebSocketMessage<?> message) throws IOException {
            sentMessages.add(((TextMessage) message).getPayload());
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public void close(CloseStatus status) {
            open = false;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 8192;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 8192;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return Collections.emptyList();
        }

        @Override
        public HttpHeaders getHandshakeHeaders() {
            return new HttpHeaders();
        }
    }
}
