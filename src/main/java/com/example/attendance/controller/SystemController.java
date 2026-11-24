package com.example.attendance.controller;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import com.example.attendance.service.*;
import com.example.attendance.util.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Controller
public class SystemController {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final MenuMapper menuMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final NoticeMapper noticeMapper;
    private final OperLogMapper operLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final SystemService systemService;
    private final AccountService accountService;
    private final AccountLogMapper accountLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityHelper security;

    public SystemController(UserMapper userMapper, RoleMapper roleMapper, MenuMapper menuMapper,
                            DepartmentMapper departmentMapper, EmployeeMapper employeeMapper,
                            NoticeMapper noticeMapper, OperLogMapper operLogMapper,
                            LoginLogMapper loginLogMapper, SystemService systemService,
                            AccountService accountService, AccountLogMapper accountLogMapper,
                            PasswordEncoder passwordEncoder, SecurityHelper security) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.noticeMapper = noticeMapper;
        this.operLogMapper = operLogMapper;
        this.loginLogMapper = loginLogMapper;
        this.systemService = systemService;
        this.accountService = accountService;
        this.accountLogMapper = accountLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.security = security;
    }

    // ---------- 员工账号（管理员 + 超管） ----------
    @GetMapping("/admin/system/users")
    public String users(@RequestParam(required = false) String keyword,
                        @RequestParam(required = false) Long deptId, Model model) {
        // 超管进入管理员子系统时同样看到「员工账号」，与页面文案一致；
        // 层级固定 EMPLOYEE，管理员永远看不到管理员及以上账户。
        model.addAttribute("rows", accountService.list("EMPLOYEE", keyword, deptId));
        model.addAttribute("keyword", keyword);
        model.addAttribute("deptId", deptId);
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("roles", roleMapper.findAll());
        model.addAttribute("employees", employeeMapper.findAllActive());
        return "admin/system/users";
    }

    @PostMapping("/admin/system/users/save")
    public String saveUser(@ModelAttribute User u,
                           @RequestParam(required = false) Long roleId,
                           @RequestParam(defaultValue = "false") boolean create,
                           RedirectAttributes ra) {
        try {
            if (roleId == null) {
                Role r = roleMapper.findByCode(u.getRole() == null ? "EMPLOYEE" : u.getRole());
                roleId = r == null ? 4L : r.getId();
            }
            accountService.save(u, roleId, u.getPlainPassword(), create, passwordEncoder);
            ra.addFlashAttribute("msg", create ? "员工账号已创建并分配角色" : "员工账号信息已更新");
        } catch (BizException e) {
            accountService.writeFailure(create ? "CREATE" : "UPDATE", u.getUsername(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/system/users";
    }

    @PostMapping("/admin/system/users/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try { accountService.toggle(id); ra.addFlashAttribute("msg", "账号状态已变更"); }
        catch (BizException e) {
            accountService.writeFailure("DISABLE", String.valueOf(id), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/system/users";
    }

    @PostMapping("/admin/system/users/reset/{id}")
    public String reset(@PathVariable Long id, RedirectAttributes ra) {
        try {
            accountService.resetPassword(id, passwordEncoder.encode("123456"), "123456");
            ra.addFlashAttribute("msg", "密码已重置为 123456");
        } catch (BizException e) {
            accountService.writeFailure("RESET_PWD", String.valueOf(id), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/system/users";
    }

    @PostMapping("/admin/system/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        try { accountService.delete(id); ra.addFlashAttribute("msg", "账号已删除"); }
        catch (BizException e) {
            accountService.writeFailure("DELETE", String.valueOf(id), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/system/users";
    }

    // ---------- 角色权限：仅超级管理员 ----------
    @GetMapping("/admin/system/roles")
    public String roles(Model model) {
        model.addAttribute("roles", roleMapper.findAll());
        return "admin/system/roles";
    }

    @PostMapping("/admin/system/roles/save")
    public String saveRole(@ModelAttribute Role r, RedirectAttributes ra) {
        try {
            assertRoleManageable(r.getId());
            if (r.getId() == null) roleMapper.insert(r);
            else roleMapper.update(r);
            ra.addFlashAttribute("msg", "角色已保存");
        } catch (BizException e) {
            accountService.writeFailure("GRANT", r.getCode(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/system/roles";
    }

    @GetMapping("/admin/system/roles/menu/{id}")
    public String roleMenu(@PathVariable Long id, Model model) {
        model.addAttribute("role", roleMapper.findById(id));
        model.addAttribute("menus", menuMapper.findByRoleId(id));
        return "admin/system/role-menu";
    }

    @PostMapping("/admin/system/roles/grant")
    public String grant(@RequestParam Long roleId,
                        @RequestParam(required = false) List<Long> menuIds, RedirectAttributes ra) {
        try {
            assertRoleManageable(roleId);
            systemService.grantMenus(roleId, menuIds);
            Role r = roleMapper.findById(roleId);
            User t = new User();
            t.setUsername("角色:" + (r == null ? roleId : r.getCode()));
            accountService.writeLog("GRANT", t, "调整角色「"
                    + (r == null ? roleId : r.getName()) + "」的菜单权限，共 "
                    + (menuIds == null ? 0 : menuIds.size()) + " 项；下次请求即生效");
            ra.addFlashAttribute("msg", "菜单权限已保存，相关用户下次请求即生效");
        } catch (BizException e) {
            accountService.writeFailure("GRANT", "角色:" + roleId, e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/system/roles";
    }

    /** 纵深防御：URL 已被 SecurityConfig 收紧，这里再断言一次，防止直接调用 service 绕过 */
    private void assertRoleManageable(Long roleId) {
        if (!security.isSuperAdmin()) {
            throw new BizException("只有超级管理员可以调整角色与菜单权限");
        }
        if (roleId != null) {
            Role r = roleMapper.findById(roleId);
            if (r != null && "SUPER_ADMIN".equals(r.getCode())) {
                throw new BizException("超级管理员角色不可修改");
            }
        }
    }

    // ---------- 公告 ----------
    @GetMapping("/admin/system/notices")
    public String notices(Model model) {
        model.addAttribute("rows", noticeMapper.findAll());
        return "admin/system/notices";
    }

    @PostMapping("/admin/system/notices/save")
    public String saveNotice(@ModelAttribute Notice n, RedirectAttributes ra) {
        n.setPublisher(security.currentUser() == null ? "system" : security.currentUser().getRealName());
        if (n.getId() == null) noticeMapper.insert(n);
        else noticeMapper.update(n);
        ra.addFlashAttribute("msg", "公告已发布");
        return "redirect:/admin/system/notices";
    }

    @PostMapping("/admin/system/notices/delete/{id}")
    public String deleteNotice(@PathVariable Long id, RedirectAttributes ra) {
        noticeMapper.deleteById(id);
        ra.addFlashAttribute("msg", "公告已删除");
        return "redirect:/admin/system/notices";
    }

    // ---------- 日志 ----------
    @GetMapping("/admin/system/oper-logs")
    public String operLogs(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("rows", operLogMapper.findRecent(keyword, 200));
        model.addAttribute("keyword", keyword);
        model.addAttribute("total", operLogMapper.countAll());
        model.addAttribute("today", operLogMapper.countToday());
        return "admin/system/oper-logs";
    }

    @GetMapping("/admin/login-logs")
    public String loginLogs(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("rows", loginLogMapper.findRecent(keyword, 200));
        model.addAttribute("keyword", keyword);
        model.addAttribute("today", loginLogMapper.countToday());
        model.addAttribute("failed", loginLogMapper.countTodayFailed());
        return "admin/system/login-logs";
    }
}
