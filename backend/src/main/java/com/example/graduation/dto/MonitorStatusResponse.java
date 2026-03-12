package com.example.graduation.dto;

import lombok.Data;

@Data
public class MonitorStatusResponse {
    private long uptimeMillis;
    private String uptime;
    private double heapUsedMb;
    private double heapMaxMb;
    private int threadCount;
    private double systemLoadAverage;
    private int activeDbConnections;
    private int idleDbConnections;
}

