package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface EmployeeMapper {

    @Select("""
        SELECT e.*, d.name AS department_name, g.name AS group_name, s.name AS shift_name,
               u.username AS login_username, u.plain_password AS login_password
        FROM employee e
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN attendance_group g ON e.group_id = g.id
        LEFT JOIN shift s ON e.shift_id = s.id
        LEFT JOIN sys_user u ON u.employee_id = e.id
        WHERE (#{status} IS NULL OR #{status} = '' OR e.status = #{status})
          AND (#{deptId} IS NULL OR e.department_id = #{deptId}
               OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
          AND (#{keyword} IS NULL OR #{keyword} = '' OR e.name LIKE CONCAT('%',#{keyword},'%')
               OR e.employee_no LIKE CONCAT('%',#{keyword},'%') OR e.phone LIKE CONCAT('%',#{keyword},'%'))
        ORDER BY e.id DESC
    """)
    List<Employee> search(@Param("keyword") String keyword, @Param("deptId") Long deptId,
                          @Param("status") String status);

    @Select("""
        SELECT e.*, d.name AS department_name, g.name AS group_name, s.name AS shift_name,
               u.username AS login_username, u.plain_password AS login_password
        FROM employee e
        LEFT JOIN department d ON e.department_id = d.id
        LEFT JOIN attendance_group g ON e.group_id = g.id
        LEFT JOIN shift s ON e.shift_id = s.id
        LEFT JOIN sys_user u ON u.employee_id = e.id
        WHERE e.id = #{id}
    """)
    Employee findById(Long id);

    @Select("SELECT * FROM employee WHERE employee_no = #{employeeNo}")
    Employee findByEmployeeNo(String employeeNo);

    @Select("""
        SELECT e.*, d.name AS department_name FROM employee e
        LEFT JOIN department d ON e.department_id = d.id
        WHERE e.status = 'ACTIVE' ORDER BY e.id
    """)
    List<Employee> findAllActive();

    @Select("SELECT * FROM employee WHERE department_id = #{deptId} AND status = 'ACTIVE' ORDER BY id")
    List<Employee> findByDeptId(Long deptId);

    @Insert("""
        INSERT INTO employee(employee_no, name, gender, phone, email, department_id, group_id, shift_id,
                             position, hire_date, status, employment_type, birth_date, id_card, education,
                             address, emergency_contact, emergency_phone, remark)
        VALUES(#{employeeNo}, #{name}, #{gender}, #{phone}, #{email}, #{departmentId}, #{groupId}, #{shiftId},
               #{position}, #{hireDate}, 'ACTIVE', #{employmentType}, #{birthDate}, #{idCard}, #{education},
               #{address}, #{emergencyContact}, #{emergencyPhone}, #{remark})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Employee e);

    @Update("""
        UPDATE employee SET employee_no=#{employeeNo}, name=#{name}, gender=#{gender}, phone=#{phone},
            email=#{email}, department_id=#{departmentId}, group_id=#{groupId}, shift_id=#{shiftId},
            position=#{position}, hire_date=#{hireDate}, employment_type=#{employmentType},
            birth_date=#{birthDate}, id_card=#{idCard}, education=#{education}, address=#{address},
            emergency_contact=#{emergencyContact}, emergency_phone=#{emergencyPhone}, remark=#{remark}
        WHERE id=#{id}
    """)
    int update(Employee e);

    @Update("UPDATE employee SET status=#{status}, leave_date=#{leaveDate} WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("leaveDate") LocalDate leaveDate);

    @Update("UPDATE employee SET group_id=#{groupId}, shift_id=#{shiftId} WHERE id=#{id}")
    int updateGroupShift(@Param("id") Long id, @Param("groupId") Long groupId, @Param("shiftId") Long shiftId);

    @Delete("DELETE FROM employee WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM employee WHERE status = 'ACTIVE'")
    long countActive();

    @Select("SELECT status, COUNT(*) AS cnt FROM employee GROUP BY status")
    List<Map<String, Object>> countByStatus();

    @Select("""
        SELECT d.id, d.name, COUNT(e.id) AS cnt FROM department d
        LEFT JOIN employee e ON e.department_id = d.id AND e.status = 'ACTIVE'
        GROUP BY d.id, d.name ORDER BY d.sort_no, d.id
    """)
    List<Map<String, Object>> countByDept();
}
