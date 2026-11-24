-- ============================================================
-- 企业员工考勤管理系统 V2 结构升级脚本
-- 参考模型：钉钉考勤组/班次、劳勤工时制度、盖雅实时考勤核算、
--           北森假期额度、多级审批与部门数据权限
-- 特性：幂等，可重复执行；不删除任何已有业务数据
-- 执行：mysql --default-character-set=utf8mb4 -uroot -p attendance_db < schema_v2.sql
-- ============================================================
SET NAMES utf8mb4;

-- ---------- 通用补列存储过程（MySQL 8.x 不支持 ADD COLUMN IF NOT EXISTS） ----------
DROP PROCEDURE IF EXISTS sp_ensure_col;
DELIMITER $$
CREATE PROCEDURE sp_ensure_col(IN p_table VARCHAR(64), IN p_col VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col) THEN
        SET @ddl_stmt = p_ddl;
        PREPARE st FROM @ddl_stmt;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 一、RBAC 权限域
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE COMMENT '角色编码',
    name        VARCHAR(50)  NOT NULL COMMENT '角色名称',
    description VARCHAR(200) COMMENT '角色说明',
    data_scope  VARCHAR(20)  NOT NULL DEFAULT 'SELF' COMMENT '数据范围: ALL全公司/DEPT本部门/SELF仅本人',
    enabled     TINYINT(1)   NOT NULL DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID,0为一级',
    name        VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    icon        VARCHAR(50)  COMMENT '图标标识',
    url         VARCHAR(120) COMMENT '路由地址',
    perm_code   VARCHAR(80)  COMMENT '权限标识',
    menu_type   VARCHAR(20)  NOT NULL DEFAULT 'MENU' COMMENT 'DIR目录/MENU菜单',
    sort_no     INT          NOT NULL DEFAULT 0,
    visible     TINYINT(1)   NOT NULL DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单与权限表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ============================================================
