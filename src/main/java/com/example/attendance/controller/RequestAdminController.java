package com.example.attendance.controller;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import com.example.attendance.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RequestAdminController {

    private final OvertimeRequestMapper overtimeMapper;
    private final BusinessTripMapper tripMapper;
    private final MakeupRequestMapper makeupMapper;
    private final AttendanceAppealMapper appealMapper;
    private final ApprovalService approvalService;
    private final SecurityHelper security;
    private final ObjectMapper objectMapper;

    public RequestAdminController(OvertimeRequestMapper overtimeMapper, BusinessTripMapper tripMapper,
                                  MakeupRequestMapper makeupMapper, AttendanceAppealMapper appealMapper,
                                  ApprovalService approvalService, SecurityHelper security, ObjectMapper objectMapper) {
        this.overtimeMapper = overtimeMapper;
        this.tripMapper = tripMapper;
        this.makeupMapper = makeupMapper;
        this.appealMapper = appealMapper;
        this.approvalService = approvalService;
        this.security = security;
        this.objectMapper = objectMapper;
    }

    private Long scope() { return security.scopeDeptId(); }

    @GetMapping("/admin/overtime")
    public String overtime(@RequestParam(required = false) String status, Model model) {
        return render("OVERTIME", "加班申请台账", status,
                overtimeMapper.findList(null, scope(), status), model);
    }

    @GetMapping("/admin/trips")
    public String trips(@RequestParam(required = false) String status, Model model) {
        return render("TRIP", "出差申请台账", status,
                tripMapper.findList(null, scope(), status), model);
    }

    @GetMapping("/admin/makeups")
    public String makeups(@RequestParam(required = false) String status, Model model) {
        return render("MAKEUP", "补卡申请台账", status,
                makeupMapper.findList(null, scope(), status), model);
    }

    @GetMapping("/admin/appeals")
    public String appeals(@RequestParam(required = false) String status, Model model) {
        return render("APPEAL", "考勤异常申诉", status,
                appealMapper.findList(null, scope(), status), model);
    }

    private String render(String type, String title, String status, List<?> rows, Model model) {
        // 四类单据字段不一致，统一转成 Map 后再交给模板，避免类型差异导致取值异常
        List<Map<String, Object>> unified = new ArrayList<>();
        for (Object o : rows) {
            unified.add(objectMapper.convertValue(o,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
        }
        model.addAttribute("type", type);
        model.addAttribute("title", title);
        model.addAttribute("status", status);
        model.addAttribute("rows", unified);
        return "admin/approval/request-list";
    }
}
