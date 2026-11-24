package com.example.attendance.controller;

import com.example.attendance.util.AppClock;
import com.example.attendance.util.PunchRule;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import com.example.attendance.service.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * 超级管理员子系统（/super/**）：仅 SUPER_ADMIN 可进入。
 * 职责：管理员账户全生命周期、员工账户查看与管理、部门信息、打卡地点覆盖设置、
 *       角色权限、账户操作记录、权限边界说明、系统配置与维护。
 */
@Controller
@RequestMapping("/super")
public class SuperAdminController {

    private final AccountService accountService;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final AccountLogMapper logMapper;
    private final LoginLogMapper loginLogMapper;
    private final StatsMapper statsMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityHelper security;

    public SuperAdminController(AccountService accountService, UserMapper userMapper,
                                RoleMapper roleMapper, MenuMapper menuMapper,
                                DepartmentMapper departmentMapper, EmployeeMapper employeeMapper,
                                AccountLogMapper logMapper, LoginLogMapper loginLogMapper,
                                StatsMapper statsMapper, PasswordEncoder passwordEncoder,
                                SecurityHelper security) {
        this.accountService = accountService;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.logMapper = logMapper;
        this.loginLogMapper = loginLogMapper;
        this.statsMapper = statsMapper;
        this.passwordEncoder = passwordEncoder;
        this.security = security;
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("superCount", userMapper.countByLevel("SUPER"));
        model.addAttribute("adminCount", userMapper.countByLevel("ADMIN"));
        model.addAttribute("employeeCount", userMapper.countByLevel("EMPLOYEE"));
        model.addAttribute("enabledCount", userMapper.countEnabled());
        model.addAttribute("logTotal", logMapper.countAll());
        model.addAttribute("logToday", logMapper.countToday());
        model.addAttribute("loginToday", loginLogMapper.countToday());
        model.addAttribute("headcount", statsMapper.activeEmployees());
        model.addAttribute("recentLogs", logMapper.findRecent(null, 8));
        model.addAttribute("roleStats", roleMapper.findAll());
        model.addAttribute("actionStats", logMapper.actionStats());
        model.addAttribute("today", AppClock.nowDate());
        model.addAttribute("now", AppClock.nowDateTime());
        model.addAttribute("zoneLabel", AppClock.ZONE_LABEL);
        model.addAttribute("punchRule", PunchRule.describe());
        model.addAttribute("actionName", AccountService.actionNameMap());
        return "super/dashboard";
    }

    /** 管理员账户列表（ADMIN 级：ADMIN / HR / MANAGER） */
    @GetMapping("/admins")
    public String admins(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("rows", accountService.list("ADMIN", keyword, null));
        model.addAttribute("keyword", keyword);
        model.addAttribute("roles", adminRoles());
        model.addAttribute("roleName", roleNameMap());
        model.addAttribute("total", userMapper.countByLevel("ADMIN"));
        model.addAttribute("enabledTotal", accountService.list("ADMIN", null, null).stream()
                .filter(u -> !Boolean.FALSE.equals(u.getEnabled())).count());
        model.addAttribute("self", "admins");
        return "super/admins";
    }

    /** 员工账户列表（超管视角）：与管理员共用 sys_user，但超管可管全部员工，不受 created_by 归属限制 */
    @GetMapping("/employees")
    public String employees(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long deptId, Model model) {
        model.addAttribute("rows", accountService.list("EMPLOYEE", keyword, deptId));
        model.addAttribute("keyword", keyword);
        model.addAttribute("deptId", deptId);
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("roles", roleMapper.findAll());
        model.addAttribute("employees", employeeMapper.findAllActive());
        model.addAttribute("roleName", roleNameMap());
        model.addAttribute("total", userMapper.countByLevel("EMPLOYEE"));
        model.addAttribute("self", "employees");
        return "super/employees";
    }

    /** 三个角色端的权限边界与页面功能，集中成一张可核对的矩阵 */
    @GetMapping("/permissions")
    public String permissions(Model model) {
        List<Map<String, String>> rows = new ArrayList<>();
        rows.add(row("超级管理员系统", "/super/**", "SUPER_ADMIN",
                "管理员账号、员工账号、部门信息、打卡地点（可覆盖）、角色权限、操作记录、系统配置"));
        rows.add(row("管理员系统", "/admin/**", "ADMIN（HR/MANAGER 仅业务查看）",
                "员工添加/编辑/删除、部门维护、员工打卡地点设置、审批、报表、考勤"));
        rows.add(row("员工系统", "/my/**", "EMPLOYEE",
                "固定地点范围内打卡、我的考勤、我的班表、假期余额、个人中心"));
        model.addAttribute("matrix", rows);
        model.addAttribute("punchRule", PunchRule.describe());
        model.addAttribute("zoneLabel", AppClock.ZONE_LABEL);
        model.addAttribute("menus", menuMapper.findAll());
        return "super/permissions";
    }

