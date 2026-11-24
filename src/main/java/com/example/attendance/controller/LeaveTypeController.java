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
public class LeaveTypeController {

    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveBalanceMapper balanceMapper;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final SecurityHelper security;

    public LeaveTypeController(LeaveTypeMapper leaveTypeMapper, LeaveBalanceMapper balanceMapper,
                               DepartmentMapper departmentMapper, EmployeeMapper employeeMapper,
                               SecurityHelper security) {
        this.leaveTypeMapper = leaveTypeMapper;
        this.balanceMapper = balanceMapper;
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.security = security;
    }

    @GetMapping("/admin/leave-types")
    public String types(Model model) {
        model.addAttribute("types", leaveTypeMapper.findAll());
        return "admin/att/leave-types";
    }

    @PostMapping("/admin/leave-types/save")
    public String saveType(@ModelAttribute LeaveType t, RedirectAttributes ra) {
        if (t.getCode() == null || t.getCode().trim().isEmpty()) {
            ra.addFlashAttribute("error", "假别编码不能为空");
            return "redirect:/admin/leave-types";
        }
        if (t.getId() == null) leaveTypeMapper.insert(t);
        else leaveTypeMapper.update(t);
        ra.addFlashAttribute("msg", "假别规则已保存");
        return "redirect:/admin/leave-types";
    }

    @PostMapping("/admin/leave-types/delete/{id}")
    public String deleteType(@PathVariable Long id, RedirectAttributes ra) {
        leaveTypeMapper.deleteById(id);
        ra.addFlashAttribute("msg", "假别已删除");
        return "redirect:/admin/leave-types";
    }

    @GetMapping("/admin/leave-balances")
    public String balances(@RequestParam(required = false) Integer year,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(defaultValue = "1") int page, Model model) {
        int y = year == null ? AppClock.nowDate().getYear() : year;
        List<LeaveBalance> all = balanceMapper.findByYear(y, null,
                security.scopeDeptId() != null ? security.scopeDeptId() : deptId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            all = all.stream().filter(b -> (b.getEmployeeName() != null && b.getEmployeeName().contains(k))
                    || (b.getEmployeeNo() != null && b.getEmployeeNo().contains(k))).toList();
        }
        Page<LeaveBalance> p = Page.of(all, page, 20);
        model.addAttribute("rows", p.getList());
        model.addAttribute("pageData", p);
        model.addAttribute("year", y);
        model.addAttribute("deptId", deptId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("departments", departmentMapper.findAll());
        return "admin/att/leave-balances";
    }

    @PostMapping("/admin/leave-balances/grant")
    public String grant(@RequestParam int year, RedirectAttributes ra) {
        int n = balanceMapper.grantYearQuota(year);
        ra.addFlashAttribute("msg", year + " 年度额度发放完成，新增 " + n + " 条额度记录（已有额度不覆盖）");
        return "redirect:/admin/leave-balances?year=" + year;
    }

    @PostMapping("/admin/leave-balances/adjust")
    public String adjust(@RequestParam Long id, @RequestParam BigDecimal days, RedirectAttributes ra) {
        balanceMapper.addDays(id, days);
        ra.addFlashAttribute("msg", "额度已调整 " + days + " 天");
        return "redirect:/admin/leave-balances";
    }
}
