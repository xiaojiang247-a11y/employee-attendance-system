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
@RequestMapping("/admin/holidays")
public class HolidayController {

    private final HolidayCalendarMapper holidayMapper;

    public HolidayController(HolidayCalendarMapper holidayMapper) {
        this.holidayMapper = holidayMapper;
    }

    @GetMapping
    public String list(@RequestParam(required = false) Integer year, Model model) {
        int y = year == null ? AppClock.nowDate().getYear() : year;
        LocalDate start = LocalDate.of(y, 1, 1), end = LocalDate.of(y, 12, 31);
        List<HolidayCalendar> all = holidayMapper.findByRange(start, end);
        long rest = all.stream().filter(h -> !"WORKDAY".equals(h.getDayType())).count();
        long holiday = all.stream().filter(h -> "HOLIDAY".equals(h.getDayType())).count();
        model.addAttribute("rows", all.size() > 200 ? all.subList(0, 200) : all);
        model.addAttribute("total", all.size());
        model.addAttribute("restCount", rest);
        model.addAttribute("holidayCount", holiday);
        model.addAttribute("workdayCount", all.stream().filter(h -> "WORKDAY".equals(h.getDayType())).count());
        model.addAttribute("year", y);
        return "admin/att/holidays";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute HolidayCalendar h, RedirectAttributes ra) {
        holidayMapper.upsert(h);
        ra.addFlashAttribute("msg", "工作日历已更新，排班与请假天数计算会立即采用新日历");
        return "redirect:/admin/holidays?year=" + h.getCalDate().getYear();
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        holidayMapper.deleteById(id);
        ra.addFlashAttribute("msg", "日历条目已删除");
        return "redirect:/admin/holidays";
    }
}
