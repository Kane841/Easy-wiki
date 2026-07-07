package com.easywiki.config;

import com.easywiki.security.WsAuthInterceptor;
import com.easywiki.websocket.WsMessageHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WsMessageHandler wsMessageHandler;
    private final WsAuthInterceptor wsAuthInterceptor;

    public WebSocketConfig(WsMessageHandler wsMessageHandler, WsAuthInterceptor wsAuthInterceptor) {
        this.wsMessageHandler = wsMessageHandler;
        this.wsAuthInterceptor = wsAuthInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wsMessageHandler, "/ws")
                .addInterceptors(wsAuthInterceptor)
                .setAllowedOrigins("*");
    }
}
