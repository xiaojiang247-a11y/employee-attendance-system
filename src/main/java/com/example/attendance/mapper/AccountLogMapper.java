package com.example.attendance.mapper;

import com.example.attendance.entity.AccountLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AccountLogMapper {

    /** create_time 由业务层显式写北京时间，避免依赖数据库服务器本地时区 */
    @Insert("""
        INSERT INTO sys_account_log(operator, operator_role, action, target_user, target_role,
                                    detail, ip, result, create_time)
        VALUES(#{operator}, #{operatorRole}, #{action}, #{targetUser}, #{targetRole},
               #{detail}, #{ip}, #{result}, #{createTime})
    """)
    int insert(AccountLog log);

    @Select("""
        SELECT * FROM sys_account_log
        WHERE (#{keyword} IS NULL OR #{keyword} = '' OR target_user LIKE CONCAT('%',#{keyword},'%')
              OR operator LIKE CONCAT('%',#{keyword},'%') OR detail LIKE CONCAT('%',#{keyword},'%'))
        ORDER BY id DESC LIMIT #{limit}
    """)
    List<AccountLog> findRecent(@Param("keyword") String keyword, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM sys_account_log")
    long countAll();

    @Select("SELECT COUNT(*) FROM sys_account_log WHERE DATE(create_time) = CURDATE()")
    long countToday();

    @Select("""
        SELECT action, COUNT(*) AS cnt FROM sys_account_log
        GROUP BY action ORDER BY cnt DESC
    """)
    List<java.util.Map<String, Object>> actionStats();

    @Select("SELECT COUNT(*) FROM sys_account_log WHERE operator = #{operator}")
    long countByOperator(@Param("operator") String operator);
}
