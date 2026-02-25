package com.selenium.model;

import java.time.LocalDateTime;


public class SecurityLog {

    private long logId;
    private LocalDateTime createdAt;
    private String level;
    private String action;
    private Long userId;
    private String email;
    private String details;

    public SecurityLog(long logId,
                       LocalDateTime createdAt,
                       String level,
                       String action,
                       Long userId,
                       String email,
                       String details) {

        this.logId = logId;
        this.createdAt = createdAt;
        this.level = level;
        this.action = action;
        this.userId = userId;
        this.email = email;
        this.details = details;
    }


    public long getLogId() { return logId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getLevel() { return level; }

    public String getAction() { return action; }

    public Long getUserId() { return userId; }

    public String getEmail() { return email; }

    public String getDetails() { return details; }
}