-- 二、考勤规则域（班次 / 考勤组 / 排班 / 工作日历）
-- ============================================================
CREATE TABLE IF NOT EXISTS shift (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    code         VARCHAR(30)  NOT NULL UNIQUE COMMENT '班次编码',
    name         VARCHAR(50)  NOT NULL COMMENT '班次名称',
    start_time   TIME         NOT NULL COMMENT '上班时间',
    end_time     TIME         NOT NULL COMMENT '下班时间',
    late_grace   INT          NOT NULL DEFAULT 0 COMMENT '迟到宽限(分钟)',
    early_grace  INT          NOT NULL DEFAULT 0 COMMENT '早退宽限(分钟)',
    flex_minutes INT          NOT NULL DEFAULT 0 COMMENT '弹性打卡(分钟),0为固定班',
    work_hours   DECIMAL(4,1) NOT NULL DEFAULT 8.0 COMMENT '标准工时(小时)',
    cross_day    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否跨天班',
    color        VARCHAR(20)  NOT NULL DEFAULT '#4f6bed' COMMENT '日历显示色',
    remark       VARCHAR(200),
    enabled      TINYINT(1)   NOT NULL DEFAULT 1,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班次表';

CREATE TABLE IF NOT EXISTS attendance_group (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(50)  NOT NULL COMMENT '考勤组名称',
    group_type       VARCHAR(20)  NOT NULL DEFAULT 'FIXED' COMMENT 'FIXED固定班制/SCHEDULE排班制/FLEX自由工时',
    department_id    BIGINT       COMMENT '适用部门',
    default_shift_id BIGINT       COMMENT '默认班次',
    workdays         VARCHAR(20)  NOT NULL DEFAULT '2,3,4,5,6' COMMENT '工作日(1=周日..7=周六,MySQL DAYOFWEEK)',
    allow_ip         VARCHAR(100) COMMENT '允许打卡IP段,逗号分隔',
    wifi_ssid        VARCHAR(100) COMMENT '允许WiFi SSID',
    punch_ahead_min  INT          NOT NULL DEFAULT 120 COMMENT '最早可提前打卡分钟',
    remark           VARCHAR(200),
    enabled          TINYINT(1)   NOT NULL DEFAULT 1,
    create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组表';

CREATE TABLE IF NOT EXISTS schedule (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT      NOT NULL COMMENT '员工ID',
    work_date   DATE        NOT NULL COMMENT '排班日期',
    shift_id    BIGINT      COMMENT '班次ID,NULL且is_rest=1为休息',
    is_rest     TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '是否休息日',
    source      VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL手工/AUTO周期生成/IMPORT导入',
    remark      VARCHAR(200),
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_schedule_emp_date (employee_id, work_date),
    KEY idx_schedule_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工排班表';

CREATE TABLE IF NOT EXISTS holiday_calendar (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    cal_date DATE        NOT NULL UNIQUE COMMENT '日历日期',
    day_type VARCHAR(20) NOT NULL COMMENT 'WORKDAY调休上班/REST休息日/HOLIDAY法定节假日',
    name     VARCHAR(50) COMMENT '节日或调整说明',
    remark   VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作日历表';

-- ============================================================
-- 三、假期域
-- ============================================================
CREATE TABLE IF NOT EXISTS leave_type (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(30)  NOT NULL UNIQUE COMMENT '假别编码',
    name          VARCHAR(50)  NOT NULL COMMENT '假别名称',
    annual_quota  DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '年度额度(天),0表示不限额',
    paid          TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否带薪',
    need_approval TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否需要审批',
    carry_forward TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否可结转次年',
    deduct_rule   VARCHAR(20)  NOT NULL DEFAULT 'WORKDAY' COMMENT 'WORKDAY仅扣工作日/CALENDAR按自然日',
    color         VARCHAR(20)  NOT NULL DEFAULT '#4f6bed',
    sort_no       INT          NOT NULL DEFAULT 0,
    enabled       TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期类型表';

CREATE TABLE IF NOT EXISTS leave_balance (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT       NOT NULL,
    year_num    INT          NOT NULL COMMENT '额度年度',
    type_code   VARCHAR(30)  NOT NULL COMMENT '假别编码',
    total_days  DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '总额度',
    used_days   DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '已使用',
    remain_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '剩余',
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_balance (employee_id, year_num, type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='假期余额表';

-- ============================================================
-- 四、申请单域（加班 / 出差 / 补卡 / 异常申诉）
-- ============================================================
CREATE TABLE IF NOT EXISTS overtime_request (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id     BIGINT       NOT NULL,
    ot_date         DATE         NOT NULL COMMENT '加班日期',
    start_time      DATETIME     COMMENT '开始时间',
    end_time        DATETIME     COMMENT '结束时间',
    hours           DECIMAL(5,1) NOT NULL COMMENT '加班时长(小时)',
    ot_type         VARCHAR(20)  NOT NULL DEFAULT 'WORKDAY' COMMENT 'WORKDAY工作日/WEEKEND周末/HOLIDAY节假日',
    compensate_type VARCHAR(20)  NOT NULL DEFAULT 'COMP' COMMENT 'COMP转调休/FEE加班费',
    reason          VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    current_step    INT          NOT NULL DEFAULT 1,
    approver_id     BIGINT,
    approve_time    DATETIME,
    approve_remark  VARCHAR(300),
    apply_time      DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班申请表';

CREATE TABLE IF NOT EXISTS business_trip (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id    BIGINT       NOT NULL,
    destination    VARCHAR(100) NOT NULL COMMENT '出差地点',
    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    days           DECIMAL(4,1) NOT NULL COMMENT '出差天数',
    reason         VARCHAR(500),
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    approver_id    BIGINT,
    approve_time   DATETIME,
    approve_remark VARCHAR(300),
    apply_time     DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出差申请表';

CREATE TABLE IF NOT EXISTS makeup_request (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id    BIGINT      NOT NULL,
    miss_date      DATE        NOT NULL COMMENT '漏打卡日期',
    punch_type     VARCHAR(10) NOT NULL COMMENT 'IN上班卡/OUT下班卡',
    makeup_time    DATETIME    NOT NULL COMMENT '补卡时间',
    reason         VARCHAR(500),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    approver_id    BIGINT,
    approve_time   DATETIME,
    approve_remark VARCHAR(300),
    apply_time     DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补卡申请表';

CREATE TABLE IF NOT EXISTS attendance_appeal (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id    BIGINT      NOT NULL,
    record_id      BIGINT      COMMENT '关联考勤记录',
    appeal_date    DATE        NOT NULL,
    exception_type VARCHAR(30) NOT NULL COMMENT 'LATE/EARLY_LEAVE/ABSENT/MISSING_OUT',
    reason         VARCHAR(500),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reply          VARCHAR(300),
    approver_id    BIGINT,
    apply_time     DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤异常申诉表';

-- 统一审批流水：所有单据的每一次流转都留痕，支撑审计与"已办"查询
CREATE TABLE IF NOT EXISTS approval_record (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type      VARCHAR(30) NOT NULL COMMENT 'LEAVE/OVERTIME/TRIP/MAKEUP/APPEAL',
    biz_id        BIGINT      NOT NULL,
    step_no       INT         NOT NULL DEFAULT 1,
    approver_id   BIGINT,
    approver_name VARCHAR(50),
    action        VARCHAR(20) NOT NULL COMMENT 'SUBMIT/APPROVE/REJECT/CANCEL/TRANSFER',
    remark        VARCHAR(300),
    approve_time  DATETIME    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批流水表';

-- ============================================================
-- 五、核算与报表域
-- ============================================================
CREATE TABLE IF NOT EXISTS attendance_summary (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id    BIGINT       NOT NULL,
    ym             VARCHAR(7)   NOT NULL COMMENT '统计月份 yyyy-MM',
    expected_days  DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '应出勤天数',
    actual_days    DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '实际出勤天数',
    late_cnt       INT          NOT NULL DEFAULT 0,
    early_cnt      INT          NOT NULL DEFAULT 0,
    absent_cnt     INT          NOT NULL DEFAULT 0,
    leave_days     DECIMAL(5,1) NOT NULL DEFAULT 0,
    overtime_hours DECIMAL(6,1) NOT NULL DEFAULT 0,
    makeup_cnt     INT          NOT NULL DEFAULT 0,
    work_hours     DECIMAL(7,1) NOT NULL DEFAULT 0,
    late_minutes   INT          NOT NULL DEFAULT 0,
    locked         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '月度关账锁定',
    generate_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_summary (employee_id, ym)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度考勤汇总表';

-- ============================================================
-- 六、系统域（公告 / 操作审计）
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_notice (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(150) NOT NULL,
    content      TEXT,
    notice_type  VARCHAR(20)  NOT NULL DEFAULT 'NOTICE' COMMENT 'NOTICE通知/POLICY制度/URGENT紧急',
    publisher    VARCHAR(50),
    publish_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    pinned       TINYINT(1)   NOT NULL DEFAULT 0,
    view_count   INT          NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告通知表';

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    module         VARCHAR(50)  COMMENT '业务模块',
    operation      VARCHAR(100) COMMENT '操作名称',
    request_method VARCHAR(10),
    request_url    VARCHAR(200),
    ip             VARCHAR(50),
    username       VARCHAR(50),
    params         TEXT,
    result         VARCHAR(20) COMMENT 'SUCCESS/ERROR',
    cost_ms        BIGINT,
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_oper_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================================
-- 七、扩展原有 6 张表（幂等补列）
-- ============================================================
CALL sp_ensure_col('sys_user', 'dept_id',          'ALTER TABLE sys_user ADD COLUMN dept_id BIGINT NULL COMMENT ''所属部门'' AFTER employee_id');
CALL sp_ensure_col('sys_user', 'phone',            'ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) NULL COMMENT ''手机号''');
CALL sp_ensure_col('sys_user', 'email',            'ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) NULL COMMENT ''邮箱''');
CALL sp_ensure_col('sys_user', 'avatar',           'ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(200) NULL COMMENT ''头像''');
CALL sp_ensure_col('sys_user', 'last_login_time',  'ALTER TABLE sys_user ADD COLUMN last_login_time DATETIME NULL COMMENT ''最后登录时间''');
CALL sp_ensure_col('sys_user', 'last_login_ip',    'ALTER TABLE sys_user ADD COLUMN last_login_ip VARCHAR(50) NULL COMMENT ''最后登录IP''');

CALL sp_ensure_col('department', 'code',           'ALTER TABLE department ADD COLUMN code VARCHAR(30) NULL COMMENT ''部门编码''');
CALL sp_ensure_col('department', 'parent_id',      'ALTER TABLE department ADD COLUMN parent_id BIGINT NOT NULL DEFAULT 0 COMMENT ''上级部门''');
CALL sp_ensure_col('department', 'leader_id',      'ALTER TABLE department ADD COLUMN leader_id BIGINT NULL COMMENT ''部门负责人''');
CALL sp_ensure_col('department', 'sort_no',        'ALTER TABLE department ADD COLUMN sort_no INT NOT NULL DEFAULT 0');

CALL sp_ensure_col('employee', 'group_id',         'ALTER TABLE employee ADD COLUMN group_id BIGINT NULL COMMENT ''考勤组ID'' AFTER department_id');
CALL sp_ensure_col('employee', 'shift_id',         'ALTER TABLE employee ADD COLUMN shift_id BIGINT NULL COMMENT ''默认班次ID'' AFTER group_id');
CALL sp_ensure_col('employee', 'employment_type',  'ALTER TABLE employee ADD COLUMN employment_type VARCHAR(20) NOT NULL DEFAULT ''REGULAR'' COMMENT ''PROBATION试用/REGULAR正式/INTERN实习''');
CALL sp_ensure_col('employee', 'birth_date',       'ALTER TABLE employee ADD COLUMN birth_date DATE NULL COMMENT ''出生日期''');
CALL sp_ensure_col('employee', 'id_card',          'ALTER TABLE employee ADD COLUMN id_card VARCHAR(30) NULL COMMENT ''身份证号''');
CALL sp_ensure_col('employee', 'education',        'ALTER TABLE employee ADD COLUMN education VARCHAR(30) NULL COMMENT ''学历''');
CALL sp_ensure_col('employee', 'address',          'ALTER TABLE employee ADD COLUMN address VARCHAR(200) NULL COMMENT ''住址''');
CALL sp_ensure_col('employee', 'emergency_contact','ALTER TABLE employee ADD COLUMN emergency_contact VARCHAR(50) NULL COMMENT ''紧急联系人''');
CALL sp_ensure_col('employee', 'emergency_phone',  'ALTER TABLE employee ADD COLUMN emergency_phone VARCHAR(20) NULL COMMENT ''紧急联系电话''');
CALL sp_ensure_col('employee', 'leave_date',       'ALTER TABLE employee ADD COLUMN leave_date DATE NULL COMMENT ''离职日期''');
CALL sp_ensure_col('employee', 'remark',           'ALTER TABLE employee ADD COLUMN remark VARCHAR(500) NULL COMMENT ''备注''');

CALL sp_ensure_col('attendance_record', 'shift_id',        'ALTER TABLE attendance_record ADD COLUMN shift_id BIGINT NULL COMMENT ''当日班次''');
CALL sp_ensure_col('attendance_record', 'check_in_ip',     'ALTER TABLE attendance_record ADD COLUMN check_in_ip VARCHAR(50) NULL COMMENT ''签到IP''');
CALL sp_ensure_col('attendance_record', 'check_out_ip',    'ALTER TABLE attendance_record ADD COLUMN check_out_ip VARCHAR(50) NULL COMMENT ''签退IP''');
CALL sp_ensure_col('attendance_record', 'check_in_device', 'ALTER TABLE attendance_record ADD COLUMN check_in_device VARCHAR(50) NULL COMMENT ''打卡终端''');
CALL sp_ensure_col('attendance_record', 'source',          'ALTER TABLE attendance_record ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT ''WEB'' COMMENT ''WEB/APP/DEVICE/MAKEUP''');
CALL sp_ensure_col('attendance_record', 'late_minutes',    'ALTER TABLE attendance_record ADD COLUMN late_minutes INT NOT NULL DEFAULT 0 COMMENT ''迟到分钟''');
CALL sp_ensure_col('attendance_record', 'early_minutes',   'ALTER TABLE attendance_record ADD COLUMN early_minutes INT NOT NULL DEFAULT 0 COMMENT ''早退分钟''');
CALL sp_ensure_col('attendance_record', 'work_hours',      'ALTER TABLE attendance_record ADD COLUMN work_hours DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT ''实际工时''');
CALL sp_ensure_col('attendance_record', 'is_makeup',       'ALTER TABLE attendance_record ADD COLUMN is_makeup TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否补卡生成''');
CALL sp_ensure_col('attendance_record', 'remark',          'ALTER TABLE attendance_record ADD COLUMN remark VARCHAR(300) NULL');

CALL sp_ensure_col('leave_request', 'days',           'ALTER TABLE leave_request ADD COLUMN days DECIMAL(4,1) NOT NULL DEFAULT 1 COMMENT ''请假天数''');
CALL sp_ensure_col('leave_request', 'type_code',      'ALTER TABLE leave_request ADD COLUMN type_code VARCHAR(30) NULL COMMENT ''假别编码''');
CALL sp_ensure_col('leave_request', 'approver_id',    'ALTER TABLE leave_request ADD COLUMN approver_id BIGINT NULL');
CALL sp_ensure_col('leave_request', 'approve_time',   'ALTER TABLE leave_request ADD COLUMN approve_time DATETIME NULL');
CALL sp_ensure_col('leave_request', 'approve_remark', 'ALTER TABLE leave_request ADD COLUMN approve_remark VARCHAR(300) NULL');
CALL sp_ensure_col('leave_request', 'current_step',   'ALTER TABLE leave_request ADD COLUMN current_step INT NOT NULL DEFAULT 1');

CALL sp_ensure_col('login_log', 'user_agent', 'ALTER TABLE login_log ADD COLUMN user_agent VARCHAR(200) NULL COMMENT ''客户端UA''');

DROP PROCEDURE IF EXISTS sp_ensure_col;

-- ============================================================
-- 八、基础数据初始化（幂等）
-- ============================================================
INSERT IGNORE INTO sys_role (id, code, name, description, data_scope) VALUES
(1, 'ADMIN',    '系统管理员', '全局配置、全部数据与系统管理', 'ALL'),
(2, 'HR',       '人事考勤员', '组织人员、考勤规则、核算与报表', 'ALL'),
(3, 'MANAGER',  '部门负责人', '本部门考勤查看与审批', 'DEPT'),
(4, 'EMPLOYEE', '普通员工',   '仅本人考勤与申请', 'SELF');

INSERT IGNORE INTO department (id, code, name, description, parent_id, sort_no) VALUES
(1, 'D001', '技术部', '负责系统开发与技术维护', 0, 1),
(2, 'D002', '人事部', '负责员工招聘与人事管理', 0, 2),
(3, 'D003', '财务部', '负责公司财务管理', 0, 3),
(4, 'D004', '市场部', '负责市场推广与客户拓展', 0, 4);

INSERT IGNORE INTO shift (id, code, name, start_time, end_time, late_grace, early_grace, flex_minutes, work_hours, cross_day, color, remark) VALUES
(1, 'S-ADMIN',  '行政班(弹性30分钟)', '09:00:00', '18:00:00', 10, 10, 30, 8.0, 0, '#4f6bed', '默认班次，含1小时午休'),
(2, 'S-MORNING','早班',              '08:00:00', '16:00:00',  5,  5,  0, 8.0, 0, '#12b886', '生产与客服早班'),
(3, 'S-EVENING','晚班',              '14:00:00', '22:00:00',  5,  5,  0, 8.0, 0, '#f59f00', '生产与客服晚班'),
(4, 'S-NIGHT',  '夜班(跨天)',        '22:00:00', '06:00:00',  5,  5,  0, 8.0, 1, '#845ef7', '跨天班次'),
(5, 'S-FLEX',   '自由工时',          '09:00:00', '18:00:00',  0,  0,480, 8.0, 0, '#e8590c', '不判定迟到早退，仅统计工时');

INSERT IGNORE INTO attendance_group (id, name, group_type, department_id, default_shift_id, workdays, allow_ip, wifi_ssid, remark) VALUES
(1, '总部职能考勤组', 'FIXED',    NULL, 1, '2,3,4,5,6', '127.0.0.1,192.168.1.*', 'Corp-Office', '固定班制，周一至周五 09:00-18:00'),
(2, '研发弹性考勤组', 'FLEX',     1,    1, '2,3,4,5,6', '127.0.0.1,10.0.*',     'Corp-RD',     '弹性工时，按日统计实际工时'),
(3, '客服排班考勤组', 'SCHEDULE', 4,    2, '2,3,4,5,6,7', '127.0.0.1',        'Corp-CS',     '排班制，含周末轮班');

INSERT IGNORE INTO leave_type (id, code, name, annual_quota, paid, need_approval, carry_forward, deduct_rule, color, sort_no) VALUES
(1,'PERSONAL',  '事假',   0,   0, 1, 0, 'WORKDAY',   '#868e96', 1),
(2,'SICK',      '病假',   0,   1, 1, 0, 'WORKDAY',   '#fa5252', 2),
(3,'ANNUAL',    '年假',   10,  1, 1, 1, 'WORKDAY',   '#4f6bed', 3),
(4,'COMP',      '调休',   0,   1, 1, 0, 'WORKDAY',   '#12b886', 4),
(5,'MARRIAGE',  '婚假',   10,  1, 1, 0, 'CALENDAR',  '#f06595', 5),
(6,'MATERNITY', '产假',   98,  1, 1, 0, 'CALENDAR',  '#7950f2', 6),
(7,'BEREAVE',   '丧假',   3,   1, 1, 0, 'CALENDAR',  '#adb5bd', 7);

-- 菜单：DIR 为分组，MENU 为可访问页面；图标名对应 app.css 里的 icon-* 字形
INSERT IGNORE INTO sys_menu (id, parent_id, name, icon, url, perm_code, menu_type, sort_no) VALUES
(1,  0, '工作台',     'grid',     '',                              'dashboard',       'DIR', 1),
(2,  0, '组织与人员', 'users',    '',                              'org',             'DIR', 2),
(3,  0, '考勤管理',   'calendar', '',                              'att',             'DIR', 3),
(4,  0, '审批中心',   'check',    '',                              'approval',        'DIR', 4),
(5,  0, '统计报表',   'chart',    '',                              'report',          'DIR', 5),
(6,  0, '系统管理',   'settings', '',                              'system',          'DIR', 6),
(7,  0, '员工自助',   'user',     '',                              'self',            'DIR', 7),

(11, 1, '管理驾驶舱', 'grid',       '/dashboard',                  'dashboard:admin',   'MENU', 1),
(12, 1, '我的工作台', 'home',       '/my/home',                    'dashboard:self',    'MENU', 2),

(21, 2, '部门架构',   'tree',       '/admin/org/departments',      'org:dept',          'MENU', 1),
(22, 2, '员工档案',   'users',      '/admin/employees',            'org:employee',      'MENU', 2),
(23, 2, '考勤组',     'layers',     '/admin/groups',               'org:group',         'MENU', 3),

(31, 3, '班次管理',   'clock',      '/admin/shifts',               'att:shift',         'MENU', 1),
(32, 3, '排班日历',   'calendar',   '/admin/schedule',             'att:schedule',      'MENU', 2),
(33, 3, '工作日历',   'calendar',   '/admin/holidays',             'att:holiday',       'MENU', 3),
(34, 3, '打卡流水',   'list',       '/admin/attendance/records',   'att:record',        'MENU', 4),
(35, 3, '异常考勤',   'alert',      '/admin/attendance/exceptions','att:exception',     'MENU', 5),
(36, 3, '月度汇总',   'table',      '/admin/attendance/summary',   'att:summary',       'MENU', 6),
(37, 3, '假别与额度', 'sliders',    '/admin/leave-types',          'att:leavetype',     'MENU', 7),
(38, 3, '假期余额',   'pie',        '/admin/leave-balances',       'att:balance',       'MENU', 8),
(39, 3, '打卡地点',   'pin',        '/admin/locations',            'att:location',      'MENU', 9),

(41, 4, '待办审批',   'inbox',      '/admin/approval/pending',     'approval:pending',  'MENU', 1),
(42, 4, '已办审批',   'history',    '/admin/approval/done',        'approval:done',     'MENU', 2),
(43, 4, '加班记录',   'plus',       '/admin/overtime',             'approval:overtime', 'MENU', 3),
(44, 4, '出差记录',   'plane',      '/admin/trips',                'approval:trip',     'MENU', 4),
(45, 4, '补卡记录',   'edit',       '/admin/makeups',              'approval:makeup',   'MENU', 5),

(51, 5, '考勤报表',   'chart',      '/admin/report',               'report:view',       'MENU', 1),
(52, 5, '数据导出',   'download',   '/admin/report/export',        'report:export',     'MENU', 2),

(61, 6, '用户账号',   'key',        '/admin/system/users',         'system:user',       'MENU', 1),
(62, 6, '角色权限',   'shield',     '/admin/system/roles',         'system:role',       'MENU', 2),
(63, 6, '公告通知',   'bell',       '/admin/system/notices',       'system:notice',     'MENU', 3),
(64, 6, '操作日志',   'file',       '/admin/system/oper-logs',     'system:log',        'MENU', 4),
(65, 6, '登录日志',   'activity',   '/admin/login-logs',           'system:loginlog',   'MENU', 5),

(71, 7, '我的考勤',   'list',       '/my/attendance',              'self:attendance',   'MENU', 1),
(72, 7, '我的班表',   'calendar',   '/my/schedule',                'self:schedule',     'MENU', 2),
(73, 7, '我的请假',   'file-text',  '/my/leave',                   'self:leave',        'MENU', 3),
(74, 7, '我的加班',   'plus',       '/my/overtime',                'self:overtime',     'MENU', 4),
(75, 7, '我的出差',   'plane',      '/my/trip',                    'self:trip',         'MENU', 5),
(76, 7, '我的补卡',   'edit',       '/my/makeup',                  'self:makeup',       'MENU', 6),
(77, 7, '假期余额',   'pie',        '/my/balance',                 'self:balance',      'MENU', 7),
(78, 7, '个人中心',   'user',       '/my/profile',                 'self:profile',      'MENU', 8);

-- 角色菜单授权
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 2, id FROM sys_menu WHERE perm_code <> 'system:role';
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
  SELECT 3, id FROM sys_menu
  WHERE perm_code IN ('dashboard','org','att','approval','report','dashboard:admin','org:dept','org:employee',
                      'att:schedule','att:record','att:exception','att:summary',
                      'approval:pending','approval:done','approval:overtime','approval:trip','approval:makeup',
                      'report:view','report:export','self','self:attendance','self:schedule','self:leave',
                      'self:overtime','self:trip','self:makeup','self:balance','self:profile');
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
  SELECT 4, id FROM sys_menu WHERE perm_code LIKE 'dashboard:self' OR perm_code = 'self' OR perm_code LIKE 'self:%';

-- 工作日历：当前年度的周末与主要法定节假日（幂等）
INSERT IGNORE INTO holiday_calendar (cal_date, day_type, name, remark)
WITH RECURSIVE d AS (
    SELECT DATE_FORMAT(CURDATE(), '%Y-01-01') AS dt
    UNION ALL SELECT DATE_ADD(dt, INTERVAL 1 DAY) FROM d WHERE dt < DATE_FORMAT(CURDATE(), '%Y-12-31')
)
SELECT dt, 'REST', '周末休息', '系统按周末自动生成' FROM d WHERE DAYOFWEEK(dt) IN (1,7);

INSERT IGNORE INTO holiday_calendar (cal_date, day_type, name) VALUES
(DATE_FORMAT(CURDATE(),'%Y-01-01'), 'HOLIDAY', '元旦'),
(DATE_FORMAT(CURDATE(),'%Y-05-01'), 'HOLIDAY', '劳动节'),
(DATE_FORMAT(CURDATE(),'%Y-10-01'), 'HOLIDAY', '国庆节'),
(DATE_FORMAT(CURDATE(),'%Y-10-02'), 'HOLIDAY', '国庆节'),
(DATE_FORMAT(CURDATE(),'%Y-10-03'), 'HOLIDAY', '国庆节');

-- 公告、员工档案、账号与业务流水均不预置，由管理员在后台录入

-- ============================================================
-- 九、打卡地点（地理围栏 / IP 白名单 / WiFi）
--     不预置任何地点：由管理员在「考勤管理 → 打卡地点」中配置。
--     未配置启用地点时不限制；一旦配置，员工端必须通过该地点的校验才能打卡。
-- ============================================================
CREATE TABLE IF NOT EXISTS punch_location (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(80)   NOT NULL COMMENT '地点名称',
    address     VARCHAR(200)  COMMENT '详细地址',
    longitude   DECIMAL(10,6) NULL COMMENT '中心经度',
    latitude    DECIMAL(10,6) NULL COMMENT '中心纬度',
    radius_m    INT           NOT NULL DEFAULT 200 COMMENT '允许半径(米)',
    allow_ip    VARCHAR(300)  COMMENT '允许IP,逗号分隔,支持 192.168.1.* 与 /24 CIDR',
    wifi_ssid   VARCHAR(100)  COMMENT '允许WiFi SSID',
    is_default  TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否作为未分组员工的默认地点',
    enabled     TINYINT(1)    NOT NULL DEFAULT 1,
    remark      VARCHAR(300),
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡地点表';

CREATE TABLE IF NOT EXISTS attendance_group_location (
    group_id    BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤组允许打卡地点关联表';

DROP PROCEDURE IF EXISTS sp_ensure_col_v3;
DELIMITER $$
CREATE PROCEDURE sp_ensure_col_v3(IN p_col VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'attendance_record'
                     AND COLUMN_NAME = p_col) THEN
        SET @ddl_v3 = p_ddl;
        PREPARE st3 FROM @ddl_v3;
        EXECUTE st3;
        DEALLOCATE PREPARE st3;
    END IF;
END$$
DELIMITER ;
CALL sp_ensure_col_v3('location_id',    'ALTER TABLE attendance_record ADD COLUMN location_id BIGINT NULL COMMENT ''打卡地点ID''');
CALL sp_ensure_col_v3('punch_lat',      'ALTER TABLE attendance_record ADD COLUMN punch_lat DECIMAL(10,6) NULL COMMENT ''打卡纬度''');
CALL sp_ensure_col_v3('punch_lng',      'ALTER TABLE attendance_record ADD COLUMN punch_lng DECIMAL(10,6) NULL COMMENT ''打卡经度''');
CALL sp_ensure_col_v3('distance_m',     'ALTER TABLE attendance_record ADD COLUMN distance_m INT NULL COMMENT ''距地点中心距离(米)''');
CALL sp_ensure_col_v3('punch_address',  'ALTER TABLE attendance_record ADD COLUMN punch_address VARCHAR(200) NULL COMMENT ''打卡地点名称''');
DROP PROCEDURE IF EXISTS sp_ensure_col_v3;
