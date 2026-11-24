package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Employee —— 与数据库表一一对应的持久化对象
 */
public class Employee {
    /** 主键 */
    private Long id;
    /** 工号 */
    private String employeeNo;
    /** 姓名 */
    private String name;
    /** 性别 */
    private String gender;
    /** 手机号 */
    private String phone;
    /** 邮箱 */
    private String email;
    /** 部门ID */
    private Long departmentId;
    /** 考勤组ID */
    private Long groupId;
    /** 默认班次ID */
    private Long shiftId;
    /** 岗位 */
    private String position;
    /** 入职日期 */
    private LocalDate hireDate;
    /** 在职状态 */
    private String status;
    /** 用工类型 */
    private String employmentType;
    /** 出生日期 */
    private LocalDate birthDate;
    /** 身份证 */
    private String idCard;
    /** 学历 */
    private String education;
    /** 住址 */
    private String address;
    /** 紧急联系人 */
    private String emergencyContact;
    /** 紧急联系电话 */
    private String emergencyPhone;
    /** 离职日期 */
    private LocalDate leaveDate;
    /** 备注 */
    private String remark;
    /** 部门名称 */
    private String departmentName;
    /** 考勤组 */
    private String groupName;
    /** 默认班次 */
    private String shiftName;
    /** 登录账号 */
    private String loginUsername;
    /** 登录密码 */
    private String loginPassword;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeNo() { return employeeNo; }
    public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
    public LocalDate getLeaveDate() { return leaveDate; }
    public void setLeaveDate(LocalDate leaveDate) { this.leaveDate = leaveDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    public String getLoginUsername() { return loginUsername; }
    public void setLoginUsername(String loginUsername) { this.loginUsername = loginUsername; }
    public String getLoginPassword() { return loginPassword; }
    public void setLoginPassword(String loginPassword) { this.loginPassword = loginPassword; }
}
