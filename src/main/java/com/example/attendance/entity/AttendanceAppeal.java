package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * AttendanceAppeal —— 与数据库表一一对应的持久化对象
 */
public class AttendanceAppeal {
    /** 主键 */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 考勤记录ID */
    private Long recordId;
    /** 异常日期 */
    private LocalDate appealDate;
    /** 异常类型 */
    private String exceptionType;
    /** 申诉理由 */
    private String reason;
    /** 状态 */
    private String status;
    /** 复核回复 */
    private String reply;
    /** 复核人 */
    private Long approverId;
    /** 提交时间 */
    private LocalDateTime applyTime;
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
    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }
    public LocalDate getAppealDate() { return appealDate; }
    public void setAppealDate(LocalDate appealDate) { this.appealDate = appealDate; }
    public String getExceptionType() { return exceptionType; }
    public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public LocalDateTime getApplyTime() { return applyTime; }
    public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
}
