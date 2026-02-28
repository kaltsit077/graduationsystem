package com.example.graduation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员端账号列表项（避免直接序列化 User 实体导致的枚举/日期问题）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListItem {
    private Long id;
    private String username;
    private String realName;
    /** 密码显示：仅展示掩码，不返回真实密码（系统存储为哈希不可逆） */
    private String passwordDisplay;
    private String role;
    private Integer status;
    private String createdAt;
}
