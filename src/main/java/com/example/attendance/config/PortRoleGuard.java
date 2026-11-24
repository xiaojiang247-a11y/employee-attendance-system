package com.example.attendance.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 端口级隔离：每个实例启动时带 --app.port-role，只允许对应角色访问。
 * 即使拿到别的端口的会话（同库同域），跨端口访问也一律 403。
 * 未配置 app.port-role 时保持单实例兼容模式（不做限制）。
 */
@Component
public class PortRoleGuard extends OncePerRequestFilter {

    @Value("${app.port-role:}")
    private String portRole;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (portRole == null || portRole.isBlank() || isOpen(req.getRequestURI())) {
            chain.doFilter(req, res);
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            chain.doFilter(req, res);
            return;
        }
        String role = null;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a.getAuthority().startsWith("ROLE_")) {
                role = a.getAuthority().substring(5);
                break; // CustomUserDetailsService 第一个装配的就是主角色
            }
        }
        if (!PortRoles.isAllowed(portRole, role)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "本端口仅供「" + PortRoles.label(portRole) + "」使用，当前角色无权访问");
            return;
        }
        chain.doFilter(req, res);
    }

    private static boolean isOpen(String uri) {
        return uri.equals("/") || uri.startsWith("/login") || uri.equals("/403")
                || uri.equals("/error") || uri.equals("/logout")
                || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/images/") || uri.equals("/favicon.ico");
    }
}
