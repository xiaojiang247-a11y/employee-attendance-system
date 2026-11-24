package com.example.attendance.config;

/** 端口角色：一个端口只服务一个角色端，由启动参数 app.port-role 指定 */
public final class PortRoles {

    private PortRoles() {}

    public static boolean isAllowed(String portRole, String role) {
        if (portRole == null || portRole.isBlank() || role == null) return true;
        return switch (portRole.trim().toUpperCase()) {
            case "SUPER_ADMIN", "SUPER" -> "SUPER_ADMIN".equals(role);
            // HR / MANAGER 是管理员子系统内的细分角色，同属管理员端
            case "ADMIN" -> "ADMIN".equals(role) || "HR".equals(role) || "MANAGER".equals(role);
            case "EMPLOYEE" -> "EMPLOYEE".equals(role);
            default -> true; // 未指定端口角色 = 单实例兼容模式，不额外限制
        };
    }

    public static String label(String portRole) {
        if (portRole == null || portRole.isBlank()) return "统一入口";
        return switch (portRole.trim().toUpperCase()) {
            case "SUPER_ADMIN", "SUPER" -> "超级管理员系统";
            case "ADMIN" -> "管理员系统";
            case "EMPLOYEE" -> "员工系统";
            default -> portRole;
        };
    }
}
