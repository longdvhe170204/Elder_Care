package com.eldercare.backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.administration.account.api.dto.AccessChannel;
import com.eldercare.backend.administration.account.api.dto.AccountType;
import com.eldercare.backend.administration.account.api.dto.MetricGroupRestrictions;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.ReviewRecipient;
import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.AssignmentApi;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterKind;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.reporting.internal.metric.domain.MetricGroup;
import com.eldercare.backend.reporting.internal.metric.domain.MetricRecord;
import com.eldercare.backend.reporting.internal.scope.domain.ReportScope;
import com.eldercare.backend.reporting.internal.scope.service.ReportScopeService;
import com.eldercare.backend.staffing.api.dto.ShiftView;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.staffing.FakeShifts;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
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
    private final FakeShifts shifts = new FakeShifts();
    private final List<InvalidPermissionDataException> recordedErrors = new ArrayList<>();
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
        public void recordDataError(CurrentUser user, InvalidPermissionDataException error) {
            recordedErrors.add(error);
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
        public ScopeFilter assignedFilter(CurrentUser user, Set<AssignmentKind> kinds) {
            Set<Long> floors = new HashSet<>();
            Set<Long> residents = new HashSet<>();
            for (AssignmentView a : assignments.activeAt(STAFF, NOW)) {
                if (kinds.contains(a.kind()) && a.floorAreaId() != null) {
                    floors.add(a.floorAreaId());
                }
                if (kinds.contains(a.kind()) && a.residentId() != null) {
                    residents.add(a.residentId());
                }
            }
            return new ScopeFilter(false, floors, Set.of(), residents);
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
                shifts,
                parameterApi,
                new BusinessTime(Clock.fixed(NOW, BusinessTime.ZONE)));
    }

    // ca 06:00 → 14:00 giờ Việt Nam ngày 05/10
    private static final Instant SHIFT_START = Instant.parse("2026-10-04T23:00:00Z");
    private static final Instant SHIFT_END = Instant.parse("2026-10-05T07:00:00Z");

    @BeforeEach
    void shiftNine() {
        shifts.shift(new ShiftView(9L, 1L, SHIFT_START, SHIFT_END, "CLOSED", null), List.of(STAFF));
    }

    private static AssignmentView resident501(Instant from, Instant scopeEndsAt) {
        return new AssignmentView(AssignmentKind.SHIFT_MEMBER, null, null, 501L, 9L, from, SHIFT_END, scopeEndsAt);
    }

    private ReportScope nurseReport() {
        return service().period(new User(Set.of(Role.NURSING_STAFF)), MetricGroup.CARE_TASKS, OCT_1, NOW);
    }

    @Test
    void Q_61_Q_85_nurse_commonWorkOfShiftFloor_notOtherResidentsOnFloor() {
        assignments.assign(STAFF, resident501(SHIFT_START, null));

        ReportScope scope = nurseReport();

        assertThat(scope.covers(rec("common", "2026-10-05T01:00:00Z", 1, null))).isTrue();
        assertThat(scope.covers(rec("other", "2026-10-05T01:00:00Z", 1, 502L))).isFalse();
        assertThat(scope.covers(rec("otherFloor", "2026-10-05T01:00:00Z", 2, null)))
                .isFalse();
        assertThat(scope.covers(rec("outOfHours", "2026-10-05T12:00:00Z", 1, null)))
                .isFalse();
    }

    @Test
    void Q_195_shiftLead_wholeFloorOnlyDuringLedShift() {
        assignments.assign(
                STAFF, new AssignmentView(AssignmentKind.SHIFT_LEAD, 1L, null, null, 9L, SHIFT_START, SHIFT_END));

        ReportScope scope = nurseReport();

        assertThat(scope.covers(rec("inShift", "2026-10-05T01:00:00Z", 1, 502L)))
                .isTrue();
        assertThat(scope.covers(rec("outOfHours", "2026-10-05T12:00:00Z", 1, 502L)))
                .isFalse();
    }

    @Test
    void Q_61_dashboard_commonWorkOfCurrentShift() {
        Instant start = NOW.minusSeconds(3600);
        Instant end = NOW.plusSeconds(3600);
        shifts.shift(new ShiftView(12L, 1L, start, end, "IN_PROGRESS", null), List.of(STAFF));
        assignments.assign(STAFF, new AssignmentView(AssignmentKind.SHIFT_MEMBER, null, null, 501L, 12L, start, end));
        currentFilter = new ScopeFilter(false, Set.of(), Set.of(), Set.of(501L));

        ReportScope scope = service().dashboard(new User(Set.of(Role.NURSING_STAFF)), MetricGroup.CARE_TASKS);

        assertThat(scope.covers(rec("own", "2026-10-24T02:00:00Z", 1, 501L))).isTrue();
        assertThat(scope.covers(rec("common", "2026-10-24T02:00:00Z", 1, null))).isTrue();
        assertThat(scope.covers(rec("other", "2026-10-24T02:00:00Z", 1, 502L))).isFalse();
        assertThat(service()
                        .dashboard(new User(Set.of(Role.NURSING_STAFF)), MetricGroup.CONTAINMENT_ZONES)
                        .covers(rec("zone", "2026-10-24T02:00:00Z", 1, null)))
                .isTrue();
    }

    private static Rec rec(String key, String at, long floor, Long resident) {
        return new Rec(key, Instant.parse(at), floor, resident);
    }
}
