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
@RequestMapping("/admin/schedule")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleMapper scheduleMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final ShiftMapper shiftMapper;
    private final HolidayCalendarMapper holidayMapper;
    private final SecurityHelper security;

    public ScheduleController(ScheduleService scheduleService, ScheduleMapper scheduleMapper,
                              EmployeeMapper employeeMapper, DepartmentMapper departmentMapper,
                              ShiftMapper shiftMapper, HolidayCalendarMapper holidayMapper,
                              SecurityHelper security) {
        this.scheduleService = scheduleService;
        this.scheduleMapper = scheduleMapper;
        this.employeeMapper = employeeMapper;
        this.departmentMapper = departmentMapper;
        this.shiftMapper = shiftMapper;
        this.holidayMapper = holidayMapper;
        this.security = security;
    }

    @GetMapping
    public String view(@RequestParam(required = false) String month,
                       @RequestParam(required = false) Long deptId, Model model) {
        YearMonth ym = parseMonth(month);
        LocalDate first = ym.atDay(1), last = ym.atEndOfMonth();
        Long scope = security.scopeDeptId() != null ? security.scopeDeptId() : deptId;

        List<Schedule> list = scheduleService.month(first, last, null, scope);
        Map<String, List<Schedule>> byDate = new HashMap<>();
        for (Schedule s : list) byDate.computeIfAbsent(s.getWorkDate().toString(), k -> new ArrayList<>()).add(s);
        Map<LocalDate, HolidayCalendar> holidays = new HashMap<>();
        for (HolidayCalendar h : holidayMapper.findByRange(first, last)) holidays.put(h.getCalDate(), h);

        LocalDate gridStart = first.minusDays(first.getDayOfWeek().getValue() - 1);
        LocalDate gridEnd = last.plusDays(7 - last.getDayOfWeek().getValue());
        List<Map<String, Object>> cells = new ArrayList<>();
        for (LocalDate d = gridStart; !d.isAfter(gridEnd); d = d.plusDays(1)) {
            Map<String, Object> c = new HashMap<>();
            c.put("date", d);
            c.put("day", d.getDayOfMonth());
            c.put("inMonth", !d.isBefore(first) && !d.isAfter(last));
            c.put("weekend", d.getDayOfWeek().getValue() >= 6);
            HolidayCalendar h = holidays.get(d);
            c.put("holiday", h == null ? null : h.getName());
            c.put("dayType", h == null ? null : h.getDayType());
            c.put("items", byDate.getOrDefault(d.toString(), Collections.emptyList()));
            c.put("itemCount", byDate.getOrDefault(d.toString(), Collections.emptyList()).size());
            cells.add(c);
        }

        model.addAttribute("cells", cells);
        model.addAttribute("month", ym.toString());
        model.addAttribute("prevMonth", ym.minusMonths(1).toString());
        model.addAttribute("nextMonth", ym.plusMonths(1).toString());
        model.addAttribute("departments", departmentMapper.findAll());
        model.addAttribute("shifts", shiftMapper.findEnabled());
        model.addAttribute("employees", employeeMapper.findAllActive());
        model.addAttribute("deptId", deptId);
        model.addAttribute("rows", list.size() > 150 ? list.subList(0, 150) : list);
        model.addAttribute("totalSchedules", list.size());
        return "admin/att/schedule";
    }

    @PostMapping("/generate")
    public String generate(@RequestParam String start, @RequestParam String end,
                           @RequestParam(required = false) Long deptId,
                           @RequestParam(required = false) Long shiftId,
                           @RequestParam(defaultValue = "FIXED") String mode,
                           RedirectAttributes ra) {
        LocalDate s = LocalDate.parse(start), e = LocalDate.parse(end);
        if (e.isBefore(s) || java.time.temporal.ChronoUnit.DAYS.between(s, e) > 93) {
            ra.addFlashAttribute("error", "排班区间不合法，单次最多生成 3 个月");
            return "redirect:/admin/schedule";
        }
        Long scope = security.scopeDeptId() != null ? security.scopeDeptId() : deptId;
        int n = scheduleService.generate(s, e, scope, shiftId, mode);
        ra.addFlashAttribute("msg", "已按" + ("ROTATE".equals(mode) ? "早晚轮班模板" : "固定班制")
                + "生成 " + n + " 条排班（休息日遵循工作日历）");
        return "redirect:/admin/schedule?month=" + s.toString().substring(0, 7);
    }

    @PostMapping("/save-cell")
    public String saveCell(@RequestParam Long employeeId, @RequestParam String date,
                           @RequestParam(required = false) Long shiftId,
                           @RequestParam(defaultValue = "0") int isRest, RedirectAttributes ra) {
        Schedule s = new Schedule();
        s.setEmployeeId(employeeId);
        s.setWorkDate(LocalDate.parse(date));
        s.setShiftId(isRest == 1 ? null : shiftId);
        s.setIsRest(isRest);
        s.setSource("MANUAL");
        s.setRemark("人工调整班表");
        scheduleMapper.upsert(s);
        ra.addFlashAttribute("msg", "班表已调整");
        return "redirect:/admin/schedule?month=" + date.substring(0, 7);
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isEmpty()) return AppClock.nowYearMonth();
        try { return YearMonth.parse(month); } catch (Exception e) { return AppClock.nowYearMonth(); }
    }
}
