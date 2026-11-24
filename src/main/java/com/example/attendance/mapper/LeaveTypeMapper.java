package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LeaveTypeMapper {

    @Select("SELECT * FROM leave_type ORDER BY sort_no, id")
    List<LeaveType> findAll();

    @Select("SELECT * FROM leave_type WHERE enabled = 1 ORDER BY sort_no, id")
    List<LeaveType> findEnabled();

    @Select("SELECT * FROM leave_type WHERE id = #{id}")
    LeaveType findById(Long id);

    @Select("SELECT * FROM leave_type WHERE code = #{code}")
    LeaveType findByCode(String code);

    @Insert("""
        INSERT INTO leave_type(code, name, annual_quota, paid, need_approval, carry_forward,
                               deduct_rule, color, sort_no, enabled)
        VALUES(#{code}, #{name}, #{annualQuota}, #{paid}, #{needApproval}, #{carryForward},
               #{deductRule}, #{color}, #{sortNo}, #{enabled})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LeaveType t);

    @Update("""
        UPDATE leave_type SET name=#{name}, annual_quota=#{annualQuota}, paid=#{paid},
        need_approval=#{needApproval}, carry_forward=#{carryForward}, deduct_rule=#{deductRule},
        color=#{color}, sort_no=#{sortNo}, enabled=#{enabled} WHERE id=#{id}
    """)
    int update(LeaveType t);

    @Delete("DELETE FROM leave_type WHERE id = #{id}")
    int deleteById(Long id);
}
