package com.example.attendance.controller;

import com.example.attendance.util.AppClock;

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
public class OrganizationController {

    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final AttendanceGroupMapper groupMapper;
    private final ShiftMapper shiftMapper;
    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AttendanceRecordMapper recordMapper;
    private final LeaveRequestMapper leaveMapper;
    private final ScheduleMapper scheduleMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityHelper security;

    public OrganizationController(EmployeeMapper employeeMapper, DepartmentMapper departmentMapper,
                                  AttendanceGroupMapper groupMapper, ShiftMapper shiftMapper,
                                  UserMapper userMapper, RoleMapper roleMapper,
                                  AttendanceRecordMapper recordMapper, LeaveRequestMapper leaveMapper,
                                  ScheduleMapper scheduleMapper, PasswordEncoder passwordEncoder,
                                  SecurityHelper security) {
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
        this.groupMapper = groupMapper;
        this.shiftMapper = shiftMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.recordMapper = recordMapper;
        this.leaveMapper = leaveMapper;
        this.scheduleMapper = scheduleMapper;
        this.passwordEncoder = passwordEncoder;
        this.security = security;
    }

    // ---------- 部门架构 ----------
    @GetMapping("/admin/org/departments")
    public String departments(Model model) {
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("allDepts", departmentMapper.findAll());
        model.addAttribute("employees", employeeMapper.findAllActive());
        return "admin/org/departments";
    }

    @GetMapping("/admin/employees/departments")
    public String legacyDeptRedirect() {
        return "redirect:/admin/org/departments";
    }

    @PostMapping("/admin/org/departments/save")
    public String saveDept(@ModelAttribute Department d, RedirectAttributes ra) {
        if (d.getId() == null) departmentMapper.insert(d);
        else departmentMapper.update(d);
        ra.addFlashAttribute("msg", "部门信息已保存");
        return "redirect:/admin/org/departments";
    }

    @PostMapping("/admin/org/departments/delete/{id}")
    public String deleteDept(@PathVariable Long id, RedirectAttributes ra) {
        if (departmentMapper.countChildren(id) > 0) {
            ra.addFlashAttribute("error", "该部门下存在子部门，请先调整层级");
        } else if (departmentMapper.countEmployees(id) > 0) {
            ra.addFlashAttribute("error", "该部门下仍有在职员工，请先转移员工");
        } else {
            departmentMapper.deleteById(id);
            ra.addFlashAttribute("msg", "部门已删除");
        }
        return "redirect:/admin/org/departments";
    }

    // ---------- 员工档案 ----------
    @GetMapping("/admin/employees")
    public String employees(@RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long deptId,
                            @RequestParam(required = false) String status,
                            @RequestParam(defaultValue = "1") int page, Model model) {
        List<Employee> all = employeeMapper.search(keyword, security.scopeDeptId() == null ? deptId : security.scopeDeptId(), status);
        Page<Employee> p = Page.of(all, page, 12);
        model.addAttribute("rows", p.getList());
        model.addAttribute("pageData", p);
        model.addAttribute("keyword", keyword);
        model.addAttribute("deptId", deptId);
        model.addAttribute("status", status);
        model.addAttribute("departments", departmentMapper.findAll());
        return "admin/org/employees";
    }

    @GetMapping("/admin/employees/add")
    public String addForm(Model model) {
        model.addAttribute("employee", new Employee());
        commonFormData(model);
        model.addAttribute("formTitle", "新增员工档案");
        return "admin/org/employee-form";
    }

    @GetMapping("/admin/employees/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Employee e = employeeMapper.findById(id);
        if (e == null) throw new BizException("员工档案不存在");
        model.addAttribute("employee", e);
        commonFormData(model);
        model.addAttribute("formTitle", "编辑员工档案 · " + e.getName());
        return "admin/org/employee-form";
    }

    @GetMapping("/admin/employees/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Employee e = employeeMapper.findById(id);
        if (e == null) throw new BizException("员工档案不存在");
        LocalDate start = AppClock.nowDate().withDayOfMonth(1);
        model.addAttribute("employee", e);
        model.addAttribute("records", recordMapper.findRecords(id, null, start.minusMonths(1), AppClock.nowDate(), null, 40));
        model.addAttribute("leaves", leaveMapper.findRequests(id, null, null));
        model.addAttribute("schedules", scheduleMapper.findByEmployeeMonth(id, start, start.withDayOfMonth(start.lengthOfMonth())));
        return "admin/org/employee-detail";
    }

