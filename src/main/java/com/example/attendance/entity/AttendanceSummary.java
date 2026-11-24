package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * AttendanceSummary —— 与数据库表一一对应的持久化对象
 */
public class AttendanceSummary {
    /** 主键 */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 月份 */
    private String ym;
    /** 应出勤 */
    private BigDecimal expectedDays;
    /** 实出勤 */
    private BigDecimal actualDays;
    /** 迟到次数 */
    private Integer lateCnt;
    /** 早退次数 */
    private Integer earlyCnt;
    /** 旷工次数 */
    private Integer absentCnt;
    /** 请假天数 */
    private BigDecimal leaveDays;
    /** 加班时长 */
    private BigDecimal overtimeHours;
    /** 补卡次数 */
    private Integer makeupCnt;
    /** 总工时 */
    private BigDecimal workHours;
    /** 迟到分钟 */
    private Integer lateMinutes;
    /** 关账锁定 */
    private Integer locked;
    /** 生成时间 */
    private LocalDateTime generateTime;
    /** 姓名 */
    private String employeeName;
    /** 工号 */
    private String employeeNo;
    /** 部门 */
    private String departmentName;
    /** 部门ID */
    private Long deptId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getYm() { return ym; }
    public void setYm(String ym) { this.ym = ym; }
    public BigDecimal getExpectedDays() { return expectedDays; }
    public void setExpectedDays(BigDecimal expectedDays) { this.expectedDays = expectedDays; }
    public BigDecimal getActualDays() { return actualDays; }
    public void setActualDays(BigDecimal actualDays) { this.actualDays = actualDays; }
    public Integer getLateCnt() { return lateCnt; }
    public void setLateCnt(Integer lateCnt) { this.lateCnt = lateCnt; }
    public Integer getEarlyCnt() { return earlyCnt; }
    public void setEarlyCnt(Integer earlyCnt) { this.earlyCnt = earlyCnt; }
    public Integer getAbsentCnt() { return absentCnt; }
    public void setAbsentCnt(Integer absentCnt) { this.absentCnt = absentCnt; }
    public BigDecimal getLeaveDays() { return leaveDays; }
    public void setLeaveDays(BigDecimal leaveDays) { this.leaveDays = leaveDays; }
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }
    public Integer getMakeupCnt() { return makeupCnt; }
    public void setMakeupCnt(Integer makeupCnt) { this.makeupCnt = makeupCnt; }
    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
    public Integer getLocked() { return locked; }
    public void setLocked(Integer locked) { this.locked = locked; }
    public LocalDateTime getGenerateTime() { return generateTime; }
    public void setGenerateTime(LocalDateTime generateTime) { this.generateTime = generateTime; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
}
