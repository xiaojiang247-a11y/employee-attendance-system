package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * LeaveBalance —— 与数据库表一一对应的持久化对象
 */
public class LeaveBalance {
    /** 主键 */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 年度 */
    private Integer yearNum;
    /** 假别编码 */
    private String typeCode;
    /** 总额度 */
    private BigDecimal totalDays;
    /** 已使用 */
    private BigDecimal usedDays;
    /** 剩余 */
    private BigDecimal remainDays;
    /** 更新时间 */
    private LocalDateTime updateTime;
    /** 姓名 */
    private String employeeName;
    /** 工号 */
    private String employeeNo;
    /** 假别 */
    private String typeName;
    /** 颜色 */
    private String color;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public Integer getYearNum() { return yearNum; }
    public void setYearNum(Integer yearNum) { this.yearNum = yearNum; }
    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }
    public BigDecimal getTotalDays() { return totalDays; }
    public void setTotalDays(BigDecimal totalDays) { this.totalDays = totalDays; }
    public BigDecimal getUsedDays() { return usedDays; }
    public void setUsedDays(BigDecimal usedDays) { this.usedDays = usedDays; }
    public BigDecimal getRemainDays() { return remainDays; }
    public void setRemainDays(BigDecimal remainDays) { this.remainDays = remainDays; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
