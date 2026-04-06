package com.example.graduation.security;

import com.example.graduation.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        SecurityContextHolder.clearContext();
        
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                
                String roleUpper = role != null ? role.trim().toUpperCase() : "";
                String roleNoPrefix = roleUpper.startsWith("ROLE_") ? roleUpper.substring("ROLE_".length()) : roleUpper;
                String authority = roleNoPrefix.isEmpty() ? "" : "ROLE_" + roleNoPrefix;
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        authority.isEmpty()
                                ? Collections.emptyList()
                                : Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
                
                // 将用户信息存储到request属性中，方便后续使用
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", roleNoPrefix);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Token无效，记录错误日志以便调试
                logger.warn("JWT token 解析失败: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

