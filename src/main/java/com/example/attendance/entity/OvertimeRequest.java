package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * OvertimeRequest —— 与数据库表一一对应的持久化对象
 */
public class OvertimeRequest {
    /** 主键 */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 加班日期 */
    private LocalDate otDate;
    /** 开始 */
    private LocalDateTime startTime;
    /** 结束 */
    private LocalDateTime endTime;
    /** 时长 */
    private BigDecimal hours;
    /** 加班类型 */
    private String otType;
    /** 补偿方式 */
    private String compensateType;
    /** 事由 */
    private String reason;
    /** 状态 */
    private String status;
    /** 审批层级 */
    private Integer currentStep;
    /** 审批人 */
    private Long approverId;
    /** 审批时间 */
    private LocalDateTime approveTime;
    /** 审批意见 */
    private String approveRemark;
    /** 申请时间 */
    private LocalDateTime applyTime;
    /** 姓名 */
    private String employeeName;
    /** 工号 */
    private String employeeNo;
    /** 部门 */
    private String departmentName;
    /** 部门ID */
    private Long deptId;
    /** 审批人 */
    private String approverName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getOtDate() { return otDate; }
    public void setOtDate(LocalDate otDate) { this.otDate = otDate; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public BigDecimal getHours() { return hours; }
    public void setHours(BigDecimal hours) { this.hours = hours; }
    public String getOtType() { return otType; }
    public void setOtType(String otType) { this.otType = otType; }
    public String getCompensateType() { return compensateType; }
    public void setCompensateType(String compensateType) { this.compensateType = compensateType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCurrentStep() { return currentStep; }
    public void setCurrentStep(Integer currentStep) { this.currentStep = currentStep; }
    public Long getApproverId() { return approverId; }
    public void setApproverId(Long approverId) { this.approverId = approverId; }
    public LocalDateTime getApproveTime() { return approveTime; }
    public void setApproveTime(LocalDateTime approveTime) { this.approveTime = approveTime; }
    public String getApproveRemark() { return approveRemark; }
    public void setApproveRemark(String approveRemark) { this.approveRemark = approveRemark; }
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
    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }
}
