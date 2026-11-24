SET NAMES utf8mb4;
USE attendance_db;

-- ============================================================
-- V4：三角色端权限边界 + 打卡地点覆盖 + 固定 9:00/18:00 打卡规则 的数据库配套
--   全部幂等，可重复执行
-- ============================================================

-- ---------- punch_location 增加「谁设置的 / 谁最后改的」 ----------
DROP PROCEDURE IF EXISTS sp_v4_col;
DELIMITER $$
CREATE PROCEDURE sp_v4_col(IN p_col VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'punch_location'
                     AND COLUMN_NAME = p_col) THEN
        SET @ddl_v4 = p_ddl;
        PREPARE st4 FROM @ddl_v4;
        EXECUTE st4;
        DEALLOCATE PREPARE st4;
    END IF;
END$$
DELIMITER ;
CALL sp_v4_col('created_by',      'ALTER TABLE punch_location ADD COLUMN created_by VARCHAR(50) NULL COMMENT ''设置人账号'' AFTER remark');
CALL sp_v4_col('created_by_role', 'ALTER TABLE punch_location ADD COLUMN created_by_role VARCHAR(30) NULL COMMENT ''设置人层级 SUPER_ADMIN/ADMIN'' AFTER created_by');
CALL sp_v4_col('updated_by',      'ALTER TABLE punch_location ADD COLUMN updated_by VARCHAR(50) NULL COMMENT ''最后修改人'' AFTER created_by_role');
CALL sp_v4_col('updated_by_role', 'ALTER TABLE punch_location ADD COLUMN updated_by_role VARCHAR(30) NULL COMMENT ''最后修改人层级'' AFTER updated_by');
CALL sp_v4_col('update_time',     'ALTER TABLE punch_location ADD COLUMN update_time DATETIME NULL COMMENT ''最后修改时间（北京时间）'' AFTER updated_by_role');
DROP PROCEDURE IF EXISTS sp_v4_col;

-- 历史地点一律视为管理员设置；超管在 /super/locations 新建或覆盖时会写 SUPER_ADMIN
UPDATE punch_location SET created_by = COALESCE(created_by, 'superadmin') WHERE created_by IS NULL;
UPDATE punch_location SET created_by_role = COALESCE(created_by_role, 'ADMIN') WHERE created_by_role IS NULL;
UPDATE punch_location SET update_time = COALESCE(update_time, create_time, NOW()) WHERE update_time IS NULL;

-- ---------- 超级管理员专属菜单（url 只在 /super/** 下，不影响其它角色的菜单校验） ----------
INSERT IGNORE INTO sys_menu (id, parent_id, name, icon, url, perm_code, menu_type, sort_no, visible) VALUES
(86, 8, '员工账号',       'users',    '/super/employees',   'super:employee',   'MENU', 6, 1),
(87, 8, '打卡地点(覆盖)', 'map-pin',  '/super/locations',   'super:location',   'MENU', 7, 1),
(88, 8, '权限边界',       'list',     '/super/permissions', 'super:permission', 'MENU', 8, 1);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 5, id FROM sys_menu WHERE id IN (86, 87, 88);

-- ---------- 账户操作记录：结果字段允许 FAIL（失败/越权也留痕），表结构无需改动 ----------
-- sys_account_log.result 为 VARCHAR(20)，create_time 由业务层显式写北京时间。

-- ---------- 打卡规则（固定，不写库；由 com.example.attendance.util.PunchRule 统一定义） ----------
-- 北京时间 09:00 上班 / 18:00 下班
--   09:00 之前        正常签到
--   09:00 ~ 09:02     宽限内正常
--   09:02 之后        迟到（迟到分钟从 09:00 起算）
--   18:00 及之后      正常下班
--   17:59 ~ 18:00     宽限内正常
--   17:59 之前        早退（早退分钟按距 18:00 的差值算）
