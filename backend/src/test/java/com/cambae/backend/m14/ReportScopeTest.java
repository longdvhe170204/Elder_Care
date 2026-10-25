package com.cambae.backend.m14;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m14.internal.metric.domain.MetricGroup;
import com.cambae.backend.m14.internal.metric.domain.MetricRecord;
import com.cambae.backend.m14.internal.scope.domain.ReportScope;
import com.cambae.backend.m14.internal.scope.service.ReportScopeService;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.cambae.backend.m15.account.api.dto.AccessChannel;
import com.cambae.backend.m15.account.api.dto.AccountType;
import com.cambae.backend.m15.account.api.dto.MetricGroupRestrictions;
import com.cambae.backend.m15.account.api.dto.MetricGroupRestrictions.Kind;
import com.cambae.backend.m15.account.api.dto.MetricGroupRestrictions.Restriction;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.PermissionResult;
import com.cambae.backend.m15.account.api.dto.ReportChannel;
import com.cambae.backend.m15.account.api.dto.ReviewRecipient;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentApi;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.parameter.api.ParameterApi;
import com.cambae.backend.m15.parameter.api.ParameterCode;
import com.cambae.backend.m15.parameter.api.ParameterKind;
import com.cambae.backend.m15.parameter.api.ParameterValue;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

/** Phạm vi báo cáo (016 T004, {@code PhamViBaoCaoTest}; research O-03): FR-010a, FR-012, FR-015, FR-016, FR-017. */
class ReportScopeTest {

    private static final Instant NOW = Instant.parse("2026-10-24T03:00:00Z");
    private static final Instant OCT_1 = Instant.parse("2026-09-30T17:00:00Z");
    private static final long STAFF = 77L;

    record User(Set<Role> roles) implements CurrentUser {
        @Override
        public long accountId() {
            return 1L;
        }

        @Override
        public AccountType type() {
            return AccountType.STAFF;
        }

        @Override
        public Optional<Long> staffId() {
            return Optional.of(STAFF);
        }

        @Override
        public Optional<Long> familyMemberId() {
            return Optional.empty();
        }

        @Override
        public Optional<Long> residentId() {
            return Optional.empty();
        }

        @Override
        public boolean isScheduler() {
            return false;
        }

        @Override
        public String deviceId() {
            return "d";
        }

        @Override
        public AccessChannel channel() {
            return AccessChannel.WEB;
        }
    }

    record Rec(String sourceKey, Instant referenceTime, Long floorAreaId, Long residentId) implements MetricRecord {}

    private final FakeAssignments assignments = new FakeAssignments();
    private Set<PermissionKey> keys = Set.of(ReportScopeService.KEY);
    private ScopeFilter currentFilter = ScopeFilter.FACILITY;
    private MetricGroupRestrictions restrictions = MetricGroupRestrictions.NONE;

