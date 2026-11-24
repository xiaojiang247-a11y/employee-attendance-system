package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface RoleMapper {

    @Select("""
        SELECT r.*, (SELECT COUNT(*) FROM sys_user_role ur WHERE ur.role_id = r.id) AS user_count
        FROM sys_role r ORDER BY r.id
    """)
    List<Role> findAll();

    @Select("SELECT * FROM sys_role WHERE id = #{id}")
    Role findById(Long id);

    @Select("SELECT * FROM sys_role WHERE code = #{code}")
    Role findByCode(String code);

    @Insert("INSERT INTO sys_role(code, name, description, data_scope, enabled) VALUES(#{code}, #{name}, #{description}, #{dataScope}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Role r);

    @Update("UPDATE sys_role SET name=#{name}, description=#{description}, data_scope=#{dataScope}, enabled=#{enabled} WHERE id=#{id}")
    int update(Role r);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int clearMenus(Long roleId);

    @Insert("INSERT IGNORE INTO sys_role_menu(role_id, menu_id) VALUES(#{roleId}, #{menuId})")
    int grantMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);

    @Delete("DELETE FROM sys_role WHERE id = #{id}")
    int deleteById(Long id);
}
