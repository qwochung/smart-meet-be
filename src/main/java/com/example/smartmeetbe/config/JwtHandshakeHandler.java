package com.example.smartmeetbe.config;

import com.example.smartmeetbe.security.UserDetailsServiceImpl;
import com.example.smartmeetbe.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeFailureException;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;

/**
 * Handshake handler that authenticates incoming WebSocket/STOMP handshake using JWT.
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        HttpServletRequest servletRequest =
                ((ServletServerHttpRequest) request).getServletRequest();

        String token = servletRequest.getParameter("token");

        if (token == null || token.isBlank()) {
            String authHeader = servletRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (token == null || token.isBlank()) {
            throw new HandshakeFailureException("Missing token");
        }

        try {
            String username = jwtUtil.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtUtil.isTokenValid(token, userDetails)) {
                throw new HandshakeFailureException("Invalid JWT token");
            }

            final String principalName = username;
            return () -> principalName;

        } catch (HandshakeFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new HandshakeFailureException("JWT validation failed: " + e.getMessage(), e);
        }
    }
}


