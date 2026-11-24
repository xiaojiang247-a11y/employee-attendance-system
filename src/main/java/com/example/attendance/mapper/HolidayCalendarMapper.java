package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface HolidayCalendarMapper {

    @Select("SELECT * FROM holiday_calendar WHERE cal_date BETWEEN #{start} AND #{end} ORDER BY cal_date")
    List<HolidayCalendar> findByRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Select("SELECT * FROM holiday_calendar WHERE cal_date = #{date}")
    HolidayCalendar findByDate(LocalDate date);

    @Select("SELECT * FROM holiday_calendar WHERE cal_date BETWEEN #{start} AND #{end} AND day_type <> 'WORKDAY' ORDER BY cal_date")
    List<HolidayCalendar> findRestDays(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Insert("""
        INSERT INTO holiday_calendar(cal_date, day_type, name, remark)
        VALUES(#{calDate}, #{dayType}, #{name}, #{remark})
        ON DUPLICATE KEY UPDATE day_type=VALUES(day_type), name=VALUES(name), remark=VALUES(remark)
    """)
    int upsert(HolidayCalendar h);

    @Delete("DELETE FROM holiday_calendar WHERE id = #{id}")
    int deleteById(Long id);
}
