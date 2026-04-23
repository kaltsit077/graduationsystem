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
@TableName("thesis")
public class Thesis {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long topicId;
    
    private Long studentId;
    
    private String fileUrl;
    
    private String fileName;
    
    private Long fileSize;

    /** 环节：与 {@link com.example.graduation.entity.CollabStage#name()} 对应；旧数据可能为 LEGACY_FILE */
    private String stage;
    
    private ThesisStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    public enum ThesisStatus {
        /** 已提交，导师待审核 */
        UPLOADED,
        /** 退回修改，学生可重新提交新版本 */
        NEED_REVISION,
        /** 已通过（含导师打分后视为通过） */
        REVIEWED
    }
}

