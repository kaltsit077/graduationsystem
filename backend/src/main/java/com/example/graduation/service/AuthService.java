package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.dto.LoginRequest;
import com.example.graduation.dto.LoginResponse;
import com.example.graduation.dto.RegisterRequest;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.UserMapper;
import com.example.graduation.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 查询用户
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 生成JWT token
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
        
        return new LoginResponse(
                token,
                user.getRole().name(),
                user.getRealName(),
                user.getId()
        );
    }
    
    /**
     * 用户注册
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));
        
        if (existingUser != null) {
            throw new RuntimeException("该学号或教工号已被注册");
        }
        
        // 验证角色
        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
            // 不允许注册管理员账号
            if (role == User.Role.ADMIN) {
                throw new RuntimeException("不允许注册管理员账号");
            }
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的角色类型，只支持 STUDENT 或 TEACHER");
        }
        
        // 创建新用户
        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        newUser.setRealName(request.getRealName());
        newUser.setRole(role);
        newUser.setStatus(1); // 默认启用
        
        userMapper.insert(newUser);
        
        // 注册成功后自动登录
        String token = jwtUtil.generateToken(
                newUser.getId(),
                newUser.getUsername(),
                newUser.getRole().name()
        );
        
        return new LoginResponse(
                token,
                newUser.getRole().name(),
                newUser.getRealName(),
                newUser.getId()
        );
    }
}

