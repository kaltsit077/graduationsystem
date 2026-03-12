package com.example.graduation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.graduation.entity.Thesis;
import com.example.graduation.entity.ThesisEvaluation;
import com.example.graduation.entity.Topic;
import com.example.graduation.entity.TopicMetrics;
import com.example.graduation.mapper.ThesisEvaluationMapper;
import com.example.graduation.mapper.ThesisMapper;
import com.example.graduation.mapper.TopicMapper;
import com.example.graduation.mapper.TopicMetricsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {

    @Autowired
    private ThesisEvaluationMapper thesisEvaluationMapper;

    @Autowired
    private ThesisMapper thesisMapper;

    @Autowired
    private TopicMapper topicMapper;

    @Autowired
    private TopicMetricsMapper topicMetricsMapper;

    /**
     * 教师对某篇论文进行评价（新增或更新）
     */
    @Transactional
    public void saveEvaluation(Long teacherId,
                               Long thesisId,
                               Integer score,
                               Integer defenseScore,
                               Integer reviewScore,
                               String gradeLevel,
                               String comment) {
        Thesis thesis = thesisMapper.selectById(thesisId);
        if (thesis == null) {
            throw new RuntimeException("论文不存在");
        }

        ThesisEvaluation existing = thesisEvaluationMapper.selectOne(
                new LambdaQueryWrapper<ThesisEvaluation>()
                        .eq(ThesisEvaluation::getThesisId, thesisId)
        );

        if (existing == null) {
            existing = new ThesisEvaluation();
            existing.setThesisId(thesisId);
            existing.setStudentId(thesis.getStudentId());
            existing.setTeacherId(teacherId);
            existing.setCreatedAt(LocalDateTime.now());
        }

        if (score != null) {
            existing.setScore(BigDecimal.valueOf(score));
        }
        if (defenseScore != null) {
            existing.setDefenseScore(BigDecimal.valueOf(defenseScore));
        }
        if (reviewScore != null) {
            existing.setReviewScore(BigDecimal.valueOf(reviewScore));
        }
        existing.setGradeLevel(gradeLevel);
        existing.setComment(comment);

        if (existing.getId() == null) {
            thesisEvaluationMapper.insert(existing);
        } else {
            thesisEvaluationMapper.updateById(existing);
        }

        // 论文状态标记为已评审
        thesis.setStatus(Thesis.ThesisStatus.REVIEWED);
        thesisMapper.updateById(thesis);
    }

    /**
     * 学生对论文质量和导师指导进行评价
     */
    @Transactional
    public void saveStudentFeedback(Long studentId,
                                    Long thesisId,
                                    Integer studentScore,
                                    String studentComment) {
        Thesis thesis = thesisMapper.selectById(thesisId);
        if (thesis == null) {
            throw new RuntimeException("论文不存在");
        }
        if (!thesis.getStudentId().equals(studentId)) {
            throw new RuntimeException("只能评价自己的论文");
        }

        ThesisEvaluation existing = thesisEvaluationMapper.selectOne(
                new LambdaQueryWrapper<ThesisEvaluation>()
                        .eq(ThesisEvaluation::getThesisId, thesisId)
        );

        if (existing == null) {
            existing = new ThesisEvaluation();
            existing.setThesisId(thesisId);
            existing.setStudentId(studentId);
            // 尝试填充导师ID
            Topic topic = topicMapper.selectById(thesis.getTopicId());
            if (topic != null) {
                existing.setTeacherId(topic.getTeacherId());
            }
            existing.setCreatedAt(LocalDateTime.now());
        }

        if (studentScore != null) {
            existing.setStudentScore(BigDecimal.valueOf(studentScore));
        }
        existing.setStudentComment(studentComment);

        if (existing.getId() == null) {
            thesisEvaluationMapper.insert(existing);
        } else {
            thesisEvaluationMapper.updateById(existing);
        }
    }

    /**
     * 为指定导师重算其所有选题的统计指标
     */
    @Transactional
    public void recalcTopicMetricsForTeacher(Long teacherId) {
        // 查询该导师的所有选题
        List<Topic> topics = topicMapper.selectList(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getTeacherId, teacherId)
        );
        if (topics.isEmpty()) {
            return;
        }

        Map<Long, Topic> topicMap = new HashMap<>();
        for (Topic t : topics) {
            topicMap.put(t.getId(), t);
        }

        // 查询这些选题下的所有论文评价
        List<Thesis> theses = thesisMapper.selectList(
                new LambdaQueryWrapper<Thesis>()
                        .in(Thesis::getTopicId, topicMap.keySet())
        );
        if (theses.isEmpty()) {
            return;
        }

        Map<Long, Thesis> thesisMap = new HashMap<>();
        for (Thesis th : theses) {
            thesisMap.put(th.getId(), th);
        }

        List<ThesisEvaluation> evaluations = thesisEvaluationMapper.selectList(
                new LambdaQueryWrapper<ThesisEvaluation>()
                        .in(ThesisEvaluation::getThesisId, thesisMap.keySet())
        );

        // 按 topicId 聚合
        Map<Long, TopicMetrics> metricsMap = new HashMap<>();
        for (ThesisEvaluation eval : evaluations) {
            Thesis thesis = thesisMap.get(eval.getThesisId());
            if (thesis == null) {
                continue;
            }
            Long topicId = thesis.getTopicId();
            TopicMetrics metrics = metricsMap.computeIfAbsent(topicId, id -> {
                TopicMetrics m = new TopicMetrics();
                m.setTopicId(id);
                m.setTeacherId(teacherId);
                m.setTotalStudents(0);
                m.setAvgScore(BigDecimal.ZERO);
                m.setExcellentRatio(BigDecimal.ZERO);
                m.setFailRatio(BigDecimal.ZERO);
                return m;
            });

            metrics.setTotalStudents(metrics.getTotalStudents() + 1);

            if (eval.getScore() != null) {
                BigDecimal score = eval.getScore();
                metrics.setAvgScore(metrics.getAvgScore().add(score));
                if (score.compareTo(BigDecimal.valueOf(90)) >= 0) {
                    metrics.setExcellentRatio(metrics.getExcellentRatio().add(BigDecimal.ONE));
                }
                if (score.compareTo(BigDecimal.valueOf(60)) < 0) {
                    metrics.setFailRatio(metrics.getFailRatio().add(BigDecimal.ONE));
                }
            }
        }

        // 计算平均值与比例，写回 topic_metrics
        for (TopicMetrics metrics : metricsMap.values()) {
            int total = metrics.getTotalStudents();
            if (total == 0) {
                continue;
            }
            if (metrics.getAvgScore() != null) {
                metrics.setAvgScore(
                        metrics.getAvgScore()
                                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                );
            }
            metrics.setExcellentRatio(
                    metrics.getExcellentRatio()
                            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
            );
            metrics.setFailRatio(
                metrics.getFailRatio()
                        .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
            );
            metrics.setLastUpdatedAt(LocalDateTime.now());

            TopicMetrics existing = topicMetricsMapper.selectOne(
                    new LambdaQueryWrapper<TopicMetrics>()
                            .eq(TopicMetrics::getTopicId, metrics.getTopicId())
            );
            if (existing == null) {
                topicMetricsMapper.insert(metrics);
            } else {
                metrics.setId(existing.getId());
                topicMetricsMapper.updateById(metrics);
            }
        }
    }
}