    private final PermissionApi permissionApi = new PermissionApi() {
        @Override
        public PermissionResult hasPermission(CurrentUser user, PermissionKey key, Target target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PermissionResult hasPermissionAt(CurrentUser user, PermissionKey key, Target target, Instant at) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<PermissionKey> permissions(CurrentUser user) {
            return keys;
        }

        @Override
        public MetricGroupRestrictions metricGroupRestrictions(CurrentUser user) {
            return restrictions;
        }

        @Override
        public Set<String> allowedCommands(CurrentUser user, Target target, Map<String, PermissionKey> commands) {
            throw new UnsupportedOperationException();
        }
    };

    private final ScopeApi scopeApi = new ScopeApi() {
        @Override
        public boolean inScope(CurrentUser user, Target target) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScopeFilter filter(CurrentUser user, PermissionKey key) {
            assignments.history(STAFF, NOW, NOW.plusNanos(1)); // dữ liệu nguồn lỗi ném như bản thật
            return currentFilter;
        }

        @Override
        public ScopeFilter historyFilter(CurrentUser user, PermissionKey key, Instant from, Instant to) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Long> shiftLead(long floorAreaId, Instant at) {
            return Optional.empty();
        }

        @Override
        public Optional<Long> assignedFloorManager(long floorAreaId) {
            return Optional.empty();
        }

        @Override
        public ReviewRecipient reviewDecider(Optional<Long> floorAreaId) {
            throw new UnsupportedOperationException();
        }
    };

    private final ParameterApi parameterApi = new ParameterApi() {
        @Override
        public ParameterValue read(ParameterCode code) {
            return new ParameterValue(
                    code,
                    ParameterKind.DURATION,
                    JsonMapper.builder().build().readTree("{\"value\":2,\"unit\":\"HOURS\"}"));
        }

        @Override
        public ParameterValue readAt(ParameterCode code, Instant at) {
            return read(code);
        }
    };

    private ReportScopeService service() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory(Map.of("assignments", assignments));
        return new ReportScopeService(
                permissionApi,
                scopeApi,
                beans.getBeanProvider(AssignmentApi.class),
                parameterApi,
                new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));
    }

    private static Rec rec(String key, String at, long floor, Long resident) {
        return new Rec(key, Instant.parse(at), floor, resident);
    }

    @Test
    void FR_010_roleWithoutGroupIsDenied() {
        assertThatThrownBy(() -> service().dashboard(new User(Set.of(Role.OFFICE_STAFF)), MetricGroup.HEALTH_EVENTS))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_010a_restrictedDashboardReportKeyIsDenied() {
        keys = Set.of();
        assertThatThrownBy(
                        () -> service().dashboard(new User(Set.of(Role.NURSING_HOME_MANAGER)), MetricGroup.RESIDENTS))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void FR_012_managerIsFacilityWide() {
        ReportScope scope =
                service().period(new User(Set.of(Role.NURSING_HOME_MANAGER)), MetricGroup.CARE_TASKS, OCT_1, NOW);
        assertThat(scope.facilityWide()).isTrue();
    }

    @Test
    void FR_012_floorManager_currentFloorsAtAnyTime_formerFloorNotVisible() {
        assignments.assign(
                STAFF,
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        1L,
                        null,
                        null,
                        null,
                        Instant.parse("2026-10-20T00:00:00Z"),
                        null));
        assignments.assign(
                STAFF,
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        2L,
                        null,
                        null,
                        null,
                        Instant.parse("2026-09-01T00:00:00Z"),
                        Instant.parse("2026-10-10T00:00:00Z")));
        currentFilter = new ScopeFilter(false, Set.of(1L), Set.of(), Set.of());

        ReportScope scope = service().period(new User(Set.of(Role.FLOOR_MANAGER)), MetricGroup.CARE_TASKS, OCT_1, NOW);

        // bản ghi tầng 1 trước ngày được giao vẫn tính; tầng 2 đã thôi giao thì không
        assertThat(scope.covers(rec("a", "2026-10-02T01:00:00Z", 1, null))).isTrue();
        assertThat(scope.covers(rec("b", "2026-10-02T01:00:00Z", 2, null))).isFalse();
        assertThat(scope.rough().floorAreaIds()).containsExactly(1L);
    }

    @Test
    void FR_012_nurse_onlyAssignedResidentsWithinShiftHoursPlusMargin() {
        // ca 06:00 → 14:00 giờ Việt Nam ngày 05/10, phân công người cao tuổi 501; CFG-M15-07 = 2 giờ
        assignments.assign(
                STAFF,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        501L,
                        9L,
                        Instant.parse("2026-10-04T23:00:00Z"),
                        Instant.parse("2026-10-05T07:00:00Z")));
        currentFilter = new ScopeFilter(false, Set.of(), Set.of(), Set.of());

        ReportScope scope = service().period(new User(Set.of(Role.NURSING_STAFF)), MetricGroup.CARE_TASKS, OCT_1, NOW);

        assertThat(scope.covers(rec("in", "2026-10-05T08:30:00Z", 1, 501L))).isTrue();
        assertThat(scope.covers(rec("early", "2026-10-04T21:30:00Z", 1, 501L))).isTrue();
        assertThat(scope.covers(rec("after", "2026-10-05T09:30:00Z", 1, 501L))).isFalse();
        assertThat(scope.covers(rec("other", "2026-10-05T01:00:00Z", 1, 502L))).isFalse();
    }

    @Test
    void FR_015_floorManagerAndNurse_unionOfBothParts() {
        assignments.assign(
                STAFF,
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        1L,
                        null,
                        null,
                        null,
                        Instant.parse("2026-10-20T00:00:00Z"),
                        null));
        assignments.assign(
                STAFF,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        null,
                        null,
                        501L,
                        9L,
                        Instant.parse("2026-10-04T23:00:00Z"),
                        Instant.parse("2026-10-05T07:00:00Z")));
        currentFilter = new ScopeFilter(false, Set.of(1L), Set.of(), Set.of(501L));

        ReportScope scope = service()
                .period(new User(Set.of(Role.FLOOR_MANAGER, Role.NURSING_STAFF)), MetricGroup.CARE_TASKS, OCT_1, NOW);

        assertThat(scope.covers(rec("floor1", "2026-10-03T01:00:00Z", 1, null))).isTrue();
        assertThat(scope.covers(rec("r501", "2026-10-05T01:00:00Z", 3, 501L))).isTrue();
    }

    @Test
    void FR_010a_facilityCellLoweredByRestriction_isComputedAsAssignment() {
        assignments.assign(
                STAFF,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        4L,
                        null,
                        null,
                        9L,
                        Instant.parse("2026-10-04T23:00:00Z"),
                        Instant.parse("2026-10-05T07:00:00Z")));
        currentFilter = new ScopeFilter(false, Set.of(4L), Set.of(), Set.of());

        ReportScope scope = service().period(new User(Set.of(Role.DOCTOR)), MetricGroup.HEALTH_EVENTS, OCT_1, NOW);

        assertThat(scope.facilityWide()).isFalse();
        assertThat(scope.covers(rec("x", "2026-10-05T01:00:00Z", 4, null))).isTrue();
        assertThat(scope.covers(rec("y", "2026-10-05T01:00:00Z", 5, null))).isFalse();
    }

    @Test
    void FR_010a_groupRemovedOnDashboardOnly_reportStillOpen() {
        restrictions = new MetricGroupRestrictions(
                Map.of(), List.of(new Restriction("RESIDENTS", ReportChannel.DASHBOARD, Kind.REMOVE_OPERATION)));
        User manager = new User(Set.of(Role.NURSING_HOME_MANAGER));

        assertThatThrownBy(() -> service().dashboard(manager, MetricGroup.RESIDENTS))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(service().period(manager, MetricGroup.RESIDENTS, OCT_1, NOW).facilityWide())
                .isTrue();
    }

    @Test
    void FR_010a_groupLoweredOnDashboard_currentAssignmentsOnly() {
        restrictions = new MetricGroupRestrictions(
                Map.of(), List.of(new Restriction("HEALTH_EVENTS", null, Kind.LOWER_SCOPE)));
        // ca đang diễn ra quanh NOW, phân công tầng 4
        assignments.assign(
                STAFF,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        4L,
                        null,
                        null,
                        9L,
                        NOW.minusSeconds(3600),
                        NOW.plusSeconds(3600)));

        ReportScope scope = service().dashboard(new User(Set.of(Role.DOCTOR)), MetricGroup.HEALTH_EVENTS);

        assertThat(scope.facilityWide()).isFalse();
        assertThat(scope.covers(rec("x", "2026-10-24T02:00:00Z", 4, null))).isTrue();
        assertThat(scope.covers(rec("y", "2026-10-24T02:00:00Z", 5, null))).isFalse();
        assertThat(service()
                        .dashboard(new User(Set.of(Role.DOCTOR)), MetricGroup.CARE_TASKS)
                        .facilityWide())
                .isTrue();
    }

    @Test
    void FR_020_dashboardWithoutAssignments_isEmpty() {
        currentFilter = new ScopeFilter(false, Set.of(), Set.of(), Set.of());
        assertThat(service()
                        .dashboard(new User(Set.of(Role.NURSING_STAFF)), MetricGroup.CARE_TASKS)
                        .isEmpty())
                .isTrue();
    }

    @Test
    void FR_016_invalidSourceData_deniedWithSourceDataLayer() {
        assignments.invalid(STAFF, "CONFLICTING_ASSIGNMENT");

        assertThatThrownBy(() -> service().dashboard(new User(Set.of(Role.NURSING_STAFF)), MetricGroup.CARE_TASKS))
                .isInstanceOfSatisfying(
                        PermissionDeniedException.class,
                        e -> assertThat(e.properties()).containsEntry("failedLayer", "SOURCE_DATA"));
    }
}
