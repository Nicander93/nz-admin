package com.nz.admin.modules.demo.service;

import cn.hutool.core.util.IdUtil;
import com.nz.admin.common.core.BusinessException;
import com.nz.admin.common.module.NzWorkflowBusinessHandler;
import com.nz.admin.common.module.NzWorkflowBusinessLauncher;
import com.nz.admin.framework.auth.core.LoginUser;
import com.nz.admin.framework.auth.core.LoginUserContext;
import com.nz.admin.framework.tenant.core.TenantContextHolder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 请假示例保留单据归属校验，并演示顺序、幂等的业务回调。 */
@Service
public class LeaveApplicationService implements NzWorkflowBusinessHandler {
    private final JdbcTemplate jdbc;
    private final LoginUserContext users;
    private final ObjectProvider<NzWorkflowBusinessLauncher> launchers;
    public LeaveApplicationService(JdbcTemplate jdbc, LoginUserContext users,
                                   ObjectProvider<NzWorkflowBusinessLauncher> launchers) {
        this.jdbc = jdbc;
        this.users = users;
        this.launchers = launchers;
    }
    private LoginUser identity() {
        var user = users.getLoginUserOrNull();
        if (user == null || !Objects.equals(user.getTenantId(), TenantContextHolder.getTenantIdOrNull()))
            throw new BusinessException("缺少可信登录租户身份");
        return user;
    }
    public List<Map<String, Object>> list() {
        var user = identity();
        return jdbc.queryForList("SELECT id,reason,start_date::text AS \"startDate\",end_date::text AS \"endDate\",flow_code AS \"flowCode\",status,instance_id AS \"instanceId\" FROM demo_leave WHERE tenant_id=? AND applicant_id=? ORDER BY create_time DESC LIMIT 200", user.getTenantId(), user.getUserId());
    }
    @Transactional
    public String save(String id, String reason, LocalDate start, LocalDate end, String code) {
        var user = identity();
        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) > 365) throw new BusinessException("请假日期需在一年内且结束日期不早于开始日期");
        if (id == null) {
            id = IdUtil.fastSimpleUUID();
            jdbc.update("INSERT INTO demo_leave(id,tenant_id,applicant_id,reason,start_date,end_date,flow_code,status,create_time,update_time,last_sequence) VALUES(?,?,?,?,?,?,?,'DRAFT',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,0)", id, user.getTenantId(), user.getUserId(), reason, start, end, code);
        } else {
            var row = owned(id, user);
            if (!Set.of("DRAFT", "REJECTED", "CANCELLED").contains(row.get("status").toString())) throw new BusinessException("审批中的申请不能修改");
            if ("REJECTED".equals(row.get("status")) && !Objects.equals(code, row.get("flow_code")))
                throw new BusinessException("退回的申请继续使用原流程，请新建申请以更换流程");
            jdbc.update("UPDATE demo_leave SET reason=?,start_date=?,end_date=?,flow_code=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?", reason, start, end, code, id, user.getTenantId());
        }
        return id;
    }
    @Transactional
    public String submit(String id) {
        var user = identity();
        var row = owned(id, user);
        if ("PENDING".equals(row.get("status"))) return row.get("instance_id").toString();
        if (!Set.of("DRAFT", "REJECTED", "CANCELLED").contains(row.get("status").toString())) throw new BusinessException("申请不能再次提交");
        var launcher = launchers.getIfAvailable();
        if (launcher == null) throw new BusinessException(503, "新引擎尚未启用");
        long days = ChronoUnit.DAYS.between(((java.sql.Date) row.get("start_date")).toLocalDate(), ((java.sql.Date) row.get("end_date")).toLocalDate()) + 1;
        String instance = launcher.start(businessType(), id, row.get("flow_code").toString(), Map.of("days", days));
        boolean resumed = Objects.equals(instance, row.get("instance_id"));
        jdbc.update("UPDATE demo_leave SET status='PENDING',instance_id=?,last_sequence=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?",
                instance, resumed ? row.get("last_sequence") : 0, id, user.getTenantId());
        return instance;
    }
    private Map<String, Object> owned(String id, LoginUser user) {
        var rows = jdbc.queryForList("SELECT * FROM demo_leave WHERE id=? AND tenant_id=? AND applicant_id=? FOR UPDATE", id, user.getTenantId(), user.getUserId());
        if (rows.isEmpty()) throw new BusinessException("申请不存在或无权访问");
        return rows.get(0);
    }
    @Override
    public String businessType() { return "leave"; }
    @Override
    public String businessName() { return "请假申请"; }
    public java.util.List<NzWorkflowBusinessLauncher.Definition> flows() {
        identity();
        var launcher = launchers.getIfAvailable();
        return launcher == null ? java.util.List.of() : launcher.available(businessType());
    }
    @Override
    public Map<String, Object> detail(String tenant, String id) {
        var rows = jdbc.queryForList("SELECT reason,start_date,end_date,applicant_id FROM demo_leave WHERE tenant_id=? AND id=?", Long.valueOf(tenant), id);
        if (rows.isEmpty()) return Map.of();
        var row = rows.get(0);
        return Map.of("申请原因", row.get("reason"), "开始日期", row.get("start_date").toString(),
                "结束日期", row.get("end_date").toString(), "申请人", row.get("applicant_id").toString());
    }

    @Override
    @Transactional
    public void apply(Event event) {
        String status = switch (event.flowStatus()) {
            case "2", "3", "8" -> "APPROVED";
            case "9" -> (event.nodeType() == 0 || event.nodeType() == 2) ? "REJECTED" : "PENDING";
            case "4", "5", "6", "7", "10", "11" -> "CANCELLED";
            default -> "PENDING";
        };
        // 回调不使用请求上下文身份，按可信事件的租户、当前实例与序号一起限定。
        jdbc.update("UPDATE demo_leave SET status=?,last_sequence=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=? AND instance_id=? AND last_sequence<?",
                status, event.sequence(), event.businessId(), Long.valueOf(event.tenantId()), event.instanceId(), event.sequence());
    }
}
