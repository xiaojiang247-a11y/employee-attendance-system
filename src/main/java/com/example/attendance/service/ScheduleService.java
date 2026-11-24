package com.example.attendance.service;

import com.example.attendance.entity.*;
import com.example.attendance.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** 排班：按部门/日期区间批量生成班表，遵循工作日历，支持固定班与早晚轮班 */
@Service
public class ScheduleService {

    private final EmployeeMapper employeeMapper;
    private final ScheduleMapper scheduleMapper;
    private final HolidayCalendarMapper holidayMapper;

    public ScheduleService(EmployeeMapper employeeMapper, ScheduleMapper scheduleMapper,
                           HolidayCalendarMapper holidayMapper) {
        this.employeeMapper = employeeMapper;
        this.scheduleMapper = scheduleMapper;
        this.holidayMapper = holidayMapper;
    }

    /**
     * 重新生成指定区间排班。先清除该区间内这些员工的既有排班（含手工调整），
     * 再按 "工作日历 > 周末 > 班次/轮班模板" 的顺序落库。
     * @return 生成的排班条数
     */
    @Transactional
    public int generate(LocalDate start, LocalDate end, Long deptId, Long shiftId, String mode) {
        List<Employee> employees = (deptId == null)
                ? employeeMapper.findAllActive() : employeeMapper.findByDeptId(deptId);
        if (employees.isEmpty()) return 0;

        Map<LocalDate, HolidayCalendar> holidays = new HashMap<>();
        for (HolidayCalendar h : holidayMapper.findByRange(start, end)) holidays.put(h.getCalDate(), h);

        for (Employee e : employees) scheduleMapper.deleteByRange(e.getId(), start, end);

        List<Schedule> batch = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            HolidayCalendar h = holidays.get(d);
            boolean weekend = d.getDayOfWeek().getValue() >= 6;
            boolean rest = (h != null && !"WORKDAY".equals(h.getDayType())) || (h == null && weekend);
            long offset = ChronoUnit.DAYS.between(start, d);

            for (Employee e : employees) {
                Schedule s = new Schedule();
                s.setEmployeeId(e.getId());
                s.setWorkDate(d);
                s.setSource("AUTO");
                if (rest) {
                    s.setIsRest(1);
                    s.setShiftId(null);
                } else {
                    s.setIsRest(0);
                    Long sid;
                    if ("ROTATE".equals(mode)) sid = (offset % 2 == 0) ? 2L : 3L;
                    else sid = (shiftId != null) ? shiftId : (e.getShiftId() != null ? e.getShiftId() : 1L);
                    s.setShiftId(sid);
                }
                batch.add(s);
            }
        }
        // 分批写入，避免单条 INSERT 过长（每批 400 行）
        int written = 0;
        for (int i = 0; i < batch.size(); i += 400) {
            List<Schedule> part = batch.subList(i, Math.min(i + 400, batch.size()));
            written += scheduleMapper.batchInsert(part);
        }
        return written;
    }

    public List<Schedule> month(LocalDate start, LocalDate end, Long employeeId, Long deptId) {
        return scheduleMapper.findByRange(start, end, employeeId, deptId);
    }

    public List<HolidayCalendar> holidays(LocalDate start, LocalDate end) {
        return holidayMapper.findByRange(start, end);
    }
}
