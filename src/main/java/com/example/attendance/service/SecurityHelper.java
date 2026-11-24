package com.example.attendance.service;

import com.example.attendance.entity.User;
import com.example.attendance.mapper.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 当前登录上下文与数据权限。
 * ADMIN / HR 看全公司，MANAGER 看本部门及子部门，EMPLOYEE 只看本人。
 * 所有查询统一通过 scopeDeptId() 拿到限定条件，避免各 Controller 各写一套。
 */
@Component
public class SecurityHelper {

    private final UserMapper userMapper;

    public SecurityHelper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userMapper.findByUsername(auth.getName());
    }

    public String username() {
        User u = currentUser();
        return u == null ? "anonymous" : u.getUsername();
    }

    public String role() {
        User u = currentUser();
        return u == null ? "" : u.getRole();
    }

    public boolean isSuperAdmin() { return "SUPER_ADMIN".equals(role()); }
    public boolean isAdmin() { return "ADMIN".equals(role()); }
    public boolean isHR() { return "HR".equals(role()); }
    public boolean isManager() { return "MANAGER".equals(role()); }
    public boolean isEmployee() { return "EMPLOYEE".equals(role()); }

    /** 能否进入管理端（审批中心、报表、组织人员等） */
    public boolean canManage() { return isSuperAdmin() || isAdmin() || isHR() || isManager(); }

    /** 账户层级：SUPER / ADMIN / EMPLOYEE，用于账户管理的垂直越权断言 */
    public String accountLevel() {
        String r = role();
        if ("SUPER_ADMIN".equals(r)) return "SUPER";
        if ("ADMIN".equals(r) || "HR".equals(r) || "MANAGER".equals(r)) return "ADMIN";
        return "EMPLOYEE";
    }

    public static String levelOf(String role) {
        if ("SUPER_ADMIN".equals(role)) return "SUPER";
        if ("ADMIN".equals(role) || "HR".equals(role) || "MANAGER".equals(role)) return "ADMIN";
        return "EMPLOYEE";
    }

    /** 数据范围：MANAGER 返回其部门 ID（含子部门由 SQL 展开），其余返回 null 表示不限制 */
    public Long scopeDeptId() {
        User u = currentUser();
        return (u != null && "MANAGER".equals(u.getRole())) ? u.getDeptId() : null;
    }

    public Long employeeId() {
        User u = currentUser();
        return u == null ? null : u.getEmployeeId();
    }

    public Long userId() {
        User u = currentUser();
        return u == null ? null : u.getId();
    }
}
