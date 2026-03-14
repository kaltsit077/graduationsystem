package com.example.graduation.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 可选：按配置间隔定期清空监控日志文件。
 * 在 application.yml 中设置 monitor.log.clear-interval-hours &gt; 0 时生效（单位：小时）。
 */
@Component
public class MonitorLogClearScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitorLogClearScheduler.class);

    @Value("${logging.file.name:logs/graduation-backend.log}")
    private String logFilePath;

    @Value("${monitor.log.clear-interval-hours:0}")
    private int clearIntervalHours;

    private final AtomicLong lastClearMillis = new AtomicLong(0);

    @Scheduled(fixedRate = 3600000) // 每 1 小时检查一次
    public void clearLogIfConfigured() {
        if (clearIntervalHours <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = (now - lastClearMillis.get()) / 3600000;
        if (elapsed < clearIntervalHours) {
            return;
        }
        Path path = Paths.get(logFilePath);
        if (!Files.exists(path)) {
            return;
        }
        try {
            Files.write(path, Collections.emptyList());
            lastClearMillis.set(now);
            log.info("监控日志已按配置定期清除，间隔 {} 小时", clearIntervalHours);
        } catch (Exception e) {
            log.warn("定期清除监控日志失败: {}", e.getMessage());
        }
    }
}
