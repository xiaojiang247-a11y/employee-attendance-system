package com.example.attendance.controller;

import com.example.attendance.util.AppClock;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import com.example.attendance.service.*;
import com.example.attendance.util.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** 员工自助：打卡、我的考勤、班表、请假/加班/出差/补卡、异常申诉、假期余额、个人中心 */
@Controller
@RequestMapping("/my")
public class EmployeePortalController {

    private final PortalService portalService;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final SecurityHelper security;
    private final AttendanceRecordMapper recordMapper;
    private final ScheduleMapper scheduleMapper;
    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveBalanceMapper balanceMapper;
    private final LeaveRequestMapper leaveMapper;
    private final OvertimeRequestMapper overtimeMapper;
    private final BusinessTripMapper tripMapper;
    private final MakeupRequestMapper makeupMapper;
    private final AttendanceAppealMapper appealMapper;
    private final ApprovalRecordMapper approvalRecordMapper;
    private final EmployeeMapper employeeMapper;
    private final UserMapper userMapper;
    private final NoticeMapper noticeMapper;
    private final PasswordEncoder passwordEncoder;

    public EmployeePortalController(PortalService portalService, AttendanceService attendanceService,
                                    LeaveService leaveService, SecurityHelper security,
                                    AttendanceRecordMapper recordMapper, ScheduleMapper scheduleMapper,
                                    LeaveTypeMapper leaveTypeMapper, LeaveBalanceMapper balanceMapper,
                                    LeaveRequestMapper leaveMapper, OvertimeRequestMapper overtimeMapper,
                                    BusinessTripMapper tripMapper, MakeupRequestMapper makeupMapper,
                                    AttendanceAppealMapper appealMapper, ApprovalRecordMapper approvalRecordMapper,
                                    EmployeeMapper employeeMapper, UserMapper userMapper,
                                    NoticeMapper noticeMapper, PasswordEncoder passwordEncoder) {
        this.portalService = portalService;
        this.attendanceService = attendanceService;
        this.leaveService = leaveService;
        this.security = security;
        this.recordMapper = recordMapper;
        this.scheduleMapper = scheduleMapper;
        this.leaveTypeMapper = leaveTypeMapper;
        this.balanceMapper = balanceMapper;
        this.leaveMapper = leaveMapper;
        this.overtimeMapper = overtimeMapper;
        this.tripMapper = tripMapper;
        this.makeupMapper = makeupMapper;
        this.appealMapper = appealMapper;
        this.approvalRecordMapper = approvalRecordMapper;
        this.employeeMapper = employeeMapper;
        this.userMapper = userMapper;
        this.noticeMapper = noticeMapper;
        this.passwordEncoder = passwordEncoder;
    }

    private Long empId() {
        Long id = security.employeeId();
        if (id == null) throw new BizException("当前账号未绑定员工档案，请联系人事开通后再使用自助功能");
        return id;
    }

    private String clientIp(HttpServletRequest req) {
        // X-Forwarded-For / X-Real-IP 由客户端任意构造，不能作为 IP 白名单依据：
        // 服务直连、未部署可信反向代理，网络层唯一可信值就是 TCP 对端地址。
        return req.getRemoteAddr();
    }

    private void submitFlow(String bizType, Long bizId, String remark) {
        ApprovalRecord ar = new ApprovalRecord();
        ar.setBizType(bizType);
        ar.setBizId(bizId);
        ar.setStepNo(1);
        ar.setAction("SUBMIT");
        ar.setRemark(remark);
        approvalRecordMapper.insert(ar);
    }

    // ---------- 工作台 ----------
    @GetMapping("/home")
    public String home(Model model) {
        Long id = security.employeeId();
        model.addAllAttributes(portalService.overview(id, AppClock.nowDate().getYear()));
        model.addAttribute("today", AppClock.nowDate());
        model.addAttribute("now", AppClock.nowDateTime());
        model.addAttribute("zoneLabel", AppClock.ZONE_LABEL);
        model.addAttribute("punchRule", com.example.attendance.util.PunchRule.describe());
        return "portal/home";
    }

