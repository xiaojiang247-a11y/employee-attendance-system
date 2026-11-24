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
 * 超级管理员子系统 - 打卡地点（/super/locations）。
 * 与管理员共用 punch_location 表，但超管可修改/删除/覆盖任何管理员设置的地点；
 * 超管「设为默认」后，围栏只认这一处（PunchLocationService.findSuperDefault），实现覆盖语义。
 */
@Controller
@RequestMapping("/super/locations")
public class SuperLocationController {

    private final PunchLocationMapper locationMapper;
    private final AttendanceGroupMapper groupMapper;
    private final AccountService accountService;
    private final SecurityHelper security;

    public SuperLocationController(PunchLocationMapper locationMapper, AttendanceGroupMapper groupMapper,
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
        return "super/locations";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute PunchLocation l,
                       @RequestParam(required = false) List<Long> groupIds,
                       RedirectAttributes ra) {
        try {
            validate(l);
            String who = actor();
            boolean create = l.getId() == null;
            PunchLocation old = create ? null : locationMapper.findById(l.getId());
            l.setUpdatedBy(who);
            l.setUpdatedByRole("SUPER_ADMIN");
            if (create) {
                l.setCreatedBy(who);
                l.setCreatedByRole("SUPER_ADMIN");
                locationMapper.insert(l);
            } else {
                locationMapper.update(l);
                // 覆盖修改管理员设置的地点：必须补写接管标记，否则 created_by_role 仍是 ADMIN，
                // findSuperDefault 命中不到、管理员还能反向改回去。
                locationMapper.takeover(l.getId(), who);
            }
            locationMapper.clearGroups(l.getId());
            if (groupIds != null) {
                for (Long gid : groupIds) locationMapper.grantGroup(gid, l.getId());
            }
            if (l.getIsDefault() != null && l.getIsDefault() == 1) locationMapper.setDefault(l.getId());
            String owner = old == null ? "" : "（原设置人 " + (old.getCreatedBy() == null ? "未知" : old.getCreatedBy()) + "）";
            accountService.writeLocationLog("LOCATION_SAVE", l.getName(),
                    (create ? "新增" : "覆盖修改") + "打卡地点" + owner + "，半径 " + l.getRadiusM() + "m");
            ra.addFlashAttribute("msg", "打卡地点「" + l.getName() + "」已保存");
        } catch (BizException e) {
            accountService.writeFailure("LOCATION_SAVE", l.getName(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super/locations";
    }

    @PostMapping("/default/{id}")
    public String setDefault(@PathVariable Long id, RedirectAttributes ra) {
        return mutate(id, ra, "LOCATION_DEFAULT", "设为默认地点（覆盖管理员设置）",
                () -> locationMapper.setSuperDefault(id));
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

    private String mutate(Long id, RedirectAttributes ra, String action, String desc, Runnable op) {
        PunchLocation l = locationMapper.findById(id);
        if (l == null) {
            accountService.writeFailure(action, String.valueOf(id), "地点不存在或已被删除");
            ra.addFlashAttribute("error", "地点不存在或已被删除");
            return "redirect:/super/locations";
        }
        try {
            op.run();
            accountService.writeLocationLog(action, l == null ? String.valueOf(id) : l.getName(), desc);
            ra.addFlashAttribute("msg", desc + "成功");
        } catch (BizException e) {
            accountService.writeFailure(action, l == null ? String.valueOf(id) : l.getName(), e.getMessage());
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super/locations";
    }

    private void validate(PunchLocation l) {
        if (l.getName() == null || l.getName().trim().isEmpty())
            throw new BizException("地点名称不能为空");
        if (l.getEnabled() == null) l.setEnabled(1);
        if (l.getRadiusM() == null || l.getRadiusM() <= 0) l.setRadiusM(200);
        if (l.getIsDefault() == null) l.setIsDefault(0);
        if ((l.getLongitude() == null) != (l.getLatitude() == null))
            throw new BizException("经度和纬度必须同时填写");
        if (l.getLongitude() != null) {
            BigDecimal lon = l.getLongitude(), lat = l.getLatitude();
            if (lon.abs().compareTo(new BigDecimal("180")) > 0
                    || lat.abs().compareTo(new BigDecimal("90")) > 0)
                throw new BizException("经纬度超出有效范围（经度 ±180、纬度 ±90）");
        }
    }

    private String actor() {
        User u = security.currentUser();
        return u == null ? "system" : u.getUsername();
    }
}
