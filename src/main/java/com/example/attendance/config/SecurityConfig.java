package com.example.attendance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * 三个角色端的权限边界（静态 URL 分段 + MenuAccessInterceptor 动态菜单校验）：
 *
 *   /super/**              超级管理员系统：SUPER_ADMIN 独占
 *                          —— 管理员账号、员工账号、部门信息、打卡地点、角色权限、操作记录、系统配置
 *   /admin/system/roles/** 角色与菜单授权：SUPER_ADMIN 独占（管理员不得改自己/超管的权限）
 *   /admin/system/users/** 员工账号：SUPER_ADMIN + ADMIN
 *   /admin/org/**          部门维护：SUPER_ADMIN + ADMIN
 *   /admin/locations/**    打卡地点：SUPER_ADMIN + ADMIN（超管可覆盖管理员设置）
 *   /admin/**              其余管理端业务：SUPER_ADMIN / ADMIN / HR / MANAGER
 *   /my/**                 员工系统：仅 EMPLOYEE（打卡与本人相关查看）
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RoleBasedSuccessHandler successHandler;

    public SecurityConfig(RoleBasedSuccessHandler successHandler) {
        this.successHandler = successHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 停用账户时据此让在线会话立即失效 */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/403", "/error", "/css/**", "/js/**",
                                 "/images/**", "/favicon.ico", "/favicon.svg").permitAll()
                .requestMatchers("/super/**").hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/system/roles", "/admin/system/roles/**")
                    .hasRole("SUPER_ADMIN")
                .requestMatchers("/admin/system/users", "/admin/system/users/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/org/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/locations", "/admin/locations/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN")
                .requestMatchers("/admin/**")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "HR", "MANAGER")
                .requestMatchers("/dashboard")
                    .hasAnyRole("SUPER_ADMIN", "ADMIN", "HR", "MANAGER")
                .requestMatchers("/my/**").hasRole("EMPLOYEE")
                .anyRequest().authenticated()
            )
            // 同一账号只允许一个有效会话；新登录挤掉旧会话，停用账户由 AccountService 主动 expire
            .sessionManagement(s -> s
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .sessionRegistry(sessionRegistry())
            )
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(successHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .permitAll()
            );
        return http.build();
    }
}
