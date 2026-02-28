package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.StudentProfile;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.StudentProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {
    
    @Autowired
    private StudentProfileMapper studentProfileMapper;
    
    @Autowired
    private TagService tagService;
    
    /**
     * 完善学生信息
     */
    @Transactional
    public StudentProfile updateProfile(Long userId, String major, String grade, String interestDesc) {
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, userId)
        );
        
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setMajor(major);
            profile.setGrade(grade);
            profile.setInterestDesc(interestDesc);
            studentProfileMapper.insert(profile);
        } else {
            profile.setMajor(major);
            profile.setGrade(grade);
            profile.setInterestDesc(interestDesc);
            studentProfileMapper.updateById(profile);
        }
        
        // 自动生成标签
        if (interestDesc != null || major != null) {
            tagService.generateStudentTags(userId, interestDesc, major);
        }
        
        return profile;
    }
    
    /**
     * 获取学生信息
     */
    public StudentProfile getProfile(Long userId) {
        return studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, userId)
        );
    }
    
    /**
     * 获取学生标签
     */
    public List<UserTag> getTags(Long userId) {
        return tagService.getUserTags(userId);
    }
    
    /**
     * 更新学生标签
     */
    @Transactional
    public void updateTags(Long userId, List<UserTag> tags) {
        tagService.updateUserTags(userId, tags);
    }
}

