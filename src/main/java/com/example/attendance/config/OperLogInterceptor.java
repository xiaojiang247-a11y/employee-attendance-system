package com.example.attendance.config;

import com.example.attendance.entity.OperLog;
import com.example.attendance.mapper.OperLogMapper;
import com.example.attendance.service.SecurityHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 操作审计：记录非静态请求的模块、URL、参数、操作人、耗时与结果。
 * 参数截断到 500 字符，避免大表单撑爆日志表。
 */
@Component
public class OperLogInterceptor implements HandlerInterceptor {

    private static final String START = "__oper_start__";
    private final OperLogMapper operLogMapper;
    private final SecurityHelper security;

    public OperLogInterceptor(OperLogMapper operLogMapper, SecurityHelper security) {
        this.operLogMapper = operLogMapper;
        this.security = security;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            String uri = request.getRequestURI();
            if (uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images")
                    || uri.equals("/favicon.ico") || uri.equals("/error")) return;
            OperLog log = new OperLog();
            log.setModule(moduleOf(uri));
            log.setOperation(request.getMethod() + " " + uri);
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(uri);
            log.setIp(request.getRemoteAddr());
            log.setUsername(security.username());
            log.setParams(params(request.getParameterMap()));
            log.setResult(ex == null && response.getStatus() < 400 ? "SUCCESS" : "ERROR");
            Object start = request.getAttribute(START);
            log.setCostMs(start == null ? 0L : System.currentTimeMillis() - (Long) start);
            operLogMapper.insert(log);
        } catch (Exception ignored) {
            // 审计写入失败不影响业务响应
        }
    }

    private String moduleOf(String uri) {
        if (uri.startsWith("/my/")) return "员工自助";
        if (uri.contains("/approval")) return "审批中心";
        if (uri.contains("/attendance") || uri.contains("/schedule") || uri.contains("/shift")) return "考勤管理";
        if (uri.contains("/employees") || uri.contains("/org/") || uri.contains("/groups")) return "组织与人员";
        if (uri.contains("/report") || uri.contains("/summary")) return "统计报表";
        if (uri.contains("/system") || uri.contains("/login-logs")) return "系统管理";
        if (uri.equals("/login") || uri.equals("/logout")) return "认证";
        return "工作台";
    }

    private String params(Map<String, String[]> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> e : map.entrySet()) {
            if (e.getKey().contains("password") || e.getKey().contains("_csrf")) continue;
            sb.append(e.getKey()).append('=')
              .append(e.getValue().length == 0 ? "" : e.getValue()[0]).append('&');
        }
        String s = sb.toString();
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
