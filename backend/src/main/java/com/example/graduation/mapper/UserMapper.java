package com.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduation.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

