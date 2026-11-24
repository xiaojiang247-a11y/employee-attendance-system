package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * LoginLog —— 与数据库表一一对应的持久化对象
 */
public class LoginLog {
    /** 主键 */
    private Long id;
    /** 登录名 */
    private String username;
    /** IP */
    private String ip;
    /** 登录时间 */
    private LocalDateTime loginTime;
    /** 是否成功 */
    private Boolean success;
    /** 客户端UA */
    private String userAgent;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
}
