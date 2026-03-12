package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.dto.UserCreateRequest;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * 创建用户（管理员功能）
     */
    @Transactional
    public User createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        User existing = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
        );
        
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 验证角色
        User.Role role;
        try {
            role = User.Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("无效的角色类型，必须是 STUDENT、TEACHER 或 ADMIN");
        }
        
        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setRole(role);
        user.setStatus(1); // 默认启用
        
        userMapper.insert(user);
        
        return user;
    }

    /** 默认重置密码 */
    public static final String DEFAULT_PASSWORD = "123456";

    /**
     * 管理员修改指定用户密码
     */
    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("新密码不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword.trim()));
        userMapper.updateById(user);
    }

    /**
     * 管理员批量重置密码为默认密码 123456
     */
    @Transactional
    public int resetPasswordsToDefault(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        String encoded = passwordEncoder.encode(DEFAULT_PASSWORD);
        int count = 0;
        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setPasswordHash(encoded);
                userMapper.updateById(user);
                count++;
            }
        }
        return count;
    }

    /**
     * 管理员批量删除账号（数据库外键会级联删除关联数据）
     */
    @Transactional
    public int deleteUsers(List<Long> userIds, Long currentAdminId) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        if (currentAdminId != null && userIds.contains(currentAdminId)) {
            throw new RuntimeException("不能删除当前登录账号");
        }

        // 禁止删除管理员账号（包括其他管理员）
        List<User> users = userMapper.selectBatchIds(userIds);
        for (User u : users) {
            if (u != null && u.getRole() == User.Role.ADMIN) {
                throw new RuntimeException("禁止删除管理员账号");
            }
        }

        return userMapper.deleteBatchIds(userIds);
    }
}

