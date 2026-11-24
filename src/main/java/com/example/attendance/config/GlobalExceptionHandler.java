package com.example.attendance.config;

import com.example.attendance.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;

/** 业务异常回源页面并带提示，系统异常渲染统一错误页 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 静态资源不存在（/favicon.ico、拼错的 css/js、referer 里的旧图片路径）不是系统故障。
     * 交给 handleOther 会渲染 500 错误页并记 ERROR 日志 —— 浏览器每个页面都会自动请求
     * /favicon.ico，结果是每次刷新都刷一条假故障日志。这里按 404 静默处理。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNoResource(NoResourceFoundException e, HttpServletResponse response) throws java.io.IOException {
        if (!response.isCommitted()) {
            response.sendError(HttpStatus.NOT_FOUND.value());
        }
    }

    @ExceptionHandler(BizException.class)
    public String handleBiz(BizException e, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", e.getMessage());
        String ref = request.getHeader("Referer");
        if (ref != null) {
            try {
                String path = URI.create(ref).getRawPath();
                if (path != null && !path.equals(request.getRequestURI())) return "redirect:" + path;
            } catch (IllegalArgumentException ignored) { }
        }
        // 无 Referer（API / 直接 POST / 隐私策略剥离）时按请求所属端回源：
        // 统一兜底 /dashboard 会让员工端与超管端跳到无权访问的页面，业务提示随之丢失。
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/my/")) return "redirect:/my/home";
        if (uri != null && uri.startsWith("/super/")) return "redirect:/super/dashboard";
        return "redirect:/dashboard";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception e, HttpServletRequest request,
                              HttpServletResponse response, Model model) {
        log.error("请求处理失败: {} {}", request.getMethod(), request.getRequestURI(), e);
        // 视图渲染不会自动设置状态码：不显式写 500 会把服务端错误伪装成 HTTP 200
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("errorTitle", "系统处理请求时出错");
        model.addAttribute("errorMessage", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        model.addAttribute("requestUri", request.getRequestURI());
        return "error/500";
    }
}
