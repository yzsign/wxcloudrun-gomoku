package com.gomoku.sync.config;

import com.gomoku.sync.websocket.GomokuWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GomokuWebSocketHandler gomokuWebSocketHandler;

    public WebSocketConfig(GomokuWebSocketHandler gomokuWebSocketHandler) {
        this.gomokuWebSocketHandler = gomokuWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
                .addHandler(gomokuWebSocketHandler, "/ws/gomoku")
                .setAllowedOrigins("*");
    }
}
