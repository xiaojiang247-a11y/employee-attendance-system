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
@RequestMapping("/admin/attendance")
public class AttendanceAdminController {

    private final AttendanceRecordMapper recordMapper;
    private final AttendanceSummaryMapper summaryMapper;
    private final AttendanceService attendanceService;
    private final DepartmentMapper departmentMapper;
    private final EmployeeMapper employeeMapper;
    private final SecurityHelper security;

    public AttendanceAdminController(AttendanceRecordMapper recordMapper,
                                     AttendanceSummaryMapper summaryMapper,
                                     AttendanceService attendanceService,
                                     DepartmentMapper departmentMapper,
                                     EmployeeMapper employeeMapper, SecurityHelper security) {
        this.recordMapper = recordMapper;
        this.summaryMapper = summaryMapper;
        this.attendanceService = attendanceService;
        this.departmentMapper = departmentMapper;
        this.employeeMapper = employeeMapper;
        this.security = security;
    }

    @GetMapping("/records")
    public String records(@RequestParam(required = false) String startDate,
                          @RequestParam(required = false) String endDate,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) Long deptId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") int page, Model model) {
        LocalDate end = parse(endDate, AppClock.nowDate());
        LocalDate start = parse(startDate, end.minusDays(29));
        Long scope = security.scopeDeptId() != null ? security.scopeDeptId() : deptId;
        List<AttendanceRecord> all = recordMapper.findRecords(null, scope, start, end, status, 800);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            all = all.stream().filter(r -> (r.getEmployeeName() != null && r.getEmployeeName().contains(k))
                    || (r.getEmployeeNo() != null && r.getEmployeeNo().contains(k))).toList();
        }
        Page<AttendanceRecord> p = Page.of(all, page, 15);
        model.addAttribute("rows", p.getList());
        model.addAttribute("pageData", p);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("status", status);
        model.addAttribute("deptId", deptId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("totalRecords", all.size());
        return "admin/att/records";
    }

    @GetMapping("/exceptions")
    public String exceptions(@RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @RequestParam(required = false) Long deptId, Model model) {
        LocalDate end = parse(endDate, AppClock.nowDate());
        LocalDate start = parse(startDate, end.minusDays(29));
        List<AttendanceRecord> rows = recordMapper.findExceptions(start, end,
                security.scopeDeptId() != null ? security.scopeDeptId() : deptId);
        Map<String, Long> byType = new LinkedHashMap<>();
        for (AttendanceRecord r : rows) byType.merge(r.getStatus(), 1L, Long::sum);
        model.addAttribute("rows", rows);
        model.addAttribute("byType", byType);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("departments", departmentMapper.findAll());
        return "admin/att/exceptions";
    }

    @GetMapping("/summary")
    public String summary(@RequestParam(required = false) String ym,
                          @RequestParam(required = false) Long deptId,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "1") int page, Model model) {
        if (ym == null || ym.isEmpty()) ym = AppClock.nowYearMonth().toString();
        List<AttendanceSummary> all = summaryMapper.findByMonth(ym,
                security.scopeDeptId() != null ? security.scopeDeptId() : deptId, keyword);
        Page<AttendanceSummary> p = Page.of(all, page, 15);
        model.addAttribute("rows", p.getList());
        model.addAttribute("pageData", p);
        model.addAttribute("ym", ym);
        model.addAttribute("months", summaryMapper.findAllMonths());
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("keyword", keyword);
        model.addAttribute("locked", summaryMapper.countLocked(ym));
        return "admin/att/summary";
    }

    @PostMapping("/summary/generate")
    public String generateSummary(@RequestParam String ym, RedirectAttributes ra) {
        if (summaryMapper.countLocked(ym) > 0) {
            ra.addFlashAttribute("error", ym + " 已关账锁定，请先解锁后再重算");
        } else {
            int n = attendanceService.generateSummary(ym);
            ra.addFlashAttribute("msg", ym + " 月度考勤已重算，共处理 " + n + " 名员工");
        }
        return "redirect:/admin/attendance/summary?ym=" + ym;
    }

    @PostMapping("/summary/lock")
    public String lock(@RequestParam String ym, @RequestParam int locked, RedirectAttributes ra) {
        summaryMapper.updateLock(ym, locked);
        ra.addFlashAttribute("msg", locked == 1 ? ym + " 已关账锁定" : ym + " 已解锁");
        return "redirect:/admin/attendance/summary?ym=" + ym;
    }

    @PostMapping("/records/correct/{id}")
    public String correct(@PathVariable Long id, RedirectAttributes ra) {
        AttendanceRecord r = recordMapper.findById(id);
        if (r == null) throw new BizException("考勤记录不存在");
        r.setStatus("NORMAL");
        r.setLateMinutes(0);
        r.setEarlyMinutes(0);
        r.setRemark("管理员人工修正为正常");
        recordMapper.update(r);
        ra.addFlashAttribute("msg", "考勤结果已修正为正常");
        return "redirect:/admin/attendance/exceptions";
    }

    @PostMapping("/records/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        recordMapper.deleteById(id);
        ra.addFlashAttribute("msg", "考勤记录已删除");
        return "redirect:/admin/attendance/records";
    }

    private LocalDate parse(String s, LocalDate fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return LocalDate.parse(s); } catch (Exception e) { return fallback; }
    }
}
