package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Shift —— 与数据库表一一对应的持久化对象
 */
public class Shift {
    /** 主键 */
    private Long id;
    /** 班次编码 */
    private String code;
    /** 班次名称 */
    private String name;
    /** 上班时间 */
    private LocalTime startTime;
    /** 下班时间 */
    private LocalTime endTime;
    /** 迟到宽限 */
    private Integer lateGrace;
    /** 早退宽限 */
    private Integer earlyGrace;
    /** 弹性分钟 */
    private Integer flexMinutes;
    /** 标准工时 */
    private BigDecimal workHours;
    /** 跨天 */
    private Integer crossDay;
    /** 日历颜色 */
    private String color;
    /** 说明 */
    private String remark;
    /** 启用 */
    private Integer enabled;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 使用人数 */
    private Integer empCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getLateGrace() { return lateGrace; }
    public void setLateGrace(Integer lateGrace) { this.lateGrace = lateGrace; }
    public Integer getEarlyGrace() { return earlyGrace; }
    public void setEarlyGrace(Integer earlyGrace) { this.earlyGrace = earlyGrace; }
    public Integer getFlexMinutes() { return flexMinutes; }
    public void setFlexMinutes(Integer flexMinutes) { this.flexMinutes = flexMinutes; }
    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }
    public Integer getCrossDay() { return crossDay; }
    public void setCrossDay(Integer crossDay) { this.crossDay = crossDay; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public Integer getEmpCount() { return empCount; }
    public void setEmpCount(Integer empCount) { this.empCount = empCount; }
}
