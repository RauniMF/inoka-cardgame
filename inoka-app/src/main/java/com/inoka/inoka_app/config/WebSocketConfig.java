package com.inoka.inoka_app.config;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.inoka.inoka_app.security.JwtWebSocketInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    private final JwtWebSocketInterceptor jwtWebSocketInterceptor;
    
    public WebSocketConfig(JwtWebSocketInterceptor jwtWebSocketInterceptor) {
        this.jwtWebSocketInterceptor = jwtWebSocketInterceptor;
    }
    
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Server broadcasts to these destinations (no auth needed - server is trusted)
        config.enableSimpleBroker("/topic");
        
        // Client sends messages to these destinations (auth required in handlers)
        config.setApplicationDestinationPrefixes("/app");

        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .setAllowedOrigins("http://localhost:4200")
            .addInterceptors(new HandshakeInterceptor() {
                @Override
                public boolean beforeHandshake(@NonNull ServerHttpRequest request, 
                                               @NonNull ServerHttpResponse response,
                                               @NonNull WebSocketHandler wsHandler, 
                                               @NonNull Map<String, Object> attributes) {
                    // Initialize session attributes map - needed for storing user during CONNECT
                    return true;
                }

                @Override
                public void afterHandshake(@NonNull ServerHttpRequest request, 
                                           @NonNull ServerHttpResponse response,
                                           @NonNull WebSocketHandler wsHandler, 
                                           Exception exception) {
                    // No action needed
                }
            })
            .withSockJS();
    }
    
    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        // This intercepts ALL client→server messages (CONNECT, SUBSCRIBE, SEND)
        // Authentication is enforced here
        registration.interceptors(jwtWebSocketInterceptor);
    }
}
