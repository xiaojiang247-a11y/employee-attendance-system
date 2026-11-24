-- ============================================================
-- 企业员工考勤管理系统 V2 演示数据
-- 依赖：先执行 schema_v2.sql
-- 幂等：全部使用 INSERT IGNORE / UPDATE，重复执行不会产生重复数据
-- ============================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 0. 幂等重置：仅清除本脚本生成的演示申请单，不触碰真实业务数据
--    排班、考勤流水、账号、额度均带唯一键，INSERT IGNORE 天然幂等
-- ----------------------------
DELETE FROM leave_request WHERE reason IN (
  '家中有事，申请年假两天','感冒发热，就医休息一天','办理个人证件，需请假两天',
  '计划外出旅行，申请年假','上周加班调休一天');
DELETE FROM overtime_request WHERE reason IN (
  '版本上线前联调，需加班完成','月末报表核对','客服系统周末割接支持');
DELETE FROM business_trip WHERE reason IN (
  '参加行业展会并拜访重点客户','客户需求调研与现场答疑');
DELETE FROM makeup_request WHERE reason IN (
  '下班时系统卡顿未打上卡','外出拜访客户，返回后忘记打卡');
DELETE FROM attendance_appeal WHERE reason IN (
  '当日地铁故障导致迟到，附车站延误证明','下班打卡设备离线，已提交补卡说明');
DELETE ar FROM approval_record ar
WHERE (ar.biz_type='LEAVE'    AND NOT EXISTS (SELECT 1 FROM leave_request l    WHERE l.id=ar.biz_id))
   OR (ar.biz_type='OVERTIME' AND NOT EXISTS (SELECT 1 FROM overtime_request o WHERE o.id=ar.biz_id))
   OR (ar.biz_type='TRIP'     AND NOT EXISTS (SELECT 1 FROM business_trip t    WHERE t.id=ar.biz_id))
   OR (ar.biz_type='MAKEUP'   AND NOT EXISTS (SELECT 1 FROM makeup_request m   WHERE m.id=ar.biz_id));

-- 演示账号 BCrypt 密文取自唯一内置超级管理员账户；明文密码以该账户为准。
-- 注：本脚本用于生成演示数据，正式交付环境不执行。
SET @emp_hash := (SELECT password FROM sys_user WHERE username = 'superadmin' LIMIT 1);

-- ----------------------------
-- 1. 补充员工档案（覆盖 6 个部门、多岗位、试用/正式、在职/离职）
-- ----------------------------
INSERT IGNORE INTO employee (employee_no, name, gender, phone, email, department_id, position,
                             hire_date, status, group_id, shift_id, employment_type,
                             education, emergency_contact, emergency_phone, remark) VALUES
