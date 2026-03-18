package com.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduation.entity.TopicApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TopicApplicationMapper extends BaseMapper<TopicApplication> {
    @Select("""
            SELECT COUNT(DISTINCT ta.student_id)
            FROM topic_application ta
            JOIN topic t ON ta.topic_id = t.id
            WHERE t.teacher_id = #{teacherId}
              AND ta.status = 'APPROVED'
            """)
    Long countDistinctApprovedStudentsByTeacher(@Param("teacherId") Long teacherId);
}

