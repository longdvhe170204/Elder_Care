package com.cambae.backend.m09.duty.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m09.staff.internal.domain.StaffInput;
import com.cambae.backend.m09.staff.internal.persistence.StaffStore;
import com.cambae.backend.m15.account.api.spi.StaffProfileApi;
import com.cambae.backend.m15.account.api.spi.StaffStatus;
import com.cambae.backend.m15.auditlog.api.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T007: bản cài hồ sơ cho 002; không cấp tài khoản theo chức danh tự do. */
@Service
@Transactional(readOnly = true)
class StaffDirectory implements StaffProfileApi {
    private final StaffStore staff;
    private final BusinessTime time;
    private final AuditLogApi audit;
    private static final Set<Role> STAFF_ROLES = Set.of(
            Role.NURSING_HOME_MANAGER,
            Role.FLOOR_MANAGER,
            Role.DOCTOR,
            Role.NURSING_STAFF,
            Role.CAREGIVER,
            Role.KITCHEN_STAFF,
            Role.OFFICE_STAFF,
            Role.ACCOUNTANT);
    private static final Map<String, Role> POSITIONS = Map.ofEntries(
            Map.entry("quản lý viện", Role.NURSING_HOME_MANAGER),
            Map.entry("trưởng tầng", Role.FLOOR_MANAGER),
            Map.entry("bác sĩ", Role.DOCTOR),
            Map.entry("điều dưỡng", Role.NURSING_STAFF),
            Map.entry("nhân viên chăm sóc", Role.CAREGIVER),
            Map.entry("nhân viên bếp", Role.KITCHEN_STAFF),
            Map.entry("hành chính", Role.OFFICE_STAFF),
            Map.entry("kế toán", Role.ACCOUNTANT));

    StaffDirectory(StaffStore staff, BusinessTime time, AuditLogApi audit) {
        this.staff = staff;
        this.time = time;
        this.audit = audit;
    }

    public StaffStatus status(long staffId, LocalDate date) {
        return StaffStatus.valueOf(
                staff.find(staffId).orElseThrow(NotFoundException::new).status());
    }

    public Optional<Role> positionRole(long staffId) {
        String position = staff.find(staffId)
                .orElseThrow(NotFoundException::new)
                .position()
                .strip();
        Role mapped = POSITIONS.get(position.toLowerCase(Locale.ROOT));
        if (mapped != null) return Optional.of(mapped);
        try {
            Role role = Role.valueOf(position);
            return STAFF_ROLES.contains(role) ? Optional.of(role) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Optional<String> fullName(long staffId) {
        return staff.find(staffId).map(s -> s.fullName());
    }

    public Optional<String> phoneNumber(long staffId) {
        return staff.find(staffId).map(s -> s.phoneNumber());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public long createMinimal(String fullName, Role positionRole) {
        if (!STAFF_ROLES.contains(positionRole) || fullName == null || fullName.isBlank())
            throw new IllegalArgumentException("Hồ sơ tối thiểu cần họ tên và chức danh nhân viên");
        long id = staff.insert(
                new StaffInput(fullName, positionRole.name(), null, null, null, null, null), null, time.now());
        audit.writeAsBootstrap(AuditLogEntry.builder()
                .action(new AuditAction("STAFF_CREATED", false))
                .entity("M09.STAFF", id)
                .after(Map.of("fullName", fullName, "position", positionRole.name()))
                .basis(new Basis("BR-M15-22", null, null, null))
                .build());
        return id;
    }
}