    @PostMapping("/check-in")
    public String checkIn(HttpServletRequest req,
                          @RequestParam(required = false) Double lat,
                          @RequestParam(required = false) Double lng,
                          @RequestParam(required = false) String wifiSsid,
                          RedirectAttributes ra) {
        try {
            ra.addFlashAttribute("msg", attendanceService.checkIn(
                    security.employeeId(), clientIp(req), "WEB", lat, lng, wifiSsid));
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my/home";
    }

    @PostMapping("/check-out")
    public String checkOut(HttpServletRequest req,
                           @RequestParam(required = false) Double lat,
                           @RequestParam(required = false) Double lng,
                           @RequestParam(required = false) String wifiSsid,
                           RedirectAttributes ra) {
        try {
            ra.addFlashAttribute("msg", attendanceService.checkOut(
                    security.employeeId(), clientIp(req), lat, lng, wifiSsid));
        } catch (BizException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/my/home";
    }

    // ---------- 我的考勤 ----------
    @GetMapping("/attendance")
    public String attendance(@RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @RequestParam(required = false) String status,
                             @RequestParam(defaultValue = "1") int page, Model model) {
        LocalDate end = parse(endDate, AppClock.nowDate());
        LocalDate start = parse(startDate, end.minusDays(29));
        List<AttendanceRecord> all = recordMapper.findRecords(empId(), null, start, end, status, 500);
        Page<AttendanceRecord> p = Page.of(all, page, 15);
        model.addAttribute("rows", p.getList());
        model.addAttribute("pageData", p);
        model.addAttribute("startDate", start);
        model.addAttribute("endDate", end);
        model.addAttribute("status", status);
        model.addAttribute("total", all.size());
        return "portal/attendance";
    }

    @PostMapping("/appeal")
    public String appeal(@RequestParam Long recordId, @RequestParam String reason,
                         RedirectAttributes ra) {
        AttendanceRecord r = recordMapper.findById(recordId);
        if (r == null || !r.getEmployeeId().equals(empId())) throw new BizException("考勤记录不存在");
        AttendanceAppeal a = new AttendanceAppeal();
        a.setEmployeeId(empId());
        a.setRecordId(recordId);
        a.setAppealDate(r.getAttendanceDate());
        a.setExceptionType(r.getStatus());
        a.setReason(reason);
        a.setStatus("PENDING");
        appealMapper.insert(a);
        submitFlow("APPEAL", a.getId(), "提交异常申诉：" + r.getStatus());
        ra.addFlashAttribute("msg", "异常申诉已提交，等待考勤员复核");
        return "redirect:/my/attendance";
    }

    // ---------- 我的班表 ----------
    @GetMapping("/schedule")
    public String schedule(@RequestParam(required = false) String month, Model model) {
        YearMonth ym = parseMonth(month);
        LocalDate start = ym.atDay(1), end = ym.atEndOfMonth();
        model.addAttribute("rows", scheduleMapper.findByEmployeeMonth(empId(), start, end));
        model.addAttribute("month", ym.toString());
        model.addAttribute("prevMonth", ym.minusMonths(1).toString());
        model.addAttribute("nextMonth", ym.plusMonths(1).toString());
        return "portal/schedule";
    }

    // ---------- 请假 ----------
    @GetMapping("/leave")
    public String leave(Model model) {
        model.addAttribute("types", leaveTypeMapper.findEnabled());
        model.addAttribute("balances", balanceMapper.findByYear(AppClock.nowDate().getYear(), empId(), null));
        model.addAttribute("rows", leaveMapper.findRequests(empId(), null, null));
        return "portal/leave";
    }

    @PostMapping("/leave/apply")
    public String applyLeave(@RequestParam String typeCode, @RequestParam String startDate,
                             @RequestParam String endDate, @RequestParam String reason,
                             RedirectAttributes ra) {
        LocalDate s = reqDate(startDate, "开始"), e = reqDate(endDate, "结束");
        if (e.isBefore(s)) throw new BizException("结束日期不能早于开始日期");
        LeaveRequest lr = leaveService.apply(empId(), typeCode, s, e, reason);
        submitFlow("LEAVE", lr.getId(), "提交请假申请 " + lr.getDays() + " 天");
        ra.addFlashAttribute("msg", "请假申请已提交（" + lr.getDays() + " 天），等待审批");
        return "redirect:/my/leave";
    }

    @PostMapping("/leave/cancel/{id}")
    public String cancelLeave(@PathVariable Long id, RedirectAttributes ra) {
        leaveService.cancel(id, empId());
        ra.addFlashAttribute("msg", "请假单已撤销");
        return "redirect:/my/leave";
    }

    // ---------- 加班 ----------
    @GetMapping("/overtime")
    public String overtime(Model model) {
        model.addAttribute("rows", overtimeMapper.findList(empId(), null, null));
        return "portal/overtime";
    }

    @PostMapping("/overtime/apply")
    public String applyOvertime(@RequestParam String otDate, @RequestParam BigDecimal hours,
                                @RequestParam String otType, @RequestParam String compensateType,
                                @RequestParam String reason, RedirectAttributes ra) {
        LocalDate d = reqDate(otDate, "加班");
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0
                || hours.compareTo(BigDecimal.valueOf(24)) > 0)
            throw new BizException("加班时长必须在 0~24 小时之间");
        OvertimeRequest o = new OvertimeRequest();
        o.setEmployeeId(empId());
        o.setOtDate(d);
        o.setStartTime(LocalDateTime.of(d, java.time.LocalTime.of(18, 0)));
        o.setEndTime(LocalDateTime.of(d, java.time.LocalTime.of(18, 0)).plusMinutes(hours.multiply(BigDecimal.valueOf(60)).longValue()));
        o.setHours(hours);
        o.setOtType(otType);
        o.setCompensateType(compensateType);
        o.setReason(reason);
        o.setStatus("PENDING");
        o.setCurrentStep(1);
        overtimeMapper.insert(o);
        submitFlow("OVERTIME", o.getId(), "提交加班申请 " + hours + " 小时");
        ra.addFlashAttribute("msg", "加班申请已提交，审批通过后自动计入月度汇总");
        return "redirect:/my/overtime";
    }

    @PostMapping("/overtime/cancel/{id}")
    public String cancelOvertime(@PathVariable Long id, RedirectAttributes ra) {
        OvertimeRequest o = overtimeMapper.findById(id);
        Long my = security.employeeId();
        // 归属 + 状态双重校验：员工只能撤回本人、且尚未审批的申请
        if (o == null || my == null || !my.equals(o.getEmployeeId())) {
            ra.addFlashAttribute("error", "只能撤回本人提交的加班申请");
            return "redirect:/my/overtime";
        }
        if (!"PENDING".equals(o.getStatus())) {
            ra.addFlashAttribute("error", "该申请已进入审批流程，不能撤回");
            return "redirect:/my/overtime";
        }
        overtimeMapper.deleteById(id);
        ra.addFlashAttribute("msg", "加班申请已撤回");
        return "redirect:/my/overtime";
    }

    // ---------- 出差 ----------
    @GetMapping("/trip")
    public String trip(Model model) {
        model.addAttribute("rows", tripMapper.findList(empId(), null, null));
        return "portal/trip";
    }

    @PostMapping("/trip/apply")
    public String applyTrip(@RequestParam String destination, @RequestParam String startDate,
                            @RequestParam String endDate, @RequestParam String reason,
                            RedirectAttributes ra) {
        LocalDate s = reqDate(startDate, "开始"), e = reqDate(endDate, "结束");
        if (e.isBefore(s)) throw new BizException("结束日期不能早于开始日期");
        BusinessTrip t = new BusinessTrip();
        t.setEmployeeId(empId());
        t.setDestination(destination);
        t.setStartDate(s);
        t.setEndDate(e);
        t.setDays(BigDecimal.valueOf(ChronoUnit.DAYS.between(s, e) + 1));
        t.setReason(reason);
        t.setStatus("PENDING");
        tripMapper.insert(t);
        submitFlow("TRIP", t.getId(), "提交出差申请 " + t.getDays() + " 天");
        ra.addFlashAttribute("msg", "出差申请已提交");
        return "redirect:/my/trip";
    }

    // ---------- 补卡 ----------
    @GetMapping("/makeup")
    public String makeup(Model model) {
        model.addAttribute("rows", makeupMapper.findList(empId(), null, null));
        return "portal/makeup";
    }

    @PostMapping("/makeup/apply")
    public String applyMakeup(@RequestParam String missDate, @RequestParam String punchType,
                              @RequestParam String makeupTime, @RequestParam String reason,
                              RedirectAttributes ra) {
        LocalDate d = reqDate(missDate, "缺卡");
        String mt = makeupTime != null && makeupTime.length() == 4 ? "0" + makeupTime : makeupTime;
        java.time.LocalTime time;
        try { time = java.time.LocalTime.parse(mt); }
        catch (Exception ex) { throw new BizException("补卡时间格式不正确，应为 HH:mm"); }
        MakeupRequest m = new MakeupRequest();
        m.setEmployeeId(empId());
        m.setMissDate(d);
        m.setPunchType(punchType);
        m.setMakeupTime(LocalDateTime.of(d, time));
        m.setReason(reason);
        m.setStatus("PENDING");
        makeupMapper.insert(m);
        submitFlow("MAKEUP", m.getId(), "提交补卡申请 " + missDate + " " + punchType);
        ra.addFlashAttribute("msg", "补卡申请已提交，审批通过后将自动回填打卡记录");
        return "redirect:/my/makeup";
    }

    // ---------- 假期余额 ----------
    @GetMapping("/balance")
    public String balance(@RequestParam(required = false) Integer year, Model model) {
        int y = year == null ? AppClock.nowDate().getYear() : year;
        model.addAttribute("year", y);
        model.addAttribute("rows", balanceMapper.findByYear(y, empId(), null));
        return "portal/balance";
    }

    // ---------- 个人中心 ----------
    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("employee", employeeMapper.findById(empId()));
        // 个人中心只需要展示资料，不要把密码哈希 / 明文密码带进页面模型
        User view = security.currentUser();
        if (view != null) { view.setPassword(null); view.setPlainPassword(null); }
        model.addAttribute("user", view);
        return "portal/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String phone, @RequestParam String email,
                                @RequestParam String address, RedirectAttributes ra) {
        Employee e = employeeMapper.findById(empId());
        if (e != null) {
            e.setPhone(phone);
            e.setEmail(email);
            e.setAddress(address);
            employeeMapper.update(e);
        }
        ra.addFlashAttribute("msg", "个人资料已更新");
        return "redirect:/my/profile";
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String oldPassword, @RequestParam String newPassword,
                                 RedirectAttributes ra) {
        User u = security.currentUser();
        if (u == null || !passwordEncoder.matches(oldPassword, u.getPassword())) {
            ra.addFlashAttribute("error", "原密码不正确");
            return "redirect:/my/profile";
        }
        if (newPassword.length() < 6) {
            ra.addFlashAttribute("error", "新密码长度不能少于 6 位");
            return "redirect:/my/profile";
        }
        userMapper.updatePassword(u.getId(), passwordEncoder.encode(newPassword), newPassword);
        ra.addFlashAttribute("msg", "密码已修改，请牢记新密码");
        return "redirect:/my/profile";
    }

    /** 员工端表单日期：非法格式转成业务提示，不让 DateTimeParseException 冒到 500 页 */
    private LocalDate reqDate(String s, String label) {
        try { return LocalDate.parse(s); }
        catch (Exception e) { throw new BizException(label + "日期格式不正确，应为 yyyy-MM-dd"); }
    }

    private LocalDate parse(String s, LocalDate fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return LocalDate.parse(s); } catch (Exception e) { return fallback; }
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isEmpty()) return AppClock.nowYearMonth();
        try { return YearMonth.parse(month); } catch (Exception e) { return AppClock.nowYearMonth(); }
    }
}
