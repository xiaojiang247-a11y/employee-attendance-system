package com.example.attendance.config;

import com.example.attendance.entity.Role;
import com.example.attendance.entity.User;
import com.example.attendance.mapper.RoleMapper;
import com.example.attendance.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时保证唯一超级管理员账户可用。
 *
 * 系统只允许存在一个 SUPER_ADMIN。旧实现兜底的是内置 admin，删掉之后每次重启都会
 * 重新 insert，账户列表里又冒出第二个超管；这里只认 superadmin，且不再创建 admin。
 * 新建时同步写 sys_user_role 绑定：缺绑定的话 MenuAccessInterceptor 会直接 403。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    public static final String SUPER_USER = "superadmin";

    /** 初始口令走配置，不写死在源码里；公开仓库只保留占位默认值 */
    @Value("${app.super-admin.password:ChangeMe@2026}")
    private String superPassword;

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserMapper userMapper, RoleMapper roleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        User su = userMapper.findByUsernameIgnoreEnabled(SUPER_USER);

        if (su == null) {
            User u = new User();
            u.setUsername(SUPER_USER);
            u.setPassword(passwordEncoder.encode(superPassword));
            u.setPlainPassword(superPassword);
            u.setRealName("超级管理员");
            u.setRole("SUPER_ADMIN");
            u.setRoleLevel("SUPER");
            u.setCreatedBy("system");
            u.setEnabled(true);
            // 超管不绑定员工档案；employee_id 留空，避免删除员工档案时连带影响登录账户
            userMapper.insert(u);

            Role role = roleMapper.findByCode("SUPER_ADMIN");
            if (role != null) {
                userMapper.grantRole(u.getId(), role.getId());
            } else {
                log.warn(">>> 未找到 SUPER_ADMIN 角色，{} 已创建但缺角色绑定，请先执行 sql/schema_v4.sql", SUPER_USER);
            }
            log.info(">>> 唯一超级管理员账户已创建: {}（初始口令取自 app.super-admin.password）", SUPER_USER);
            return;
        }

        if (Boolean.FALSE.equals(su.getEnabled())) {
            // 只启用，不重置密码：密码可能被用户改过，重置会把人锁在门外
            userMapper.resetPasswordAndEnable(su.getId(), su.getPassword(), su.getPlainPassword());
            log.warn(">>> 超级管理员 {} 原为禁用状态，已重新启用", SUPER_USER);
        }
    }
}
