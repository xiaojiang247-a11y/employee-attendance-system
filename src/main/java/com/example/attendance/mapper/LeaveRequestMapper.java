package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LeaveRequestMapper {

    @Select("""
        SELECT lr.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name, lt.name AS type_name
        FROM leave_request lr
        LEFT JOIN employee e ON lr.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN leave_type lt ON lt.code = COALESCE(lr.type_code, lr.type)
        WHERE (#{employeeId} IS NULL OR lr.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
          AND (#{status} IS NULL OR #{status} = '' OR lr.status = #{status})
        ORDER BY lr.apply_time DESC, lr.id DESC
    """)
    List<LeaveRequest> findRequests(@Param("employeeId") Long employeeId, @Param("deptId") Long deptId,
                                    @Param("status") String status);

    @Select("""
        SELECT lr.*, e.department_id AS dept_id, d.name AS department_name
        FROM leave_request lr LEFT JOIN employee e ON e.id = lr.employee_id
        LEFT JOIN department d ON d.id = e.department_id WHERE lr.id = #{id}
    """)
    LeaveRequest findById(Long id);

    @Select("""
        SELECT COUNT(*) FROM leave_request WHERE employee_id = #{employeeId} AND status = 'APPROVED'
          AND start_date <= #{end} AND end_date >= #{start}
    """)
    int countOverlap(@Param("employeeId") Long employeeId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Insert("""
        INSERT INTO leave_request(employee_id, type, type_code, start_date, end_date, days, reason,
                                  status, apply_time, current_step)
        VALUES(#{employeeId}, #{typeCode}, #{typeCode}, #{startDate}, #{endDate}, #{days}, #{reason},
               'PENDING', NOW(), 1)
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LeaveRequest r);

    @Update("""
        UPDATE leave_request SET status=#{status}, approver_id=#{approverId}, approve_time=NOW(),
            approve_remark=#{remark}, current_step=current_step+1 WHERE id=#{id}
    """)
    int updateApproval(@Param("id") Long id, @Param("status") String status,
                       @Param("approverId") Long approverId, @Param("remark") String remark);

    @Update("UPDATE leave_request SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    @Select("SELECT COUNT(*) FROM leave_request WHERE status = 'PENDING'")
    long countPending();

    @Select("""
        SELECT COALESCE(SUM(lr.days),0) FROM leave_request lr
        WHERE lr.employee_id = #{employeeId} AND lr.status = 'APPROVED'
          AND lr.type_code = #{typeCode} AND YEAR(lr.start_date) = #{year}
    """)
    BigDecimal sumApprovedDays(@Param("employeeId") Long employeeId, @Param("typeCode") String typeCode,
                               @Param("year") int year);

    @Delete("DELETE FROM leave_request WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(Long employeeId);

    @Delete("DELETE FROM leave_request WHERE id = #{id}")
    int deleteById(Long id);
}
