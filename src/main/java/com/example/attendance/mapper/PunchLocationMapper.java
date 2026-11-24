package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PunchLocationMapper {

    @Select("""
        SELECT l.*,
          (SELECT GROUP_CONCAT(g.name SEPARATOR ' / ') FROM attendance_group g
           JOIN attendance_group_location gl ON gl.group_id = g.id
           WHERE gl.location_id = l.id) AS group_names
        FROM punch_location l ORDER BY l.is_default DESC, l.enabled DESC, l.id
    """)
    List<PunchLocation> findAll();

    @Select("SELECT * FROM punch_location WHERE enabled = 1 ORDER BY is_default DESC, id")
    List<PunchLocation> findEnabled();

    @Select("SELECT * FROM punch_location WHERE enabled = 1 AND is_default = 1 ORDER BY id LIMIT 1")
    PunchLocation findDefault();

    @Select("""
        SELECT l.* FROM punch_location l
        JOIN attendance_group_location gl ON gl.location_id = l.id
        WHERE gl.group_id = #{groupId} AND l.enabled = 1 ORDER BY l.id
    """)
    List<PunchLocation> findByGroup(Long groupId);

    @Select("SELECT * FROM punch_location WHERE id = #{id}")
    PunchLocation findById(Long id);

    @Select("SELECT group_id FROM attendance_group_location WHERE location_id = #{locationId}")
    List<Long> findGroupIds(Long locationId);

    @Select("SELECT COUNT(*) FROM punch_location WHERE enabled = 1")
    long countEnabled();

    /** 新增即写 update_time：与 update 同口径，列表「最后修改」列才有值；NOW() 走 +08:00 会话 */
    @Insert("""
        INSERT INTO punch_location(name, address, longitude, latitude, radius_m, allow_ip,
                                   wifi_ssid, is_default, enabled, remark,
                                   created_by, created_by_role, updated_by, updated_by_role, update_time)
        VALUES(#{name}, #{address}, #{longitude}, #{latitude}, #{radiusM}, #{allowIp},
               #{wifiSsid}, #{isDefault}, #{enabled}, #{remark},
               #{createdBy}, #{createdByRole}, #{updatedBy}, #{updatedByRole}, NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PunchLocation l);

    /** update_time 用数据库 NOW()，连接会话时区已固定 +08:00（北京时间） */
    @Update("""
        UPDATE punch_location SET name=#{name}, address=#{address}, longitude=#{longitude},
        latitude=#{latitude}, radius_m=#{radiusM}, allow_ip=#{allowIp}, wifi_ssid=#{wifiSsid},
        enabled=#{enabled}, remark=#{remark},
        updated_by=#{updatedBy}, updated_by_role=#{updatedByRole}, update_time=NOW()
        WHERE id=#{id}
    """)
    int update(PunchLocation l);

    /** 超管覆盖：超管设为默认的启用地点一旦存在，围栏只用该地点 */
    @Select("""
        SELECT * FROM punch_location
        WHERE enabled = 1 AND is_default = 1 AND created_by_role = 'SUPER_ADMIN'
        ORDER BY id LIMIT 1
    """)
    PunchLocation findSuperDefault();

    @Update("UPDATE punch_location SET is_default = IF(id = #{id}, 1, 0) WHERE id = #{id} OR is_default = 1")
    int setDefault(Long id);

    /** 超管接管管理员创建的地点：补写接管标记，使 findSuperDefault 与层级保护同时生效 */
    @Update("""
        UPDATE punch_location SET created_by=#{actor}, created_by_role='SUPER_ADMIN',
               updated_by=#{actor}, updated_by_role='SUPER_ADMIN', update_time=NOW()
        WHERE id=#{id}
    """)
    int takeover(@Param("id") Long id, @Param("actor") String actor);

    /** 超管设为默认：同时标记接管，否则管理员创建的地点 findSuperDefault 命中不到，覆盖语义失效 */
    @Update("""
        UPDATE punch_location
        SET is_default = IF(id = #{id}, 1, 0),
            created_by_role = IF(id = #{id}, 'SUPER_ADMIN', created_by_role)
        WHERE id = #{id} OR is_default = 1
    """)
    int setSuperDefault(@Param("id") Long id);

    @Update("UPDATE punch_location SET enabled = #{enabled} WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") int enabled);

    @Delete("DELETE FROM attendance_group_location WHERE location_id = #{locationId}")
    int clearGroups(Long locationId);

    @Insert("INSERT IGNORE INTO attendance_group_location(group_id, location_id) VALUES(#{groupId}, #{locationId})")
    int grantGroup(@Param("groupId") Long groupId, @Param("locationId") Long locationId);

    @Delete("DELETE FROM punch_location WHERE id = #{id}")
    int deleteById(Long id);
}
