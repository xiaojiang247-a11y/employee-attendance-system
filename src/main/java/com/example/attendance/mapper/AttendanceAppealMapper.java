package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceAppealMapper {

    @Select("""
        SELECT a.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name
        FROM attendance_appeal a
        LEFT JOIN employee e ON a.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        WHERE (#{employeeId} IS NULL OR a.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
          AND (#{status} IS NULL OR #{status} = '' OR a.status = #{status})
        ORDER BY a.apply_time DESC, a.id DESC
    """)
    List<AttendanceAppeal> findList(@Param("employeeId") Long employeeId, @Param("deptId") Long deptId,
                                    @Param("status") String status);

    @Select("""
        SELECT a.*, e.department_id AS dept_id FROM attendance_appeal a
        LEFT JOIN employee e ON e.id = a.employee_id WHERE a.id = #{id}
    """)
    AttendanceAppeal findById(Long id);

    @Insert("""
        INSERT INTO attendance_appeal(employee_id, record_id, appeal_date, exception_type, reason,
                                      status, apply_time)
        VALUES(#{employeeId}, #{recordId}, #{appealDate}, #{exceptionType}, #{reason}, 'PENDING', NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceAppeal a);

    @Update("""
        UPDATE attendance_appeal SET status=#{status}, reply=#{reply}, approver_id=#{approverId}
        WHERE id=#{id}
    """)
    int updateReply(@Param("id") Long id, @Param("status") String status, @Param("reply") String reply,
                    @Param("approverId") Long approverId);

    @Select("SELECT COUNT(*) FROM attendance_appeal WHERE status = 'PENDING'")
    long countPending();

    @Delete("DELETE FROM attendance_appeal WHERE id = #{id}")
    int deleteById(Long id);
}
