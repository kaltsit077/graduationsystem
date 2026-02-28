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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        String token = jwtUtil.extractToken(authHeader);
        
        if (token != null && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                
                // 确保角色格式正确（转换为大写，并添加 ROLE_ 前缀）
                String roleUpper = role != null ? role.toUpperCase() : "";
                String authority = "ROLE_" + roleUpper;
                
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority(authority))
                );
                
                // 将用户信息存储到request属性中，方便后续使用
                request.setAttribute("userId", userId);
                request.setAttribute("username", username);
                request.setAttribute("role", roleUpper);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // Token无效，记录错误日志以便调试
                logger.warn("JWT token 解析失败: " + e.getMessage());
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

