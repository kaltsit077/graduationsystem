package com.example.graduation.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 管理员账号初始化器
 * 应用启动时自动创建默认管理员账号（如果不存在）
 */
@Component
public class AdminInitializer implements CommandLineRunner {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // 从配置文件读取管理员账号信息
    @Value("${admin.username:admin}")
    private String adminUsername;
    
    @Value("${admin.password:123456}")
    private String adminPassword;
    
    @Value("${admin.real-name:系统管理员}")
    private String adminRealName;
    
    @Value("${admin.auto-create:true}")
    private boolean autoCreate;
    
    @Override
    public void run(String... args) throws Exception {
        // 如果禁用了自动创建，直接返回
        if (!autoCreate) {
            System.out.println("========================================");
            System.out.println("管理员账号自动创建已禁用");
            System.out.println("========================================");
            return;
        }
        
        // 检查管理员账号是否存在
        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, adminUsername)
        );
        
        if (admin == null) {
            // 创建默认管理员账号
            User newAdmin = new User();
            newAdmin.setUsername(adminUsername);
            newAdmin.setPasswordHash(passwordEncoder.encode(adminPassword));
            newAdmin.setRealName(adminRealName);
            newAdmin.setRole(User.Role.ADMIN);
            newAdmin.setStatus(1);
            
            userMapper.insert(newAdmin);
            
            System.out.println("========================================");
            System.out.println("✅ 管理员账号已自动创建");
            System.out.println("========================================");
            System.out.println("用户名: " + adminUsername);
            System.out.println("密码: " + adminPassword);
            System.out.println("真实姓名: " + adminRealName);
            System.out.println("========================================");
            System.out.println("⚠️  请在生产环境中修改默认密码！");
            System.out.println("========================================");
        } else {
            System.out.println("========================================");
            System.out.println("ℹ️  管理员账号已存在，跳过创建");
            System.out.println("用户名: " + adminUsername);
            System.out.println("========================================");
        }
    }
}

