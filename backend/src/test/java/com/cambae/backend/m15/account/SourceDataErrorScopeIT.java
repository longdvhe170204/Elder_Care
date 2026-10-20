package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.BedAllocationView;
import com.cambae.backend.m15.account.api.FailedLayer;
import com.cambae.backend.m15.account.api.Function;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.PermissionBasis;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.PermissionResult;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.account.api.events.PermissionDataError;
import com.cambae.backend.m15.account.auth.internal.ActiveSession;
import com.cambae.backend.m15.account.auth.internal.SessionAuthentication;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * 002 T051 (US4 #13; FR-041b, CHK058, CHK059): phân công trỏ tới tầng đã ngừng hiệu lực nên không xác định được phạm
 * vi: xem hồ sơ, ghi nhận công việc bị từ chối lớp dữ liệu nguồn và Quản lý viện được báo một lần cho lỗi đó; ghi nhận
 * sự cố vẫn được (FR-044a). Gộp lỗi, đánh dấu khắc phục, giữ lỗi khi lệnh rollback ở {@code PermissionDataErrorIT}.
 */
@Import(FakeM15Config.class)
@RecordApplicationEvents
class SourceDataErrorScopeIT extends MySqlIntegrationTestBase {

    @Autowired
    PermissionApi permissions;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ApplicationEvents events;

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void US4_13_assignmentToInactiveFloor_viewAndRecordDenied_reportedOnce_incidentAllowed() {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long account = fixtures.staffAccount("AC-05");
        long staffId = fixtures.staffId(account);
        ActiveSession user = fixtures.user(account);
        SecurityContextHolder.getContext().setAuthentication(new SessionAuthentication(user));
        long resident = AccountFixtures.nextId();
        bedAllocations.allocate(
                resident,
                new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), 1, AccountFixtures.nextId()));
        assignments.invalid(staffId, "FLOOR_INACTIVE");
        Target target = new Target("M01.RESIDENT", String.valueOf(resident), resident, null);

        assertThat(permissions
                        .hasPermission(user, new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW), target)
                        .failedLayer())
                .contains(FailedLayer.SOURCE_DATA);
        assertThat(permissions
                        .hasPermission(user, new PermissionKey(Function.TASK_CHECKLIST, Operation.CREATE), target)
                        .failedLayer())
                .contains(FailedLayer.SOURCE_DATA);
        PermissionResult incident = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE), target);
        assertThat(incident.allowed()).isTrue();
        assertThat(incident.basis()).contains(PermissionBasis.OUT_OF_SCOPE);

        assertThat(events.stream(PermissionDataError.class).filter(e -> e.recordId() == staffId))
                .containsExactly(new PermissionDataError("M09.ASSIGNMENT", staffId, "FLOOR_INACTIVE"));
        assertThat(jdbc.queryForObject(
                        "SELECT occurrences FROM permission_data_error WHERE record_id = ? AND fixed_at IS NULL",
                        Integer.class,
                        staffId))
                .isGreaterThanOrEqualTo(2);
    }
}
