package com.example.attendance.util;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * 固定打卡时间规则（全系统唯一口径，业务时区固定 Asia/Shanghai / 北京时间）。
 *
 * 上班边界 09:00：
 *   - 09:00 之前        正常签到
 *   - 09:00 ~ 09:02     宽限内，仍算正常
 *   - 09:02 之后        迟到，迟到分钟从 09:00 起算（向上取整到分钟）
 * 下班边界 18:00：
 *   - 18:00 及之后      正常下班
 *   - 17:59 ~ 18:00     宽限内，仍算正常
 *   - 17:59 之前        早退，早退分钟按距 18:00 还差多久计算（向上取整）
 *
 * 宽限区间是默认值（按「9点02之后才算迟到」「17点59之前才算早退」的字面边界推导），
 * 全部集中在常量里，改规则只改这一个类。
 */
public final class PunchRule {

    public static final LocalTime WORK_START    = LocalTime.of(9, 0);
    public static final LocalTime LATE_AFTER    = LocalTime.of(9, 2);
    public static final LocalTime EARLY_BEFORE  = LocalTime.of(17, 59);
    public static final LocalTime WORK_END      = LocalTime.of(18, 0);

    private PunchRule() {}

    public static boolean isLate(LocalTime in) {
        return in != null && in.isAfter(LATE_AFTER);
    }

    public static boolean isEarlyLeave(LocalTime out) {
        return out != null && out.isBefore(EARLY_BEFORE);
    }

    /** 迟到分钟：仅在 09:02 之后才 > 0，从 09:00 起算 */
    public static int lateMinutes(LocalTime in) {
        if (!isLate(in)) return 0;
        return ceilMinutes(ChronoUnit.SECONDS.between(WORK_START, in));
    }

    /** 早退分钟：仅在 17:59 之前才 > 0，按距 18:00 的差值算 */
    public static int earlyMinutes(LocalTime out) {
        if (!isEarlyLeave(out)) return 0;
        return ceilMinutes(ChronoUnit.SECONDS.between(out, WORK_END));
    }

    private static int ceilMinutes(long seconds) {
        return (int) Math.max(1, Math.ceil(seconds / 60.0));
    }

    public static String describe() {
        return "北京时间 09:00 上班 / 18:00 下班；09:02 之后签到记迟到，17:59 之前签退记早退";
    }
}
