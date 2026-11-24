package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceSummaryMapper {

    @Select("""
        SELECT s.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name
        FROM attendance_summary s JOIN employee e ON e.id = s.employee_id
        LEFT JOIN department d ON d.id = e.department_id
        WHERE (#{ym} IS NULL OR #{ym} = '' OR s.ym = #{ym})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
          AND (#{keyword} IS NULL OR #{keyword} = '' OR e.name LIKE CONCAT('%',#{keyword},'%')
               OR e.employee_no LIKE CONCAT('%',#{keyword},'%'))
        ORDER BY s.ym DESC, e.id
    """)
    List<AttendanceSummary> findByMonth(@Param("ym") String ym, @Param("deptId") Long deptId,
                                        @Param("keyword") String keyword);

    @Select("SELECT * FROM attendance_summary WHERE employee_id = #{employeeId} AND ym = #{ym}")
    AttendanceSummary findOne(@Param("employeeId") Long employeeId, @Param("ym") String ym);

    @Select("SELECT * FROM attendance_summary WHERE employee_id = #{employeeId} ORDER BY ym DESC LIMIT 12")
    List<AttendanceSummary> findByEmployee(Long employeeId);

    @Insert("""
        INSERT INTO attendance_summary(employee_id, ym, expected_days, actual_days, late_cnt,
            early_cnt, absent_cnt, leave_days, overtime_hours, makeup_cnt, work_hours,
            late_minutes, locked, generate_time)
        VALUES(#{employeeId}, #{ym}, #{expectedDays}, #{actualDays}, #{lateCnt}, #{earlyCnt},
               #{absentCnt}, #{leaveDays}, #{overtimeHours}, #{makeupCnt}, #{workHours},
               #{lateMinutes}, 0, NOW())
        ON DUPLICATE KEY UPDATE expected_days=VALUES(expected_days), actual_days=VALUES(actual_days),
            late_cnt=VALUES(late_cnt), early_cnt=VALUES(early_cnt), absent_cnt=VALUES(absent_cnt),
            leave_days=VALUES(leave_days), overtime_hours=VALUES(overtime_hours),
            makeup_cnt=VALUES(makeup_cnt), work_hours=VALUES(work_hours),
            late_minutes=VALUES(late_minutes), generate_time=NOW()
    """)
    int upsert(AttendanceSummary s);

    @Update("UPDATE attendance_summary SET locked = #{locked} WHERE ym = #{ym}")
    int updateLock(@Param("ym") String ym, @Param("locked") int locked);

    @Select("SELECT COUNT(*) FROM attendance_summary WHERE ym = #{ym} AND locked = 1")
    int countLocked(String ym);

    @Select("SELECT DISTINCT ym FROM attendance_summary ORDER BY ym DESC")
    List<String> findAllMonths();
}
