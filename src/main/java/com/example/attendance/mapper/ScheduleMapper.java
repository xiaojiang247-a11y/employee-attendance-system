package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ScheduleMapper {

    @Select("""
        SELECT sc.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name, s.name AS shift_name, s.color AS shift_color,
               s.start_time, s.end_time
        FROM schedule sc
        LEFT JOIN employee e ON sc.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN shift s ON sc.shift_id = s.id
        WHERE sc.work_date BETWEEN #{start} AND #{end}
          AND (#{employeeId} IS NULL OR sc.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId}
               OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
        ORDER BY sc.work_date, e.id
    """)
    List<Schedule> findByRange(@Param("start") LocalDate start, @Param("end") LocalDate end,
                               @Param("employeeId") Long employeeId, @Param("deptId") Long deptId);

    @Select("SELECT * FROM schedule WHERE employee_id = #{employeeId} AND work_date = #{date}")
    Schedule findByEmployeeAndDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Select("""
        SELECT sc.*, s.name AS shift_name, s.color AS shift_color, s.start_time, s.end_time
        FROM schedule sc LEFT JOIN shift s ON sc.shift_id = s.id
        WHERE sc.employee_id = #{employeeId} AND sc.work_date BETWEEN #{start} AND #{end}
        ORDER BY sc.work_date
    """)
    List<Schedule> findByEmployeeMonth(@Param("employeeId") Long employeeId,
                                       @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Insert("""
        INSERT INTO schedule(employee_id, work_date, shift_id, is_rest, source, remark)
        VALUES(#{employeeId}, #{workDate}, #{shiftId}, #{isRest}, #{source}, #{remark})
        ON DUPLICATE KEY UPDATE shift_id=VALUES(shift_id), is_rest=VALUES(is_rest), source=VALUES(source)
    """)
    int upsert(Schedule s);

    @Insert("<script>" +
            "INSERT IGNORE INTO schedule(employee_id, work_date, shift_id, is_rest, source) VALUES " +
            "<foreach collection='list' item='it' separator=','>" +
            "(#{it.employeeId}, #{it.workDate}, #{it.shiftId}, #{it.isRest}, #{it.source})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<Schedule> list);

    @Delete("DELETE FROM schedule WHERE employee_id = #{employeeId} AND work_date BETWEEN #{start} AND #{end}")
    int deleteByRange(@Param("employeeId") Long employeeId, @Param("start") LocalDate start,
                      @Param("end") LocalDate end);

    @Delete("DELETE FROM schedule WHERE work_date BETWEEN #{start} AND #{end} AND source = 'AUTO'")
    int deleteAutoByRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT COUNT(*) FROM schedule WHERE employee_id = #{employeeId} AND work_date BETWEEN #{start} AND #{end} AND is_rest = 0")
    int countWorkdays(@Param("employeeId") Long employeeId, @Param("start") LocalDate start,
                      @Param("end") LocalDate end);
}
