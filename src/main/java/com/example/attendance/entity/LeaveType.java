package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * LeaveType —— 与数据库表一一对应的持久化对象
 */
public class LeaveType {
    /** 主键 */
    private Long id;
    /** 编码 */
    private String code;
    /** 名称 */
    private String name;
    /** 年度额度 */
    private BigDecimal annualQuota;
    /** 带薪 */
    private Integer paid;
    /** 需审批 */
    private Integer needApproval;
    /** 可结转 */
    private Integer carryForward;
    /** 扣减规则 */
    private String deductRule;
    /** 颜色 */
    private String color;
    /** 排序 */
    private Integer sortNo;
    /** 启用 */
    private Integer enabled;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getAnnualQuota() { return annualQuota; }
    public void setAnnualQuota(BigDecimal annualQuota) { this.annualQuota = annualQuota; }
    public Integer getPaid() { return paid; }
    public void setPaid(Integer paid) { this.paid = paid; }
    public Integer getNeedApproval() { return needApproval; }
    public void setNeedApproval(Integer needApproval) { this.needApproval = needApproval; }
    public Integer getCarryForward() { return carryForward; }
    public void setCarryForward(Integer carryForward) { this.carryForward = carryForward; }
    public String getDeductRule() { return deductRule; }
    public void setDeductRule(String deductRule) { this.deductRule = deductRule; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
}
