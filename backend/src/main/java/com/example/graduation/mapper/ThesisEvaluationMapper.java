package com.example.graduation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.graduation.dto.TeacherMetricsResponse;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.example.graduation.entity.ThesisEvaluation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ThesisEvaluationMapper extends BaseMapper<ThesisEvaluation> {

    @Select("""
            <script>
            SELECT th.topic_id AS topicId, AVG(te.student_score) AS avgStudentScore
            FROM thesis_evaluation te
            JOIN thesis th ON te.thesis_id = th.id
            WHERE te.student_score IS NOT NULL
              AND th.topic_id IN
              <foreach collection="topicIds" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            GROUP BY th.topic_id
            </script>
            """)
    List<Map<String, Object>> selectAvgStudentScoreByTopicIds(@Param("topicIds") List<Long> topicIds);

    @Select("""
            SELECT
              t.teacher_id AS teacherId,
              u.real_name AS teacherName,
              COUNT(DISTINCT th.student_id) AS totalStudents,
              AVG(te.score) AS avgScore,
              (SUM(CASE WHEN te.score >= 90 THEN 1 ELSE 0 END) / NULLIF(COUNT(te.score), 0)) AS excellentRatio,
              (SUM(CASE WHEN te.score < 60 THEN 1 ELSE 0 END) / NULLIF(COUNT(te.score), 0)) AS failRatio,
              AVG(te.student_score) AS avgStudentScore
            FROM thesis_evaluation te
            JOIN thesis th ON te.thesis_id = th.id
            JOIN topic t ON th.topic_id = t.id
            JOIN `user` u ON t.teacher_id = u.id
            GROUP BY t.teacher_id, u.real_name
            ORDER BY avgScore DESC
            """)
    List<TeacherMetricsResponse> selectAllTeacherMetrics();

    @Select("""
            SELECT
              t.teacher_id AS teacherId,
              u.real_name AS teacherName,
              COUNT(DISTINCT th.student_id) AS totalStudents,
              AVG(te.score) AS avgScore,
              (SUM(CASE WHEN te.score >= 90 THEN 1 ELSE 0 END) / NULLIF(COUNT(te.score), 0)) AS excellentRatio,
              (SUM(CASE WHEN te.score < 60 THEN 1 ELSE 0 END) / NULLIF(COUNT(te.score), 0)) AS failRatio,
              AVG(te.student_score) AS avgStudentScore
            FROM thesis_evaluation te
            JOIN thesis th ON te.thesis_id = th.id
            JOIN topic t ON th.topic_id = t.id
            JOIN `user` u ON t.teacher_id = u.id
            WHERE t.teacher_id = #{teacherId}
            GROUP BY t.teacher_id, u.real_name
            """)
    TeacherMetricsResponse selectTeacherMetrics(@Param("teacherId") Long teacherId);
}

