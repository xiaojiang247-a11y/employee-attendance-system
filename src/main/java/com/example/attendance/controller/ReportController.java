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
@RequestMapping("/admin/report")
public class ReportController {

    private final ReportService reportService;
    private final StatsMapper statsMapper;
    private final AttendanceSummaryMapper summaryMapper;
    private final DepartmentMapper departmentMapper;
    private final SecurityHelper security;

    public ReportController(ReportService reportService, StatsMapper statsMapper,
                            AttendanceSummaryMapper summaryMapper,
                            DepartmentMapper departmentMapper, SecurityHelper security) {
        this.reportService = reportService;
        this.statsMapper = statsMapper;
        this.summaryMapper = summaryMapper;
        this.departmentMapper = departmentMapper;
        this.security = security;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String ym,
                        @RequestParam(required = false) Long deptId, Model model) {
        if (ym == null || ym.isEmpty()) ym = AppClock.nowYearMonth().toString();
        Long scope = security.scopeDeptId() != null ? security.scopeDeptId() : deptId;
        LocalDate today = AppClock.nowDate();
        model.addAttribute("ym", ym);
        model.addAttribute("months", summaryMapper.findAllMonths());
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("deptId", deptId);
        model.addAttribute("rows", reportService.monthly(ym, scope));
        model.addAttribute("monthlyTrend", reportService.monthlyTrend(
                YearMonth.parse(ym).minusMonths(5).toString()));
        model.addAttribute("statusDist", statsMapper.statusDistribution(today.minusDays(29), today));
        model.addAttribute("leaveUsage", statsMapper.leaveUsage(today.minusDays(89), today));
        model.addAttribute("overtimeRanking", statsMapper.overtimeRanking(today.minusDays(89), today));
        model.addAttribute("deptRank", statsMapper.deptAttendance(today.minusDays(29), today));
        return "admin/report/index";
    }

    /** 月度汇总导出为 CSV（UTF-8 BOM，Excel 直接打开） */
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String ym,
                       @RequestParam(required = false) Long deptId,
                       HttpServletResponse response) throws java.io.IOException {
        if (ym == null || ym.isEmpty()) ym = AppClock.nowYearMonth().toString();
        Long scope = security.scopeDeptId() != null ? security.scopeDeptId() : deptId;
        String csv = reportService.monthlyCsv(ym, scope);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=attendance_summary_" + ym + ".csv");
        response.getWriter().write(csv);
        response.getWriter().flush();
    }
}
