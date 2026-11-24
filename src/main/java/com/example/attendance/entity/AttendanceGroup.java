package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * AttendanceGroup —— 与数据库表一一对应的持久化对象
 */
public class AttendanceGroup {
    /** 主键 */
    private Long id;
    /** 考勤组名称 */
    private String name;
    /** 班制类型 */
    private String groupType;
    /** 适用部门 */
    private Long departmentId;
    /** 默认班次 */
    private Long defaultShiftId;
    /** 工作日 */
    private String workdays;
    /** 允许IP */
    private String allowIp;
    /** WiFi */
    private String wifiSsid;
    /** 提前打卡分钟 */
    private Integer punchAheadMin;
    /** 说明 */
    private String remark;
    /** 启用 */
    private Integer enabled;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 部门名称 */
    private String departmentName;
    /** 默认班次 */
    private String shiftName;
    /** 人数 */
    private Integer empCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGroupType() { return groupType; }
    public void setGroupType(String groupType) { this.groupType = groupType; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getDefaultShiftId() { return defaultShiftId; }
    public void setDefaultShiftId(Long defaultShiftId) { this.defaultShiftId = defaultShiftId; }
    public String getWorkdays() { return workdays; }
    public void setWorkdays(String workdays) { this.workdays = workdays; }
    public String getAllowIp() { return allowIp; }
    public void setAllowIp(String allowIp) { this.allowIp = allowIp; }
    public String getWifiSsid() { return wifiSsid; }
    public void setWifiSsid(String wifiSsid) { this.wifiSsid = wifiSsid; }
    public Integer getPunchAheadMin() { return punchAheadMin; }
    public void setPunchAheadMin(Integer punchAheadMin) { this.punchAheadMin = punchAheadMin; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    public Integer getEmpCount() { return empCount; }
    public void setEmpCount(Integer empCount) { this.empCount = empCount; }
}
