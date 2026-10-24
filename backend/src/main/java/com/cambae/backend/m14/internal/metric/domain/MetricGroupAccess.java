package com.cambae.backend.m14.internal.metric.domain;

import static com.cambae.backend.m14.internal.metric.domain.GroupScope.ASSIGNED_FLOORS;
import static com.cambae.backend.m14.internal.metric.domain.GroupScope.ASSIGNMENT;
import static com.cambae.backend.m14.internal.metric.domain.GroupScope.ASSIGNMENT_AREA;
import static com.cambae.backend.m14.internal.metric.domain.GroupScope.FACILITY;
import static com.cambae.backend.m14.internal.metric.domain.GroupScope.FLOOR_STAFF;

import com.cambae.backend.common.Role;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Bảng FR-010 ({@code QuyenNhomChiTieu}, research O-03): nhóm chỉ tiêu vai trò được thấy và phạm vi. Ô "—" không có
 * trong bảng; Nhân viên chăm sóc, Nhân viên bếp, Người thân, Người cao tuổi không có dòng nào. Người phụ trách ca dùng
 * cột Điều dưỡng. Thu hẹp của 002 FR-025 áp sau bảng này (FR-010a).
 */
public final class MetricGroupAccess {

    private static final Map<Role, Map<MetricGroup, Set<GroupScope>>> TABLE = new EnumMap<>(Role.class);

    static {
        Map<MetricGroup, Set<GroupScope>> manager = new EnumMap<>(MetricGroup.class);
        for (MetricGroup g : MetricGroup.values()) {
            manager.put(g, EnumSet.of(FACILITY));
        }
        TABLE.put(Role.NURSING_HOME_MANAGER, manager);

        TABLE.put(
                Role.FLOOR_MANAGER,
                row(
                        MetricGroup.RESIDENTS, ASSIGNED_FLOORS,
                        MetricGroup.BEDS, ASSIGNED_FLOORS,
                        MetricGroup.CARE_TASKS, ASSIGNED_FLOORS,
                        MetricGroup.PER_STAFF, FLOOR_STAFF,
                        MetricGroup.BED_AWAITING_READY, ASSIGNED_FLOORS,
                        MetricGroup.MEAL_TICKETS, ASSIGNED_FLOORS,
                        MetricGroup.ACTIVITIES, ASSIGNED_FLOORS,
                        MetricGroup.QUALITY_CHECKS, ASSIGNED_FLOORS,
                        MetricGroup.HEALTH_EVENTS, ASSIGNED_FLOORS,
                        MetricGroup.NON_HEALTH_INCIDENTS, ASSIGNED_FLOORS,
                        MetricGroup.OVERDUE_REASSESSMENTS, ASSIGNED_FLOORS,
                        MetricGroup.MEDICATION_DOSES, ASSIGNED_FLOORS,
                        MetricGroup.CONTAINMENT_ZONES, ASSIGNED_FLOORS,
                        MetricGroup.HANDOVERS, ASSIGNED_FLOORS,
                        MetricGroup.STAFFING_RATIO, ASSIGNED_FLOORS,
                        MetricGroup.STAFF_LICENSES, FLOOR_STAFF,
                        MetricGroup.ASSETS, ASSIGNED_FLOORS,
                        MetricGroup.CRITICAL_FLAGS, ASSIGNED_FLOORS));
        TABLE.get(Role.FLOOR_MANAGER).put(MetricGroup.STAFFING_SHIFTS, EnumSet.of(ASSIGNED_FLOORS, FLOOR_STAFF));

        TABLE.put(
                Role.DOCTOR,
                row(
                        MetricGroup.RESIDENTS, FACILITY,
                        MetricGroup.CARE_TASKS, FACILITY,
                        MetricGroup.HEALTH_EVENTS, FACILITY,
                        MetricGroup.NON_HEALTH_INCIDENTS, FACILITY,
                        MetricGroup.OVERDUE_REASSESSMENTS, FACILITY,
                        MetricGroup.MEDICATION_DOSES, FACILITY,
                        MetricGroup.MEDICATION_RECONCILIATIONS, FACILITY,
                        MetricGroup.CONTAINMENT_ZONES, FACILITY,
                        MetricGroup.HANDOVERS, FACILITY,
                        MetricGroup.INVENTORY, FACILITY,
                        MetricGroup.CRITICAL_FLAGS, FACILITY));

        TABLE.put(
                Role.NURSING_STAFF,
                row(
                        MetricGroup.RESIDENTS, ASSIGNMENT,
                        MetricGroup.CARE_TASKS, ASSIGNMENT,
                        MetricGroup.HEALTH_EVENTS, ASSIGNMENT,
                        MetricGroup.NON_HEALTH_INCIDENTS, ASSIGNMENT,
                        MetricGroup.OVERDUE_REASSESSMENTS, ASSIGNMENT,
                        MetricGroup.MEDICATION_DOSES, ASSIGNMENT,
                        MetricGroup.MEDICATION_RECONCILIATIONS, ASSIGNMENT,
                        MetricGroup.CONTAINMENT_ZONES, ASSIGNMENT_AREA,
                        MetricGroup.HANDOVERS, ASSIGNMENT_AREA,
                        MetricGroup.CRITICAL_FLAGS, ASSIGNMENT));

        TABLE.put(
                Role.OFFICE_STAFF,
                row(
                        MetricGroup.RESIDENTS, FACILITY,
                        MetricGroup.WAITING_LIST, FACILITY,
                        MetricGroup.BEDS, FACILITY,
                        MetricGroup.NON_HEALTH_INCIDENTS, FACILITY,
                        MetricGroup.CONTAINMENT_ZONES, FACILITY,
                        MetricGroup.INCURRED_COSTS, FACILITY,
                        MetricGroup.BALANCES, FACILITY,
                        MetricGroup.RESIDENCE_REGISTRATION, FACILITY,
                        MetricGroup.ASSETS, FACILITY));

        // Kế toán: chỉ hai nhóm chi phí phát sinh, số dư (Q-212, Phụ lục 27 chú thích 29)
        TABLE.put(Role.ACCOUNTANT, row(MetricGroup.INCURRED_COSTS, FACILITY, MetricGroup.BALANCES, FACILITY));
    }

    private MetricGroupAccess() {}

    /** Phạm vi của nhóm với tài khoản nhiều vai trò: hợp các vai trò (FR-015); rỗng là không thấy nhóm. */
    public static Set<GroupScope> scopes(Set<Role> roles, MetricGroup group) {
        Set<GroupScope> result = EnumSet.noneOf(GroupScope.class);
        for (Role role : roles) {
            result.addAll(TABLE.getOrDefault(role, Map.of()).getOrDefault(group, Set.of()));
        }
        return result;
    }

    /** Các nhóm vai trò thấy được (cho {@code GET /metrics}, khối dashboard). */
    public static Set<MetricGroup> visibleGroups(Set<Role> roles) {
        Set<MetricGroup> result = EnumSet.noneOf(MetricGroup.class);
        for (Role role : roles) {
            result.addAll(TABLE.getOrDefault(role, Map.of()).keySet());
        }
        return result;
    }

    private static Map<MetricGroup, Set<GroupScope>> row(Object... pairs) {
        Map<MetricGroup, Set<GroupScope>> row = new EnumMap<>(MetricGroup.class);
        for (int i = 0; i < pairs.length; i += 2) {
            row.put((MetricGroup) pairs[i], EnumSet.of((GroupScope) pairs[i + 1]));
        }
        return row;
    }
}
