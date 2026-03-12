package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.SystemSetting;
import com.example.graduation.mapper.SystemSettingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SystemSettingService {

    @Autowired
    private SystemSettingMapper systemSettingMapper;

    /** 获取系统设置（如不存在则创建一条默认记录） */
    public SystemSetting getOrCreate() {
        SystemSetting setting = systemSettingMapper.selectOne(new LambdaQueryWrapper<SystemSetting>()
                .last("LIMIT 1"));
        if (setting == null) {
            setting = new SystemSetting();
            setting.setSelectionEnabled(Boolean.TRUE);
            setting.setSelectionStartTime(null);
            setting.setSelectionEndTime(null);
            systemSettingMapper.insert(setting);
        }
        return setting;
    }

    public SystemSetting updateSelectionSetting(Boolean enabled, LocalDateTime start, LocalDateTime end) {
        SystemSetting setting = getOrCreate();
        if (enabled != null) {
            setting.setSelectionEnabled(enabled);
        }
        setting.setSelectionStartTime(start);
        setting.setSelectionEndTime(end);
        setting.setUpdatedAt(LocalDateTime.now());
        systemSettingMapper.updateById(setting);
        return setting;
    }

    /** 当前时刻选题是否对学生开放 */
    public boolean isSelectionOpenNow() {
        SystemSetting setting = getOrCreate();
        if (setting.getSelectionEnabled() == null || !setting.getSelectionEnabled()) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = setting.getSelectionStartTime();
        LocalDateTime end = setting.getSelectionEndTime();
        if (start != null && now.isBefore(start)) {
            return false;
        }
        if (end != null && now.isAfter(end)) {
            return false;
        }
        return true;
    }
}

