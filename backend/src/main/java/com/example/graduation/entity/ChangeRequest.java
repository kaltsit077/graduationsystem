package com.example.graduation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("change_request")
public class ChangeRequest {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起变更的学生ID */
    private Long studentId;

    /** 当前生效的选题申请ID（topic_application.id） */
    private Long currentApplicationId;

    /** 变更类型：更换选题 / 更换导师 */
    private ChangeType type;

    /** 学生填写的变更原因 */
    private String reason;

    /** 学生建议的新选题（可选） */
    private Long targetTopicId;

    /** 学生建议的新导师（可选） */
    private Long targetTeacherId;

    /** 当前处理状态 */
    private ChangeStatus status;

    /** 导师审批结果（仅更换选题时使用） */
    private Decision teacherDecision;

    /** 导师审批意见 */
    private String teacherComment;

    /** 管理员审批结果（仅更换导师 / 最终确认时使用） */
    private Decision adminDecision;

    /** 管理员审批意见 */
    private String adminComment;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum ChangeType {
        CHANGE_TOPIC,
        CHANGE_TEACHER
    }

    public enum ChangeStatus {
        PENDING_TEACHER,
        PENDING_ADMIN,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    public enum Decision {
        APPROVED,
        REJECTED
    }
}

