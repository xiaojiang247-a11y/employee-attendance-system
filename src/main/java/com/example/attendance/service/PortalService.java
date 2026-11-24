package com.example.attendance.service;

import com.example.attendance.util.AppClock;

import com.example.attendance.entity.*;
import com.example.attendance.mapper.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/** 员工自助：工作台、我的考勤、我的班表、假期余额、我的申请进度 */
@Service
public class PortalService {

    private final EmployeeMapper employeeMapper;
    private final AttendanceRecordMapper recordMapper;
    private final AttendanceSummaryMapper summaryMapper;
    private final ScheduleMapper scheduleMapper;
    private final LeaveBalanceMapper balanceMapper;
    private final LeaveRequestMapper leaveMapper;
    private final OvertimeRequestMapper overtimeMapper;
    private final BusinessTripMapper tripMapper;
    private final MakeupRequestMapper makeupMapper;
    private final NoticeMapper noticeMapper;
    private final StatsMapper statsMapper;

    public PortalService(EmployeeMapper employeeMapper, AttendanceRecordMapper recordMapper,
                         AttendanceSummaryMapper summaryMapper, ScheduleMapper scheduleMapper,
                         LeaveBalanceMapper balanceMapper, LeaveRequestMapper leaveMapper,
                         OvertimeRequestMapper overtimeMapper, BusinessTripMapper tripMapper,
                         MakeupRequestMapper makeupMapper, NoticeMapper noticeMapper,
                         StatsMapper statsMapper) {
        this.employeeMapper = employeeMapper;
        this.recordMapper = recordMapper;
        this.summaryMapper = summaryMapper;
        this.scheduleMapper = scheduleMapper;
        this.balanceMapper = balanceMapper;
        this.leaveMapper = leaveMapper;
        this.overtimeMapper = overtimeMapper;
        this.tripMapper = tripMapper;
        this.makeupMapper = makeupMapper;
        this.noticeMapper = noticeMapper;
        this.statsMapper = statsMapper;
    }

    public Map<String, Object> overview(Long employeeId, int year) {
        Map<String, Object> m = new HashMap<>();
        LocalDate today = AppClock.nowDate();
        String ym = today.toString().substring(0, 7);
        m.put("employee", employeeId == null ? null : employeeMapper.findById(employeeId));
        m.put("todayRecord", employeeId == null ? null : recordMapper.findTodayRecord(employeeId, today));
        m.put("summary", employeeId == null ? null : summaryMapper.findOne(employeeId, ym));
        m.put("balances", employeeId == null ? Collections.emptyList() : balanceMapper.findByYear(year, employeeId, null));
        m.put("weekRecords", employeeId == null ? Collections.emptyList()
                : recordMapper.findRecords(employeeId, null, today.minusDays(6), today, null, 20));
        m.put("weekSchedule", employeeId == null ? Collections.emptyList()
                : scheduleMapper.findByEmployeeMonth(employeeId, today, today.plusDays(6)));
        m.put("myLeaves", employeeId == null ? Collections.emptyList()
                : leaveMapper.findRequests(employeeId, null, null));
        m.put("myOvertimes", employeeId == null ? Collections.emptyList()
                : overtimeMapper.findList(employeeId, null, null));
        m.put("myTrips", employeeId == null ? Collections.emptyList()
                : tripMapper.findList(employeeId, null, null));
        m.put("myMakeups", employeeId == null ? Collections.emptyList()
                : makeupMapper.findList(employeeId, null, null));
        m.put("notices", noticeMapper.findTop(4));
        // 员工工作台只展示「分配给本人」的信息，不再暴露全公司人数 / 全公司今日打卡数
        return m;
    }
}