('EMP010', '张伟',   '男', '13800138010', 'zhangwei@company.com',   5, 'Java后端工程师',   '2021-03-15', 'ACTIVE', 2, 1, 'REGULAR',   '本科', '张父', '13900000010', '研发一组组长'),
('EMP011', '李娜',   '女', '13800138011', 'lina@company.com',       5, '架构师',           '2020-07-01', 'ACTIVE', 2, 1, 'REGULAR',   '硕士', '李母', '13900000011', ''),
('EMP012', '王强',   '男', '13800138012', 'wangqiang@company.com',  6, '前端工程师',       '2023-02-20', 'ACTIVE', 2, 1, 'REGULAR',   '本科', '王父', '13900000012', ''),
('EMP013', '刘洋',   '女', '13800138013', 'liuyang@company.com',    6, 'UI设计师',         '2024-06-03', 'ACTIVE', 2, 1, 'PROBATION', '本科', '刘母', '13900000013', '试用期'),
('EMP014', '陈静',   '女', '13800138014', 'chenjing@company.com',   2, '人事经理',         '2020-09-10', 'ACTIVE', 1, 1, 'REGULAR',   '本科', '陈父', '13900000014', ''),
('EMP015', '赵敏',   '女', '13800138015', 'zhaomin@company.com',    2, '招聘专员',         '2024-01-08', 'ACTIVE', 1, 1, 'REGULAR',   '本科', '赵母', '13900000015', ''),
('EMP016', '孙浩',   '男', '13800138016', 'sunhao@company.com',     3, '财务主管',         '2019-11-11', 'ACTIVE', 1, 1, 'REGULAR',   '本科', '孙父', '13900000016', ''),
('EMP017', '周琳',   '女', '13800138017', 'zhoulin@company.com',    3, '会计',             '2023-08-21', 'ACTIVE', 1, 1, 'REGULAR',   '本科', '周母', '13900000017', ''),
('EMP018', '吴磊',   '男', '13800138018', 'wulei@company.com',      4, '市场经理',         '2021-05-16', 'ACTIVE', 3, 2, 'REGULAR',   '本科', '吴父', '13900000018', ''),
('EMP019', '郑爽',   '女', '13800138019', 'zhengshuang@company.com',4, '客户经理',         '2024-03-01', 'ACTIVE', 3, 3, 'REGULAR',   '本科', '郑母', '13900000019', '轮班制'),
('EMP020', '冯超',   '男', '13800138020', 'fengchao@company.com',   4, '客服专员',         '2025-01-06', 'ACTIVE', 3, 2, 'PROBATION', '大专', '冯父', '13900000020', '轮班制'),
('EMP021', '蒋雯',   '女', '13800138021', 'jiangwen@company.com',   1, '测试工程师',       '2022-10-10', 'ACTIVE', 2, 1, 'REGULAR',   '本科', '蒋母', '13900000021', ''),
('EMP009', '许峰',   '男', '13800138009', 'xufeng@company.com',     1, '运维工程师',       '2021-08-01', 'LEAVE',  1, 1, 'REGULAR',   '本科', '许父', '13900000009', '已离职，用于演示离职档案管理');

-- 原库存量员工补齐 V2 字段
UPDATE employee SET employee_no = 'EMP008', name = '李四', gender = '男', phone = '13800138008',
                    email = 'lisi@company.com', department_id = 5, position = 'Java开发工程师',
                    hire_date = '2023-06-01', group_id = 2, shift_id = 1,
                    employment_type = 'REGULAR', education = '本科',
                    emergency_contact = '李父', emergency_phone = '13900000008'
WHERE id = 21 AND employee_no = '2431232234';

UPDATE employee SET group_id = COALESCE(group_id, 1), shift_id = COALESCE(shift_id, 1)
WHERE group_id IS NULL OR shift_id IS NULL;

UPDATE department SET leader_id = (SELECT id FROM employee WHERE employee_no='EMP011' LIMIT 1) WHERE id = 5;
UPDATE department SET leader_id = (SELECT id FROM employee WHERE employee_no='EMP014' LIMIT 1) WHERE id = 2;
UPDATE department SET leader_id = (SELECT id FROM employee WHERE employee_no='EMP018' LIMIT 1) WHERE id = 4;

