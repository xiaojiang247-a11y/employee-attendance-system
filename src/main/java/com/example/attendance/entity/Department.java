package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Department —— 与数据库表一一对应的持久化对象
 */
public class Department {
    /** 主键 */
    private Long id;
    /** 部门编码 */
    private String code;
    /** 部门名称 */
    private String name;
    /** 描述 */
    private String description;
    /** 上级部门 */
    private Long parentId;
    /** 负责人ID */
    private Long leaderId;
    /** 排序 */
    private Integer sortNo;
    /** 负责人姓名 */
    private String leaderName;
    /** 在职人数 */
    private Integer empCount;
    /** 子部门 */
    private List<Department> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getLeaderId() { return leaderId; }
    public void setLeaderId(Long leaderId) { this.leaderId = leaderId; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
    public Integer getEmpCount() { return empCount; }
    public void setEmpCount(Integer empCount) { this.empCount = empCount; }
    public List<Department> getChildren() { return children; }
    public void setChildren(List<Department> children) { this.children = children; }
}
