package com.example.attendance.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LayoutInterceptor layoutInterceptor;
    private final OperLogInterceptor operLogInterceptor;
    private final MenuAccessInterceptor menuAccessInterceptor;

    public WebConfig(LayoutInterceptor layoutInterceptor, OperLogInterceptor operLogInterceptor,
                     MenuAccessInterceptor menuAccessInterceptor) {
        this.layoutInterceptor = layoutInterceptor;
        this.operLogInterceptor = operLogInterceptor;
        this.menuAccessInterceptor = menuAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] skip = {"/login", "/403", "/error", "/css/**", "/js/**", "/images/**",
                         "/favicon.ico", "/favicon.svg"};
        // 顺序：先做菜单授权判定，再记操作日志、注入页面公共模型
        registry.addInterceptor(menuAccessInterceptor).addPathPatterns("/**").excludePathPatterns(skip);
        registry.addInterceptor(operLogInterceptor).addPathPatterns("/**").excludePathPatterns(skip);
        registry.addInterceptor(layoutInterceptor).addPathPatterns("/**").excludePathPatterns(skip);
    }
}