-- ----------------------------
-- 2. 登录账号与角色授权
-- ----------------------------
INSERT IGNORE INTO sys_user (username, password, plain_password, real_name, role, employee_id, enabled, dept_id) VALUES
('hr01',      @emp_hash, '123456', '陈静',   'HR',      (SELECT id FROM employee WHERE employee_no='EMP014'), 1, 2),
('manager01', @emp_hash, '123456', '李娜',   'MANAGER', (SELECT id FROM employee WHERE employee_no='EMP011'), 1, 1),
('zhangwei',  @emp_hash, '123456', '张伟',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP010'), 1, 5),
('wangqiang', @emp_hash, '123456', '王强',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP012'), 1, 6),
('liuyang',   @emp_hash, '123456', '刘洋',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP013'), 1, 6),
('zhaomin',   @emp_hash, '123456', '赵敏',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP015'), 1, 2),
('sunhao',    @emp_hash, '123456', '孙浩',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP016'), 1, 3),
('zhoulin',   @emp_hash, '123456', '周琳',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP017'), 1, 3),
('wulei',     @emp_hash, '123456', '吴磊',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP018'), 1, 4),
('zhengshuang',@emp_hash,'123456', '郑爽',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP019'), 1, 4),
('fengchao',  @emp_hash, '123456', '冯超',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP020'), 1, 4),
('jiangwen',  @emp_hash, '123456', '蒋雯',   'EMPLOYEE',(SELECT id FROM employee WHERE employee_no='EMP021'), 1, 1);

-- 一个员工可能有主角色之外的附加角色（此处主角色与关联表保持一致）
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id,
       CASE u.role WHEN 'ADMIN' THEN 1 WHEN 'HR' THEN 2 WHEN 'MANAGER' THEN 3 ELSE 4 END
FROM sys_user u;

UPDATE sys_user SET dept_id = (SELECT department_id FROM employee WHERE employee.id = sys_user.employee_id)
WHERE dept_id IS NULL AND employee_id IS NOT NULL;
-- 部门负责人以技术部(父部门)为数据范围，覆盖其下研发一组/二组
UPDATE sys_user SET dept_id = 1 WHERE username = 'manager01';

-- ----------------------------
-- 3. 假期额度发放（当年年假 10 天 / 调休按审批通过的加班累计）
-- ----------------------------
INSERT IGNORE INTO leave_balance (employee_id, year_num, type_code, total_days, used_days, remain_days)
SELECT e.id, YEAR(CURDATE()), 'ANNUAL', 10, 0, 10 FROM employee e WHERE e.status = 'ACTIVE';
INSERT IGNORE INTO leave_balance (employee_id, year_num, type_code, total_days, used_days, remain_days)
SELECT e.id, YEAR(CURDATE()), 'COMP', 0, 0, 0 FROM employee e WHERE e.status = 'ACTIVE';
INSERT IGNORE INTO leave_balance (employee_id, year_num, type_code, total_days, used_days, remain_days)
SELECT e.id, YEAR(CURDATE()), 'MARRIAGE', 10, 0, 10 FROM employee e WHERE e.status = 'ACTIVE';

-- ----------------------------
-- 4. 排班 + 近 90 天考勤流水（存储过程按日按人生成，含随机异常分布）
-- ----------------------------
DROP PROCEDURE IF EXISTS sp_seed_attendance;
DELIMITER $$
CREATE PROCEDURE sp_seed_attendance()
BEGIN
    DECLARE v_date DATE;
    DECLARE v_end  DATE;
    DECLARE v_emp  BIGINT;
    DECLARE v_shift BIGINT;
    DECLARE v_done INT DEFAULT 0;
    DECLARE v_rand DECIMAL(6,4);
    DECLARE v_status VARCHAR(24);
    DECLARE v_in  DATETIME;
    DECLARE v_out DATETIME;
    DECLARE v_late INT;
    DECLARE v_early INT;
    DECLARE v_hours DECIMAL(5,1);
    DECLARE cur CURSOR FOR SELECT id, COALESCE(shift_id, 1) FROM employee WHERE status = 'ACTIVE';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_done = 1;

    SET v_date = DATE_SUB(CURDATE(), INTERVAL 89 DAY);
    SET v_end  = DATE_ADD(CURDATE(), INTERVAL 30 DAY);

    WHILE v_date <= v_end DO
        SET v_done = 0;
        OPEN cur;
        emp_loop: LOOP
            FETCH cur INTO v_emp, v_shift;
            IF v_done THEN LEAVE emp_loop; END IF;

            IF DAYOFWEEK(v_date) IN (1, 7) THEN
                INSERT IGNORE INTO schedule (employee_id, work_date, shift_id, is_rest, source)
                VALUES (v_emp, v_date, NULL, 1, 'AUTO');
            ELSE
                INSERT IGNORE INTO schedule (employee_id, work_date, shift_id, is_rest, source)
                VALUES (v_emp, v_date, v_shift, 0, 'AUTO');

                IF v_date < CURDATE() THEN
                    SET v_rand = RAND();
                    SET v_in = NULL;
                    SET v_out = NULL;
                    SET v_late = 0;
                    SET v_early = 0;
                    SET v_hours = 0;

                    IF v_rand < 0.035 THEN
                        SET v_status = 'ABSENT';
                    ELSEIF v_rand < 0.075 THEN
                        SET v_status = 'LEAVE';
                    ELSE
                        IF RAND() < 0.14 THEN SET v_late = FLOOR(6 + RAND() * 34); END IF;
                        IF RAND() < 0.09 THEN SET v_early = FLOOR(6 + RAND() * 40); END IF;
                        SET v_in = DATE_ADD(
                            STR_TO_DATE(CONCAT(v_date, ' 09:00:00'), '%Y-%m-%d %H:%i:%s'),
                            INTERVAL (8 + v_late) MINUTE);
                        SET v_out = DATE_SUB(
                            STR_TO_DATE(CONCAT(v_date, ' 18:00:00'), '%Y-%m-%d %H:%i:%s'),
                            INTERVAL v_early MINUTE);
                        IF RAND() < 0.045 THEN
                            SET v_status = 'MISSING_OUT';
                            SET v_out = NULL;
                            SET v_hours = 0;
                        ELSE
                            SET v_status = 'NORMAL';
                            IF v_late > 0 AND v_early > 0 THEN SET v_status = 'LATE_AND_EARLY';
                            ELSEIF v_late > 0 THEN SET v_status = 'LATE';
                            ELSEIF v_early > 0 THEN SET v_status = 'EARLY_LEAVE';
                            END IF;
                            SET v_hours = GREATEST(0, TIMESTAMPDIFF(MINUTE, v_in, v_out) / 60.0 - 1.0);
                        END IF;
                    END IF;

                    INSERT IGNORE INTO attendance_record
                        (employee_id, attendance_date, check_in_time, check_out_time, status,
                         shift_id, check_in_ip, check_out_ip, check_in_device, source,
                         late_minutes, early_minutes, work_hours, remark)
                    VALUES
                        (v_emp, v_date, v_in, v_out, v_status, v_shift,
                         IF(v_in IS NULL, NULL, CONCAT('192.168.1.', (v_emp % 200) + 1)),
                         IF(v_out IS NULL, NULL, CONCAT('192.168.1.', (v_emp % 200) + 1)),
                         'WEB', 'WEB', v_late, v_early, v_hours, '演示数据');
                END IF;
            END IF;
        END LOOP;
        CLOSE cur;
        SET v_date = DATE_ADD(v_date, INTERVAL 1 DAY);
    END WHILE;
END$$
DELIMITER ;
CALL sp_seed_attendance();
DROP PROCEDURE IF EXISTS sp_seed_attendance;

-- ----------------------------
-- 5. 申请单演示数据（请假 / 加班 / 出差 / 补卡 / 异常申诉）
-- ----------------------------
INSERT IGNORE INTO leave_request (employee_id, type, type_code, start_date, end_date, days, reason, status, apply_time, approver_id, approve_time, approve_remark, current_step)
SELECT e.id, 'ANNUAL', 'ANNUAL', DATE_SUB(CURDATE(), INTERVAL 12 DAY), DATE_SUB(CURDATE(), INTERVAL 11 DAY), 2,
       '家中有事，申请年假两天', 'APPROVED', DATE_SUB(NOW(), INTERVAL 15 DAY), u.id, DATE_SUB(NOW(), INTERVAL 13 DAY), '同意，注意工作交接', 2
FROM employee e, sys_user u WHERE e.employee_no='EMP010' AND u.username='manager01';

INSERT IGNORE INTO leave_request (employee_id, type, type_code, start_date, end_date, days, reason, status, apply_time, approver_id, approve_time, approve_remark, current_step)
SELECT e.id, 'SICK', 'SICK', DATE_SUB(CURDATE(), INTERVAL 5 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY), 1,
       '感冒发热，就医休息一天', 'APPROVED', DATE_SUB(NOW(), INTERVAL 6 DAY), u.id, DATE_SUB(NOW(), INTERVAL 5 DAY), '准假，注意休息', 2
FROM employee e, sys_user u WHERE e.employee_no='EMP017' AND u.username='hr01';

INSERT IGNORE INTO leave_request (employee_id, type, type_code, start_date, end_date, days, reason, status, apply_time, current_step)
SELECT e.id, 'PERSONAL', 'PERSONAL', DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY), 2,
       '办理个人证件，需请假两天', 'PENDING', NOW(), 1
FROM employee e WHERE employee_no='EMP012';

INSERT IGNORE INTO leave_request (employee_id, type, type_code, start_date, end_date, days, reason, status, apply_time, current_step)
SELECT e.id, 'ANNUAL', 'ANNUAL', DATE_ADD(CURDATE(), INTERVAL 8 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), 3,
       '计划外出旅行，申请年假', 'PENDING', NOW(), 1
FROM employee e WHERE employee_no='EMP015';

INSERT IGNORE INTO leave_request (employee_id, type, type_code, start_date, end_date, days, reason, status, apply_time, approver_id, approve_time, approve_remark, current_step)
SELECT e.id, 'COMP', 'COMP', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY), 1,
       '上周加班调休一天', 'REJECTED', DATE_SUB(NOW(), INTERVAL 22 DAY), u.id, DATE_SUB(NOW(), INTERVAL 21 DAY), '当周排班紧张，建议另行安排', 2
FROM employee e, sys_user u WHERE e.employee_no='EMP019' AND u.username='manager01';

INSERT IGNORE INTO overtime_request (employee_id, ot_date, start_time, end_time, hours, ot_type, compensate_type, reason, status, approver_id, approve_time, approve_remark, apply_time)
SELECT e.id, DATE_SUB(CURDATE(), INTERVAL 7 DAY),
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 7 DAY), ' 18:30:00'),
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 7 DAY), ' 21:30:00'), 3.0,
       'WORKDAY', 'COMP', '版本上线前联调，需加班完成', 'APPROVED', u.id, DATE_SUB(NOW(), INTERVAL 6 DAY), '确认加班时长，计入调休', DATE_SUB(NOW(), INTERVAL 8 DAY)
FROM employee e, sys_user u WHERE e.employee_no='EMP010' AND u.username='manager01';

INSERT IGNORE INTO overtime_request (employee_id, ot_date, start_time, end_time, hours, ot_type, compensate_type, reason, status, apply_time)
SELECT id, DATE_SUB(CURDATE(), INTERVAL 2 DAY),
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 18:30:00'),
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 2 DAY), ' 20:30:00'), 2.0,
       'WORKDAY', 'FEE', '月末报表核对', 'PENDING', NOW()
FROM employee WHERE employee_no='EMP016';

INSERT IGNORE INTO overtime_request (employee_id, ot_date, start_time, end_time, hours, ot_type, compensate_type, reason, status, approver_id, approve_time, apply_time)
SELECT e.id, DATE_SUB(CURDATE(), INTERVAL 16 DAY),
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 16 DAY), ' 09:00:00'),
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 16 DAY), ' 17:00:00'), 8.0,
       'WEEKEND', 'COMP', '客服系统周末割接支持', 'APPROVED', u.id, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 18 DAY)
