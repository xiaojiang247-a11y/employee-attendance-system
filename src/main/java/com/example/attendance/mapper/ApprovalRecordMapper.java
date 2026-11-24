package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ApprovalRecordMapper {

    @Insert("""
        INSERT INTO approval_record(biz_type, biz_id, step_no, approver_id, approver_name,
                                    action, remark, approve_time)
        VALUES(#{bizType}, #{bizId}, #{stepNo}, #{approverId}, #{approverName},
               #{action}, #{remark}, NOW())
    """)
    int insert(ApprovalRecord r);

    @Select("SELECT * FROM approval_record WHERE biz_type = #{bizType} AND biz_id = #{bizId} ORDER BY step_no, id")
    List<ApprovalRecord> findByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    @Select("SELECT * FROM approval_record ORDER BY approve_time DESC, id DESC LIMIT #{limit}")
    List<ApprovalRecord> findRecent(@Param("limit") int limit);

    @Select("""
        SELECT * FROM approval_record
        WHERE approver_id = #{approverId} AND action IN ('APPROVE','REJECT')
        ORDER BY approve_time DESC LIMIT #{limit}
    """)
    List<ApprovalRecord> findDone(@Param("approverId") Long approverId, @Param("limit") int limit);
}