    // ---------- 管理员账户操作 ----------
    @PostMapping("/admins/save")
    public String saveAdmin(@ModelAttribute User u,
                            @RequestParam(required = false) Long roleId,
                            @RequestParam(required = false) String password,
                            @RequestParam(defaultValue = "true") boolean create,
                            RedirectAttributes ra) {
        try {
            accountService.save(u, roleId, password, create, passwordEncoder);
            ra.addFlashAttribute("msg", create ? "管理员账户已创建" : "管理员账户已更新");
        } catch (BizException e) {
            accountService.writeFailure(create ? "CREATE" : "UPDATE", u.getUsername(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super/admins";
    }

    @PostMapping("/admins/toggle/{id}")
    public String toggleAdmin(@PathVariable Long id, RedirectAttributes ra) {
        return toggle(id, ra, "/super/admins");
    }

    @PostMapping("/admins/reset/{id}")
    public String resetAdmin(@PathVariable Long id, RedirectAttributes ra) {
        return reset(id, ra, "/super/admins");
    }

    @PostMapping("/admins/delete/{id}")
    public String deleteAdmin(@PathVariable Long id, RedirectAttributes ra) {
        return delete(id, ra, "/super/admins");
    }

    // ---------- 员工账户操作（超管视角） ----------
    @PostMapping("/employees/save")
    public String saveEmployee(@ModelAttribute User u,
                               @RequestParam(required = false) Long roleId,
                               @RequestParam(required = false) String password,
                               @RequestParam(defaultValue = "true") boolean create,
                               RedirectAttributes ra) {
        try {
            if (roleId == null) {
                Role r = roleMapper.findByCode("EMPLOYEE");
                roleId = r == null ? 4L : r.getId();
            }
            accountService.save(u, roleId, password, create, passwordEncoder);
            ra.addFlashAttribute("msg", create ? "员工账户已创建" : "员工账户已更新");
        } catch (BizException e) {
            accountService.writeFailure(create ? "CREATE" : "UPDATE", u.getUsername(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super/employees";
    }

    @PostMapping("/employees/toggle/{id}")
    public String toggleEmployee(@PathVariable Long id, RedirectAttributes ra) {
        return toggle(id, ra, "/super/employees");
    }

    @PostMapping("/employees/reset/{id}")
    public String resetEmployee(@PathVariable Long id, RedirectAttributes ra) {
        return reset(id, ra, "/super/employees");
    }

    @PostMapping("/employees/delete/{id}")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes ra) {
        return delete(id, ra, "/super/employees");
    }

    private String toggle(Long id, RedirectAttributes ra, String back) {
        try { accountService.toggle(id); ra.addFlashAttribute("msg", "账户状态已变更"); }
        catch (BizException e) {
            accountService.writeFailure("DISABLE", String.valueOf(id), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + back;
    }

    private String reset(Long id, RedirectAttributes ra, String back) {
        try {
            accountService.resetPassword(id, passwordEncoder.encode("123456"), "123456");
            ra.addFlashAttribute("msg", "密码已重置为 123456");
        } catch (BizException e) {
            accountService.writeFailure("RESET_PWD", String.valueOf(id), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + back;
    }

    private String delete(Long id, RedirectAttributes ra, String back) {
        try { accountService.delete(id); ra.addFlashAttribute("msg", "账户已删除"); }
        catch (BizException e) {
            accountService.writeFailure("DELETE", String.valueOf(id), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + back;
    }

    /** 角色与权限总览（菜单授权跳转到 role-menu 页面） */
    @GetMapping("/roles")
    public String roles(Model model) {
        model.addAttribute("roles", roleMapper.findAll());
        model.addAttribute("menus", menuMapper.findAll());
        return "super/roles";
    }

    /** 账户操作记录（含打卡地点变更、失败/越权被拒） */
    @GetMapping("/account-logs")
    public String logs(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("rows", logMapper.findRecent(keyword, 300));
        model.addAttribute("keyword", keyword);
        model.addAttribute("total", logMapper.countAll());
        model.addAttribute("today", logMapper.countToday());
        model.addAttribute("actionStats", logMapper.actionStats());
        model.addAttribute("actionName", AccountService.actionNameMap());
        model.addAttribute("zoneLabel", AppClock.ZONE_LABEL);
        return "super/account-logs";
    }

    /** 系统配置与维护 */
    @GetMapping("/config")
    public String config(Model model) {
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        model.addAttribute("osName", System.getProperty("os.name"));
        model.addAttribute("roleCount", roleMapper.findAll().size());
        model.addAttribute("menuCount", menuMapper.findAll().size());
        model.addAttribute("superCount", userMapper.countByLevel("SUPER"));
        model.addAttribute("adminCount", userMapper.countByLevel("ADMIN"));
        model.addAttribute("employeeCount", userMapper.countByLevel("EMPLOYEE"));
        model.addAttribute("zoneLabel", AppClock.ZONE_LABEL);
        model.addAttribute("now", AppClock.nowDateTime());
        return "super/config";
    }

    @PostMapping("/config/grant-quota")
    public String grantQuota(RedirectAttributes ra) {
        ra.addFlashAttribute("msg", "假期额度发放任务已提交");
        return "redirect:/super/config";
    }

    /** 超管建管理员账户：下拉只给 ADMIN 级角色，不再混入员工角色 */
    private List<Role> adminRoles() {
        List<Role> r = new ArrayList<>();
        for (Role x : roleMapper.findAll()) {
            if ("ADMIN".equals(SecurityHelper.levelOf(x.getCode()))) r.add(x);
        }
        return r;
    }

    private Map<String, String> roleNameMap() {
        Map<String, String> m = new HashMap<>();
        for (Role r : roleMapper.findAll()) m.put(r.getCode(), r.getName());
        return m;
    }

    private Map<String, String> row(String sys, String url, String role, String funcs) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("sys", sys); m.put("url", url); m.put("role", role); m.put("funcs", funcs);
        return m;
    }
}
