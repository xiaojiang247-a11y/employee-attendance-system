package com.example.attendance.service;

import com.example.attendance.util.AppClock;
import com.example.attendance.util.PunchRule;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 考勤核算核心：打卡判定、迟到早退与工时计算、日结算、月度汇总、补卡与申诉修正。
 * 判定链：工作日历 → 排班 → 班次 → 宽限/弹性 → 状态。
 */
@Service
public class AttendanceService {

    private final EmployeeMapper employeeMapper;
    private final ShiftMapper shiftMapper;
    private final ScheduleMapper scheduleMapper;
    private final HolidayCalendarMapper holidayMapper;
    private final AttendanceRecordMapper recordMapper;
    private final AttendanceSummaryMapper summaryMapper;
    private final LeaveRequestMapper leaveMapper;
    private final OvertimeRequestMapper overtimeMapper;
    private final MakeupRequestMapper makeupMapper;
    private final PunchLocationService locationService;

    public AttendanceService(EmployeeMapper employeeMapper, ShiftMapper shiftMapper,
                             ScheduleMapper scheduleMapper, HolidayCalendarMapper holidayMapper,
                             AttendanceRecordMapper recordMapper, AttendanceSummaryMapper summaryMapper,
                             LeaveRequestMapper leaveMapper, OvertimeRequestMapper overtimeMapper,
                             MakeupRequestMapper makeupMapper, PunchLocationService locationService) {
        this.employeeMapper = employeeMapper;
        this.shiftMapper = shiftMapper;
        this.scheduleMapper = scheduleMapper;
        this.holidayMapper = holidayMapper;
        this.recordMapper = recordMapper;
        this.summaryMapper = summaryMapper;
        this.leaveMapper = leaveMapper;
        this.overtimeMapper = overtimeMapper;
        this.makeupMapper = makeupMapper;
        this.locationService = locationService;
    }

    /** 解析某员工某天应使用的班次；返回 null 表示当天休息 */
    public Shift resolveShift(Long employeeId, LocalDate date) {
        Employee emp = employeeMapper.findById(employeeId);
        if (emp == null) return null;
        Schedule sc = scheduleMapper.findByEmployeeAndDate(employeeId, date);
        HolidayCalendar hol = holidayMapper.findByDate(date);

        boolean rest;
        if (sc != null) {
            rest = sc.getIsRest() != null && sc.getIsRest() == 1;
        } else if (hol != null) {
            rest = !"WORKDAY".equals(hol.getDayType());
        } else {
            rest = date.getDayOfWeek().getValue() >= 6;
        }
        if (rest) return null;

        Long shiftId = (sc != null && sc.getShiftId() != null) ? sc.getShiftId() : emp.getShiftId();
        if (shiftId == null) shiftId = 1L;
        Shift shift = shiftMapper.findById(shiftId);
        return shift != null ? shift : shiftMapper.findById(1L);
    }

    public boolean isWorkday(Long employeeId, LocalDate date) {
        return resolveShift(employeeId, date) != null;
    }

    /** 兼容旧调用：不传坐标时仍走 IP 白名单校验 */
    @Transactional
    public String checkIn(Long employeeId, String ip, String device) {
        return checkIn(employeeId, ip, device, null, null, null);
    }

    /** 位置校验在写库之前：不在围栏内直接抛 BizException，事务不会留下半条流水 */
    @Transactional
    public String checkIn(Long employeeId, String ip, String device,
                          Double lat, Double lng, String wifiSsid) {
        if (employeeId == null) throw new BizException("当前账号未绑定员工档案，请联系人事开通");
        LocalDate today = AppClock.nowDate();
        LocalDateTime now = AppClock.nowDateTime();
        Shift shift = resolveShift(employeeId, today);
        if (shift == null) return "今日为休息日，无需打卡";

        AttendanceRecord r = recordMapper.findTodayRecord(employeeId, today);
        if (r != null && r.getCheckInTime() != null) {
            return "今日已签到（" + r.getCheckInTime().toLocalTime() + "），请勿重复打卡";
        }
        PunchLocationService.Result loc = locationService.verify(employeeId, lat, lng, ip, wifiSsid);
        if (r == null) {
            r = new AttendanceRecord();
            r.setEmployeeId(employeeId);
            r.setAttendanceDate(today);
            r.setIsMakeup(0);
            r.setRemark("");
        }
        r.setCheckInTime(now);
        r.setCheckInIp(ip);
        r.setCheckInDevice(device);
        r.setSource("WEB");
        r.setShiftId(shift.getId());
        applyLocation(r, loc, lat, lng);
        recalc(r, shift);
        if (r.getId() == null) recordMapper.insert(r); else recordMapper.update(r);

        int late = r.getLateMinutes() == null ? 0 : r.getLateMinutes();
        return "上班打卡成功 " + now.toLocalTime() + (late > 0 ? "，迟到 " + late + " 分钟" : "，准时")
                + suffix(loc);
    }

