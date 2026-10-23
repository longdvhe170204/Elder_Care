package com.cambae.backend.m09.staff.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m09.api.ShiftApi;
import com.cambae.backend.m09.staff.internal.domain.*;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.*;
import com.cambae.backend.m15.auditlog.api.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T006: mô tả hồ sơ, không tự tạo tài khoản hoặc thực hiện lệnh nghỉ việc/đổi bộ phận. */
@Service
public class StaffService {
    private final StaffStore staff;
    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;
    private final ObjectProvider<ShiftApi> shifts;
    private final AuditLogApi audit;

    public StaffService(
            StaffStore staff,
            JdbcTemplate jdbc,
            BusinessTime time,
            ObjectProvider<CurrentUser> users,
            ObjectProvider<ShiftApi> shifts,
            AuditLogApi audit) {
        this.staff = staff;
        this.jdbc = jdbc;
        this.time = time;
        this.users = users;
        this.shifts = shifts;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.CREATE)
    @Transactional
    public StaffProfile create(StaffInput input) {
        validateDepartment(input.departmentId());
        long id = staff.insert(input, users.getObject().accountId(), time.now());
        var result = staff.find(id).orElseThrow();
        audit("STAFF_CREATED", null, result);
        return result;
    }

    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public StaffProfile update(long id, String ifMatch, StaffInput input) {
        var before = staff.lock(id);
        VersionEtag.requireMatch(ifMatch, before.version(), before);
        if (input.departmentId() != null && !Objects.equals(input.departmentId(), before.departmentId()))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "DEPARTMENT_CHANGE_REQUIRES_COMMAND", "Dùng lệnh Đổi bộ phận để thay bộ phận");
        staff.update(id, input);
        var result = staff.find(id).orElseThrow();
        audit("STAFF_UPDATED", before, result);
        return result;
    }

    @Transactional(readOnly = true)
    public StaffProfile get(long id) {
        var result = staff.find(id).orElseThrow(NotFoundException::new);
        if (!canRead(id)) throw new NotFoundException();
        return result;
    }

    @Transactional(readOnly = true)
    public List<StaffProfile> list(String q, String status, Long departmentId) {
        return staff.all().stream()
                .filter(s -> canRead(s.id()))
                .filter(s -> q == null
                        || s.fullName()
                                .toLowerCase(java.util.Locale.ROOT)
                                .contains(q.toLowerCase(java.util.Locale.ROOT)))
                .filter(s -> status == null || s.status().equals(status))
                .filter(s -> departmentId == null || Objects.equals(s.departmentId(), departmentId))
                .toList();
    }

    public boolean canRead(long id) {
        var user = users.getObject();
        if (user.roles().contains(Role.NURSING_HOME_MANAGER) || user.roles().contains(Role.FLOOR_MANAGER)) return true;
        var api = shifts.getIfAvailable();
        if (api == null || user.staffId().isEmpty()) return false;
        var now = time.now();
        var mine = api.staffShifts(user.staffId().orElseThrow(), now, now.plusNanos(1));
        var theirs = api.staffShifts(id, now, now.plusNanos(1));
        return mine.stream()
                .filter(s -> "IN_PROGRESS".equals(s.status()))
                .anyMatch(a -> theirs.stream().anyMatch(b -> a.id() == b.id()));
    }

    private void validateDepartment(Long id) {
        if (id != null
                && jdbc.queryForObject(
                                "SELECT COUNT(*) FROM department WHERE id=? AND status='ACTIVE'", Integer.class, id)
                        == 0)
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DEPARTMENT", "Bộ phận không còn hiệu lực");
    }

    private Map<String, Object> snapshot(StaffProfile s) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("fullName", s.fullName());
        values.put("position", s.position());
        values.put("specialty", s.specialty());
        values.put("startDate", s.startDate());
        values.put("phoneNumber", s.phoneNumber());
        values.put("email", s.email());
        return values;
    }

    private void audit(String action, StaffProfile before, StaffProfile after) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, false))
                .entity("M09.STAFF", after.id())
                .before(before == null ? null : snapshot(before))
                .after(snapshot(after))
                .details(Map.of("permissionBasis", "ROLE"))
                .build());
    }
}
