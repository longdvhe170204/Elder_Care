package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.m14.internal.metric.domain.GroupScope;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricGroupAccess;
import com.cambae.backend.m15.account.api.dto.MetricGroupRestrictions;
import com.cambae.backend.m15.account.api.dto.MetricGroupRestrictions.Kind;
import com.cambae.backend.m15.account.api.dto.MetricGroupRestrictions.Restriction;
import com.cambae.backend.m15.account.api.dto.ReportChannel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bảng FR-010 (016 T004, {@code QuyenNhomChiTieu}). */
class MetricGroupAccessTest {

    @Test
    void FR_010_managerSeesEveryGroupFacilityWide() {
        for (MetricGroup g : MetricGroup.values()) {
            assertThat(MetricGroupAccess.scopes(Set.of(Role.NURSING_HOME_MANAGER), g))
                    .containsExactly(GroupScope.FACILITY);
        }
    }

    @Test
    void FR_014_officeStaffHasNoHealthOrMedicationGroups_FR_042a_onlyNonHealthIncidents() {
        Set<Role> office = Set.of(Role.OFFICE_STAFF);
        assertThat(MetricGroupAccess.scopes(office, MetricGroup.HEALTH_EVENTS)).isEmpty();
        assertThat(MetricGroupAccess.scopes(office, MetricGroup.MEDICATION_DOSES))
                .isEmpty();
        assertThat(MetricGroupAccess.scopes(office, MetricGroup.CARE_TASKS)).isEmpty();
        assertThat(MetricGroupAccess.scopes(office, MetricGroup.NON_HEALTH_INCIDENTS))
                .containsExactly(GroupScope.FACILITY);
    }

    @Test
    void FR_010_accountantOnlyCostsAndBalances() {
        assertThat(MetricGroupAccess.visibleGroups(Set.of(Role.ACCOUNTANT)))
                .containsExactlyInAnyOrder(MetricGroup.INCURRED_COSTS, MetricGroup.BALANCES);
    }

    @Test
    void FR_010_caregiverKitchenFamilyResidentHaveNothing() {
        assertThat(MetricGroupAccess.visibleGroups(
                        Set.of(Role.CAREGIVER, Role.KITCHEN_STAFF, Role.FAMILY_MEMBER, Role.RESIDENT)))
                .isEmpty();
    }

    @Test
    void FR_010_floorManagerAndNurseCells() {
        Set<Role> tt = Set.of(Role.FLOOR_MANAGER);
        assertThat(MetricGroupAccess.scopes(tt, MetricGroup.WAITING_LIST)).isEmpty();
        assertThat(MetricGroupAccess.scopes(tt, MetricGroup.PER_STAFF)).containsExactly(GroupScope.FLOOR_STAFF);
        assertThat(MetricGroupAccess.scopes(tt, MetricGroup.STAFFING_SHIFTS))
                .containsExactlyInAnyOrder(GroupScope.ASSIGNED_FLOORS, GroupScope.FLOOR_STAFF);

        Set<Role> nurse = Set.of(Role.NURSING_STAFF);
        assertThat(MetricGroupAccess.scopes(nurse, MetricGroup.CARE_TASKS)).containsExactly(GroupScope.ASSIGNMENT);
        assertThat(MetricGroupAccess.scopes(nurse, MetricGroup.HANDOVERS)).containsExactly(GroupScope.ASSIGNMENT_AREA);
        // Quy tắc 3: "P" ở dòng lịch ca không mở chỉ tiêu tổng hợp của ca
        assertThat(MetricGroupAccess.scopes(nurse, MetricGroup.STAFFING_RATIO)).isEmpty();
        assertThat(MetricGroupAccess.scopes(nurse, MetricGroup.STAFFING_SHIFTS)).isEmpty();
    }

    @Test
    void FR_010a_roleRestriction_appliesBeforeUnion_accountRestrictionAfter() {
        Restriction removeBedsDashboard = new Restriction("BEDS", ReportChannel.DASHBOARD, Kind.REMOVE_OPERATION);
        MetricGroupRestrictions byOfficeRole =
                new MetricGroupRestrictions(Map.of(Role.OFFICE_STAFF, List.of(removeBedsDashboard)), List.of());

        Set<Role> office = Set.of(Role.OFFICE_STAFF);
        assertThat(MetricGroupAccess.scopes(office, MetricGroup.BEDS, ReportChannel.DASHBOARD, byOfficeRole))
                .isEmpty();
        assertThat(MetricGroupAccess.scopes(office, MetricGroup.BEDS, ReportChannel.REPORT, byOfficeRole))
                .containsExactly(GroupScope.FACILITY);
        // vai trò khác của cùng tài khoản vẫn cho ô của nó
        assertThat(MetricGroupAccess.scopes(
                        Set.of(Role.OFFICE_STAFF, Role.FLOOR_MANAGER),
                        MetricGroup.BEDS,
                        ReportChannel.DASHBOARD,
                        byOfficeRole))
                .containsExactly(GroupScope.ASSIGNED_FLOORS);

        MetricGroupRestrictions byAccount = new MetricGroupRestrictions(Map.of(), List.of(removeBedsDashboard));
        assertThat(MetricGroupAccess.scopes(
                        Set.of(Role.OFFICE_STAFF, Role.FLOOR_MANAGER),
                        MetricGroup.BEDS,
                        ReportChannel.DASHBOARD,
                        byAccount))
                .isEmpty();
    }

    @Test
    void FR_010a_lowerScope_facilityBecomesAssignment_allGroupsWhenGroupEmpty() {
        MetricGroupRestrictions lowerAll =
                new MetricGroupRestrictions(Map.of(), List.of(new Restriction(null, null, Kind.LOWER_SCOPE)));

        assertThat(MetricGroupAccess.scopes(
                        Set.of(Role.DOCTOR), MetricGroup.HEALTH_EVENTS, ReportChannel.REPORT, lowerAll))
                .containsExactly(GroupScope.ASSIGNMENT);
        assertThat(MetricGroupAccess.visibleGroups(Set.of(Role.ACCOUNTANT), ReportChannel.DASHBOARD, lowerAll))
                .containsExactlyInAnyOrder(MetricGroup.INCURRED_COSTS, MetricGroup.BALANCES);
    }

    @Test
    void FR_015_multipleRolesAreUnioned() {
        assertThat(MetricGroupAccess.scopes(Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF), MetricGroup.CARE_TASKS))
                .containsExactlyInAnyOrder(GroupScope.ASSIGNED_FLOORS, GroupScope.ASSIGNMENT);
        assertThat(MetricGroupAccess.scopes(Set.of(Role.OFFICE_STAFF, Role.DOCTOR), MetricGroup.BEDS))
                .containsExactly(GroupScope.FACILITY);
    }
}
