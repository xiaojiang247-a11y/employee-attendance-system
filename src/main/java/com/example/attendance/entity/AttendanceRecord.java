package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * AttendanceRecord —— 与数据库表一一对应的持久化对象
 */
public class AttendanceRecord {
    /** 主键 */
    private Long id;
    /** 员工ID */
    private Long employeeId;
    /** 考勤日期 */
    private LocalDate attendanceDate;
    /** 签到时间 */
    private LocalDateTime checkInTime;
    /** 签退时间 */
    private LocalDateTime checkOutTime;
    /** 考勤状态 */
    private String status;
    /** 当日班次 */
    private Long shiftId;
    /** 签到IP */
    private String checkInIp;
    /** 签退IP */
    private String checkOutIp;
    /** 打卡终端 */
    private String checkInDevice;
    /** 数据来源 */
    private String source;
    /** 迟到分钟 */
    private Integer lateMinutes;
    /** 早退分钟 */
    private Integer earlyMinutes;
    /** 实际工时 */
    private BigDecimal workHours;
    /** 是否补卡生成 */
    private Integer isMakeup;
    /** 备注 */
    private String remark;
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
    /** 打卡地点ID */
    private Long locationId;
    /** 打卡纬度 */
    private BigDecimal punchLat;
    /** 打卡经度 */
    private BigDecimal punchLng;
    /** 距地点中心距离(米) */
    private Integer distanceM;
    /** 打卡地点名称 */
    private String punchAddress;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
    public String getCheckInIp() { return checkInIp; }
    public void setCheckInIp(String checkInIp) { this.checkInIp = checkInIp; }
    public String getCheckOutIp() { return checkOutIp; }
    public void setCheckOutIp(String checkOutIp) { this.checkOutIp = checkOutIp; }
    public String getCheckInDevice() { return checkInDevice; }
    public void setCheckInDevice(String checkInDevice) { this.checkInDevice = checkInDevice; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
    public Integer getEarlyMinutes() { return earlyMinutes; }
    public void setEarlyMinutes(Integer earlyMinutes) { this.earlyMinutes = earlyMinutes; }
    public BigDecimal getWorkHours() { return workHours; }
    public void setWorkHours(BigDecimal workHours) { this.workHours = workHours; }
    public Integer getIsMakeup() { return isMakeup; }
    public void setIsMakeup(Integer isMakeup) { this.isMakeup = isMakeup; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
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
    public Long getLocationId() { return locationId; }
    public void setLocationId(Long locationId) { this.locationId = locationId; }
    public BigDecimal getPunchLat() { return punchLat; }
    public void setPunchLat(BigDecimal punchLat) { this.punchLat = punchLat; }
    public BigDecimal getPunchLng() { return punchLng; }
    public void setPunchLng(BigDecimal punchLng) { this.punchLng = punchLng; }
    public Integer getDistanceM() { return distanceM; }
    public void setDistanceM(Integer distanceM) { this.distanceM = distanceM; }
    public String getPunchAddress() { return punchAddress; }
    public void setPunchAddress(String punchAddress) { this.punchAddress = punchAddress; }
}
