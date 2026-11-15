package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions;
import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions.Kind;
import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions.Restriction;
import com.eldercare.backend.administration.account.api.dto.ReportChannel;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.reporting.internal.metric.domain.GroupScope;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroupAccess;
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
}
