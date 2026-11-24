package com.example.attendance.service;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** 假期规则：工作日扣减计算、额度校验、审批通过与撤销的余额回滚 */
@Service
public class LeaveService {

    private final LeaveTypeMapper leaveTypeMapper;
    private final LeaveBalanceMapper balanceMapper;
    private final HolidayCalendarMapper holidayMapper;
    private final LeaveRequestMapper leaveMapper;
    private final ApprovalRecordMapper approvalMapper;
    private final AttendanceRecordMapper recordMapper;

    public LeaveService(LeaveTypeMapper leaveTypeMapper, LeaveBalanceMapper balanceMapper,
                        HolidayCalendarMapper holidayMapper, LeaveRequestMapper leaveMapper,
                        ApprovalRecordMapper approvalMapper, AttendanceRecordMapper recordMapper) {
        this.leaveTypeMapper = leaveTypeMapper;
        this.balanceMapper = balanceMapper;
        this.holidayMapper = holidayMapper;
        this.leaveMapper = leaveMapper;
        this.approvalMapper = approvalMapper;
        this.recordMapper = recordMapper;
    }

    /**
     * 请假天数：CALENDAR 按自然日；WORKDAY 跳过周末与节假日，但调休上班日(WORKDAY)要计入。
     * 复杂度 O(天数)，单次申请通常不超过 100 天。
     */
    public BigDecimal calcDays(LocalDate start, LocalDate end, String typeCode) {
        if (start == null || end == null || end.isBefore(start)) return BigDecimal.ZERO;
        LeaveType type = leaveTypeMapper.findByCode(typeCode);
        if (type != null && "CALENDAR".equals(type.getDeductRule())) {
            return BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end) + 1);
        }
        Set<LocalDate> restDays = new HashSet<>();
        Set<LocalDate> makeUpWorkdays = new HashSet<>();
        for (HolidayCalendar h : holidayMapper.findByRange(start, end)) {
            if ("WORKDAY".equals(h.getDayType())) makeUpWorkdays.add(h.getCalDate());
            else restDays.add(h.getCalDate());
        }
        long days = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (isWorkday(d, restDays, makeUpWorkdays)) days++;
        }
        return BigDecimal.valueOf(days);
    }

    /** 单日是否应出勤：调休上班日计入、周末与节假日剔除；calcDays 与 onApproved 共用同一口径 */
    private boolean isWorkday(LocalDate d, Set<LocalDate> restDays, Set<LocalDate> makeUpWorkdays) {
        if (makeUpWorkdays.contains(d)) return true;
        if (d.getDayOfWeek().getValue() >= 6) return false;
        return !restDays.contains(d);
    }

    @Transactional
    public LeaveRequest apply(Long employeeId, String typeCode, LocalDate start, LocalDate end, String reason) {
        if (employeeId == null) throw new BizException("当前账号未绑定员工档案，无法提交请假");
        LeaveType type = leaveTypeMapper.findByCode(typeCode);
        if (type == null) throw new BizException("假别不存在或已停用");
        BigDecimal days = calcDays(start, end, typeCode);
        if (days.compareTo(BigDecimal.ZERO) <= 0) throw new BizException("所选区间没有可扣减的工作日");
        if (leaveMapper.countOverlap(employeeId, start, end) > 0) {
            throw new BizException("该区间已有审批通过或待审批的请假单，请勿重复申请");
        }
        if (type.getAnnualQuota() != null && type.getAnnualQuota().compareTo(BigDecimal.ZERO) > 0) {
            LeaveBalance b = balanceMapper.findOne(employeeId, start.getYear(), typeCode);
            if (b == null || b.getRemainDays().compareTo(days) < 0) {
                throw new BizException(type.getName() + "余额不足，剩余 "
                        + (b == null ? "0" : b.getRemainDays()) + " 天，本次需 " + days + " 天");
            }
        }
        LeaveRequest r = new LeaveRequest();
        r.setEmployeeId(employeeId);
        r.setType(typeCode);
        r.setTypeCode(typeCode);
        r.setStartDate(start);
        r.setEndDate(end);
        r.setDays(days);
        r.setReason(reason);
        r.setStatus("PENDING");
        r.setCurrentStep(1);
        leaveMapper.insert(r);

        ApprovalRecord ar = new ApprovalRecord();
        ar.setBizType("LEAVE");
        ar.setBizId(r.getId());
        ar.setStepNo(1);
        ar.setAction("SUBMIT");
        ar.setRemark("提交" + type.getName() + "申请 " + days + " 天");
        approvalMapper.insert(ar);
        return r;
    }

    /** 审批通过后扣减额度，并把请假期内的工作日考勤记录标记为 LEAVE */
    @Transactional
    public void onApproved(LeaveRequest lr) {
        LeaveType type = leaveTypeMapper.findByCode(lr.getTypeCode());
        if (type != null && type.getAnnualQuota() != null
                && type.getAnnualQuota().compareTo(BigDecimal.ZERO) > 0) {
            LeaveBalance b = balanceMapper.findOne(lr.getEmployeeId(), lr.getStartDate().getYear(), lr.getTypeCode());
            if (b != null) balanceMapper.consume(b.getId(), lr.getDays());
        }
        Set<LocalDate> restDays = new HashSet<>();
        Set<LocalDate> makeUpWorkdays = new HashSet<>();
        for (HolidayCalendar h : holidayMapper.findByRange(lr.getStartDate(), lr.getEndDate())) {
            if ("WORKDAY".equals(h.getDayType())) makeUpWorkdays.add(h.getCalDate());
            else restDays.add(h.getCalDate());
        }
        for (LocalDate d = lr.getStartDate(); !d.isAfter(lr.getEndDate()); d = d.plusDays(1)) {
            if (!isWorkday(d, restDays, makeUpWorkdays)) continue;
            AttendanceRecord r = recordMapper.findTodayRecord(lr.getEmployeeId(), d);
            if (r == null) {
                r = new AttendanceRecord();
                r.setEmployeeId(lr.getEmployeeId());
                r.setAttendanceDate(d);
                r.setStatus("LEAVE");
                r.setSource("LEAVE");
                r.setIsMakeup(0);
                r.setLateMinutes(0);
                r.setEarlyMinutes(0);
                r.setWorkHours(BigDecimal.ZERO);
                r.setRemark("请假审批通过");
                recordMapper.insert(r);
            } else {
                String old = r.getStatus();
                r.setStatus("LEAVE");
                r.setRemark("请假审批通过（原状态 " + old + "）");
                recordMapper.update(r);
            }
        }
    }

    @Transactional
    public void cancel(Long id, Long employeeId) {
        LeaveRequest lr = leaveMapper.findById(id);
        if (lr == null || !lr.getEmployeeId().equals(employeeId)) throw new BizException("单据不存在");
        if (!"PENDING".equals(lr.getStatus())) throw new BizException("仅待审批的单据可以撤销");
        leaveMapper.updateStatus(id, "CANCELLED");
        ApprovalRecord ar = new ApprovalRecord();
        ar.setBizType("LEAVE");
        ar.setBizId(id);
        ar.setStepNo(1);
        ar.setAction("CANCEL");
        ar.setRemark("员工主动撤销");
        approvalMapper.insert(ar);
    }
}
