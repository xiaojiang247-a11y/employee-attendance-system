package com.example.attendance.service;

import com.example.attendance.entity.*;
import com.example.attendance.exception.BizException;
import com.example.attendance.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 统一审批中心：5 类单据走同一个入口，审批动作与业务副作用在同一事务内完成。
 * 副作用包括：假期额度扣减、加班转调休、补卡回填打卡、异常申诉修正考勤结果。
 */
@Service
public class ApprovalService {

    private final ApprovalMapper approvalMapper;
    private final ApprovalRecordMapper recordMapper;
    private final LeaveRequestMapper leaveMapper;
    private final OvertimeRequestMapper overtimeMapper;
    private final BusinessTripMapper tripMapper;
    private final MakeupRequestMapper makeupMapper;
    private final AttendanceAppealMapper appealMapper;
    private final LeaveBalanceMapper balanceMapper;
    private final DepartmentMapper departmentMapper;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;
    private final SecurityHelper security;

    public ApprovalService(ApprovalMapper approvalMapper, ApprovalRecordMapper recordMapper,
                           LeaveRequestMapper leaveMapper, OvertimeRequestMapper overtimeMapper,
                           BusinessTripMapper tripMapper, MakeupRequestMapper makeupMapper,
                           AttendanceAppealMapper appealMapper, LeaveBalanceMapper balanceMapper,
                           DepartmentMapper departmentMapper,
                           LeaveService leaveService, AttendanceService attendanceService,
                           SecurityHelper security) {
        this.approvalMapper = approvalMapper;
        this.recordMapper = recordMapper;
        this.leaveMapper = leaveMapper;
        this.overtimeMapper = overtimeMapper;
        this.tripMapper = tripMapper;
        this.makeupMapper = makeupMapper;
        this.appealMapper = appealMapper;
        this.balanceMapper = balanceMapper;
        this.departmentMapper = departmentMapper;
        this.leaveService = leaveService;
        this.attendanceService = attendanceService;
        this.security = security;
    }

    public List<ApprovalTask> pending() {
        return approvalMapper.findPending(security.scopeDeptId());
    }

    public long pendingCount() {
        return pending().size();
    }

    public List<ApprovalRecord> history(String bizType, Long bizId) {
        return recordMapper.findByBiz(bizType, bizId);
    }

    public List<ApprovalRecord> done() {
        return recordMapper.findDone(security.userId(), 100);
    }

    @Transactional
    public void decide(String bizType, Long bizId, boolean approve, String remark) {
        String status = approve ? "APPROVED" : "REJECTED";
        Long approverId = security.userId();
        String approverName = security.currentUser() == null ? "system" : security.currentUser().getRealName();

        switch (bizType) {
            case "LEAVE": {
                LeaveRequest lr = leaveMapper.findById(bizId);
                if (lr == null || !"PENDING".equals(lr.getStatus())) throw new BizException("单据不存在或已处理");
                assertDeptScope(lr.getDeptId());
                leaveMapper.updateApproval(bizId, status, approverId, remark);
                if (approve) leaveService.onApproved(lr);
                break;
            }
            case "OVERTIME": {
                OvertimeRequest o = overtimeMapper.findById(bizId);
                if (o == null || !"PENDING".equals(o.getStatus())) throw new BizException("单据不存在或已处理");
                assertDeptScope(o.getDeptId());
                overtimeMapper.updateApproval(bizId, status, approverId, remark);
                if (approve && "COMP".equals(o.getCompensateType())) {
                    BigDecimal days = o.getHours().divide(BigDecimal.valueOf(8), 1, RoundingMode.HALF_UP);
                    LeaveBalance b = balanceMapper.findOne(o.getEmployeeId(), o.getOtDate().getYear(), "COMP");
                    if (b == null) {
                        b = new LeaveBalance();
                        b.setEmployeeId(o.getEmployeeId());
                        b.setYearNum(o.getOtDate().getYear());
                        b.setTypeCode("COMP");
                        b.setTotalDays(days);
                        b.setUsedDays(BigDecimal.ZERO);
                        b.setRemainDays(days);
                        balanceMapper.upsert(b);
                    } else {
                        balanceMapper.addDays(b.getId(), days);
                    }
                }
                break;
            }
            case "TRIP": {
                BusinessTrip t = tripMapper.findById(bizId);
                if (t == null || !"PENDING".equals(t.getStatus())) throw new BizException("单据不存在或已处理");
                assertDeptScope(t.getDeptId());
                tripMapper.updateApproval(bizId, status, approverId, remark);
                break;
            }
            case "MAKEUP": {
                MakeupRequest m = makeupMapper.findById(bizId);
                if (m == null || !"PENDING".equals(m.getStatus())) throw new BizException("单据不存在或已处理");
                assertDeptScope(m.getDeptId());
                makeupMapper.updateApproval(bizId, status, approverId, remark);
                if (approve) attendanceService.applyMakeup(m);
                break;
            }
            case "APPEAL": {
                AttendanceAppeal a = appealMapper.findById(bizId);
                if (a == null || !"PENDING".equals(a.getStatus())) throw new BizException("单据不存在或已处理");
                assertDeptScope(a.getDeptId());
                appealMapper.updateReply(bizId, status, remark, approverId);
                if (approve) attendanceService.correctAppeal(a);
                break;
            }
            default:
                throw new BizException("未知单据类型: " + bizType);
        }

        ApprovalRecord ar = new ApprovalRecord();
        ar.setBizType(bizType);
        ar.setBizId(bizId);
        ar.setStepNo(2);
        ar.setApproverId(approverId);
        ar.setApproverName(approverName);
        ar.setAction(approve ? "APPROVE" : "REJECT");
        ar.setRemark(remark);
        recordMapper.insert(ar);
    }

    /** 部门负责人只能处理本部门（含子部门）单据；ADMIN/HR 不受限 */
    private void assertDeptScope(Long deptId) {
        Long scope = security.scopeDeptId();
        if (scope == null || deptId == null) return;
        if (scope.equals(deptId)) return;
        Department d = departmentMapper.findById(deptId);
        if (d != null && d.getParentId() != null && d.getParentId().equals(scope)) return;
        throw new BizException("无权审批其它部门的单据");
    }

    public String bizTypeName(String type) {
        switch (type) {
            case "LEAVE": return "请假";
            case "OVERTIME": return "加班";
            case "TRIP": return "出差";
            case "MAKEUP": return "补卡";
            case "APPEAL": return "异常申诉";
            default: return type;
        }
    }
}
