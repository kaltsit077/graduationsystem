package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.TeacherProfile;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.TeacherProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherService {
    
    @Autowired
    private TeacherProfileMapper teacherProfileMapper;
    
    @Autowired
    private TagService tagService;
    
    /**
     * 完善导师信息
     */
    @Transactional
    public TeacherProfile updateProfile(Long userId, String title, String researchDirection, Integer maxStudentCount) {
        TeacherProfile profile = teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getUserId, userId)
        );
        
        if (profile == null) {
            profile = new TeacherProfile();
            profile.setUserId(userId);
            profile.setTitle(title);
            profile.setResearchDirection(researchDirection);
            profile.setMaxStudentCount(maxStudentCount != null ? maxStudentCount : 10);
            teacherProfileMapper.insert(profile);
        } else {
            profile.setTitle(title);
            profile.setResearchDirection(researchDirection);
            if (maxStudentCount != null) {
                profile.setMaxStudentCount(maxStudentCount);
            }
            teacherProfileMapper.updateById(profile);
        }
        
        // 自动生成标签（研究方向标签权重0.9）
        if (researchDirection != null) {
            tagService.generateTeacherTags(userId, researchDirection);
        }
        
        return profile;
    }
    
    /**
     * 获取导师信息
     */
    public TeacherProfile getProfile(Long userId) {
        return teacherProfileMapper.selectOne(
                new LambdaQueryWrapper<TeacherProfile>()
                        .eq(TeacherProfile::getUserId, userId)
        );
    }
    
    /**
     * 获取导师标签
     */
    public List<UserTag> getTags(Long userId) {
        return tagService.getUserTags(userId);
    }
    
    /**
     * 更新导师标签
     */
    @Transactional
    public void updateTags(Long userId, List<UserTag> tags) {
        tagService.updateUserTags(userId, tags);
    }
}

