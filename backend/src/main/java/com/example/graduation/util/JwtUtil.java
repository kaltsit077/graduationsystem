package com.example.graduation.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration-minutes:120}")
    private Long expirationMinutes;
    
    private SecretKey getSigningKey() {
        // 尝试将密钥作为 Base64 编码字符串解码
        // 如果解码失败，则作为普通字符串使用
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret);
            // 确保密钥长度至少为 512 位（64 字节）以满足 HS512 要求
            if (keyBytes.length < 64) {
                // 如果解码后的密钥不够长，使用原始字符串的字节
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException e) {
            // 如果不是 Base64 格式，使用原始字符串的字节
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        
        // 如果密钥长度不够，使用 Keys.secretKeyFor 生成一个安全的密钥
        // 但这里我们假设配置文件中已经提供了足够长的密钥
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                "JWT 密钥长度不足。HS512 算法要求密钥至少 512 位（64 字节），当前密钥长度为 " + keyBytes.length + " 字节。"
            );
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 生成JWT token
     */
    public String generateToken(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMinutes * 60 * 1000);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 从token中获取Claims
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * 从token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("userId", Long.class);
    }
    
    /**
     * 从token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.getSubject();
    }
    
    /**
     * 从token中获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims.get("role", String.class);
    }
    
    /**
     * 验证token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从Authorization header中提取token
     */
    public String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

