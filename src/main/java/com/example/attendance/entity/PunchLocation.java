package com.example.attendance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PunchLocation —— 与数据库表一一对应的持久化对象。
 * created_by_role / updated_by_role 用于权限层级：ADMIN 建的地点 SUPER_ADMIN 可覆盖，反之不允许。
 */
public class PunchLocation {
    private Long id;
    private String name;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer radiusM;
    private String allowIp;
    private String wifiSsid;
    private Integer isDefault;
    private Integer enabled;
    private String remark;
    private LocalDateTime createTime;
    private String groupNames;

    /** 谁设置的（账号） */
    private String createdBy;
    /** 设置者层级：SUPER_ADMIN / ADMIN */
    private String createdByRole;
    /** 最后修改人 */
    private String updatedBy;
    private String updatedByRole;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public Integer getRadiusM() { return radiusM; }
    public void setRadiusM(Integer radiusM) { this.radiusM = radiusM; }
    public String getAllowIp() { return allowIp; }
    public void setAllowIp(String allowIp) { this.allowIp = allowIp; }
    public String getWifiSsid() { return wifiSsid; }
    public void setWifiSsid(String wifiSsid) { this.wifiSsid = wifiSsid; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getGroupNames() { return groupNames; }
    public void setGroupNames(String groupNames) { this.groupNames = groupNames; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getCreatedByRole() { return createdByRole; }
    public void setCreatedByRole(String createdByRole) { this.createdByRole = createdByRole; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getUpdatedByRole() { return updatedByRole; }
    public void setUpdatedByRole(String updatedByRole) { this.updatedByRole = updatedByRole; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
