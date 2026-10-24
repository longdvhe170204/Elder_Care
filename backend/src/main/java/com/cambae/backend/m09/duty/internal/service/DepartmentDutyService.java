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
