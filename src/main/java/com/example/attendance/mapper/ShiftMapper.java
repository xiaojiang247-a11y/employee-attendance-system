package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ShiftMapper {

    @Select("""
        SELECT s.*, (SELECT COUNT(*) FROM employee e WHERE e.shift_id = s.id AND e.status='ACTIVE') AS emp_count
        FROM shift s ORDER BY s.id
    """)
    List<Shift> findAll();

    @Select("SELECT * FROM shift WHERE enabled = 1 ORDER BY id")
    List<Shift> findEnabled();

    @Select("SELECT * FROM shift WHERE id = #{id}")
    Shift findById(Long id);

    @Insert("""
        INSERT INTO shift(code, name, start_time, end_time, late_grace, early_grace, flex_minutes,
                          work_hours, cross_day, color, remark, enabled)
        VALUES(#{code}, #{name}, #{startTime}, #{endTime}, #{lateGrace}, #{earlyGrace}, #{flexMinutes},
               #{workHours}, #{crossDay}, #{color}, #{remark}, #{enabled})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Shift s);

    @Update("""
        UPDATE shift SET name=#{name}, start_time=#{startTime}, end_time=#{endTime},
        late_grace=#{lateGrace}, early_grace=#{earlyGrace}, flex_minutes=#{flexMinutes},
        work_hours=#{workHours}, cross_day=#{crossDay}, color=#{color}, remark=#{remark},
        enabled=#{enabled} WHERE id=#{id}
    """)
    int update(Shift s);

    @Delete("DELETE FROM shift WHERE id = #{id}")
    int deleteById(Long id);
}
