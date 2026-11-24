package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MakeupRequestMapper {

    @Select("""
        SELECT m.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name, u.real_name AS approver_name
        FROM makeup_request m
        LEFT JOIN employee e ON m.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN sys_user u ON m.approver_id = u.id
        WHERE (#{employeeId} IS NULL OR m.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
          AND (#{status} IS NULL OR #{status} = '' OR m.status = #{status})
        ORDER BY m.apply_time DESC, m.id DESC
    """)
    List<MakeupRequest> findList(@Param("employeeId") Long employeeId, @Param("deptId") Long deptId,
                                 @Param("status") String status);

    @Select("""
        SELECT m.*, e.department_id AS dept_id FROM makeup_request m
        LEFT JOIN employee e ON e.id = m.employee_id WHERE m.id = #{id}
    """)
    MakeupRequest findById(Long id);

    @Insert("""
        INSERT INTO makeup_request(employee_id, miss_date, punch_type, makeup_time, reason, status, apply_time)
        VALUES(#{employeeId}, #{missDate}, #{punchType}, #{makeupTime}, #{reason}, 'PENDING', NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MakeupRequest m);

    @Update("""
        UPDATE makeup_request SET status=#{status}, approver_id=#{approverId}, approve_time=NOW(),
        approve_remark=#{remark} WHERE id=#{id}
    """)
    int updateApproval(@Param("id") Long id, @Param("status") String status,
                       @Param("approverId") Long approverId, @Param("remark") String remark);

    @Select("SELECT COUNT(*) FROM makeup_request WHERE status = 'PENDING'")
    long countPending();

    @Delete("DELETE FROM makeup_request WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM makeup_request WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(Long employeeId);
}
