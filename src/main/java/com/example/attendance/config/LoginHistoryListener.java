package com.example.attendance.config;

import com.example.attendance.util.AppClock;

import com.example.attendance.entity.LoginLog;
import com.example.attendance.entity.User;
import com.example.attendance.mapper.LoginLogMapper;
import com.example.attendance.mapper.UserMapper;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/** 登录审计：成功与失败都落库，记录 IP、UA，并回写账号最后登录信息 */
@Component
public class LoginHistoryListener {

    private final LoginLogMapper loginLogMapper;
    private final UserMapper userMapper;

    public LoginHistoryListener(LoginLogMapper loginLogMapper, UserMapper userMapper) {
        this.loginLogMapper = loginLogMapper;
        this.userMapper = userMapper;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        save(event.getAuthentication().getName(), true);
        User u = userMapper.findByUsername(event.getAuthentication().getName());
        if (u != null) userMapper.updateLoginInfo(u.getId(), AppClock.nowDateTime(), clientIp());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String name = event.getAuthentication() == null ? "unknown" : event.getAuthentication().getName();
        save(name, false);
    }

    private void save(String username, boolean success) {
        try {
            LoginLog log = new LoginLog();
            log.setUsername(username);
            log.setIp(clientIp());
            log.setSuccess(success);
            log.setUserAgent(userAgent());
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
            // 审计失败不能阻断登录主流程
        }
    }

    private String clientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "";
        String xf = attrs.getRequest().getHeader("X-Forwarded-For");
        return (xf == null || xf.isEmpty()) ? attrs.getRequest().getRemoteAddr() : xf.split(",")[0].trim();
    }

    private String userAgent() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "";
        String ua = attrs.getRequest().getHeader("User-Agent");
        return ua == null ? "" : (ua.length() > 190 ? ua.substring(0, 190) : ua);
    }
}