    @Transactional
    public String checkOut(Long employeeId, String ip) {
        return checkOut(employeeId, ip, null, null, null);
    }

    @Transactional
    public String checkOut(Long employeeId, String ip, Double lat, Double lng, String wifiSsid) {
        if (employeeId == null) throw new BizException("当前账号未绑定员工档案，请联系人事开通");
        LocalDate today = AppClock.nowDate();
        LocalDateTime now = AppClock.nowDateTime();
        Shift shift = resolveShift(employeeId, today);
        if (shift == null) return "今日为休息日，无需打卡";

        AttendanceRecord r = recordMapper.findTodayRecord(employeeId, today);
        if (r == null || r.getCheckInTime() == null) return "请先完成上班签到";
        if (r.getCheckOutTime() != null) return "今日已签退（" + r.getCheckOutTime().toLocalTime() + "）";

        PunchLocationService.Result loc = locationService.verify(employeeId, lat, lng, ip, wifiSsid);
        r.setCheckOutTime(now);
        r.setCheckOutIp(ip);
        applyLocation(r, loc, lat, lng);
        recalc(r, shift);
        recordMapper.update(r);
        int early = r.getEarlyMinutes() == null ? 0 : r.getEarlyMinutes();
        return "下班签退成功 " + now.toLocalTime() + "，今日工时 " + r.getWorkHours()
                + " 小时" + (early > 0 ? "，早退 " + early + " 分钟" : "") + suffix(loc);
    }

    /** 把定位结果写入流水，供审计回溯「在哪打的卡、怎么判通过的」 */
    private void applyLocation(AttendanceRecord r, PunchLocationService.Result loc,
                               Double lat, Double lng) {
        if (loc == null) return;
        r.setLocationId(loc.getLocationId());
        r.setPunchLat(lat == null ? null : BigDecimal.valueOf(lat));
        r.setPunchLng(lng == null ? null : BigDecimal.valueOf(lng));
        r.setDistanceM(loc.getDistanceM());
        if (loc.getLocationName() != null) {
            String label = PunchLocationService.matchLabel(loc.getMatchType());
            r.setPunchAddress(loc.getLocationName()
                    + (label.isEmpty() ? "" : " · " + label)
                    + (loc.getDistanceM() == null ? "" : " " + loc.getDistanceM() + "m"));
        }
    }

    private String suffix(PunchLocationService.Result loc) {
        return (loc == null || loc.getLocationName() == null) ? "" : "（" + loc.getLocationName() + "）";
    }

    /**
     * 由签到/签退时间反推状态。抽成独立方法是为了让补卡、申诉修正、日结算复用同一套口径，
     * 避免"页面上显示正常、报表里算迟到"这类双口径问题。
     */
    public void recalc(AttendanceRecord r, Shift s) {
        // 固定 9:00/18:00 边界由 PunchRule 统一判定；班次 s 只用于排班与工时展示，
        // 不再叠加 lateGrace/earlyGrace/flexMinutes，避免「班次宽限」与「系统规定」两套口径打架。
        int late = 0, early = 0;
        LocalDateTime in = r.getCheckInTime(), out = r.getCheckOutTime();
        if (in != null) late = PunchRule.lateMinutes(in.toLocalTime());
        if (out != null) early = PunchRule.earlyMinutes(out.toLocalTime());
        if (in != null && out != null) {
            long minutes = ChronoUnit.MINUTES.between(in, out);
            // 扣除 1 小时午休；跨天班按 24 小时制补正
            if (minutes < 0) minutes += 24 * 60;
            double hours = Math.max(0, minutes / 60.0 - 1.0);
            r.setWorkHours(BigDecimal.valueOf(hours).setScale(1, RoundingMode.HALF_UP));
        } else {
            r.setWorkHours(BigDecimal.ZERO);
        }
        String status;
        if (in == null && out == null) status = "ABSENT";
        else if (in == null) status = "MISSING_IN";
        else if (out == null) status = "MISSING_OUT";
        else if (late > 0 && early > 0) status = "LATE_AND_EARLY";
        else if (late > 0) status = "LATE";
        else if (early > 0) status = "EARLY_LEAVE";
        else status = "NORMAL";
        r.setStatus(status);
        r.setLateMinutes(late);
        r.setEarlyMinutes(early);
    }

