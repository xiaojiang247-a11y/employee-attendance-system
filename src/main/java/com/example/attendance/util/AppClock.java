package com.example.attendance.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * 全系统统一时间源：固定使用北京时间 Asia/Shanghai (UTC+8)。
 * 所有业务日期、打卡判定、月度关账、日志时间戳一律经由本类，
 * 不直接调用 LocalDate.now() / LocalDateTime.now()，避免部署环境默认时区漂移。
 */
public final class AppClock {

    /** 业务时区：北京时间，UTC+8，无夏令时 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    public static final String ZONE_ID = "Asia/Shanghai";
    public static final String ZONE_LABEL = "北京时间 (UTC+8)";

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private AppClock() {}

    public static LocalDate nowDate() { return LocalDate.now(ZONE); }
    public static LocalDateTime nowDateTime() { return LocalDateTime.now(ZONE); }
    public static LocalTime nowTime() { return LocalTime.now(ZONE); }
    public static YearMonth nowYearMonth() { return YearMonth.now(ZONE); }
    public static Instant nowInstant() { return Instant.now(); }
    public static long nowMillis() { return System.currentTimeMillis(); }

    public static String formatDateTime(LocalDateTime t) { return t == null ? "" : t.format(DT); }
    public static String formatDate(LocalDate d) { return d == null ? "" : d.format(D); }
}