FROM employee e, sys_user u WHERE e.employee_no='EMP020' AND u.username='manager01';

INSERT IGNORE INTO business_trip (employee_id, destination, start_date, end_date, days, reason, status, approver_id, approve_time, approve_remark, apply_time)
SELECT e.id, '上海', DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_SUB(CURDATE(), INTERVAL 23 DAY), 3,
       '参加行业展会并拜访重点客户', 'APPROVED', u.id, DATE_SUB(NOW(), INTERVAL 26 DAY), '同意，出差费用按标准报销', DATE_SUB(NOW(), INTERVAL 28 DAY)
FROM employee e, sys_user u WHERE e.employee_no='EMP018' AND u.username='hr01';

INSERT IGNORE INTO business_trip (employee_id, destination, start_date, end_date, days, reason, status, apply_time)
SELECT id, '深圳', DATE_ADD(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 8 DAY), 3,
       '客户需求调研与现场答疑', 'PENDING', NOW()
FROM employee WHERE employee_no='EMP012';

INSERT IGNORE INTO makeup_request (employee_id, miss_date, punch_type, makeup_time, reason, status, approver_id, approve_time, approve_remark, apply_time)
SELECT e.id, DATE_SUB(CURDATE(), INTERVAL 9 DAY), 'OUT',
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 9 DAY), ' 18:05:00'),
       '下班时系统卡顿未打上卡', 'APPROVED', u.id, DATE_SUB(NOW(), INTERVAL 8 DAY), '核实监控，同意补卡', DATE_SUB(NOW(), INTERVAL 9 DAY)
