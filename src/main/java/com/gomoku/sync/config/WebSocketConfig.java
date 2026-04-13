package com.gomoku.sync.config;

import com.gomoku.sync.websocket.GomokuWebSocketHandler;
import com.gomoku.sync.websocket.UserWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final @NonNull GomokuWebSocketHandler gomokuWebSocketHandler;
    private final @NonNull UserWebSocketHandler userWebSocketHandler;

    public WebSocketConfig(
            @NonNull GomokuWebSocketHandler gomokuWebSocketHandler,
            @NonNull UserWebSocketHandler userWebSocketHandler) {
        this.gomokuWebSocketHandler = gomokuWebSocketHandler;
        this.userWebSocketHandler = userWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry
                .addHandler(gomokuWebSocketHandler, "/ws/gomoku")
                .setAllowedOrigins("*");
        registry
                .addHandler(userWebSocketHandler, "/ws/user")
                .setAllowedOrigins("*");
    }
}
