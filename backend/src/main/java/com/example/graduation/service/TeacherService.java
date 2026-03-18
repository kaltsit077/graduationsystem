package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.dto.UserTagResponse;
import com.example.graduation.entity.TeacherProfile;
import com.example.graduation.entity.User;
import com.example.graduation.entity.UserTag;
import com.example.graduation.mapper.TeacherProfileMapper;
import com.example.graduation.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherService {
    
    @Autowired
    private TeacherProfileMapper teacherProfileMapper;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TagService tagService;
    
    /**
     * 完善导师信息
     */
    @Transactional
    public TeacherProfile updateProfile(Long userId, String realName, String title, String researchDirection, Integer maxStudentCount) {
        if (realName != null && !realName.trim().isEmpty()) {
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setRealName(realName.trim());
                userMapper.updateById(user);
            }
        }

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

    /**
     * 交互式重生成导师标签：保留 pinnedTags，并排除 excludeTagNames。
     * 这里复用学生标签的生成逻辑，但仅基于研究方向文本。
     */
    @Transactional
    public List<UserTag> regenerateTags(
            Long userId,
            String researchDirection,
            List<UserTagResponse> pinnedTags,
            List<String> excludeTagNames,
            Integer desiredTotal) {
        List<UserTag> pinned = new java.util.ArrayList<>();
        if (pinnedTags != null) {
            for (UserTagResponse t : pinnedTags) {
                if (t == null || t.getTagName() == null || t.getTagName().trim().isEmpty()) {
                    continue;
                }
                UserTag tag = new UserTag();
                tag.setTagName(t.getTagName().trim());
                tag.setTagType(t.getTagType());
                tag.setWeight(t.getWeight());
                pinned.add(tag);
            }
        }
        // 复用学生标签重生成逻辑：
        // interestDesc 使用导师研究方向，专业与主修课程为空，标签模式固定为 INTEREST
        return tagService.regenerateStudentTags(
                userId,
                researchDirection,
                null,
                null,
                "INTEREST",
                pinned,
                excludeTagNames,
                desiredTotal
        );
    }
}

