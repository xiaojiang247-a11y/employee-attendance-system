package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceRecordMapper {

    @Select("SELECT * FROM attendance_record WHERE employee_id = #{employeeId} AND attendance_date = #{date}")
    AttendanceRecord findTodayRecord(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Select("SELECT * FROM attendance_record WHERE id = #{id}")
    AttendanceRecord findById(Long id);

    @Select("""
        SELECT r.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name, s.name AS shift_name
        FROM attendance_record r
        LEFT JOIN employee e ON r.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN shift s ON r.shift_id = s.id
        WHERE (#{employeeId} IS NULL OR r.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId}
               OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
          AND (#{startDate} IS NULL OR r.attendance_date >= #{startDate})
          AND (#{endDate} IS NULL OR r.attendance_date <= #{endDate})
          AND (#{status} IS NULL OR #{status} = '' OR r.status = #{status})
        ORDER BY r.attendance_date DESC, r.id DESC
        LIMIT #{limit}
    """)
    List<AttendanceRecord> findRecords(@Param("employeeId") Long employeeId, @Param("deptId") Long deptId,
                                       @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
                                       @Param("status") String status, @Param("limit") int limit);

    @Select("""
        SELECT r.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name, s.name AS shift_name
        FROM attendance_record r
        LEFT JOIN employee e ON r.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN shift s ON r.shift_id = s.id
        WHERE r.status NOT IN ('NORMAL')
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
          AND r.attendance_date BETWEEN #{startDate} AND #{endDate}
        ORDER BY r.attendance_date DESC, r.id DESC LIMIT 300
    """)
    List<AttendanceRecord> findExceptions(@Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate, @Param("deptId") Long deptId);

    @Select("SELECT status, COUNT(*) AS cnt FROM attendance_record WHERE attendance_date BETWEEN #{startDate} AND #{endDate} GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM attendance_record WHERE employee_id = #{employeeId} AND attendance_date = #{date}")
    int countByEmployeeAndDate(@Param("employeeId") Long employeeId, @Param("date") LocalDate date);

    @Select("SELECT * FROM attendance_record WHERE employee_id = #{employeeId} AND attendance_date BETWEEN #{start} AND #{end} ORDER BY attendance_date")
    List<AttendanceRecord> findByEmployeeMonth(@Param("employeeId") Long employeeId,
                                               @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Insert("""
        INSERT INTO attendance_record(employee_id, attendance_date, check_in_time, check_out_time, status,
            shift_id, check_in_ip, check_out_ip, check_in_device, source, late_minutes, early_minutes,
            work_hours, is_makeup, remark, location_id, punch_lat, punch_lng, distance_m, punch_address)
        VALUES(#{employeeId}, #{attendanceDate}, #{checkInTime}, #{checkOutTime}, #{status},
            #{shiftId}, #{checkInIp}, #{checkOutIp}, #{checkInDevice}, #{source}, #{lateMinutes},
            #{earlyMinutes}, #{workHours}, #{isMakeup}, #{remark}, #{locationId}, #{punchLat},
            #{punchLng}, #{distanceM}, #{punchAddress})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceRecord r);

    @Update("""
        UPDATE attendance_record SET check_in_time=#{checkInTime}, check_out_time=#{checkOutTime},
            status=#{status}, late_minutes=#{lateMinutes}, early_minutes=#{earlyMinutes},
            work_hours=#{workHours}, is_makeup=#{isMakeup}, remark=#{remark}, shift_id=#{shiftId},
            location_id=#{locationId}, punch_lat=#{punchLat}, punch_lng=#{punchLng},
            distance_m=#{distanceM}, punch_address=#{punchAddress}
        WHERE id=#{id}
    """)
    int update(AttendanceRecord r);

    @Update("UPDATE attendance_record SET check_out_time = #{checkOutTime}, status = #{status} WHERE id = #{id}")
    int updateCheckOut(AttendanceRecord r);

    @Delete("DELETE FROM attendance_record WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(Long employeeId);

    @Delete("DELETE FROM attendance_record WHERE id = #{id}")
    int deleteById(Long id);
}
