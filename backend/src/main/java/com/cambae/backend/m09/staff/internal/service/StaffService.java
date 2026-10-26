package com.cambae.backend.m09.staff.internal.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.data.BusinessCommand;
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

/** T006/T033: hồ sơ và lệnh nhân sự; không tự tạo hoặc mở lại tài khoản. */
@Service
public class StaffService {
    private final StaffStore staff;
    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;
    private final ObjectProvider<ShiftApi> shifts;
    private final AuditLogApi audit;
    private final com.cambae.backend.m09.duty.internal.service.DepartmentDutyService departments;
    private final com.cambae.backend.m09.staff.internal.persistence.StaffEmploymentStore employment;
    private final StaffEmploymentEffects employmentEffects;

    public StaffService(
            StaffStore staff,
            JdbcTemplate jdbc,
            BusinessTime time,
            ObjectProvider<CurrentUser> users,
            ObjectProvider<ShiftApi> shifts,
            AuditLogApi audit,
            com.cambae.backend.m09.duty.internal.service.DepartmentDutyService departments,
            com.cambae.backend.m09.staff.internal.persistence.StaffEmploymentStore employment,
            StaffEmploymentEffects employmentEffects) {
        this.staff = staff;
        this.jdbc = jdbc;
        this.time = time;
        this.users = users;
        this.shifts = shifts;
        this.audit = audit;
        this.departments = departments;
        this.employment = employment;
        this.employmentEffects = employmentEffects;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public void terminateEmployment(long id, java.time.Instant effectiveAt, Long replacementStaffId, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        if (effectiveAt == null || effectiveAt.isBefore(time.now()))
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EFFECTIVE_TIME", "Hiệu lực không được sớm hơn hiện tại");
        java.util.stream.Stream.of(id, replacementStaffId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(staff::lock);
        var person = staff.find(id).orElseThrow();
        if (!"WORKING".equals(person.status()))
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Nhân viên đã nghỉ việc");
        if (departments.isLead(id)) {
            if (replacementStaffId == null)
                throw new BusinessException(
                        HttpStatus.CONFLICT, "REPLACEMENT_LEAD_REQUIRED", "Cần chỉ định người thay Phụ trách bộ phận");
            departments.replaceLead(id, replacementStaffId, reason);
        }
        employment.request(id, effectiveAt, reason.value());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("STAFF_TERMINATION_REQUESTED", false))
                .entity("M09.STAFF", id)
                .reason(reason)
                .after(Map.of("effectiveAt", effectiveAt))
                .build());
        if (!effectiveAt.isAfter(time.now())) employmentEffects.apply(id, effectiveAt);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public void rehire(long id, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        var person = staff.lock(id);
        if (!"RESIGNED".equals(person.status()))
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATE", "Nhân viên chưa ở trạng thái Nghỉ việc");
        employment.status(id, "WORKING", time.now());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("STAFF_REHIRED", false))
                .entity("M09.STAFF", id)
                .before(Map.of("status", "RESIGNED"))
                .after(Map.of("status", "WORKING"))
                .reason(reason)
                .build());
    }

    @BusinessCommand
    @RequiresPermission(function = Function.STAFF_PROFILE, operation = Operation.UPDATE)
    @Transactional
    public void changeDepartment(long id, long departmentId, Long replacementStaffId, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        java.util.stream.Stream.of(id, replacementStaffId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(staff::lock);
        var before = staff.find(id).orElseThrow();
        validateDepartment(departmentId);
        if (jdbc.queryForObject("SELECT COUNT(*) FROM department WHERE id=? FOR UPDATE", Integer.class, departmentId)
                == 0) throw new NotFoundException();
        var account = employmentEffects.account(id);
        if (account.map(a -> a.roles().contains(Role.NURSING_HOME_MANAGER)).orElse(false))
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_DEPARTMENT", "Quản lý viện không thuộc bộ phận");
        if (Objects.equals(before.departmentId(), departmentId)) return;
        if (departments.isLead(id)) {
            if (replacementStaffId == null)
                throw new BusinessException(
                        HttpStatus.CONFLICT, "REPLACEMENT_LEAD_REQUIRED", "Cần chỉ định người thay Phụ trách bộ phận");
            departments.replaceLead(id, replacementStaffId, reason);
        }
        var at = com.cambae.backend.m09.shift.internal.persistence.ShiftStore.sql(time.now());
        jdbc.update("UPDATE staff_department SET ends_at=? WHERE staff_id=? AND ends_at IS NULL", at, id);
        jdbc.update(
                "INSERT INTO staff_department(staff_id,department_id,starts_at,actor_id,reason) VALUES (?,?,?,?,?)",
                id,
                departmentId,
                at,
                users.getObject().accountId(),
                reason.value());
        jdbc.update("UPDATE staff_profile SET version=version+1 WHERE id=?", id);
        var previous = new LinkedHashMap<String, Object>();
        previous.put("departmentId", before.departmentId());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("STAFF_DEPARTMENT_CHANGED", false))
                .entity("M09.STAFF", id)
                .before(previous)
                .after(Map.of("departmentId", departmentId))
                .reason(reason)
                .build());
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
        return withAccount(result);
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
                .map(this::withAccount)
                .toList();
    }

    private StaffProfile withAccount(StaffProfile profile) {
        Long accountId = employmentEffects
                .account(profile.id())
                .map(AccountView::accountId)
                .orElse(null);
        if (Objects.equals(accountId, profile.accountId())) return profile;
        return new StaffProfile(
                profile.id(),
                profile.fullName(),
                profile.position(),
                profile.specialty(),
                profile.startDate(),
                profile.phoneNumber(),
                profile.email(),
                profile.status(),
                accountId,
                profile.source(),
                profile.importBatchId(),
                profile.departmentId(),
                profile.version());
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
