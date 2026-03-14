package com.example.graduation.service;

import com.example.graduation.dto.TagRegenerateTaskStatusResponse;
import com.example.graduation.dto.UserTagResponse;
import com.example.graduation.entity.UserTag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class TagRegenerateTaskService {

    public enum Status {
        PENDING,
        RUNNING,
        SUCCEEDED,
        FAILED
    }

    private static class TaskRecord {
        final Instant createdAt = Instant.now();
        volatile Instant updatedAt = Instant.now();
        volatile Status status = Status.PENDING;
        volatile String error;
        volatile List<UserTagResponse> tags;
    }

    private final Executor executor;
    private final Map<String, TaskRecord> tasks = new ConcurrentHashMap<>();

    public TagRegenerateTaskService(@Qualifier("tagRegenerateExecutor") Executor executor) {
        this.executor = executor;
    }

    public String submit(String description, java.util.concurrent.Callable<List<UserTag>> job) {
        String taskId = UUID.randomUUID().toString();
        TaskRecord record = new TaskRecord();
        tasks.put(taskId, record);

        CompletableFuture.runAsync(() -> {
            record.status = Status.RUNNING;
            record.updatedAt = Instant.now();
            try {
                List<UserTag> result = job.call();
                record.tags = result == null
                        ? List.of()
                        : result.stream().map(t -> {
                            UserTagResponse r = new UserTagResponse();
                            r.setTagName(t.getTagName());
                            r.setWeight(t.getWeight());
                            return r;
                        }).collect(Collectors.toList());
                record.status = Status.SUCCEEDED;
                record.updatedAt = Instant.now();
            } catch (Exception e) {
                record.error = (e.getMessage() == null || e.getMessage().isBlank())
                        ? "AI 标签生成失败"
                        : e.getMessage();
                record.status = Status.FAILED;
                record.updatedAt = Instant.now();
            }
        }, executor);

        return taskId;
    }

    public TagRegenerateTaskStatusResponse getStatus(String taskId) {
        TaskRecord record = tasks.get(taskId);
        if (record == null) {
            TagRegenerateTaskStatusResponse res = new TagRegenerateTaskStatusResponse();
            res.setStatus(Status.FAILED.name());
            res.setError("任务不存在或已过期");
            return res;
        }
        TagRegenerateTaskStatusResponse res = new TagRegenerateTaskStatusResponse();
        res.setStatus(record.status.name());
        res.setError(record.error);
        res.setTags(record.tags);
        return res;
    }

    /**
     * 清理过期任务，避免内存增长。
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        Instant now = Instant.now();
        Duration ttl = Duration.ofMinutes(30);
        for (Map.Entry<String, TaskRecord> e : tasks.entrySet()) {
            TaskRecord r = e.getValue();
            if (r == null) continue;
            Instant base = r.updatedAt == null ? r.createdAt : r.updatedAt;
            if (base != null && base.plus(ttl).isBefore(now)) {
                tasks.remove(e.getKey());
            }
        }
    }
}

