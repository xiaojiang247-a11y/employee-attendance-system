package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BusinessTripMapper {

    @Select("""
        SELECT t.*, e.name AS employee_name, e.employee_no, e.department_id AS dept_id,
               d.name AS department_name, u.real_name AS approver_name
        FROM business_trip t
        LEFT JOIN employee e ON t.employee_id = e.id
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN sys_user u ON t.approver_id = u.id
        WHERE (#{employeeId} IS NULL OR t.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
          AND (#{status} IS NULL OR #{status} = '' OR t.status = #{status})
        ORDER BY t.apply_time DESC, t.id DESC
    """)
    List<BusinessTrip> findList(@Param("employeeId") Long employeeId, @Param("deptId") Long deptId,
                                @Param("status") String status);

    @Select("""
        SELECT t.*, e.department_id AS dept_id FROM business_trip t
        LEFT JOIN employee e ON e.id = t.employee_id WHERE t.id = #{id}
    """)
    BusinessTrip findById(Long id);

    @Insert("""
        INSERT INTO business_trip(employee_id, destination, start_date, end_date, days, reason,
                                  status, apply_time)
        VALUES(#{employeeId}, #{destination}, #{startDate}, #{endDate}, #{days}, #{reason}, 'PENDING', NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BusinessTrip t);

    @Update("""
        UPDATE business_trip SET status=#{status}, approver_id=#{approverId}, approve_time=NOW(),
        approve_remark=#{remark} WHERE id=#{id}
    """)
    int updateApproval(@Param("id") Long id, @Param("status") String status,
                       @Param("approverId") Long approverId, @Param("remark") String remark);

    @Select("SELECT COUNT(*) FROM business_trip WHERE status = 'PENDING'")
    long countPending();

    @Delete("DELETE FROM business_trip WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM business_trip WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(Long employeeId);
}
