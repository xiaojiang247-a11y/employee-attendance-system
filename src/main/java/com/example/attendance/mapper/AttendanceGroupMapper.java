package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AttendanceGroupMapper {

    @Select("""
        SELECT g.*, d.name AS department_name, s.name AS shift_name,
               (SELECT COUNT(*) FROM employee e WHERE e.group_id = g.id AND e.status='ACTIVE') AS emp_count
        FROM attendance_group g
        LEFT JOIN department d ON g.department_id = d.id
        LEFT JOIN shift s ON g.default_shift_id = s.id
        ORDER BY g.id
    """)
    List<AttendanceGroup> findAll();

    @Select("SELECT * FROM attendance_group WHERE id = #{id}")
    AttendanceGroup findById(Long id);

    @Insert("""
        INSERT INTO attendance_group(name, group_type, department_id, default_shift_id, workdays,
                                     allow_ip, wifi_ssid, punch_ahead_min, remark, enabled)
        VALUES(#{name}, #{groupType}, #{departmentId}, #{defaultShiftId}, #{workdays},
               #{allowIp}, #{wifiSsid}, #{punchAheadMin}, #{remark}, #{enabled})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AttendanceGroup g);

    @Update("""
        UPDATE attendance_group SET name=#{name}, group_type=#{groupType}, department_id=#{departmentId},
        default_shift_id=#{defaultShiftId}, workdays=#{workdays}, allow_ip=#{allowIp},
        wifi_ssid=#{wifiSsid}, punch_ahead_min=#{punchAheadMin}, remark=#{remark}, enabled=#{enabled}
        WHERE id=#{id}
    """)
    int update(AttendanceGroup g);

    @Delete("DELETE FROM attendance_group WHERE id = #{id}")
    int deleteById(Long id);
}
