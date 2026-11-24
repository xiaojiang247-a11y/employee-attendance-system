package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface DepartmentMapper {

    @Select("""
        SELECT d.*, e.name AS leader_name,
          (SELECT COUNT(*) FROM employee x WHERE x.department_id = d.id AND x.status = 'ACTIVE') AS emp_count
        FROM department d LEFT JOIN employee e ON d.leader_id = e.id
        ORDER BY d.sort_no, d.id
    """)
    List<Department> findAll();

    @Select("SELECT * FROM department WHERE id = #{id}")
    Department findById(Long id);

    @Select("SELECT * FROM department WHERE parent_id = #{parentId} ORDER BY sort_no, id")
    List<Department> findByParentId(Long parentId);

    @Select("SELECT COUNT(*) FROM employee WHERE department_id = #{id} AND status = 'ACTIVE'")
    int countEmployees(Long id);

    @Select("SELECT COUNT(*) FROM department WHERE parent_id = #{id}")
    int countChildren(Long id);

    @Insert("""
        INSERT INTO department(code, name, description, parent_id, leader_id, sort_no)
        VALUES(#{code}, #{name}, #{description}, #{parentId}, #{leaderId}, #{sortNo})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Department d);

    @Update("""
        UPDATE department SET code=#{code}, name=#{name}, description=#{description},
        parent_id=#{parentId}, leader_id=#{leaderId}, sort_no=#{sortNo} WHERE id=#{id}
    """)
    int update(Department d);

    @Delete("DELETE FROM department WHERE id = #{id}")
    int deleteById(Long id);
}
