package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Schedule —— 与数据库表一一对应的持久化对象
 */
public class Schedule {
    /** 主键 */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 日期 */
    private LocalDate workDate;
    /** 班次ID */
    private Long shiftId;
    /** 是否休息 */
    private Integer isRest;
    /** 来源 */
    private String source;
    /** 备注 */
    private String remark;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 姓名 */
    private String employeeName;
    /** 工号 */
    private String employeeNo;
    /** 部门 */
    private String departmentName;
    /** 部门ID */
    private Long deptId;
    /** 班次 */
    private String shiftName;
    /** 班次颜色 */
    private String shiftColor;
    /** 上班时间 */
    private LocalTime startTime;
    /** 下班时间 */
    private LocalTime endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
    public Integer getIsRest() { return isRest; }
    public void setIsRest(Integer isRest) { this.isRest = isRest; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    public String getShiftColor() { return shiftColor; }
    public void setShiftColor(String shiftColor) { this.shiftColor = shiftColor; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