    private void commonFormData(Model model) {
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("groups", groupMapper.findAll());
        model.addAttribute("shifts", shiftMapper.findEnabled());
    }

    @PostMapping("/admin/employees/save")
    public String saveEmployee(@ModelAttribute Employee e,
                               @RequestParam(required = false) String loginUsername,
                               @RequestParam(required = false) String loginPassword,
                               RedirectAttributes ra) {
        if (employeeMapper.findByEmployeeNo(e.getEmployeeNo()) != null) {
            ra.addFlashAttribute("error", "工号 " + e.getEmployeeNo() + " 已存在");
            return "redirect:/admin/employees/add";
        }
        if (loginUsername != null && !loginUsername.trim().isEmpty()
                && userMapper.findByUsernameIgnoreEnabled(loginUsername.trim()) != null) {
            ra.addFlashAttribute("error", "登录账号 " + loginUsername + " 已被占用");
            return "redirect:/admin/employees/add";
        }
        if (e.getHireDate() == null) e.setHireDate(AppClock.nowDate());
        employeeMapper.insert(e);
        if (loginUsername != null && !loginUsername.trim().isEmpty()) {
            String pwd = (loginPassword == null || loginPassword.isEmpty()) ? "123456" : loginPassword;
            User u = new User();
            u.setUsername(loginUsername.trim());
            u.setPassword(passwordEncoder.encode(pwd));
            u.setPlainPassword(pwd);
            u.setRealName(e.getName());
            u.setRole("EMPLOYEE");
            u.setRoleLevel("EMPLOYEE");
            u.setCreatedBy(security.username());
            u.setEmployeeId(e.getId());
            u.setDeptId(e.getDepartmentId());
            u.setEnabled(true);
            userMapper.insert(u);
            Role role = roleMapper.findByCode("EMPLOYEE");
            if (role != null) userMapper.grantRole(u.getId(), role.getId());
        }
        ra.addFlashAttribute("msg", "员工档案创建成功" + (loginUsername != null && !loginUsername.isEmpty() ? "，登录账号已开通" : ""));
        return "redirect:/admin/employees";
    }

    @PostMapping("/admin/employees/update")
    public String updateEmployee(@ModelAttribute Employee e, RedirectAttributes ra) {
        Employee old = employeeMapper.findById(e.getId());
        if (old == null) throw new BizException("员工档案不存在");
        Employee sameNo = employeeMapper.findByEmployeeNo(e.getEmployeeNo());
        if (sameNo != null && !sameNo.getId().equals(e.getId())) {
            ra.addFlashAttribute("error", "工号已被其他员工占用");
            return "redirect:/admin/employees/edit/" + e.getId();
        }
        employeeMapper.update(e);
        User u = userMapper.findByEmployeeId(e.getId());
        if (u != null) userMapper.update(u);
        ra.addFlashAttribute("msg", "员工档案已更新");
        return "redirect:/admin/employees";
    }

    @PostMapping("/admin/employees/resign/{id}")
    public String resign(@PathVariable Long id, RedirectAttributes ra) {
        employeeMapper.updateStatus(id, "LEAVE", AppClock.nowDate());
        User u = userMapper.findByEmployeeId(id);
        if (u != null) userMapper.updateEnabled(u.getId(), 0);
        ra.addFlashAttribute("msg", "已办理离职：档案标记为离职、登录账号停用，历史考勤数据完整保留");
        return "redirect:/admin/employees";
    }

    // ---------- 考勤组 ----------
    @GetMapping("/admin/groups")
    public String groups(Model model) {
        model.addAttribute("groups", groupMapper.findAll());
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("shifts", shiftMapper.findEnabled());
        return "admin/org/groups";
    }

    @PostMapping("/admin/groups/save")
    public String saveGroup(@ModelAttribute AttendanceGroup g, RedirectAttributes ra) {
        if (g.getId() == null) groupMapper.insert(g);
        else {
            AttendanceGroup old = groupMapper.findById(g.getId());
            if (old != null) g.setCreateTime(old.getCreateTime());
            groupMapper.update(g);
        }
        ra.addFlashAttribute("msg", "考勤组已保存");
        return "redirect:/admin/groups";
    }

    @PostMapping("/admin/groups/delete/{id}")
    public String deleteGroup(@PathVariable Long id, RedirectAttributes ra) {
        groupMapper.deleteById(id);
        ra.addFlashAttribute("msg", "考勤组已删除（原组内员工改用个人默认班次）");
        return "redirect:/admin/groups";
    }
}
