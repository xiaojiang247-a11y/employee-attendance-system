package com.example.attendance.service;

import com.example.attendance.util.AppClock;

import com.example.attendance.entity.*;
import com.example.attendance.mapper.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/** 管理驾驶舱：一次聚合 KPI、趋势、分布、部门排行、异常预警、待办与公告 */
@Service
public class DashboardService {

    private final StatsMapper statsMapper;
    private final ApprovalMapper approvalMapper;
    private final NoticeMapper noticeMapper;
    private final AttendanceSummaryMapper summaryMapper;
    private final SecurityHelper security;

    public DashboardService(StatsMapper statsMapper, ApprovalMapper approvalMapper,
                            NoticeMapper noticeMapper, AttendanceSummaryMapper summaryMapper,
                            SecurityHelper security) {
        this.statsMapper = statsMapper;
        this.approvalMapper = approvalMapper;
        this.noticeMapper = noticeMapper;
        this.summaryMapper = summaryMapper;
        this.security = security;
    }

    public Map<String, Object> admin() {
        Map<String, Object> m = new HashMap<>();
        LocalDate today = AppClock.nowDate();
        LocalDate from30 = today.minusDays(29);
        LocalDate from7 = today.minusDays(6);
        Long deptId = security.scopeDeptId();

        long active = statsMapper.activeEmployees();
        long punched = statsMapper.todayCheckIn();
        m.put("totalEmployees", active);
        m.put("todayCheckIn", punched);
        m.put("todayLate", statsMapper.todayLate());
        m.put("todayAbsent", statsMapper.todayAbsent());
        m.put("onLeaveToday", statsMapper.onLeaveToday());
        m.put("pendingAll", statsMapper.pendingAll());
        m.put("attendanceRate", active == 0 ? 0 : Math.round(punched * 1000.0 / active) / 10.0);
        m.put("headcountByDept", statsMapper.headcountByDept());
        m.put("trend", statsMapper.dailyTrend(from30, today));
        m.put("weekTrend", statsMapper.dailyTrend(from7, today));
        m.put("statusDist", statsMapper.statusDistribution(from30, today));
        m.put("deptRank", statsMapper.deptAttendance(from30, today));
        m.put("risk", statsMapper.riskEmployees(from30, today, deptId));
        m.put("leaveUsage", statsMapper.leaveUsage(from30, today));
        m.put("overtimeRanking", statsMapper.overtimeRanking(from30, today));
        m.put("todayDept", statsMapper.todayDeptProgress());
        m.put("monthlyTrend", statsMapper.monthlyTrend(today.minusMonths(5).withDayOfMonth(1).toString().substring(0, 7)));
        m.put("months", summaryMapper.findAllMonths());

        List<ApprovalTask> tasks = approvalMapper.findPending(deptId);
        m.put("pendingTasks", tasks.size() > 6 ? tasks.subList(0, 6) : tasks);
        m.put("pendingTaskCount", tasks.size());
        m.put("notices", noticeMapper.findTop(5));
        return m;
    }
}
