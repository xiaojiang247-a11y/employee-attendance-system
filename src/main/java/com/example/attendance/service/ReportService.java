package com.example.attendance.service;

import com.example.attendance.mapper.StatsMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/** 报表与导出：统一在这里生成 CSV，避免各 Controller 各自拼格式 */
@Service
public class ReportService {

    private final StatsMapper statsMapper;

    public ReportService(StatsMapper statsMapper) {
        this.statsMapper = statsMapper;
    }

    public List<Map<String, Object>> monthly(String ym, Long deptId) {
        return statsMapper.monthlyReport(ym, deptId);
    }

    public List<Map<String, Object>> monthlyTrend(String fromYm) {
        return statsMapper.monthlyTrend(fromYm);
    }

    /** 月度汇总 CSV（UTF-8 BOM，Excel 直接打开不乱码） */
    public String monthlyCsv(String ym, Long deptId) {
        List<Map<String, Object>> rows = monthly(ym, deptId);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("工号,姓名,部门,统计月份,应出勤天数,实出勤天数,迟到次数,早退次数,旷工次数,请假天数,加班时长(小时),补卡次数,总工时,迟到分钟,关账状态\n");
        for (Map<String, Object> r : rows) {
            sb.append(csv(r.get("employee_no"))).append(',')
              .append(csv(r.get("employee_name"))).append(',')
              .append(csv(r.get("department_name"))).append(',')
              .append(csv(r.get("ym"))).append(',')
              .append(csv(r.get("expected_days"))).append(',')
              .append(csv(r.get("actual_days"))).append(',')
              .append(csv(r.get("late_cnt"))).append(',')
              .append(csv(r.get("early_cnt"))).append(',')
              .append(csv(r.get("absent_cnt"))).append(',')
              .append(csv(r.get("leave_days"))).append(',')
              .append(csv(r.get("overtime_hours"))).append(',')
              .append(csv(r.get("makeup_cnt"))).append(',')
              .append(csv(r.get("work_hours"))).append(',')
              .append(csv(r.get("late_minutes"))).append(',')
              .append("1".equals(String.valueOf(r.get("locked"))) ? "已关账" : "未关账")
              .append('\n');
        }
        return sb.toString();
    }

    private String csv(Object v) {
        if (v == null) return "";
        String s = String.valueOf(v);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
