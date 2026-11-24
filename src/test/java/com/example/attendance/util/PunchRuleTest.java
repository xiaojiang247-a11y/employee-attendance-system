package com.example.attendance.util;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** 打卡时间边界回归测试：北京时间 09:00 / 18:00，宽限到 09:02 / 17:59 */
class PunchRuleTest {

    @Test
    void checkInBoundary() {
        assertFalse(PunchRule.isLate(LocalTime.of(8, 59)), "9 点之前签到正常");
        assertFalse(PunchRule.isLate(LocalTime.of(9, 0)), "9:00 整正常");
        assertFalse(PunchRule.isLate(LocalTime.of(9, 2)), "9:02 属于宽限内");
        assertTrue(PunchRule.isLate(LocalTime.of(9, 2, 1)), "9:02 之后才算迟到");
        assertTrue(PunchRule.isLate(LocalTime.of(9, 3)), "9:03 迟到");

        assertEquals(0, PunchRule.lateMinutes(LocalTime.of(9, 2)));
        assertEquals(3, PunchRule.lateMinutes(LocalTime.of(9, 3)));
        assertEquals(3, PunchRule.lateMinutes(LocalTime.of(9, 2, 30)), "迟到分钟向上取整");
    }

    @Test
    void checkOutBoundary() {
        assertTrue(PunchRule.isEarlyLeave(LocalTime.of(17, 58)), "17:59 之前早退");
        assertFalse(PunchRule.isEarlyLeave(LocalTime.of(17, 59)), "17:59 起不再算早退");
        assertFalse(PunchRule.isEarlyLeave(LocalTime.of(18, 0)), "18:00 正常下班");
        assertFalse(PunchRule.isEarlyLeave(LocalTime.of(18, 30)), "18:00 之后正常下班");

        assertEquals(2, PunchRule.earlyMinutes(LocalTime.of(17, 58)));
        assertEquals(0, PunchRule.earlyMinutes(LocalTime.of(17, 59)));
        assertEquals(0, PunchRule.earlyMinutes(LocalTime.of(18, 30)));
    }
}
