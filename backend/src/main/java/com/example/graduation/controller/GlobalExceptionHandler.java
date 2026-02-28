package com.example.graduation.controller;

import com.example.graduation.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Object> handleRuntimeException(RuntimeException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return ApiResponse.error(e.getMessage());
    }
    
    @ExceptionHandler(BindException.class)
    public ApiResponse<Object> handleBindException(BindException e) {
        StringBuilder message = new StringBuilder();
        for (FieldError error : e.getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }
        logger.warn("参数验证失败: {}", message.toString());
        return ApiResponse.error(message.toString());
    }
    
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> handleException(Exception e) {
        logger.error("服务器内部错误", e);
        return ApiResponse.error("服务器内部错误: " + e.getMessage());
    }
}

