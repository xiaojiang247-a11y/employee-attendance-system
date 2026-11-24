SET NAMES utf8mb4;

-- ============================================================
-- V3 三级权限隔离：超级管理员 / 管理员 / 员工
--   1) 新增 SUPER_ADMIN 角色与超级管理员子系统菜单
--   2) 新增账户管理范围字段与账户操作流水
--   3) 原有 ADMIN 降级为「管理员子系统」，HR/MANAGER 作为管理员子系统内的细分角色保留
--   4) 全部幂等，可重复执行
-- ============================================================

-- ---------- 角色 ----------
INSERT IGNORE INTO sys_role (id, code, name, description, data_scope, enabled) VALUES
(5, 'SUPER_ADMIN', '超级管理员', '主管理系统最高权限：统一管理管理员账户、角色权限、系统配置与审计', 'ALL', 1);

UPDATE sys_role SET name='管理员', description='管理员子系统：管理员工账户、组织人事、考勤规则、审批与报表' WHERE code='ADMIN';
UPDATE sys_role SET name='人事考勤员', description='管理员子系统（人事岗）：员工账户与考勤核算' WHERE code='HR';
UPDATE sys_role SET name='部门负责人', description='管理员子系统（部门岗）：本部门考勤与审批' WHERE code='MANAGER';
UPDATE sys_role SET name='员工', description='员工子系统：仅本人信息与自助功能入口' WHERE code='EMPLOYEE';

-- ---------- 账户管理范围 ----------
-- created_by：记录账号由谁创建，用于「管理员只能管理自己创建的员工账号」隔离
DROP PROCEDURE IF EXISTS sp_ensure_usr_v3;
DELIMITER $$
CREATE PROCEDURE sp_ensure_usr_v3(IN p_col VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user'
                     AND COLUMN_NAME = p_col) THEN
        SET @ddl_v3 = p_ddl;
        PREPARE st3 FROM @ddl_v3;
        EXECUTE st3;
        DEALLOCATE PREPARE st3;
    END IF;
END$$
DELIMITER ;
CALL sp_ensure_usr_v3('created_by', 'ALTER TABLE sys_user ADD COLUMN created_by VARCHAR(50) NULL COMMENT ''创建人账号'' AFTER avatar');
CALL sp_ensure_usr_v3('role_level', 'ALTER TABLE sys_user ADD COLUMN role_level VARCHAR(20) NULL COMMENT ''账户层级 SUPER/ADMIN/EMPLOYEE'' AFTER created_by');
DROP PROCEDURE IF EXISTS sp_ensure_usr_v3;

-- 账户层级回填：SUPER_ADMIN -> SUPER，ADMIN/HR/MANAGER -> ADMIN，其余 -> EMPLOYEE
UPDATE sys_user SET role_level = CASE
    WHEN role = 'SUPER_ADMIN' THEN 'SUPER'
    WHEN role IN ('ADMIN','HR','MANAGER') THEN 'ADMIN'
    ELSE 'EMPLOYEE' END
WHERE role_level IS NULL;

-- ---------- 账户操作流水 ----------
CREATE TABLE IF NOT EXISTS sys_account_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator      VARCHAR(50)  NOT NULL COMMENT '操作人账号',
    operator_role VARCHAR(30)  NOT NULL COMMENT '操作人角色',
    action        VARCHAR(30)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/ENABLE/DISABLE/RESET_PWD/GRANT',
    target_user   VARCHAR(50)  NOT NULL COMMENT '目标账号',
    target_role   VARCHAR(30)  COMMENT '目标角色',
    detail        VARCHAR(500) COMMENT '变更摘要',
    ip            VARCHAR(64),
    result        VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_al_time (create_time),
    INDEX idx_al_target (target_user)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户管理操作记录';

-- ---------- 超级管理员子系统菜单 ----------
INSERT IGNORE INTO sys_menu (id, parent_id, name, icon, url, perm_code, menu_type, sort_no) VALUES
(8,  0, '超级管理员', 'shield',     '',                    'super',            'DIR', 0),
(81, 8, '超级工作台', 'grid',       '/super/dashboard',   'super:dashboard',  'MENU', 1),
(82, 8, '管理员账户', 'users',      '/super/admins',      'super:admin',      'MENU', 2),
(83, 8, '角色权限',   'key',        '/super/roles',       'super:role',       'MENU', 3),
(84, 8, '账户操作记录','file',      '/super/account-logs','super:accountlog', 'MENU', 4),
(85, 8, '系统配置',   'settings',  '/super/config',      'super:config',     'MENU', 5);

-- 管理员子系统菜单调整：账号页只面向员工账号
UPDATE sys_menu SET name = '员工账号' WHERE id = 61;
UPDATE sys_menu SET name = '管理员工作台' WHERE id = 11;

-- ---------- 角色菜单授权 ----------
-- 超级管理员：全部菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 5, id FROM sys_menu;
-- 管理员：业务菜单 + 员工账号，去掉超级管理员目录与角色权限、账户操作记录
DELETE FROM sys_role_menu WHERE role_id IN (1,2,3,4) AND menu_id IN (8,81,82,83,84,85,62);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu
  WHERE perm_code NOT LIKE 'super%' AND menu_type = 'MENU';
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, id FROM sys_menu
  WHERE menu_type = 'DIR' AND perm_code NOT LIKE 'super%';
-- HR / MANAGER 沿用原有授权（已排除系统管理），补上员工账号菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 2, id FROM sys_menu
  WHERE perm_code IN ('system:user','att:location','dashboard','org','att','approval','report','self');
-- 员工：仅员工自助目录与工作台
DELETE FROM sys_role_menu WHERE role_id = 4 AND menu_id NOT IN (7,12,71,72,73,74,75,76,77,78,1);
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 4, id FROM sys_menu WHERE parent_id = 7 OR id IN (7,12);

-- ---------- 唯一超级管理员 ----------
-- 系统只保留一个 SUPER_ADMIN：清理旧版内置 admin 账户，避免重启后被 DataInitializer 复活。
-- 管理员与员工账户一律由超级管理员在后台自行添加，部署脚本不再预置。
DELETE FROM sys_user_role WHERE user_id = (SELECT id FROM sys_user WHERE username = 'admin');
DELETE FROM sys_user WHERE username = 'admin';

-- 若库中已有 superadmin，确保其角色与绑定正确；不存在则由 DataInitializer 用 BCrypt
-- 生成真实密文后创建（此处不写占位哈希）。
UPDATE sys_user SET role = 'SUPER_ADMIN', role_level = 'SUPER', enabled = 1
WHERE username = 'superadmin';
DELETE FROM sys_user_role
WHERE user_id = (SELECT id FROM sys_user WHERE username = 'superadmin') AND role_id <> 5;
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT id, 5 FROM sys_user WHERE username = 'superadmin';
