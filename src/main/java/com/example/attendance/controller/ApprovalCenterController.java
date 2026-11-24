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
@RequestMapping("/admin/approval")
public class ApprovalCenterController {

    private final ApprovalService approvalService;
    private final ApprovalMapper approvalMapper;
    private final LeaveRequestMapper leaveMapper;
    private final OvertimeRequestMapper overtimeMapper;
    private final BusinessTripMapper tripMapper;
    private final MakeupRequestMapper makeupMapper;
    private final AttendanceAppealMapper appealMapper;
    private final ApprovalRecordMapper recordMapper;
    private final SecurityHelper security;
    private final ObjectMapper objectMapper;

    public ApprovalCenterController(ApprovalService approvalService, ApprovalMapper approvalMapper,
                                    LeaveRequestMapper leaveMapper, OvertimeRequestMapper overtimeMapper,
                                    BusinessTripMapper tripMapper, MakeupRequestMapper makeupMapper,
                                    AttendanceAppealMapper appealMapper, ApprovalRecordMapper recordMapper,
                                    SecurityHelper security, ObjectMapper objectMapper) {
        this.approvalService = approvalService;
        this.approvalMapper = approvalMapper;
        this.leaveMapper = leaveMapper;
        this.overtimeMapper = overtimeMapper;
        this.tripMapper = tripMapper;
        this.makeupMapper = makeupMapper;
        this.appealMapper = appealMapper;
        this.recordMapper = recordMapper;
        this.security = security;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/pending")
    public String pending(@RequestParam(required = false) String bizType, Model model) {
        List<ApprovalTask> tasks = approvalService.pending();
        if (bizType != null && !bizType.isEmpty()) {
            tasks = tasks.stream().filter(t -> bizType.equals(t.getBizType())).toList();
        }
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ApprovalTask t : approvalService.pending()) counts.merge(t.getBizType(), 1L, Long::sum);
        Map<String, String> typeNames = new HashMap<>();
        for (String k : counts.keySet()) typeNames.put(k, approvalService.bizTypeName(k));
        model.addAttribute("tasks", tasks);
        model.addAttribute("counts", counts);
        model.addAttribute("typeNames", typeNames);
        model.addAttribute("bizType", bizType);
        return "admin/approval/pending";
    }

    @GetMapping("/done")
    public String done(Model model) {
        List<ApprovalRecord> rows = approvalService.done();
        for (ApprovalRecord r : rows) {
            r.setBizTitle(approvalService.bizTypeName(r.getBizType()) + " · 单据 #" + r.getBizId());
        }
        model.addAttribute("rows", rows);
        return "admin/approval/done";
    }

    @GetMapping("/detail")
    public String detail(@RequestParam String bizType, @RequestParam Long bizId, Model model) {
        Object detail;
        switch (bizType) {
            case "LEAVE": detail = leaveMapper.findById(bizId); break;
            case "OVERTIME": detail = overtimeMapper.findById(bizId); break;
            case "TRIP": detail = tripMapper.findById(bizId); break;
            case "MAKEUP": detail = makeupMapper.findById(bizId); break;
            case "APPEAL": detail = appealMapper.findById(bizId); break;
            default: throw new BizException("未知单据类型");
        }
        if (detail == null) throw new BizException("单据不存在");
        // 不同单据是不同实体，统一转为 Map，页面用 ['字段'] 取值时缺失字段返回 null 而非报错
        model.addAttribute("detail", objectMapper.convertValue(detail,
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {}));
        model.addAttribute("bizType", bizType);
        model.addAttribute("bizTypeName", approvalService.bizTypeName(bizType));
        model.addAttribute("history", approvalService.history(bizType, bizId));
        return "admin/approval/detail";
    }

    @PostMapping("/decide")
    public String decide(@RequestParam String bizType, @RequestParam Long bizId,
                         @RequestParam boolean approve,
                         @RequestParam(required = false) String remark,
                         RedirectAttributes ra) {
        approvalService.decide(bizType, bizId, approve, remark == null ? "" : remark);
        ra.addFlashAttribute("msg", (approve ? "已通过" : "已驳回") + " "
                + approvalService.bizTypeName(bizType) + "单据 #" + bizId);
        return "redirect:/admin/approval/pending";
    }
}
