# -*- coding: utf-8 -*-
"""按最小修改原则落地审计报告中的 9 组补丁；每处要求唯一命中，否则中止。"""
import pathlib
import sys

# 默认按「本脚本位于 <项目>/test/」定位目标源码；
# 也可显式传参：python3 apply_audit_patches.py "src/main/java/com/example/attendance"
ROOT = (pathlib.Path(sys.argv[1]) if len(sys.argv) > 1
        else pathlib.Path(__file__).resolve().parents[1] / "src/main/java/com/example/attendance")
R = "src/main/java/com/example/attendance/"

PATCHES = [
    # ---- BUG-01 P0：伪造 XFF 绕过围栏 ----
    ("P1a clientIp 只用网络层对端地址",
     "controller/EmployeePortalController.java",
     r'''    private String clientIp(HttpServletRequest req) {
        String xf = req.getHeader("X-Forwarded-For");
        return (xf == null || xf.isEmpty()) ? req.getRemoteAddr() : xf.split(",")[0].trim();
    }''',
     r'''    private String clientIp(HttpServletRequest req) {
        // X-Forwarded-For / X-Real-IP 由客户端任意构造，不能作为 IP 白名单依据：
        // 服务直连、未部署可信反向代理，网络层唯一可信值就是 TCP 对端地址。
        return req.getRemoteAddr();
    }'''),

    ("P1b IP 规则升级为硬门禁 + 移除 WIFI 放行 + 坐标范围校验",
     "service/PunchLocationService.java",
     r'''        int nearest = Integer.MAX_VALUE;
        String nearestName = null;
        for (PunchLocation l : candidates) {
            if (l.getLongitude() != null && l.getLatitude() != null && lat != null && lng != null) {
                int d = (int) Math.round(haversine(lat, lng,
                        l.getLatitude().doubleValue(), l.getLongitude().doubleValue()));
                if (d < nearest) { nearest = d; nearestName = l.getName(); }
                int radius = (l.getRadiusM() == null || l.getRadiusM() <= 0) ? 200 : l.getRadiusM();
                if (d <= radius) return new Result(l.getId(), l.getName(), d, "GPS");
            }
            if (hasText(l.getAllowIp()) && ip != null && ipMatch(l.getAllowIp(), ip)) {
                return new Result(l.getId(), l.getName(), null, "IP");
            }
            if (hasText(l.getWifiSsid()) && hasText(wifiSsid)
                    && l.getWifiSsid().trim().equalsIgnoreCase(wifiSsid.trim())) {
                return new Result(l.getId(), l.getName(), null, "WIFI");
            }
            // 只填名称、三因子均未配置的地点属于无效配置，跳过而不是放行
        }''',
     r'''        int nearest = Integer.MAX_VALUE;
        String nearestName = null;
        for (PunchLocation l : candidates) {
            // IP 白名单是唯一来自网络层、服务端可校验的因子：一旦配置就必须命中。
            // 否则 GPS 坐标与 WiFi SSID 都是客户端表单上报值（可任意伪造），
            // 单独命中就放行等于围栏完全失效。
            boolean hasIpRule = hasText(l.getAllowIp());
            boolean ipOk = hasIpRule && ip != null && ipMatch(l.getAllowIp(), ip);
            if (hasIpRule && !ipOk) continue;

            if (l.getLongitude() != null && l.getLatitude() != null && lat != null && lng != null
                    && validLatLng(lat, lng)) {
                int d = (int) Math.round(haversine(lat, lng,
                        l.getLatitude().doubleValue(), l.getLongitude().doubleValue()));
                if (d < nearest) { nearest = d; nearestName = l.getName(); }
                int radius = (l.getRadiusM() == null || l.getRadiusM() <= 0) ? 200 : l.getRadiusM();
                if (d <= radius) return new Result(l.getId(), l.getName(), d, "GPS");
            }
            if (ipOk) return new Result(l.getId(), l.getName(), null, "IP");
            // WiFi SSID 由浏览器表单上报（浏览器本身无法读取 SSID），不再作为放行依据；
            // 历史流水的 WIFI 匹配类型仍由 matchLabel 兼容展示。
            // 只填名称、未配置可信因子的地点属于无效配置，跳过而不是放行
        }'''),

    ("P1b2 坐标范围校验工具",
     "service/PunchLocationService.java",
     r'''    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }''',
     r'''    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }

    /** 客户端上报坐标先做数值范围校验，避免非法值写进流水或让距离计算失真 */
    private static boolean validLatLng(double lat, double lng) {
        return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }'''),

    ("P1c 审计日志 IP 同样只用 getRemoteAddr",
     "service/AccountService.java",
     r'''            HttpServletRequest req = ra.getRequest();
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = req.getRemoteAddr();
            return ip == null ? null : ip.substring(0, Math.min(ip.length(), 64));''',
     r'''            // 审计日志同样只记网络层对端地址，避免伪造代理头污染操作记录
            HttpServletRequest req = ra.getRequest();
            String ip = req.getRemoteAddr();
            return ip == null ? null : ip.substring(0, Math.min(ip.length(), 64));'''),

    # ---- BUG-02 P1：超管接管/覆盖管理员地点语义失效 ----
    ("P2a 新增 takeover / setSuperDefault，setDefault 限定影响行",
     "mapper/PunchLocationMapper.java",
     r'''    @Update("UPDATE punch_location SET is_default = IF(id = #{id}, 1, 0)")
    int setDefault(Long id);''',
     r'''    @Update("UPDATE punch_location SET is_default = IF(id = #{id}, 1, 0) WHERE id = #{id} OR is_default = 1")
    int setDefault(Long id);

    /** 超管接管管理员创建的地点：补写接管标记，使 findSuperDefault 与层级保护同时生效 */
    @Update("""
        UPDATE punch_location SET created_by=#{actor}, created_by_role='SUPER_ADMIN',
               updated_by=#{actor}, updated_by_role='SUPER_ADMIN', update_time=NOW()
        WHERE id=#{id}
    """)
    int takeover(@Param("id") Long id, @Param("actor") String actor);

    /** 超管设为默认：同时标记接管，否则管理员创建的地点 findSuperDefault 命中不到，覆盖语义失效 */
    @Update("""
        UPDATE punch_location
        SET is_default = IF(id = #{id}, 1, 0),
            created_by_role = IF(id = #{id}, 'SUPER_ADMIN', created_by_role)
        WHERE id = #{id} OR is_default = 1
    """)
    int setSuperDefault(@Param("id") Long id);'''),

    ("P2b 覆盖修改时补写接管标记",
     "controller/SuperLocationController.java",
     r'''            } else {
                locationMapper.update(l);
            }
            locationMapper.clearGroups(l.getId());''',
     r'''            } else {
                locationMapper.update(l);
                // 覆盖修改管理员设置的地点：必须补写接管标记，否则 created_by_role 仍是 ADMIN，
                // findSuperDefault 命中不到、管理员还能反向改回去。
                locationMapper.takeover(l.getId(), who);
            }
            locationMapper.clearGroups(l.getId());'''),

    ("P2c 超管设为默认改走 setSuperDefault",
     "controller/SuperLocationController.java",
     r'''        return mutate(id, ra, "LOCATION_DEFAULT", "设为默认地点（覆盖管理员设置）",
                () -> locationMapper.setDefault(id));''',
     r'''        return mutate(id, ra, "LOCATION_DEFAULT", "设为默认地点（覆盖管理员设置）",
                () -> locationMapper.setSuperDefault(id));'''),

    # ---- BUG-03 P1：角色双来源导致跨端越权 ----
    ("P3 主角色只取 sys_user.role 单一来源",
     "config/CustomUserDetailsService.java",
     r'''        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        for (String code : userMapper.findRoleCodes(user.getId())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + code));
        }
        for (String perm : userMapper.findPermCodes(user.getId())) {''',
     r'''        List<GrantedAuthority> authorities = new ArrayList<>();
        // 主角色只取 sys_user.role 一个来源：PortRoleGuard 按「第一个 ROLE_」判定端口，
        // SecurityConfig 按全部 authorities 判定 URL。若再并入 sys_user_role 的 ROLE_，
        // 两处角色集合不同源，错配数据即可在员工端口获得超管权限。
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        for (String perm : userMapper.findPermCodes(user.getId())) {'''),

    # ---- BUG-05 P2：错误页渲染成 HTTP 200 ----
    ("P4a 导入 HttpServletResponse",
     "config/GlobalExceptionHandler.java",
     r'''import jakarta.servlet.http.HttpServletRequest;''',
     r'''import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;'''),

    ("P4b 显式设置 500 状态码",
     "config/GlobalExceptionHandler.java",
     r'''    @ExceptionHandler(Exception.class)
    public String handleOther(Exception e, HttpServletRequest request, Model model) {
        log.error("请求处理失败: {} {}", request.getMethod(), request.getRequestURI(), e);
        model.addAttribute("errorTitle", "系统处理请求时出错");''',
     r'''    @ExceptionHandler(Exception.class)
    public String handleOther(Exception e, HttpServletRequest request,
                              HttpServletResponse response, Model model) {
        log.error("请求处理失败: {} {}", request.getMethod(), request.getRequestURI(), e);
        // 视图渲染不会自动设置状态码：不显式写 500 会把服务端错误伪装成 HTTP 200
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("errorTitle", "系统处理请求时出错");'''),

    # ---- BUG-04 P2：员工端申请参数校验 ----
    ("P5a 增加 reqDate 工具",
     "controller/EmployeePortalController.java",
     r'''    private LocalDate parse(String s, LocalDate fallback) {''',
     r'''    /** 员工端表单日期：非法格式转成业务提示，不让 DateTimeParseException 冒到 500 页 */
    private LocalDate reqDate(String s, String label) {
        try { return LocalDate.parse(s); }
        catch (Exception e) { throw new BizException(label + "日期格式不正确，应为 yyyy-MM-dd"); }
    }

    private LocalDate parse(String s, LocalDate fallback) {'''),

    ("P5b 请假日期校验",
     "controller/EmployeePortalController.java",
     r'''        LeaveRequest lr = leaveService.apply(empId(), typeCode, LocalDate.parse(startDate),
                LocalDate.parse(endDate), reason);''',
     r'''        LocalDate s = reqDate(startDate, "开始"), e = reqDate(endDate, "结束");
        if (e.isBefore(s)) throw new BizException("结束日期不能早于开始日期");
        LeaveRequest lr = leaveService.apply(empId(), typeCode, s, e, reason);'''),

    ("P5c 加班时长与日期校验",
     "controller/EmployeePortalController.java",
     r'''        LocalDate d = LocalDate.parse(otDate);
        OvertimeRequest o = new OvertimeRequest();''',
     r'''        LocalDate d = reqDate(otDate, "加班");
        if (hours == null || hours.compareTo(BigDecimal.ZERO) <= 0
                || hours.compareTo(BigDecimal.valueOf(24)) > 0)
            throw new BizException("加班时长必须在 0~24 小时之间");
        OvertimeRequest o = new OvertimeRequest();'''),

    ("P5d 出差日期校验",
     "controller/EmployeePortalController.java",
     r'''        LocalDate s = LocalDate.parse(startDate), e = LocalDate.parse(endDate);
        BusinessTrip t = new BusinessTrip();''',
     r'''        LocalDate s = reqDate(startDate, "开始"), e = reqDate(endDate, "结束");
        if (e.isBefore(s)) throw new BizException("结束日期不能早于开始日期");
        BusinessTrip t = new BusinessTrip();'''),

    ("P5e 补卡时间改为结构化组装",
     "controller/EmployeePortalController.java",
     r'''        MakeupRequest m = new MakeupRequest();
        m.setEmployeeId(empId());
        m.setMissDate(LocalDate.parse(missDate));
        m.setPunchType(punchType);
        m.setMakeupTime(LocalDateTime.parse(missDate + "T" + makeupTime));''',
     r'''        LocalDate d = reqDate(missDate, "缺卡");
        String mt = makeupTime != null && makeupTime.length() == 4 ? "0" + makeupTime : makeupTime;
        java.time.LocalTime time;
        try { time = java.time.LocalTime.parse(mt); }
        catch (Exception ex) { throw new BizException("补卡时间格式不正确，应为 HH:mm"); }
        MakeupRequest m = new MakeupRequest();
        m.setEmployeeId(empId());
        m.setMissDate(d);
        m.setPunchType(punchType);
        m.setMakeupTime(LocalDateTime.of(d, time));'''),

    # ---- BUG-06 P2：申诉把缺卡洗成 NORMAL ----
    ("P6 correctAppeal 保留缺卡状态",
     "service/AttendanceService.java",
     r'''        Shift s = r.getShiftId() == null ? null : shiftMapper.findById(r.getShiftId());
        r.setLateMinutes(0);
        r.setEarlyMinutes(0);
        if (r.getCheckInTime() != null && r.getCheckOutTime() != null) {
            r.setStatus("NORMAL");
            if (s != null) {
                long minutes = ChronoUnit.MINUTES.between(r.getCheckInTime(), r.getCheckOutTime());
                r.setWorkHours(BigDecimal.valueOf(Math.max(0, minutes / 60.0 - 1.0))
                        .setScale(1, RoundingMode.HALF_UP));
            }
        } else {
            r.setStatus("NORMAL");
        }''',
     r'''        Shift s = r.getShiftId() == null ? null : shiftMapper.findById(r.getShiftId());
        // 申诉复核只撤销「迟到/早退」判定，不能凭空补造打卡事实：
        // 缺卡/旷工状态保留，否则月度汇总会把漏打卡算出勤。
        String st = r.getStatus();
        boolean missing = "ABSENT".equals(st) || "MISSING_IN".equals(st) || "MISSING_OUT".equals(st);
        r.setLateMinutes(0);
        r.setEarlyMinutes(0);
        if (r.getCheckInTime() != null && r.getCheckOutTime() != null && s != null) {
            long minutes = ChronoUnit.MINUTES.between(r.getCheckInTime(), r.getCheckOutTime());
            r.setWorkHours(BigDecimal.valueOf(Math.max(0, minutes / 60.0 - 1.0))
                    .setScale(1, RoundingMode.HALF_UP));
        }
        if (!missing) r.setStatus("NORMAL");'''),

    # ---- BUG-07 P2：请假标记范围与 calcDays 口径不一致 ----
    ("P7a 抽 isWorkday 共用口径",
     "service/LeaveService.java",
     r'''        long days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            boolean weekend = d.getDayOfWeek().getValue() >= 6;
            if (makeUpWorkdays.contains(d) || (!weekend && !restDays.contains(d))) days++;
        }
        return BigDecimal.valueOf(days);
    }''',
     r'''        long days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isWorkday(d, restDays, makeUpWorkdays)) days++;
        }
        return BigDecimal.valueOf(days);
    }

    /** 单日是否应出勤：调休上班日计入、周末与节假日剔除；calcDays 与 onApproved 共用同一口径 */
    private boolean isWorkday(LocalDate d, Set<LocalDate> restDays, Set<LocalDate> makeUpWorkdays) {
        if (makeUpWorkdays.contains(d)) return true;
        if (d.getDayOfWeek().getValue() >= 6) return false;
        return !restDays.contains(d);
    }'''),

    ("P7b onApproved 复用同一口径",
     "service/LeaveService.java",
     r'''        for (LocalDate d = lr.getStartDate(); !d.isAfter(lr.getEndDate()); d = d.plusDays(1)) {
            if (d.getDayOfWeek().getValue() >= 6) continue;
            HolidayCalendar h = holidayMapper.findByDate(d);
            if (h != null && !"WORKDAY".equals(h.getDayType())) continue;
            AttendanceRecord r = recordMapper.findTodayRecord(lr.getEmployeeId(), d);''',
     r'''        Set<LocalDate> restDays = new HashSet<>();
        Set<LocalDate> makeUpWorkdays = new HashSet<>();
        for (HolidayCalendar h : holidayMapper.findByRange(lr.getStartDate(), lr.getEndDate())) {
            if ("WORKDAY".equals(h.getDayType())) makeUpWorkdays.add(h.getCalDate());
            else restDays.add(h.getCalDate());
        }
        for (LocalDate d = lr.getStartDate(); !d.isAfter(lr.getEndDate()); d = d.plusDays(1)) {
            if (!isWorkday(d, restDays, makeUpWorkdays)) continue;
            AttendanceRecord r = recordMapper.findTodayRecord(lr.getEmployeeId(), d);'''),

    # ---- BUG-08 P2：地点操作未校验目标存在性 ----
    ("P8a 管理员端 mutate 校验",
     "controller/LocationController.java",
     r'''    private String mutate(Long id, RedirectAttributes ra, String action, String desc, Runnable op) {
        PunchLocation l = locationMapper.findById(id);
        try {''',
     r'''    private String mutate(Long id, RedirectAttributes ra, String action, String desc, Runnable op) {
        PunchLocation l = locationMapper.findById(id);
        if (l == null) {
            accountService.writeFailure(action, String.valueOf(id), "地点不存在或已被删除");
            ra.addFlashAttribute("error", "地点不存在或已被删除");
            return "redirect:/admin/locations";
        }
        try {'''),

    ("P8b 超管端 mutate 校验",
     "controller/SuperLocationController.java",
     r'''    private String mutate(Long id, RedirectAttributes ra, String action, String desc, Runnable op) {
        PunchLocation l = locationMapper.findById(id);
        try {''',
     r'''    private String mutate(Long id, RedirectAttributes ra, String action, String desc, Runnable op) {
        PunchLocation l = locationMapper.findById(id);
        if (l == null) {
            accountService.writeFailure(action, String.valueOf(id), "地点不存在或已被删除");
            ra.addFlashAttribute("error", "地点不存在或已被删除");
            return "redirect:/super/locations";
        }
        try {'''),

    # ---- BUG-10 P3：补卡制造签退早于签到导致工时 ~23h ----
    ("P9 applyMakeup 校验时间先后",
     "service/AttendanceService.java",
     r'''        if ("OUT".equals(m.getPunchType())) r.setCheckOutTime(m.getMakeupTime());
        else r.setCheckInTime(m.getMakeupTime());
        r.setIsMakeup(1);''',
     r'''        if ("OUT".equals(m.getPunchType())) r.setCheckOutTime(m.getMakeupTime());
        else r.setCheckInTime(m.getMakeupTime());
        if (r.getCheckInTime() != null && r.getCheckOutTime() != null
                && r.getCheckOutTime().isBefore(r.getCheckInTime())) {
            throw new BizException("补卡失败：签退时间不能早于签到时间");
        }
        r.setIsMakeup(1);'''),

    # ---- BUG-11 P2：BizException 无 Referer 时跳到无权访问的 /dashboard ----
    ("P10 业务异常按请求所属端回源",
     "config/GlobalExceptionHandler.java",
     r'''        return "redirect:/dashboard";
    }''',
     r'''        // 无 Referer（API / 直接 POST / 隐私策略剥离）时按请求所属端回源：
        // 统一兜底 /dashboard 会让员工端与超管端跳到无权访问的页面，业务提示随之丢失。
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/my/")) return "redirect:/my/home";
        if (uri != null && uri.startsWith("/super/")) return "redirect:/super/dashboard";
        return "redirect:/dashboard";
    }'''),
]

ok = 0
for name, rel, old, new in PATCHES:
    f = ROOT / rel
    c = f.read_text(encoding="utf-8")
    n = c.count(old)
    if n != 1:
        print(f"FAIL  {name}: matched {n} times in {rel}")
        raise SystemExit(1)
    f.write_text(c.replace(old, new, 1), encoding="utf-8")
    ok += 1
    print(f"OK    {name}")
print(f"APPLIED={ok}/{len(PATCHES)}")
