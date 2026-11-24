package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username} AND enabled = 1")
    User findByUsername(String username);

    /** 不带 enabled 过滤：初始化与用户名查重必须用它，否则被禁用账号会被当成不存在 */
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsernameIgnoreEnabled(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM sys_user WHERE employee_id = #{employeeId}")
    User findByEmployeeId(Long employeeId);

    @Select("""
        SELECT u.*, d.name AS dept_name, e.name AS employee_name, e.employee_no, r.name AS role_name
        FROM sys_user u
        LEFT JOIN department d ON u.dept_id = d.id
        LEFT JOIN employee e ON u.employee_id = e.id
        LEFT JOIN sys_role r ON r.code = u.role
        WHERE (#{keyword} IS NULL OR #{keyword} = '' OR u.username LIKE CONCAT('%',#{keyword},'%')
              OR u.real_name LIKE CONCAT('%',#{keyword},'%'))
          AND (#{deptId} IS NULL OR u.dept_id = #{deptId})
        ORDER BY u.id
    """)
    List<User> findAll(@Param("keyword") String keyword, @Param("deptId") Long deptId);

    @Select("SELECT r.code FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> findRoleCodes(Long userId);

    @Select("""
        SELECT DISTINCT m.perm_code FROM sys_user_role ur
        JOIN sys_role_menu rm ON rm.role_id = ur.role_id
        JOIN sys_menu m ON m.id = rm.menu_id
        WHERE ur.user_id = #{userId} AND m.perm_code IS NOT NULL AND m.perm_code <> ''
    """)
    List<String> findPermCodes(Long userId);

    @Select("""
        SELECT DISTINCT m.* FROM sys_menu m
        JOIN sys_role_menu rm ON rm.menu_id = m.id
        JOIN sys_user_role ur ON ur.role_id = rm.role_id
        WHERE ur.user_id = #{userId} AND m.visible = 1
        ORDER BY m.parent_id, m.sort_no, m.id
    """)
    List<Menu> findMenusByUserId(Long userId);

    @Insert("""
        INSERT INTO sys_user(username, password, plain_password, real_name, role, employee_id, dept_id, enabled,
                             created_by, role_level)
        VALUES(#{username}, #{password}, #{plainPassword}, #{realName}, #{role}, #{employeeId}, #{deptId}, #{enabled},
               #{createdBy}, #{roleLevel})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("""
        UPDATE sys_user SET real_name=#{realName}, role=#{role}, employee_id=#{employeeId},
        dept_id=#{deptId}, phone=#{phone}, email=#{email}, enabled=#{enabled},
        role_level=#{roleLevel} WHERE id=#{id}
    """)
    int update(User user);

    @Update("UPDATE sys_user SET password=#{password}, plain_password=#{plainPassword} WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password,
                       @Param("plainPassword") String plainPassword);

    @Update("UPDATE sys_user SET enabled=#{enabled} WHERE id=#{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") int enabled);

    @Update("UPDATE sys_user SET last_login_time=#{time}, last_login_ip=#{ip} WHERE id=#{id}")
    int updateLoginInfo(@Param("id") Long id, @Param("time") LocalDateTime time, @Param("ip") String ip);

    @Update("""
        UPDATE sys_user SET password = #{password}, plain_password = #{plainPassword}, enabled = 1
        WHERE id = #{id}
    """)
    int resetPasswordAndEnable(@Param("id") Long id, @Param("password") String password,
                               @Param("plainPassword") String plainPassword);

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteRoles(Long userId);

    @Insert("INSERT IGNORE INTO sys_user_role(user_id, role_id) VALUES(#{userId}, #{roleId})")
    int grantRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_user WHERE employee_id = #{employeeId}")
    int deleteByEmployeeId(Long employeeId);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM sys_user")
    long countAll();

    /** 账户层级过滤：超级管理员看 ADMIN 级，管理员看 EMPLOYEE 级，互不越界；
     *  deptId 由页面筛选下推到 SQL，不是只做前端摆设 */
    @Select("""
        SELECT u.*, d.name AS dept_name, r.name AS role_name
        FROM sys_user u
        LEFT JOIN department d ON u.dept_id = d.id
        LEFT JOIN sys_role r ON r.code = u.role
        WHERE u.role_level = #{level}
          AND (#{keyword} IS NULL OR #{keyword} = '' OR u.username LIKE CONCAT('%',#{keyword},'%')
               OR u.real_name LIKE CONCAT('%',#{keyword},'%'))
          AND (#{deptId} IS NULL OR u.dept_id = #{deptId})
        ORDER BY u.id
    """)
    List<User> findByLevel(@Param("level") String level, @Param("keyword") String keyword,
                           @Param("deptId") Long deptId);

    @Select("SELECT COUNT(*) FROM sys_user WHERE role_level = #{level}")
    long countByLevel(@Param("level") String level);

    @Select("SELECT COUNT(*) FROM sys_user WHERE enabled = 1")
    long countEnabled();

    @Update("UPDATE sys_user SET created_by = #{createdBy} WHERE id = #{id}")
    int updateCreatedBy(@Param("id") Long id, @Param("createdBy") String createdBy);
}