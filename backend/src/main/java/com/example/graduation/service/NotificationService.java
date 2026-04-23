package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.Notification;
import com.example.graduation.mapper.NotificationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    
    @Autowired
    private NotificationMapper notificationMapper;
    
    /**
     * 创建通知
     */
    public Notification createNotification(Long userId, String type, String title, String content, Long relatedId) {
        return createNotification(userId, type, title, content, relatedId, null, 0);
    }

    /**
     * 创建通知（可指定已读状态）
     */
    public Notification createNotification(Long userId, String type, String title, String content, Long relatedId, int isRead) {
        return createNotification(userId, type, title, content, relatedId, null, isRead);
    }

    public Notification createNotification(Long userId, String type, String title, String content, Long relatedId,
                                          String collabStage, int isRead) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(isRead);
        notification.setRelatedId(relatedId);
        notification.setCollabStage(collabStage);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insert(notification);
        return notification;
    }
    
    /**
     * 获取用户通知列表（可按已读状态、类型、关联ID及条数过滤）
     */
    public List<Notification> getUserNotifications(Long userId, Boolean isRead, String type, Long relatedId,
                                                   String collabStage, Integer limit) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        
        if (isRead != null) {
            wrapper.eq(Notification::getIsRead, isRead ? 1 : 0);
        }
        
        if (type != null && !type.isBlank()) {
            wrapper.eq(Notification::getType, type);
        }
        
        if (relatedId != null) {
            wrapper.eq(Notification::getRelatedId, relatedId);
        }

        if (collabStage != null && !collabStage.isBlank()) {
            wrapper.eq(Notification::getCollabStage, collabStage);
        }
        
        wrapper.orderByDesc(Notification::getCreatedAt);
        
        if (limit != null && limit > 0) {
            wrapper.last("LIMIT " + limit);
        }
        
        return notificationMapper.selectList(wrapper);
    }
    
    /**
     * 标记通知为已读
     */
    public void markAsRead(Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }
    
    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = getUserNotifications(userId, false, null, null, null, null);
        notifications.forEach(notification -> {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        });
    }
    
    /**
     * 获取未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0)
        );
    }
}

