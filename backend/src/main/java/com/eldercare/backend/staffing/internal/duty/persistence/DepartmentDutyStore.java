package com.eldercare.backend.staffing.internal.duty.persistence;

import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.staffing.internal.duty.domain.Department;
import com.eldercare.backend.staffing.internal.duty.domain.DepartmentDuty;
import com.eldercare.backend.staffing.internal.persistence.StaffingCatalogStore;
import com.eldercare.backend.staffing.internal.persistence.StaffingHistoryStore;
import com.eldercare.backend.staffing.internal.shift.persistence.ShiftStore;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** T050: dữ liệu nhân viên dùng FK vật lý; danh mục/lịch sử qua API chung và id logic. */
@Repository
public class DepartmentDutyStore {
    private final JdbcTemplate jdbc;
    private final StaffingCatalogStore catalogs;
    private final StaffingHistoryStore histories;

    public DepartmentDutyStore(JdbcTemplate jdbc, StaffingCatalogStore catalogs, StaffingHistoryStore histories) {
        this.jdbc = jdbc;
        this.catalogs = catalogs;
        this.histories = histories;
    }

    public List<Department> all() {
        return catalogs.departments().stream()
                .map(r -> new Department(
                        catalogs.departmentId(r),
                        r.name(),
                        r.attributes().leadStaffId(),
                        r.status() == CatalogStatus.ACTIVE))
                .toList();
    }

    public Department lock(long id) {
        var r = catalogs.requireDepartment(id, true);
        return new Department(
                catalogs.departmentId(r), r.name(), r.attributes().leadStaffId(), r.status() == CatalogStatus.ACTIVE);
    }

    public long add(String name, boolean active, Instant at) {
        return catalogs.addDepartment(name, active, at);
    }

    public Optional<Long> lead(long id) {
        return catalogs.department(id)
                .filter(r -> r.status() == CatalogStatus.ACTIVE)
                .map(r -> r.attributes().leadStaffId());
    }

    public List<DepartmentDuty> byLead(long person, boolean lock) {
        var matching = all().stream()
                .filter(d -> d.active() && Objects.equals(d.leadStaffId(), person))
                .toList();
        if (lock) matching.stream().map(Department::id).sorted().forEach(this::lock);
        return (lock ? all() : matching)
                .stream()
                        .filter(d -> d.active() && Objects.equals(d.leadStaffId(), person))
                        .map(d -> new DepartmentDuty(d.id(), d.name(), person))
                        .toList();
    }

    public List<Long> departments(long person, Instant at) {
        return jdbc
                .queryForList(
                        "SELECT department_id FROM staff_department WHERE staff_id=? AND starts_at<=? AND (ends_at IS NULL OR ends_at>?)",
                        Long.class,
                        person,
                        ShiftStore.sql(at),
                        ShiftStore.sql(at))
                .stream()
                .map(catalogs::departmentLogicalId)
                .toList();
    }

    public void replace(long department, long replacement, Instant at, long actor, String reason) {
        catalogs.departmentLead(department, replacement);
        histories.replaceDepartmentLead(department, replacement, at, actor, reason);
    }

    public List<Long> activeWithoutLead() {
        return all().stream()
                .filter(d -> d.active() && d.leadStaffId() == null)
                .map(Department::id)
                .toList();
    }
}