FROM employee e, sys_user u WHERE e.employee_no='EMP013' AND u.username='manager01';

INSERT IGNORE INTO makeup_request (employee_id, miss_date, punch_type, makeup_time, reason, status, apply_time)
SELECT id, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'IN',
       CONCAT(DATE_SUB(CURDATE(), INTERVAL 1 DAY), ' 08:55:00'),
       '外出拜访客户，返回后忘记打卡', 'PENDING', NOW()
FROM employee WHERE employee_no='EMP019';

INSERT IGNORE INTO attendance_appeal (employee_id, record_id, appeal_date, exception_type, reason, status, reply, approver_id, apply_time)
SELECT r.employee_id, r.id, r.attendance_date, 'LATE',
       '当日地铁故障导致迟到，附车站延误证明', 'PENDING', NULL, NULL, NOW()
FROM attendance_record r
WHERE r.status = 'LATE' AND r.attendance_date >= DATE_SUB(CURDATE(), INTERVAL 20 DAY)
ORDER BY r.id LIMIT 2;

INSERT IGNORE INTO attendance_appeal (employee_id, record_id, appeal_date, exception_type, reason, status, reply, approver_id, apply_time)
SELECT r.employee_id, r.id, r.attendance_date, 'MISSING_OUT',
       '下班打卡设备离线，已提交补卡说明', 'APPROVED', '情况属实，已修正考勤结果',
       (SELECT id FROM sys_user WHERE username='hr01'), NOW()
