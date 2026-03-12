package com.example.graduation.dto;

import com.example.graduation.entity.ChangeRequest;
import lombok.Data;

@Data
public class ChangeRequestCreateRequest {

    /** 变更类型：CHANGE_TOPIC / CHANGE_TEACHER */
    private ChangeRequest.ChangeType type;

    /** 学生填写的原因 */
    private String reason;

    /** 可选：学生建议的新选题ID */
    private Long targetTopicId;

    /** 可选：学生建议的新导师ID */
    private Long targetTeacherId;
}

