package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * HolidayCalendar —— 与数据库表一一对应的持久化对象
 */
public class HolidayCalendar {
    /** 主键 */
    private Long id;
    /** 日期 */
    private LocalDate calDate;
    /** 日期类型 */
    private String dayType;
    /** 名称 */
    private String name;
    /** 说明 */
    private String remark;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getCalDate() { return calDate; }
    public void setCalDate(LocalDate calDate) { this.calDate = calDate; }
    public String getDayType() { return dayType; }
    public void setDayType(String dayType) { this.dayType = dayType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
