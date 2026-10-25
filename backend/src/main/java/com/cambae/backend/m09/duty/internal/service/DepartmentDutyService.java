package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.*;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.duty.internal.persistence.DepartmentDutyStore;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.*;
import com.cambae.backend.m15.account.api.dto.AccountStatus;
import com.cambae.backend.m15.auditlog.api.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T021: phần bộ phận của DutyApi; chỉ bảng M09 và API tài khoản, tham gia transaction lệnh gọi. */
@Service
public class DepartmentDutyService {
    private final DepartmentDutyStore store;
    private final StaffStore staff;
    private final AccountApi accounts;
    private final ObjectProvider<CurrentUser> users;
    private final BusinessTime time;
    private final AuditLogApi audit;

    public DepartmentDutyService(
            DepartmentDutyStore store,
            StaffStore staff,
            AccountApi accounts,
            ObjectProvider<CurrentUser> users,
            BusinessTime time,
            AuditLogApi audit) {
        this.store = store;
        this.staff = staff;
        this.accounts = accounts;
        this.users = users;
        this.time = time;
        this.audit = audit;
    }

    @RequiresPermission(
            function = com.cambae.backend.m15.account.api.dto.Function.STAFF_PROFILE,
            operation = com.cambae.backend.m15.account.api.dto.Operation.VIEW)
    public List<com.cambae.backend.m09.duty.internal.domain.Department> list() {
        return store.all();
    }

    @RequiresPermission(
            function = com.cambae.backend.m15.account.api.dto.Function.STAFF_PROFILE,
            operation = com.cambae.backend.m15.account.api.dto.Operation.CREATE)
    @Transactional
    public long create(String name, boolean active) {
        if (name == null || name.isBlank() || name.strip().length() > 200) throw invalid("Tên bộ phận không hợp lệ");
        long id = store.add(name, active, time.now());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("DEPARTMENT_CREATED", false))
                .entity("M09.DEPARTMENT", id)
                .after(Map.of("name", name.strip(), "active", active))
                .build());
        return id;
    }

    @com.cambae.backend.common.data.BusinessCommand
    @RequiresPermission(
            function = com.cambae.backend.m15.account.api.dto.Function.STAFF_PROFILE,
            operation = com.cambae.backend.m15.account.api.dto.Operation.UPDATE)
    @Transactional
    public void assignLead(long department, long personId, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        staff.lock(personId);
        var dept = store.lock(department);
        var person = staff.find(personId).orElseThrow(NotFoundException::new);
        var account = accounts.byStaff(personId).orElseThrow(() -> invalid("Người phụ trách chưa có tài khoản"));
        if (!dept.active()
                || !"WORKING".equals(person.status())
                || account.status() != AccountStatus.ACTIVE
                || !departmentOf(personId, time.now()).equals(Optional.of(department))
                || !Collections.disjoint(account.roles(), Set.of(Role.NURSING_HOME_MANAGER, Role.ACCOUNTANT)))
            throw invalid("Người phụ trách phải cùng bộ phận, đang làm việc, tài khoản Hoạt động và vai trò được phép");
        if ("Hành chính – Kế toán".equalsIgnoreCase(dept.name())
                && !account.roles().contains(Role.OFFICE_STAFF))
            throw invalid("Phụ trách Hành chính – Kế toán phải có vai trò Hành chính");
        var before = new LinkedHashMap<String, Object>();
        before.put("leadStaffId", dept.leadStaffId());
        store.replace(department, personId, time.now(), users.getObject().accountId(), reason.value());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("DEPARTMENT_LEAD_ASSIGNED", false))
                .entity("M09.DEPARTMENT", department)
                .before(before)
                .after(Map.of("leadStaffId", personId))
                .reason(reason)
                .build());
    }

    public Optional<Long> lead(long department) {
        return store.lead(department);
    }

    public boolean isLead(long person) {
        return !store.byLead(person, false).isEmpty();
    }

    public Optional<Long> departmentOf(long person, Instant at) {
        var found = store.departments(person, at);
        if (found.size() > 1)
            throw new com.cambae.backend.m15.account.api.spi.InvalidPermissionDataException(
                    "M09.STAFF_DEPARTMENT", "MULTIPLE_DEPARTMENTS", person);
        return found.stream().findFirst();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void replaceLead(long current, long replacement, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        if (current == replacement) throw invalid("Người thay phải khác người đang phụ trách");
        java.util.stream.LongStream.of(current, replacement).sorted().forEach(staff::lock);
        var departments = store.byLead(current, true);
        if (departments.size() != 1) throw invalid("Không xác định được bộ phận hiện đang phụ trách");
        long department = departments.getFirst().id();
        var person = staff.find(replacement).orElseThrow(NotFoundException::new);
        var account = accounts.byStaff(replacement).orElseThrow(() -> invalid("Người thay chưa có tài khoản"));
        if (!"WORKING".equals(person.status())
                || account.status() != AccountStatus.ACTIVE
                || !departmentOf(replacement, time.now()).equals(Optional.of(department))
                || !Collections.disjoint(account.roles(), Set.of(Role.NURSING_HOME_MANAGER, Role.ACCOUNTANT)))
            throw invalid("Người thay phải đang làm việc, cùng bộ phận, tài khoản Hoạt động và có vai trò được phép");
        String name = departments.getFirst().name();
        if ("Hành chính – Kế toán".equalsIgnoreCase(name) && !account.roles().contains(Role.OFFICE_STAFF))
            throw invalid("Phụ trách Hành chính – Kế toán phải có vai trò Hành chính");
        Instant now = time.now();
        long actor = users.getObject().accountId();
        store.replace(department, replacement, now, actor, reason.value());
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("DEPARTMENT_LEAD_REPLACED", false))
                .entity("M09.DEPARTMENT", department)
                .before(Map.of("leadStaffId", current))
                .after(Map.of("leadStaffId", replacement))
                .reason(reason)
                .details(Map.of("basis", "Q-292"))
                .build());
    }

    private static BusinessException invalid(String detail) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_REPLACEMENT_DEPARTMENT_LEAD", detail);
    }
}
