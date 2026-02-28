package com.example.graduation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置
 * 生产环境建议通过 Nginx 配置 CORS，更高效
 * 
 * 配置方式：
 * 1. 通过环境变量 CORS_ALLOWED_ORIGINS 配置（多个域名用逗号分隔）
 * 2. 如果未配置，开发环境使用 *，生产环境需要配置具体域名
 */
@Configuration
public class CorsConfig {
    
    @Value("${cors.allowed-origins:}")
    private String allowedOrigins;
    
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;
    
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // 配置允许的源
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            // 通过环境变量配置的域名
            String[] origins = allowedOrigins.split(",");
            for (String origin : origins) {
                origin = origin.trim();
                if (!origin.isEmpty()) {
                    config.addAllowedOrigin(origin);
                }
            }
        } else if ("prod".equals(activeProfile)) {
            // 生产环境未配置域名，使用通配符（不推荐，但允许）
            // 建议通过环境变量 CORS_ALLOWED_ORIGINS 配置具体域名
            config.addAllowedOriginPattern("*");
            System.out.println("⚠️  警告：生产环境 CORS 使用通配符，建议配置具体域名");
        } else {
            // 开发环境使用通配符
            config.addAllowedOriginPattern("*");
        }
        
        // 允许的请求头
        config.addAllowedHeader("*");
        
        // 允许的请求方法
        config.addAllowedMethod("*");
        
        // 允许携带凭证（Cookie等）
        config.setAllowCredentials(true);
        
        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

