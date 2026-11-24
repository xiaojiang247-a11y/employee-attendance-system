package com.example.attendance.mapper;

import com.example.attendance.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ApprovalMapper {

    /** 统一待办：5 类单据 UNION，部门负责人按 deptId 做数据隔离 */
    @Select("""
        SELECT * FROM (
          SELECT 'LEAVE' AS biz_type, lr.id AS biz_id, e.id AS employee_id, e.name AS employee_name,
                 e.employee_no, e.department_id AS dept_id, d.name AS department_name,
                 CONCAT(COALESCE(lt.name,'请假'), ' ', lr.days, '天') AS title,
                 CONCAT(lr.start_date, ' ~ ', lr.end_date, '  ', IFNULL(lr.reason,'')) AS summary,
                 lr.apply_time, lr.status, lr.current_step AS step_no
          FROM leave_request lr JOIN employee e ON e.id = lr.employee_id
          LEFT JOIN department d ON d.id = e.department_id
          LEFT JOIN leave_type lt ON lt.code = COALESCE(lr.type_code, lr.type)
          WHERE lr.status = 'PENDING' AND (#{deptId} IS NULL OR e.department_id = #{deptId}
                OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
          UNION ALL
          SELECT 'OVERTIME', o.id, e.id, e.name, e.employee_no, e.department_id, d.name,
                 CONCAT('加班 ', o.hours, '小时'), CONCAT(o.ot_date, '  ', IFNULL(o.reason,'')),
                 o.apply_time, o.status, o.current_step
          FROM overtime_request o JOIN employee e ON e.id = o.employee_id
          LEFT JOIN department d ON d.id = e.department_id
          WHERE o.status = 'PENDING' AND (#{deptId} IS NULL OR e.department_id = #{deptId}
                OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
          UNION ALL
          SELECT 'TRIP', t.id, e.id, e.name, e.employee_no, e.department_id, d.name,
                 CONCAT('出差 · ', t.destination, ' ', t.days, '天'),
                 CONCAT(t.start_date, ' ~ ', t.end_date, '  ', IFNULL(t.reason,'')),
                 t.apply_time, t.status, 1
          FROM business_trip t JOIN employee e ON e.id = t.employee_id
          LEFT JOIN department d ON d.id = e.department_id
          WHERE t.status = 'PENDING' AND (#{deptId} IS NULL OR e.department_id = #{deptId}
                OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
          UNION ALL
          SELECT 'MAKEUP', m.id, e.id, e.name, e.employee_no, e.department_id, d.name,
                 CONCAT('补卡 · ', IF(m.punch_type = 'IN','上班卡','下班卡')),
                 CONCAT(m.miss_date, '  ', IFNULL(m.reason,'')), m.apply_time, m.status, 1
          FROM makeup_request m JOIN employee e ON e.id = m.employee_id
          LEFT JOIN department d ON d.id = e.department_id
          WHERE m.status = 'PENDING' AND (#{deptId} IS NULL OR e.department_id = #{deptId}
                OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
          UNION ALL
          SELECT 'APPEAL', a.id, e.id, e.name, e.employee_no, e.department_id, d.name,
                 '考勤异常申诉', CONCAT(a.appeal_date, '  ', IFNULL(a.reason,'')),
                 a.apply_time, a.status, 1
          FROM attendance_appeal a JOIN employee e ON e.id = a.employee_id
          LEFT JOIN department d ON d.id = e.department_id
          WHERE a.status = 'PENDING' AND (#{deptId} IS NULL OR e.department_id = #{deptId}
                OR e.department_id IN (SELECT id FROM department WHERE parent_id = #{deptId}))
        ) t ORDER BY t.apply_time DESC, t.biz_id DESC
    """)
    List<ApprovalTask> findPending(@Param("deptId") Long deptId);

}