FROM attendance_record r
WHERE r.status = 'MISSING_OUT' AND r.attendance_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
ORDER BY r.id LIMIT 1;

-- 审批流水：为已审批的单据补齐 SUBMIT + APPROVE 两步留痕
INSERT IGNORE INTO approval_record (biz_type, biz_id, step_no, approver_id, approver_name, action, remark, approve_time)
SELECT 'LEAVE', lr.id, 1, NULL, (SELECT name FROM employee WHERE id = lr.employee_id), 'SUBMIT', '员工提交请假申请', lr.apply_time
FROM leave_request lr WHERE lr.status <> 'PENDING';

INSERT IGNORE INTO approval_record (biz_type, biz_id, step_no, approver_id, approver_name, action, remark, approve_time)
SELECT 'LEAVE', lr.id, 2, lr.approver_id, (SELECT u.real_name FROM sys_user u WHERE u.id = lr.approver_id),
       IF(lr.status = 'APPROVED', 'APPROVE', 'REJECT'), lr.approve_remark, lr.approve_time
FROM leave_request lr WHERE lr.status <> 'PENDING' AND lr.approver_id IS NOT NULL;

INSERT IGNORE INTO approval_record (biz_type, biz_id, step_no, approver_id, approver_name, action, remark, approve_time)
SELECT 'OVERTIME', o.id, 2, o.approver_id, (SELECT u.real_name FROM sys_user u WHERE u.id = o.approver_id),
       'APPROVE', o.approve_remark, o.approve_time
FROM overtime_request o WHERE o.status = 'APPROVED';

INSERT IGNORE INTO approval_record (biz_type, biz_id, step_no, approver_id, approver_name, action, remark, approve_time)
SELECT 'TRIP', t.id, 2, t.approver_id, (SELECT u.real_name FROM sys_user u WHERE u.id = t.approver_id),
       'APPROVE', t.approve_remark, t.approve_time
FROM business_trip t WHERE t.status = 'APPROVED';

INSERT IGNORE INTO approval_record (biz_type, biz_id, step_no, approver_id, approver_name, action, remark, approve_time)
SELECT 'MAKEUP', m.id, 2, m.approver_id, (SELECT u.real_name FROM sys_user u WHERE u.id = m.approver_id),
       'APPROVE', m.approve_remark, m.approve_time
FROM makeup_request m WHERE m.status = 'APPROVED';

