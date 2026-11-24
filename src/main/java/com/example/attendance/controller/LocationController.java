package com.example.attendance.controller;

import com.example.attendance.entity.AttendanceGroup;
import com.example.attendance.entity.PunchLocation;
import com.example.attendance.entity.User;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.AttendanceGroupMapper;
import com.example.attendance.mapper.PunchLocationMapper;
import com.example.attendance.service.AccountService;
import com.example.attendance.service.SecurityHelper;
import com.example.attendance.util.AppClock;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理员子系统 - 打卡地点围栏配置。
 * 管理员可设置员工打卡地点；超级管理员创建的地点管理员不能改（层级保护），
 * 超管通过 /super/locations 可修改/覆盖管理员设置的任何地点。
 */
@Controller
@RequestMapping("/admin/locations")
public class LocationController {

    private final PunchLocationMapper locationMapper;
    private final AttendanceGroupMapper groupMapper;
    private final AccountService accountService;
    private final SecurityHelper security;

    public LocationController(PunchLocationMapper locationMapper, AttendanceGroupMapper groupMapper,
                              AccountService accountService, SecurityHelper security) {
        this.locationMapper = locationMapper;
        this.groupMapper = groupMapper;
        this.accountService = accountService;
        this.security = security;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("locations", locationMapper.findAll());
        List<AttendanceGroup> groups = groupMapper.findAll();
        model.addAttribute("groups", groups);
        model.addAttribute("enabledCount", locationMapper.countEnabled());
        model.addAttribute("zoneLabel", AppClock.ZONE_LABEL);
        return "admin/att/locations";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute PunchLocation l,
                       @RequestParam(required = false) List<Long> groupIds,
                       RedirectAttributes ra) {
        try {
            validate(l);
            PunchLocation old = l.getId() == null ? null : locationMapper.findById(l.getId());
            assertCanEdit(old);
            String who = actor(), role = actorRole();
            l.setUpdatedBy(who);
            l.setUpdatedByRole(role);
            boolean create = l.getId() == null;
            if (create) {
                l.setCreatedBy(who);
                l.setCreatedByRole(role);
                locationMapper.insert(l);
            } else {
                locationMapper.update(l);
            }
            locationMapper.clearGroups(l.getId());
            if (groupIds != null) {
                for (Long gid : groupIds) locationMapper.grantGroup(gid, l.getId());
            }
            if (l.getIsDefault() != null && l.getIsDefault() == 1) locationMapper.setDefault(l.getId());
            accountService.writeLocationLog("LOCATION_SAVE", l.getName(),
                    (create ? "新增" : "修改") + "打卡地点，半径 " + l.getRadiusM()
                            + "m，设置人 " + who + "（" + role + "）");
            ra.addFlashAttribute("msg", "打卡地点「" + l.getName() + "」已保存");
        } catch (BizException e) {
            accountService.writeFailure("LOCATION_SAVE", l.getName(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/locations";
    }

    @PostMapping("/default/{id}")
    public String setDefault(@PathVariable Long id, RedirectAttributes ra) {
        return mutate(id, ra, "LOCATION_DEFAULT", "设为默认地点", () -> locationMapper.setDefault(id));
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, @RequestParam int enabled, RedirectAttributes ra) {
        return mutate(id, ra, "LOCATION_TOGGLE", enabled == 1 ? "启用地点" : "停用地点",
                () -> locationMapper.updateEnabled(id, enabled));
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        return mutate(id, ra, "LOCATION_DELETE", "删除地点", () -> {
            locationMapper.clearGroups(id);
            locationMapper.deleteById(id);
        });
    }

    /** 统一做层级断言 + 操作留痕，避免每个动作各写一遍 */
    private String mutate(Long id, RedirectAttributes ra, String action, String desc, Runnable op) {
        PunchLocation l = locationMapper.findById(id);
        if (l == null) {
            accountService.writeFailure(action, String.valueOf(id), "地点不存在或已被删除");
            ra.addFlashAttribute("error", "地点不存在或已被删除");
            return "redirect:/admin/locations";
        }
        try {
            assertCanEdit(l);
            op.run();
            accountService.writeLocationLog(action, l == null ? String.valueOf(id) : l.getName(), desc);
            ra.addFlashAttribute("msg", desc + "成功");
        } catch (BizException e) {
            accountService.writeFailure(action, l == null ? String.valueOf(id) : l.getName(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/locations";
    }

    private void validate(PunchLocation l) {
        if (l.getName() == null || l.getName().trim().isEmpty())
            throw new BizException("地点名称不能为空");
        if (l.getEnabled() == null) l.setEnabled(1);
        if (l.getRadiusM() == null || l.getRadiusM() <= 0) l.setRadiusM(200);
        if (l.getIsDefault() == null) l.setIsDefault(0);
        // 只填一个坐标属于配置错误，直接拦下，避免围栏静默失效
        if ((l.getLongitude() == null) != (l.getLatitude() == null))
            throw new BizException("经度和纬度必须同时填写");
        if (l.getLongitude() != null) {
            BigDecimal lon = l.getLongitude(), lat = l.getLatitude();
            if (lon.abs().compareTo(new BigDecimal("180")) > 0
                    || lat.abs().compareTo(new BigDecimal("90")) > 0)
                throw new BizException("经纬度超出有效范围（经度 ±180、纬度 ±90）");
        }
    }

    /** 层级保护：管理员不能修改/停用/删除超管创建的地点 */
    private void assertCanEdit(PunchLocation old) {
        if (old != null && "SUPER_ADMIN".equals(old.getCreatedByRole()) && !security.isSuperAdmin()) {
            throw new BizException("该地点由超级管理员设置，管理员不能修改；请联系超级管理员");
        }
    }

    private String actor() {
        User u = security.currentUser();
        return u == null ? "system" : u.getUsername();
    }

    private String actorRole() {
        return security.isSuperAdmin() ? "SUPER_ADMIN" : "ADMIN";
    }
}
