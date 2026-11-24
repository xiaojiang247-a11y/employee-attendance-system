-- ============================================================
-- 清空预置演示业务数据
-- 说明：系统不预置任何员工与业务流水，员工档案、账号、考勤数据
--       全部由管理员在后台自行录入。本脚本只清除演示数据，
--       保留表结构、系统字典（班次/考勤组/假别/工作日历/角色菜单）
--       以及库中原有的真实数据。
-- 幂等：可重复执行
-- ============================================================
SET NAMES utf8mb4;

-- 1. 申请单与审批流水（全部为演示数据）
DELETE FROM approval_record;
DELETE FROM attendance_appeal;
DELETE FROM makeup_request;
DELETE FROM business_trip;
DELETE FROM overtime_request;
DELETE FROM leave_request;

-- 2. 考勤核算与排班（保留 employee_id = 21 且 remark 为空的原有真实记录）
DELETE FROM attendance_record WHERE employee_id <> 21 OR remark = '演示数据';
DELETE FROM schedule;
DELETE FROM attendance_summary;
DELETE FROM leave_balance;

-- 3. 演示公告
DELETE FROM sys_notice;

-- 4. 演示账号及其角色关联（保留 admin / lisi1 / zhangsan 三个原有账号）
DELETE FROM sys_user_role WHERE user_id IN (
    SELECT id FROM sys_user WHERE username IN (
        'hr01','manager01','zhangwei','wangqiang','liuyang','zhaomin',
        'sunhao','zhoulin','wulei','zhengshuang','fengchao','jiangwen'
    )
);
DELETE FROM sys_user WHERE username IN (
    'hr01','manager01','zhangwei','wangqiang','liuyang','zhaomin',
    'sunhao','zhoulin','wulei','zhengshuang','fengchao','jiangwen'
);

-- 5. 演示员工（保留 id = 21 的原有员工，并还原预置时改动的字段）
DELETE FROM employee WHERE id <> 21;
UPDATE employee SET
    employee_no = '2431232234',
    department_id = 1,
    gender = NULL,
    phone = NULL,
    email = NULL,
    position = NULL,
    hire_date = NULL,
    group_id = NULL,
    shift_id = NULL,
    employment_type = 'REGULAR',
    birth_date = NULL,
    id_card = NULL,
    education = NULL,
    address = NULL,
    emergency_contact = NULL,
    emergency_phone = NULL,
    leave_date = NULL,
    remark = NULL
WHERE id = 21;

-- 6. 还原部门负责人与预置的子部门
UPDATE department SET leader_id = NULL;
DELETE FROM department WHERE id IN (5, 6);

-- 7. 还原原有账号的部门字段
UPDATE sys_user SET dept_id = NULL WHERE username = 'superadmin';

SELECT '演示数据已清空' AS result,
       (SELECT COUNT(*) FROM employee) AS employees,
       (SELECT COUNT(*) FROM sys_user) AS users,
       (SELECT COUNT(*) FROM attendance_record) AS records,
       (SELECT COUNT(*) FROM shift) AS shifts,
       (SELECT COUNT(*) FROM leave_type) AS leave_types,
       (SELECT COUNT(*) FROM sys_menu) AS menus;
