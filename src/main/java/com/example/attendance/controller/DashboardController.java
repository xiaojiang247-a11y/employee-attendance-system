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
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityHelper security;

    public DashboardController(DashboardService dashboardService, SecurityHelper security) {
        this.dashboardService = dashboardService;
        this.security = security;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        if (security.isEmployee()) return "redirect:/my/home";
        model.addAllAttributes(dashboardService.admin());
        model.addAttribute("today", AppClock.nowDate());
        return "dashboard/admin";
    }
}
