package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeMapper {

    @Select("SELECT * FROM sys_notice ORDER BY pinned DESC, publish_time DESC, id DESC")
    List<Notice> findAll();

    @Select("SELECT * FROM sys_notice ORDER BY pinned DESC, publish_time DESC LIMIT #{limit}")
    List<Notice> findTop(@Param("limit") int limit);

    @Select("SELECT * FROM sys_notice WHERE id = #{id}")
    Notice findById(Long id);

    @Insert("""
        INSERT INTO sys_notice(title, content, notice_type, publisher, publish_time, pinned, view_count)
        VALUES(#{title}, #{content}, #{noticeType}, #{publisher}, NOW(), #{pinned}, 0)
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Notice n);

    @Update("""
        UPDATE sys_notice SET title=#{title}, content=#{content}, notice_type=#{noticeType},
        pinned=#{pinned} WHERE id=#{id}
    """)
    int update(Notice n);

    @Update("UPDATE sys_notice SET view_count = view_count + 1 WHERE id = #{id}")
    int incrementView(Long id);

    @Delete("DELETE FROM sys_notice WHERE id = #{id}")
    int deleteById(Long id);
}
