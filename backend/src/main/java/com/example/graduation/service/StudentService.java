package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.StudentProfile;
import com.example.graduation.entity.User;
import com.example.graduation.entity.UserTag;
import com.example.graduation.dto.UserTagResponse;
import com.example.graduation.mapper.StudentProfileMapper;
import com.example.graduation.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentService {
    
    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TagService tagService;
    
    /**
     * 完善学生信息
     */
    @Transactional
    public StudentProfile updateProfile(Long userId, String realName, String major, String majorCourses, String grade, String interestDesc, String tagMode) {
        if (realName != null && !realName.trim().isEmpty()) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setRealName(realName.trim());
                userMapper.updateById(user);
            }
        }

        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, userId)
        );
        
        if (profile == null) {
            profile = new StudentProfile();
            profile.setUserId(userId);
            profile.setMajor(major);
            profile.setMajorCourses(majorCourses);
            profile.setGrade(grade);
            profile.setInterestDesc(interestDesc);
            profile.setTagMode(tagMode);
            studentProfileMapper.insert(profile);
        } else {
            profile.setMajor(major);
            profile.setMajorCourses(majorCourses);
            profile.setGrade(grade);
            profile.setInterestDesc(interestDesc);
            // 若前端未传入标签模式，则保留原值；否则更新
            if (tagMode != null && !tagMode.trim().isEmpty()) {
                profile.setTagMode(tagMode);
            }
            studentProfileMapper.updateById(profile);
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

    /**
     * 交互式重生成标签：保留 pinnedTags，并排除 excludeTagNames。
     * 注意：该接口不负责更新 StudentProfile，仅根据传入文本重算 user_tag。
     */
    @Transactional
    public List<UserTag> regenerateTags(
            Long userId,
            String interestDesc,
            String major,
            String majorCourses,
            String tagMode,
            List<UserTagResponse> pinnedTags,
            List<String> excludeTagNames,
            Integer desiredTotal) {
        List<UserTag> pinned = new ArrayList<>();
        if (pinnedTags != null) {
            for (UserTagResponse t : pinnedTags) {
                if (t == null || t.getTagName() == null || t.getTagName().trim().isEmpty()) {
                    continue;
                }
                UserTag tag = new UserTag();
                tag.setTagName(t.getTagName().trim());
                tag.setTagType(t.getTagType());
                tag.setWeight(t.getWeight() == null ? new BigDecimal("0.90") : t.getWeight());
                pinned.add(tag);
            }
        }
        return tagService.regenerateStudentTags(userId, interestDesc, major, majorCourses, tagMode, pinned, excludeTagNames, desiredTotal);
    }
}

