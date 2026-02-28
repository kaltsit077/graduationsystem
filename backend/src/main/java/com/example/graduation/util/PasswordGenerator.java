package com.example.graduation.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成工具类
 * 用于生成 BCrypt 加密的密码哈希
 * 
 * 使用方法：
 * 1. 运行 main 方法
 * 2. 输入要加密的密码
 * 3. 复制输出的哈希值到 SQL 脚本或数据库
 */
public class PasswordGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 默认密码
        String defaultPassword = "123456";
        String hash = encoder.encode(defaultPassword);
        System.out.println("========================================");
        System.out.println("密码生成工具");
        System.out.println("========================================");
        System.out.println("原始密码: " + defaultPassword);
        System.out.println("BCrypt 哈希: " + hash);
        System.out.println("========================================");
        System.out.println();
        System.out.println("SQL 插入示例：");
        System.out.println("INSERT INTO `user` (`username`, `password_hash`, `real_name`, `role`, `status`)");
        System.out.println("VALUES ('username', '" + hash + "', '真实姓名', 'STUDENT', 1);");
        System.out.println();
        
        // 如果提供了命令行参数，使用参数作为密码
        if (args.length > 0) {
            String customPassword = args[0];
            String customHash = encoder.encode(customPassword);
            System.out.println("自定义密码: " + customPassword);
            System.out.println("BCrypt 哈希: " + customHash);
        }
    }
    
    /**
     * 生成密码哈希
     */
    public static String generateHash(String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.encode(password);
    }
}

