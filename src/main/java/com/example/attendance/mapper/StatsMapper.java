package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    @Select("SELECT COUNT(*) FROM employee WHERE status = 'ACTIVE'")
    long activeEmployees();

    @Select("SELECT COUNT(*) FROM attendance_record WHERE attendance_date = CURDATE() AND check_in_time IS NOT NULL")
    long todayCheckIn();

    @Select("SELECT COUNT(*) FROM attendance_record WHERE attendance_date = CURDATE() AND status IN ('LATE','LATE_AND_EARLY')")
    long todayLate();

    @Select("SELECT COUNT(*) FROM attendance_record WHERE attendance_date = CURDATE() AND status = 'ABSENT'")
    long todayAbsent();

    @Select("""
        SELECT (SELECT COUNT(*) FROM leave_request WHERE status='PENDING')
             + (SELECT COUNT(*) FROM overtime_request WHERE status='PENDING')
             + (SELECT COUNT(*) FROM business_trip WHERE status='PENDING')
             + (SELECT COUNT(*) FROM makeup_request WHERE status='PENDING')
             + (SELECT COUNT(*) FROM attendance_appeal WHERE status='PENDING') AS cnt
    """)
    long pendingAll();

    @Select("SELECT COUNT(*) FROM leave_request WHERE status='APPROVED' AND CURDATE() BETWEEN start_date AND end_date")
    long onLeaveToday();

    @Select("SELECT COUNT(*) FROM sys_notice")
    long noticeCount();

    @Select("""
        SELECT DATE_FORMAT(attendance_date, '%m-%d') AS label,
               COUNT(*) AS total,
               SUM(CASE WHEN check_in_time IS NOT NULL THEN 1 ELSE 0 END) AS present,
               SUM(CASE WHEN status IN ('LATE','LATE_AND_EARLY') THEN 1 ELSE 0 END) AS late,
               SUM(CASE WHEN status = 'ABSENT' THEN 1 ELSE 0 END) AS absent
        FROM attendance_record
        WHERE attendance_date BETWEEN #{start} AND #{end}
        GROUP BY attendance_date ORDER BY attendance_date
    """)
    List<Map<String, Object>> dailyTrend(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
        SELECT status AS label, COUNT(*) AS cnt FROM attendance_record
        WHERE attendance_date BETWEEN #{start} AND #{end} GROUP BY status ORDER BY cnt DESC
    """)
    List<Map<String, Object>> statusDistribution(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
        SELECT d.name AS label, COUNT(r.id) AS total,
               SUM(CASE WHEN r.status = 'NORMAL' THEN 1 ELSE 0 END) AS normal,
               ROUND(100.0 * SUM(CASE WHEN r.status = 'NORMAL' THEN 1 ELSE 0 END) / NULLIF(COUNT(r.id),0), 1) AS rate
        FROM attendance_record r
        JOIN employee e ON e.id = r.employee_id
        JOIN department d ON d.id = e.department_id
        WHERE r.attendance_date BETWEEN #{start} AND #{end}
        GROUP BY d.id, d.name ORDER BY rate DESC
    """)
    List<Map<String, Object>> deptAttendance(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
        SELECT e.id, e.name AS label, e.employee_no, d.name AS dept,
               SUM(CASE WHEN r.status IN ('LATE','LATE_AND_EARLY') THEN 1 ELSE 0 END) AS late_cnt,
               SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_cnt,
               SUM(CASE WHEN r.status = 'MISSING_OUT' THEN 1 ELSE 0 END) AS missing_cnt,
               COUNT(*) AS total
        FROM attendance_record r
        JOIN employee e ON e.id = r.employee_id
        LEFT JOIN department d ON d.id = e.department_id
        WHERE r.attendance_date BETWEEN #{start} AND #{end}
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
        GROUP BY e.id, e.name, e.employee_no, d.name
        HAVING late_cnt + absent_cnt + missing_cnt > 0
        ORDER BY (late_cnt * 2 + absent_cnt * 3 + missing_cnt) DESC LIMIT 8
    """)
    List<Map<String, Object>> riskEmployees(@Param("start") LocalDate start, @Param("end") LocalDate end,
                                            @Param("deptId") Long deptId);

    @Select("""
        SELECT d.name AS label, COUNT(e.id) AS cnt FROM department d
        LEFT JOIN employee e ON e.department_id = d.id AND e.status = 'ACTIVE'
        GROUP BY d.id, d.name ORDER BY d.sort_no, d.id
    """)
    List<Map<String, Object>> headcountByDept();

    @Select("""
        SELECT e.employee_no, e.name AS employee_name, d.name AS department_name, s.*
        FROM attendance_summary s JOIN employee e ON e.id = s.employee_id
        LEFT JOIN department d ON d.id = e.department_id
        WHERE s.ym = #{ym} AND (#{deptId} IS NULL OR e.department_id = #{deptId})
        ORDER BY d.id, e.id
    """)
    List<Map<String, Object>> monthlyReport(@Param("ym") String ym, @Param("deptId") Long deptId);

    @Select("""
        SELECT COALESCE(lt.name, lr.type) AS label, SUM(lr.days) AS cnt
        FROM leave_request lr LEFT JOIN leave_type lt ON lt.code = COALESCE(lr.type_code, lr.type)
        WHERE lr.status = 'APPROVED' AND lr.start_date BETWEEN #{start} AND #{end}
        GROUP BY label ORDER BY cnt DESC
    """)
    List<Map<String, Object>> leaveUsage(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
        SELECT e.name AS label, d.name AS dept, SUM(o.hours) AS cnt
        FROM overtime_request o JOIN employee e ON e.id = o.employee_id
        LEFT JOIN department d ON d.id = e.department_id
        WHERE o.status = 'APPROVED' AND o.ot_date BETWEEN #{start} AND #{end}
        GROUP BY e.id, e.name, d.name ORDER BY cnt DESC LIMIT 10
    """)
    List<Map<String, Object>> overtimeRanking(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("""
        SELECT d.name AS label,
               (SELECT COUNT(*) FROM employee x WHERE x.department_id = d.id AND x.status = 'ACTIVE') AS total,
               COUNT(r.id) AS punched
        FROM department d
        LEFT JOIN employee e ON e.department_id = d.id AND e.status = 'ACTIVE'
        LEFT JOIN attendance_record r ON r.employee_id = e.id
             AND r.attendance_date = CURDATE() AND r.check_in_time IS NOT NULL
        GROUP BY d.id, d.name ORDER BY d.sort_no, d.id
    """)
    List<Map<String, Object>> todayDeptProgress();

    @Select("""
        SELECT DATE_FORMAT(ym, '%Y-%m') AS label,
               SUM(actual_days) AS actual_days, SUM(late_cnt) AS late_cnt,
               SUM(overtime_hours) AS overtime_hours, SUM(leave_days) AS leave_days
        FROM attendance_summary WHERE ym >= #{fromYm} GROUP BY ym ORDER BY ym
    """)
    List<Map<String, Object>> monthlyTrend(@Param("fromYm") String fromYm);
}