    /** 补卡审批通过：回填打卡时间并重算当日状态 */
    @Transactional
    public void applyMakeup(MakeupRequest m) {
        LocalDate d = m.getMissDate();
        Shift shift = resolveShift(m.getEmployeeId(), d);
        AttendanceRecord r = recordMapper.findTodayRecord(m.getEmployeeId(), d);
        if (r == null) {
            r = new AttendanceRecord();
            r.setEmployeeId(m.getEmployeeId());
            r.setAttendanceDate(d);
            r.setIsMakeup(1);
            r.setSource("MAKEUP");
            r.setRemark("补卡审批通过");
            r.setShiftId(shift == null ? null : shift.getId());
        }
        if ("OUT".equals(m.getPunchType())) r.setCheckOutTime(m.getMakeupTime());
        else r.setCheckInTime(m.getMakeupTime());
        if (r.getCheckInTime() != null && r.getCheckOutTime() != null
                && r.getCheckOutTime().isBefore(r.getCheckInTime())) {
            throw new BizException("补卡失败：签退时间不能早于签到时间");
        }
        r.setIsMakeup(1);
        recalc(r, shift);
        if (r.getId() == null) recordMapper.insert(r); else recordMapper.update(r);
    }

    /** 异常申诉通过：撤销异常判定，保留打卡时间 */
    @Transactional
    public void correctAppeal(AttendanceAppeal a) {
        if (a.getRecordId() == null) return;
        AttendanceRecord r = recordMapper.findById(a.getRecordId());
        if (r == null) return;
        Shift s = r.getShiftId() == null ? null : shiftMapper.findById(r.getShiftId());
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
        if (!missing) r.setStatus("NORMAL");
        r.setRemark("异常申诉复核通过：" + (a.getReply() == null ? "" : a.getReply()));
        recordMapper.update(r);
    }

    /** 月度关账核算：从流水重算每名员工的汇总，可重复执行（幂等） */
    @Transactional
    public int generateSummary(String ym) {
        YearMonth y = YearMonth.parse(ym);
        LocalDate start = y.atDay(1);
        LocalDate end = y.atEndOfMonth();
        List<Employee> employees = employeeMapper.findAllActive();
        int n = 0;
        for (Employee e : employees) {
            List<AttendanceRecord> records = recordMapper.findByEmployeeMonth(e.getId(), start, end);
            int expected = scheduleMapper.countWorkdays(e.getId(), start, end);
            if (expected == 0) expected = records.size();

            int actual = 0, late = 0, early = 0, absent = 0, makeup = 0, lateMinutes = 0;
            BigDecimal leaveDays = BigDecimal.ZERO, workHours = BigDecimal.ZERO;
            for (AttendanceRecord r : records) {
                String st = r.getStatus() == null ? "NORMAL" : r.getStatus();
                if (!"ABSENT".equals(st) && !"LEAVE".equals(st)) actual++;
                if ("LATE".equals(st) || "LATE_AND_EARLY".equals(st)) late++;
                if ("EARLY_LEAVE".equals(st) || "LATE_AND_EARLY".equals(st)) early++;
                if ("ABSENT".equals(st)) absent++;
                if ("LEAVE".equals(st)) leaveDays = leaveDays.add(BigDecimal.ONE);
                if (r.getIsMakeup() != null && r.getIsMakeup() == 1) makeup++;
                lateMinutes += r.getLateMinutes() == null ? 0 : r.getLateMinutes();
                workHours = workHours.add(r.getWorkHours() == null ? BigDecimal.ZERO : r.getWorkHours());
            }
            BigDecimal overtimeHours = overtimeMapper.sumApprovedHours(e.getId(), start, end);
            if (overtimeHours == null) overtimeHours = BigDecimal.ZERO;

            AttendanceSummary s = new AttendanceSummary();
            s.setEmployeeId(e.getId());
            s.setYm(ym);
            s.setExpectedDays(BigDecimal.valueOf(expected));
            s.setActualDays(BigDecimal.valueOf(actual));
            s.setLateCnt(late);
            s.setEarlyCnt(early);
            s.setAbsentCnt(absent);
            s.setLeaveDays(leaveDays);
            s.setOvertimeHours(overtimeHours);
            s.setMakeupCnt(makeup);
            s.setWorkHours(workHours.setScale(1, RoundingMode.HALF_UP));
            s.setLateMinutes(lateMinutes);
            summaryMapper.upsert(s);
            n++;
        }
        return n;
    }
}
