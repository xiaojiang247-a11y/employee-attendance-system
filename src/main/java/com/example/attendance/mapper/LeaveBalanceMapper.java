package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LeaveBalanceMapper {

    @Select("""
        SELECT b.*, e.name AS employee_name, e.employee_no, lt.name AS type_name, lt.color
        FROM leave_balance b
        LEFT JOIN employee e ON b.employee_id = e.id
        LEFT JOIN leave_type lt ON lt.code = b.type_code
        WHERE b.year_num = #{year}
          AND (#{employeeId} IS NULL OR b.employee_id = #{employeeId})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId})
        ORDER BY b.employee_id, b.type_code
    """)
    List<LeaveBalance> findByYear(@Param("year") int year, @Param("employeeId") Long employeeId,
                                  @Param("deptId") Long deptId);

    @Select("SELECT * FROM leave_balance WHERE employee_id = #{employeeId} AND year_num = #{year} AND type_code = #{typeCode}")
    LeaveBalance findOne(@Param("employeeId") Long employeeId, @Param("year") int year,
                         @Param("typeCode") String typeCode);

    @Insert("""
        INSERT INTO leave_balance(employee_id, year_num, type_code, total_days, used_days, remain_days)
        VALUES(#{employeeId}, #{yearNum}, #{typeCode}, #{totalDays}, #{usedDays}, #{remainDays})
        ON DUPLICATE KEY UPDATE total_days=VALUES(total_days), remain_days=VALUES(remain_days)
    """)
    int upsert(LeaveBalance b);

    @Update("""
        UPDATE leave_balance SET used_days = used_days + #{delta},
        remain_days = remain_days - #{delta} WHERE id = #{id}
    """)
    int consume(@Param("id") Long id, @Param("delta") BigDecimal delta);

    @Update("""
        UPDATE leave_balance SET total_days = total_days + #{days},
        remain_days = remain_days + #{days} WHERE id = #{id}
    """)
    int addDays(@Param("id") Long id, @Param("days") BigDecimal days);

    @Insert("""
        INSERT IGNORE INTO leave_balance(employee_id, year_num, type_code, total_days, used_days, remain_days)
        SELECT e.id, #{year}, lt.code, lt.annual_quota, 0, lt.annual_quota
        FROM employee e CROSS JOIN leave_type lt
        WHERE e.status = 'ACTIVE' AND lt.enabled = 1 AND lt.annual_quota > 0
    """)
    int grantYearQuota(@Param("year") int year);
}
