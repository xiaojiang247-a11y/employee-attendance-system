package com.example.attendance.config;

import com.example.attendance.entity.Menu;
import com.example.attendance.mapper.MenuMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;

/**
 * URL 与 sys_role_menu 联动：菜单被撤销后，直接输 URL 也一律 403。
 *
 * SecurityConfig 的 hasRole 是静态分段，只能挡住「跨子系统」；同一个子系统内部
 * 某个菜单被超管取消授权时，必须靠这里按当前登录人实际装配的 perm_code 动态判定。
 * 未登记到菜单的路径（表单 action、/logout 等）放行，由 SecurityConfig 兜底。
 */
@Component
public class MenuAccessInterceptor implements HandlerInterceptor {

    /** 菜单表变化不频繁，30 秒缓存一次，避免每个请求查库 */
    private static final long TTL_MS = 30_000L;

    private final MenuMapper menuMapper;
    private volatile List<Menu> cached = Collections.emptyList();
    private volatile long cachedAt = 0L;

    public MenuAccessInterceptor(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        String uri = req.getRequestURI();
        if (isOpen(uri)) return true;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return true;
        // 超级管理员系统独占全部菜单权限
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_SUPER_ADMIN".equals(a.getAuthority())) return true;
        }

        Menu matched = null;
        for (Menu m : menus()) {
            if ("DIR".equals(m.getMenuType())) continue;
            String url = m.getUrl();
            if (url == null || url.isBlank()) continue;
            if (uri.equals(url) || uri.startsWith(url + "/")) {
                // 取最长前缀，避免 /admin 抢走 /admin/system/users 的判定
                if (matched == null || url.length() > matched.getUrl().length()) matched = m;
            }
        }
        if (matched == null) return true;
        String perm = matched.getPermCode();
        if (perm == null || perm.isBlank()) return true;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (a.getAuthority().equals(perm)) return true;
        }
        res.sendError(HttpServletResponse.SC_FORBIDDEN, "当前角色未被授权访问该功能");
        return false;
    }

    private static boolean isOpen(String uri) {
        return uri.equals("/") || uri.startsWith("/login") || uri.equals("/403")
                || uri.equals("/error") || uri.equals("/logout")
                || uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/images/") || uri.equals("/favicon.ico");
    }

    private List<Menu> menus() {
        long now = System.currentTimeMillis();
        if (now - cachedAt > TTL_MS || cached.isEmpty()) {
            synchronized (this) {
                if (now - cachedAt > TTL_MS || cached.isEmpty()) {
                    cached = menuMapper.findAll();
                    cachedAt = now;
                }
            }
        }
        return cached;
    }
}
