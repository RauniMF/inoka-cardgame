package com.inoka.inoka_app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for serving the root SPA route.
 * 
 * Note: Other SPA routes (like /lobby, /game) are now handled by SpaRoutingFilter
 * to avoid conflicts with WebSocket and API endpoints. This controller only handles
 * the explicit root path.
 */
@Controller
public class SpaController {

    /**
     * Serve index.html for the root path.
     * All other SPA routing is handled by SpaRoutingFilter.
     */
    @GetMapping("/")
    public String serveRoot() {
        return "forward:/index.html";
    }
}
