package com.inoka.inoka_app.config;

import java.io.IOException;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that forwards SPA routes to index.html for client-side routing.
 * 
 * This filter handles the common pattern where a Spring Boot application serves
 * a Single Page Application (SPA) like Angular. When users navigate directly to
 * routes like /lobby or /game, the server needs to serve index.html so the SPA
 * can bootstrap and handle routing client-side.
 * 
 * Exclusions:
 * - API endpoints (/inoka/**)
 * - WebSocket endpoints (/ws/**)
 * - Actuator endpoints (/actuator/**)
 * - Static resources (any path with a file extension)
 * - Non-GET requests (POST, PUT, DELETE, etc.)
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaRoutingFilter extends OncePerRequestFilter {
    
    /**
     * Prefixes that should NOT be forwarded to the SPA.
     * These paths are handled by their respective controllers/handlers.
     */
    private static final List<String> EXCLUDED_PREFIXES = List.of(
        "/inoka/",      // REST API endpoints
        "/ws/",         // WebSocket/SockJS endpoints
        "/actuator/"    // Spring Boot Actuator endpoints
    );
    
    /**
     * Regex pattern to match file extensions.
     * Files with extensions are static resources (CSS, JS, images, etc.)
     * and should be served directly, not forwarded to SPA.
     */
    private static final String FILE_EXTENSION_PATTERN = ".*\\.[a-zA-Z0-9]+(\\?.*)?$";
    
    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // Only process GET requests - SPA routing is only for browser navigation
        if (!"GET".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Exclude API, WebSocket, and monitoring endpoints
        for (String prefix : EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                chain.doFilter(request, response);
                return;
            }
        }
        
        // Exclude static resources (files with extensions)
        if (path.matches(FILE_EXTENSION_PATTERN)) {
            chain.doFilter(request, response);
            return;
        }
        
        // This is a SPA route - forward to index.html
        request.getRequestDispatcher("/index.html").forward(request, response);
    }
}
