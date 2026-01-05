package com.inoka.inoka_app.security;

import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.inoka.inoka_app.service.CustomUserDetailsService;

@Component
public class JwtWebSocketInterceptor implements ChannelInterceptor {
    
    private static final String SESSION_USER_KEY = "authenticatedUser";
    
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(JwtWebSocketInterceptor.class);
    
    public JwtWebSocketInterceptor(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }
    
    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        // Use wrap() - standard approach for accessing STOMP headers
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        
        if (command == null) {
            return message;
        }
        
        logger.debug("Processing STOMP command: " + command);
        
        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnection(accessor);
        } else {
            // For non-CONNECT commands, restore user from session if not present
            restoreUserFromSession(accessor);
        }
        
        // Log subscriptions for debugging
        if (StompCommand.SUBSCRIBE.equals(command)) {
            logSubscription(accessor);
        }
        
        return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }
    
    /**
     * Authenticates the WebSocket connection by validating the JWT token
     * and loading the PlayerPrincipal via userDetailsService.
     */
    private void authenticateConnection(StompHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("Authorization");
        
        if (token == null || !token.startsWith("Bearer ")) {
            logger.warn("Missing or invalid Authorization header in WebSocket CONNECT");
            throw new IllegalArgumentException("Missing or invalid token");
        }
        
        // Remove "Bearer " prefix
        token = token.substring(7);
        
        if (!jwtUtil.validateToken(token)) {
            logger.warn("Invalid or expired token in WebSocket CONNECT");
            throw new IllegalArgumentException("Invalid or expired token");
        }
        
        // Extract user ID from token
        String userId = jwtUtil.getUserId(token);
        
        // Load full PlayerPrincipal using the same pattern as JwtAuthenticationFilter
        try {
            final PlayerPrincipal userDetails = userDetailsService.loadUserById(userId);
            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            
            // Set user on the accessor
            accessor.setUser(authenticationToken);
            
            // Store in session attributes for subsequent STOMP frames (SUBSCRIBE, SEND, etc.)
            // This is necessary because SockJS transports may not preserve the user across frames
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                sessionAttributes.put(SESSION_USER_KEY, authenticationToken);
                logger.debug("Stored user in session attributes");
            } else {
                logger.warn("Session attributes not available - user may not persist across frames");
            }
            
            logger.info("WebSocket user authenticated: " + userId);
        } catch (Exception e) {
            logger.warn("Failed to load user details for WebSocket authentication: " + userId, e);
            throw new IllegalArgumentException("Failed to authenticate user");
        }
    }
    
    /**
     * Restores the authenticated user from session attributes for non-CONNECT frames.
     * This handles SockJS fallback transports where user may not be automatically preserved.
     */
    private void restoreUserFromSession(StompHeaderAccessor accessor) {
        // If user is already set, no need to restore
        if (accessor.getUser() != null) {
            return;
        }
        
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            logger.debug("No session attributes available for user restoration");
            return;
        }
        
        Object storedUser = sessionAttributes.get(SESSION_USER_KEY);
        if (storedUser instanceof UsernamePasswordAuthenticationToken authToken) {
            accessor.setUser(authToken);
            logger.debug("Restored user from session: " + authToken.getName());
        }
    }
    
    /**
     * Logs subscription attempts for debugging purposes.
     */
    private void logSubscription(StompHeaderAccessor accessor) {
        logger.info("User subscribing to: " + accessor.getDestination());
        if (accessor.getUser() != null) {
            if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authToken) {
                logger.info("   User: " + authToken.getName());
            }
        } else {
            logger.warn("   No user in session for subscription!");
        }
    }
}
