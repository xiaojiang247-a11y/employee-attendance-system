package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface MenuMapper {

    @Select("SELECT * FROM sys_menu ORDER BY parent_id, sort_no, id")
    List<Menu> findAll();

    @Select("SELECT * FROM sys_menu WHERE id = #{id}")
    Menu findById(Long id);

    @Select("""
        SELECT m.*, IF(rm.role_id IS NULL, 0, 1) AS checked FROM sys_menu m
        LEFT JOIN sys_role_menu rm ON rm.menu_id = m.id AND rm.role_id = #{roleId}
        ORDER BY m.parent_id, m.sort_no, m.id
    """)
    List<Menu> findByRoleId(Long roleId);

    @Select("SELECT COUNT(*) FROM sys_menu WHERE parent_id = #{id}")
    int countChildren(Long id);

    @Insert("""
        INSERT INTO sys_menu(parent_id, name, icon, url, perm_code, menu_type, sort_no, visible)
        VALUES(#{parentId}, #{name}, #{icon}, #{url}, #{permCode}, #{menuType}, #{sortNo}, #{visible})
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Menu m);

    @Update("""
        UPDATE sys_menu SET parent_id=#{parentId}, name=#{name}, icon=#{icon}, url=#{url},
        perm_code=#{permCode}, menu_type=#{menuType}, sort_no=#{sortNo}, visible=#{visible} WHERE id=#{id}
    """)
    int update(Menu m);

    @Delete("DELETE FROM sys_menu WHERE id = #{id}")
    int deleteById(Long id);
}
