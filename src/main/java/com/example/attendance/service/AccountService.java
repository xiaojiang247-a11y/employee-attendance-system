package com.example.attendance.service;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import com.example.attendance.util.AppClock;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

/**
 * 三级权限隔离的账户管理中枢。
 * 超管子系统 -> 管理员账户（也可显式查看员工账户）；管理员子系统 -> 只管员工账户；员工子系统无账户管理入口。
 * 所有写操作都经过 assertManageable() 垂直越权断言并写入 sys_account_log（成功与失败都留痕）。
 */
@Service
public class AccountService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final AccountLogMapper logMapper;
    private final EmployeeMapper employeeMapper;
    private final SecurityHelper security;
    private final SessionRegistry sessionRegistry;

    public AccountService(UserMapper userMapper, RoleMapper roleMapper, AccountLogMapper logMapper,
                          EmployeeMapper employeeMapper, SecurityHelper security,
                          SessionRegistry sessionRegistry) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.logMapper = logMapper;
        this.employeeMapper = employeeMapper;
        this.security = security;
        this.sessionRegistry = sessionRegistry;
    }

    /** 当前操作人默认可见/可管的账户层级 */
    public String manageableLevel() {
        return "SUPER".equals(security.accountLevel()) ? "ADMIN" : "EMPLOYEE";
    }

    /**
     * 账户列表：管理员恒定为 EMPLOYEE 级；超管可显式指定看 ADMIN 或 EMPLOYEE。
     * deptId 会真正下推到 SQL，部门筛选不再只是页面摆设。
     */
    public List<User> list(String level, String keyword, Long deptId) {
        String lv = "SUPER".equals(security.accountLevel()) ? level : "EMPLOYEE";
        return userMapper.findByLevel(lv, keyword, deptId);
    }

    public List<User> list(String keyword) {
        return list(manageableLevel(), keyword, null);
    }

    /** 垂直越权断言：目标账号必须在操作人的管理层级之内 */
    public void assertManageable(User target) {
        User me = security.currentUser();
        if (me == null) throw new BizException("登录状态已失效，请重新登录");
        if (target == null) throw new BizException("目标账户不存在");
        String myLevel = security.accountLevel();
        String targetLevel = SecurityHelper.levelOf(target.getRole());
        if ("SUPER".equals(myLevel)) {
            if ("SUPER".equals(targetLevel) && !Objects.equals(me.getId(), target.getId())) {
                throw new BizException("超级管理员账户不由其他账户管理");
            }
        } else if ("ADMIN".equals(myLevel)) {
            if (!"EMPLOYEE".equals(targetLevel)) {
                throw new BizException("管理员只能管理员工账户，无权操作管理员及以上账户");
            }
            String creator = target.getCreatedBy();
            // created_by 为空是老库遗留/系统预置账户，允许所有管理员维护；
            // 非空则按归属隔离，避免跨管理员改别人创建的账号。
            if (creator != null && !creator.trim().isEmpty() && !creator.equals(me.getUsername())) {
                throw new BizException("该员工账户由「" + creator + "」创建，您无权操作");
            }
        } else {
            throw new BizException("当前角色没有账户管理权限");
        }
    }

    /** 新建账户允许分配的角色：超管可建管理员/员工，管理员只能建员工 */
    public void assertCreatableRole(String role) {
        String level = SecurityHelper.levelOf(role);
        if ("SUPER".equals(level)) throw new BizException("不可创建超级管理员账户");
        if ("ADMIN".equals(security.accountLevel()) && !"EMPLOYEE".equals(level)) {
            throw new BizException("管理员只能创建员工账户");
        }
        if ("EMPLOYEE".equals(security.accountLevel()) && !"EMPLOYEE".equals(level)) {
            throw new BizException("当前角色只能创建员工账户");
        }
    }

    @Transactional
    public void save(User u, Long roleId, String password, boolean create,
                     PasswordEncoder encoder) {
        User me = security.currentUser();
        if (me == null) throw new BizException("登录状态已失效");
        Role role = roleId == null ? roleMapper.findByCode(u.getRole()) : roleMapper.findById(roleId);
        if (role == null) throw new BizException("所选角色不存在");
        u.setRole(role.getCode());
        u.setRoleLevel(SecurityHelper.levelOf(role.getCode()));
        assertCreatableRole(role.getCode());

        if (u.getEnabled() == null) u.setEnabled(true);
        if (u.getDeptId() == null && u.getEmployeeId() != null) {
            Employee e = employeeMapper.findById(u.getEmployeeId());
            if (e != null) u.setDeptId(e.getDepartmentId());
        }

        if (create) {
            if (u.getUsername() == null || u.getUsername().trim().isEmpty())
                throw new BizException("登录账号不能为空");
            if (userMapper.findByUsernameIgnoreEnabled(u.getUsername().trim()) != null)
                throw new BizException("登录账号 " + u.getUsername() + " 已存在");
            u.setUsername(u.getUsername().trim());
            String pwd = (password == null || password.isEmpty()) ? "123456" : password;
            u.setPassword(encoder.encode(pwd));
            u.setPlainPassword(pwd);
            u.setCreatedBy(me.getUsername());
            userMapper.insert(u);
            writeLog("CREATE", u, "新建账户，角色 " + role.getName() + "，初始密码 " + pwd);
        } else {
            User old = userMapper.findById(u.getId());
            assertManageable(old);
            // 编辑时不允许通过表单把员工提升为管理员
            if ("ADMIN".equals(security.accountLevel()) && !"EMPLOYEE".equals(SecurityHelper.levelOf(u.getRole())))
                throw new BizException("管理员不能把员工账户改为管理员及以上角色");
            u.setCreatedBy(old.getCreatedBy());
            userMapper.update(u);
            writeLog("UPDATE", u, "编辑账户资料：" + u.getRealName() + " / " + role.getName());
        }
        userMapper.deleteRoles(u.getId());
        userMapper.grantRole(u.getId(), role.getId());
    }

    @Transactional
    public void toggle(Long id) {
        User u = userMapper.findById(id);
        assertManageable(u);
        if (Objects.equals(security.userId(), id)) throw new BizException("不能停用当前登录的账户");
        int v = Boolean.FALSE.equals(u.getEnabled()) ? 1 : 0;
        userMapper.updateEnabled(id, v);
        writeLog(v == 1 ? "ENABLE" : "DISABLE", u, v == 1 ? "启用账户" : "停用账户");
        // 停用立即踢线：SecurityContext 里缓存的 GrantedAuthority 不会自动重查库
        if (v == 0) expireSessions(u.getUsername());
    }

    @Transactional
    public void resetPassword(Long id, String encoded, String plain) {
        User u = userMapper.findById(id);
        assertManageable(u);
        userMapper.updatePassword(id, encoded, plain);
        writeLog("RESET_PWD", u, "密码重置为 " + plain);
    }

    @Transactional
    public void delete(Long id) {
        User u = userMapper.findById(id);
        assertManageable(u);
        if (Objects.equals(security.userId(), id)) throw new BizException("不能删除当前登录的账户");
        // 系统只保留一个 SUPER_ADMIN；禁止删除任何 SUPER 级账户（含超管自己），避免系统无超管
        if ("SUPER".equals(SecurityHelper.levelOf(u.getRole()))) throw new BizException("唯一超级管理员账户不可删除，系统只保留一个");
        userMapper.deleteRoles(id);
        userMapper.deleteById(id);
        writeLog("DELETE", u, "删除账户");
    }

    public void writeLog(String action, User target, String detail) {
        writeLog(action, target, detail, "SUCCESS");
    }

    public void writeLog(String action, User target, String detail, String result) {
        User me = security.currentUser();
        AccountLog l = new AccountLog();
        l.setOperator(me == null ? "system" : me.getUsername());
        l.setOperatorRole(me == null ? "SYSTEM" : me.getRole());
        l.setAction(action);
        l.setTargetUser(target == null ? "-" : target.getUsername());
        l.setTargetRole(target == null ? null : target.getRole());
        l.setDetail(detail);
        l.setIp(clientIp());
        l.setResult(result);
        // 显式写北京时间，不依赖数据库服务器本地时区
        l.setCreateTime(AppClock.nowDateTime());
        logMapper.insert(l);
    }

    /** 被拒绝的越权 / 校验失败同样留痕，result=FAIL */
    public void writeFailure(String action, String target, String reason) {
        User t = new User();
        t.setUsername(target == null ? "-" : target);
        writeLog(action, t, reason, "FAIL");
    }

    /** 打卡地点变更（含超管覆盖管理员设置）写入同一张账户操作记录表 */
    public void writeLocationLog(String action, String locationName, String detail) {
        User me = security.currentUser();
        AccountLog l = new AccountLog();
        l.setOperator(me == null ? "system" : me.getUsername());
        l.setOperatorRole(me == null ? "SYSTEM" : me.getRole());
        l.setAction(action);
        l.setTargetUser("地点:" + (locationName == null ? "-" : locationName));
        l.setTargetRole(me == null ? "SYSTEM" : me.getRole());
        l.setDetail(detail);
        l.setIp(clientIp());
        l.setResult("SUCCESS");
        l.setCreateTime(AppClock.nowDateTime());
        logMapper.insert(l);
    }

    private void expireSessions(String username) {
        for (Object p : sessionRegistry.getAllPrincipals()) {
            if (p instanceof UserDetails ud && username.equals(ud.getUsername())) {
                for (SessionInformation si : sessionRegistry.getAllSessions(p, false)) {
                    si.expireNow();
                }
            }
        }
    }

    private String clientIp() {
        try {
            ServletRequestAttributes ra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (ra == null) return null;
            // 审计日志同样只记网络层对端地址，避免伪造代理头污染操作记录
            HttpServletRequest req = ra.getRequest();
            String ip = req.getRemoteAddr();
            return ip == null ? null : ip.substring(0, Math.min(ip.length(), 64));
        } catch (Exception e) { return null; }
    }

    public static String actionName(String action) {
        return actionNameMap().getOrDefault(action, action);
    }

    public static Map<String, String> actionNameMap() {
        Map<String, String> m = new HashMap<>();
        m.put("CREATE", "新建账户"); m.put("UPDATE", "编辑账户"); m.put("DELETE", "删除账户");
        m.put("ENABLE", "启用账户"); m.put("DISABLE", "停用账户"); m.put("RESET_PWD", "重置密码");
        m.put("GRANT", "权限变更"); m.put("DENY", "越权被拒");
        m.put("LOCATION_SAVE", "设置打卡地点"); m.put("LOCATION_DEFAULT", "设为默认地点");
        m.put("LOCATION_TOGGLE", "启停打卡地点"); m.put("LOCATION_DELETE", "删除打卡地点");
        return m;
    }
}
