package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OperLogMapper {

    @Insert("""
        INSERT INTO sys_oper_log(module, operation, request_method, request_url, ip, username,
                                 params, result, cost_ms, create_time)
        VALUES(#{module}, #{operation}, #{requestMethod}, #{requestUrl}, #{ip}, #{username},
               #{params}, #{result}, #{costMs}, NOW())
    """)
    int insert(OperLog log);

    @Select("""
        SELECT * FROM sys_oper_log
        WHERE (#{keyword} IS NULL OR #{keyword} = '' OR username LIKE CONCAT('%',#{keyword},'%')
              OR operation LIKE CONCAT('%',#{keyword},'%') OR request_url LIKE CONCAT('%',#{keyword},'%'))
        ORDER BY create_time DESC, id DESC LIMIT #{limit}
    """)
    List<OperLog> findRecent(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_oper_log")
    long countAll();

    @Select("SELECT COUNT(*) FROM sys_oper_log WHERE DATE(create_time) = CURDATE()")
    long countToday();
}