-- ----------------------------
-- 6. 月度考勤汇总（由真实流水聚合，演示日结算/关账结果）
-- ----------------------------
INSERT INTO attendance_summary
    (employee_id, ym, expected_days, actual_days, late_cnt, early_cnt, absent_cnt,
     leave_days, overtime_hours, makeup_cnt, work_hours, late_minutes, generate_time)
SELECT r.employee_id,
       DATE_FORMAT(r.attendance_date, '%Y-%m'),
       COUNT(*),
       SUM(CASE WHEN r.status NOT IN ('ABSENT','LEAVE') THEN 1 ELSE 0 END),
       SUM(CASE WHEN r.status IN ('LATE','LATE_AND_EARLY') THEN 1 ELSE 0 END),
       SUM(CASE WHEN r.status IN ('EARLY_LEAVE','LATE_AND_EARLY') THEN 1 ELSE 0 END),
       SUM(CASE WHEN r.status = 'ABSENT' THEN 1 ELSE 0 END),
       SUM(CASE WHEN r.status = 'LEAVE' THEN 1 ELSE 0 END),
       0, 0,
       SUM(COALESCE(r.work_hours, 0)),
       SUM(COALESCE(r.late_minutes, 0)),
       NOW()
FROM attendance_record r
GROUP BY r.employee_id, DATE_FORMAT(r.attendance_date, '%Y-%m')
ON DUPLICATE KEY UPDATE
    expected_days = VALUES(expected_days), actual_days = VALUES(actual_days),
    late_cnt = VALUES(late_cnt), early_cnt = VALUES(early_cnt), absent_cnt = VALUES(absent_cnt),
    leave_days = VALUES(leave_days), work_hours = VALUES(work_hours),
    late_minutes = VALUES(late_minutes), generate_time = NOW();

UPDATE attendance_summary s
SET overtime_hours = COALESCE((
    SELECT SUM(o.hours) FROM overtime_request o
    WHERE o.employee_id = s.employee_id AND o.status = 'APPROVED'
      AND DATE_FORMAT(o.ot_date, '%Y-%m') = s.ym), 0),
    makeup_cnt = COALESCE((
    SELECT COUNT(*) FROM makeup_request m
    WHERE m.employee_id = s.employee_id AND m.status = 'APPROVED'
      AND DATE_FORMAT(m.miss_date, '%Y-%m') = s.ym), 0);

-- 已审批通过的年假/调休/病假计入已用额度
UPDATE leave_balance b
SET used_days = COALESCE((
    SELECT SUM(lr.days) FROM leave_request lr
    WHERE lr.employee_id = b.employee_id AND lr.status = 'APPROVED'
      AND lr.type_code = b.type_code AND YEAR(lr.start_date) = b.year_num), 0),
    remain_days = total_days - COALESCE((
    SELECT SUM(lr.days) FROM leave_request lr
    WHERE lr.employee_id = b.employee_id AND lr.status = 'APPROVED'
      AND lr.type_code = b.type_code AND YEAR(lr.start_date) = b.year_num), 0)
WHERE b.type_code IN ('ANNUAL','COMP','MARRIAGE');

-- 审批通过的加班（转调休）增加调休额度
UPDATE leave_balance b
SET total_days = COALESCE((
    SELECT SUM(o.hours / 8.0) FROM overtime_request o
    WHERE o.employee_id = b.employee_id AND o.status = 'APPROVED'
      AND o.compensate_type = 'COMP' AND YEAR(o.ot_date) = b.year_num), 0),
    remain_days = COALESCE((
    SELECT SUM(o.hours / 8.0) FROM overtime_request o
    WHERE o.employee_id = b.employee_id AND o.status = 'APPROVED'
      AND o.compensate_type = 'COMP' AND YEAR(o.ot_date) = b.year_num), 0) - b.used_days
WHERE b.type_code = 'COMP';

SELECT 'V2 演示数据初始化完成' AS result,
       (SELECT COUNT(*) FROM employee) AS employees,
       (SELECT COUNT(*) FROM sys_user) AS users,
       (SELECT COUNT(*) FROM attendance_record) AS records,
       (SELECT COUNT(*) FROM schedule) AS schedules,
       (SELECT COUNT(*) FROM attendance_summary) AS summaries;
