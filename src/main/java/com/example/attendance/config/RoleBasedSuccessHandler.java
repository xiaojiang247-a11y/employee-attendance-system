package com.example.attendance.config;

import com.example.attendance.entity.User;
import com.example.attendance.mapper.UserMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 登录成功后按角色分流到对应子系统；端口角色不匹配时直接拒绝登录并清会话 */
@Component
public class RoleBasedSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserMapper userMapper;

    @Value("${app.port-role:}")
    private String portRole;

    public RoleBasedSuccessHandler(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        User u = userMapper.findByUsername(authentication.getName());
        if (u == null) return "/login";
        String role = u.getRole();
        if ("SUPER_ADMIN".equals(role)) return "/super/dashboard";
        if ("ADMIN".equals(role) || "HR".equals(role) || "MANAGER".equals(role)) return "/dashboard";
        return "/my/home";
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        User u = userMapper.findByUsername(authentication.getName());
        // 端口级隔离：超管不能在员工端登录，员工不能进管理员端
        if (u != null && !PortRoles.isAllowed(portRole, u.getRole())) {
            clearAuthenticationAttributes(request);
            SecurityContextHolder.clearContext();
            HttpSession s = request.getSession(false);
            if (s != null) s.invalidate();
            getRedirectStrategy().sendRedirect(request, response, "/login?error=port");
            return;
        }
        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response,
                determineTargetUrl(request, response, authentication));
    }
}
