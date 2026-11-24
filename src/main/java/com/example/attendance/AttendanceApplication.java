package com.example.attendance;

import com.example.attendance.util.AppClock;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.example.attendance.mapper")
public class AttendanceApplication {

    public static void main(String[] args) {
        // 业务逻辑走 AppClock；这里同步 JVM 默认时区，保证 JDBC / 日志 / Jackson 也用北京时间
        TimeZone.setDefault(TimeZone.getTimeZone(AppClock.ZONE_ID));
        SpringApplication.run(AttendanceApplication.class, args);
        System.out.println("========================================");
        System.out.println("  企业员工考勤管理系统启动成功!");
        System.out.println("  业务时区: " + AppClock.ZONE_LABEL + " / " + AppClock.ZONE_ID);
        System.out.println("  当前时间: " + AppClock.formatDateTime(AppClock.nowDateTime()));
        System.out.println("  访问地址: http://localhost:8080");
        System.out.println("  唯一超级管理员: superadmin（初始口令见 app.super-admin.password，登录后请立即修改）");
        System.out.println("========================================");
    }
}
