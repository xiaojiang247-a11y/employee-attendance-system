package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OvertimeRequestMapper {

    @Select("""
        SELECT o.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name, u.real_name AS approver_name
        FROM overtime_request o
        LEFT JOIN employee e ON o.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN sys_user u ON o.approver_id = u.id
        WHERE (#{employeeId} IS NULL OR o.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
          AND (#{status} IS NULL OR #{status} = '' OR o.status = #{status})
        ORDER BY o.apply_time DESC, o.id DESC
    """)
    List<OvertimeRequest> findList(@Param("employeeId") Long employeeId, @Param("deptId") Long deptId,
                                   @Param("status") String status);

    @Select("""
        SELECT o.*, e.department_id AS dept_id FROM overtime_request o
        LEFT JOIN employee e ON e.id = o.employee_id WHERE o.id = #{id}
    """)
    OvertimeRequest findById(Long id);

    @Insert("""
        INSERT INTO overtime_request(employee_id, ot_date, start_time, end_time, hours, ot_type,
                                     compensate_type, reason, status, current_step, apply_time)
        VALUES(#{employeeId}, #{otDate}, #{startTime}, #{endTime}, #{hours}, #{otType},
               #{compensateType}, #{reason}, 'PENDING', 1, NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(OvertimeRequest o);

    @Update("""
        UPDATE overtime_request SET status=#{status}, approver_id=#{approverId}, approve_time=NOW(),
        approve_remark=#{remark}, current_step=2 WHERE id=#{id}
    """)
    int updateApproval(@Param("id") Long id, @Param("status") String status,
                       @Param("approverId") Long approverId, @Param("remark") String remark);

    @Select("SELECT COUNT(*) FROM overtime_request WHERE status = 'PENDING'")
    long countPending();

    @Select("""
        SELECT COALESCE(SUM(hours),0) FROM overtime_request
        WHERE employee_id = #{employeeId} AND status = 'APPROVED' AND ot_date BETWEEN #{start} AND #{end}
    """)
    BigDecimal sumApprovedHours(@Param("employeeId") Long employeeId,
                                @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Delete("DELETE FROM overtime_request WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM overtime_request WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(Long employeeId);
}
