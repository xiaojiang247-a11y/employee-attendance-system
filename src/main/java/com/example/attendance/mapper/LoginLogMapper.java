package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LoginLogMapper {

    @Insert("INSERT INTO login_log(username, ip, login_time, success, user_agent) VALUES(#{username}, #{ip}, NOW(), #{success}, #{userAgent})")
    int insert(LoginLog log);

    @Select("""
        SELECT * FROM login_log
        WHERE (#{keyword} IS NULL OR #{keyword} = '' OR username LIKE CONCAT('%',#{keyword},'%'))
        ORDER BY login_time DESC LIMIT #{limit}
    """)
    List<LoginLog> findRecent(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM login_log WHERE DATE(login_time) = CURDATE()")
    long countToday();

    @Select("SELECT COUNT(*) FROM login_log WHERE DATE(login_time) = CURDATE() AND success = 0")
    long countTodayFailed();
}
