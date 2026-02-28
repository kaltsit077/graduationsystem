package com.example.graduation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求（规范：学号/教工号仅字母数字，姓名仅中文/字母/空格/·，角色仅 STUDENT/TEACHER）
 */
public class RegisterRequest {

    /** 学号/教工号：仅字母、数字，3-50 位 */
    @NotBlank(message = "学号或教工号不能为空")
    @Size(min = 3, max = 50, message = "学号或教工号长度必须在3-50个字符之间")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "学号或教工号只能包含字母和数字")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20个字符之间")
    private String password;

    /** 真实姓名：中文、字母、空格、·（间隔号），1-50 位 */
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 1, max = 50, message = "真实姓名长度必须在1-50个字符之间")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z\\s·]+$", message = "真实姓名只能包含中文、字母、空格或·")
    private String realName;

    @NotBlank(message = "角色不能为空")
    @Pattern(regexp = "^(STUDENT|TEACHER)$", message = "角色只能为学生或导师")
    private String role;
    
    public RegisterRequest() {
    }
    
    public RegisterRequest(String username, String password, String realName, String role) {
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getRealName() {
        return realName;
    }
    
    public void setRealName(String realName) {
        this.realName = realName != null ? realName.trim() : null;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role != null ? role.trim().toUpperCase() : null;
    }
}


