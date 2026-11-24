package com.example.attendance.config;

import com.example.attendance.entity.Menu;
import com.example.attendance.entity.User;
import com.example.attendance.service.ApprovalService;
import com.example.attendance.service.SecurityHelper;
import com.example.attendance.service.SystemService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.List;

/** 所有页面共享的侧边栏菜单、当前用户、待办角标，统一在这里注入 */
@Component
public class LayoutInterceptor implements HandlerInterceptor {

    private final SecurityHelper security;
    private final SystemService systemService;
    private final ApprovalService approvalService;

    public LayoutInterceptor(SecurityHelper security, SystemService systemService,
                             ApprovalService approvalService) {
        this.security = security;
        this.systemService = systemService;
        this.approvalService = approvalService;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView mav) throws Exception {
        if (mav == null || mav.getModel() == null) return;
        if (mav.getViewName() != null && mav.getViewName().startsWith("redirect:")) return;
        User user = security.currentUser();
        if (user == null) return;
        List<Menu> menus = systemService.currentUserMenuTree();
        mav.getModel().put("currentUser", user);
        mav.getModel().put("menus", menus == null ? Collections.emptyList() : menus);
        mav.getModel().put("navRole", user.getRole());
        mav.getModel().put("pendingCount", security.canManage() ? approvalService.pendingCount() : 0);
        mav.getModel().put("requestUri", request.getRequestURI());
    }
}
