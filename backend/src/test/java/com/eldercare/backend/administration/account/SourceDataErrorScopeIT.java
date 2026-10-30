package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.events.PermissionDataError;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.account.internal.auth.domain.ActiveSession;
import com.eldercare.backend.administration.account.internal.auth.service.SessionAuthentication;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.util.List;
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

        assertThat(events.stream(PermissionDataError.class)
                        .filter(e -> e.recordId() == staffId)
                        .map(e -> List.of(e.source(), e.errorKind())))
                .containsExactly(List.of("M09.ASSIGNMENT", "FLOOR_INACTIVE"));
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_VALUE(content, '$.occurrences' RETURNING SIGNED) FROM simple_record WHERE kind = 'M15.PERMISSION_DATA_ERROR'"
                                + " AND JSON_VALUE(content, '$.recordId' RETURNING SIGNED) = ?"
                                + " AND status = 'OPEN'",
                        Integer.class,
                        staffId))
                .isGreaterThanOrEqualTo(2);
    }
}
