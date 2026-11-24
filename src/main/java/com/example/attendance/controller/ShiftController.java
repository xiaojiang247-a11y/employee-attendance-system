package com.example.attendance.controller;

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
@RequestMapping("/admin/shifts")
public class ShiftController {

    private final ShiftMapper shiftMapper;

    public ShiftController(ShiftMapper shiftMapper) {
        this.shiftMapper = shiftMapper;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("shifts", shiftMapper.findAll());
        return "admin/att/shifts";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Shift s, RedirectAttributes ra) {
        if (s.getCode() == null || s.getCode().trim().isEmpty()) {
            ra.addFlashAttribute("error", "班次编码不能为空");
            return "redirect:/admin/shifts";
        }
        if (s.getId() == null) {
            if (shiftMapper.findAll().stream().anyMatch(x -> x.getCode().equals(s.getCode()))) {
                ra.addFlashAttribute("error", "班次编码已存在");
                return "redirect:/admin/shifts";
            }
            shiftMapper.insert(s);
        } else {
            shiftMapper.update(s);
        }
        ra.addFlashAttribute("msg", "班次已保存，后续打卡将按新规则判定");
        return "redirect:/admin/shifts";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        shiftMapper.deleteById(id);
        ra.addFlashAttribute("msg", "班次已删除");
        return "redirect:/admin/shifts";
    }
}
