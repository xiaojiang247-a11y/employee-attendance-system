package com.example.attendance.controller;

import com.example.attendance.config.PortRoles;
import com.example.attendance.service.SecurityHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final SecurityHelper security;

    /** 当前实例服务的角色端（--app.port-role），空表示单实例兼容模式 */
    @Value("${app.port-role:}")
    private String portRole;

    @Value("${app.port-role-label:}")
    private String portRoleLabel;

    public LoginController(SecurityHelper security) {
        this.security = security;
    }

    @GetMapping("/")
    public String root() {
        if (security.isSuperAdmin()) return "redirect:/super/dashboard";
        return security.canManage() ? "redirect:/dashboard" : "redirect:/my/home";
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout, Model model) {
        if ("port".equals(error)) {
            model.addAttribute("errorMsg", "账号角色与当前端口不匹配，请从对应子系统入口登录");
        } else if (error != null) {
            model.addAttribute("errorMsg", "账号或密码错误，或账号已被停用");
        }
        if (logout != null) model.addAttribute("logoutMsg", "已安全退出系统");
        if (portRole != null && !portRole.isBlank()) {
            model.addAttribute("portRole", portRole);
            model.addAttribute("portRoleLabel",
                    portRoleLabel == null || portRoleLabel.isBlank()
                            ? PortRoles.label(portRole) : portRoleLabel);
        }
        return "login";
    }

    /** accessDeniedPage("/403") 对 POST 请求做 forward 时保留 POST，故这里不能只接 GET */
    @RequestMapping("/403")
    public String accessDenied() {
        return "error/403";
    }
}
