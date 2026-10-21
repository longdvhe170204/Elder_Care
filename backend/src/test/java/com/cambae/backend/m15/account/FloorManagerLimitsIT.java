package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.m15.account.internal.auth.domain.ActiveSession;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T082 (FR-023; 19.3, 13.3): Trưởng tầng không duyệt thay đổi lưu trú, chi phí và không thay đổi y lệnh, kê đơn,
 * kể cả với người cao tuổi trên tầng mình được giao (lớp vai trò từ chối).
 */
@Import(FakeM15Config.class)
class FloorManagerLimitsIT extends MySqlIntegrationTestBase {

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

    @ParameterizedTest
    @CsvSource({
        "RESIDENCY_CHANGE_REQUEST, APPROVE",
        "COST_ADJUSTMENT, APPROVE",
        "PRESCRIPTION, CREATE",
        "PRESCRIPTION, UPDATE"
    })
    void FR_023_floorManager_deniedAtRoleLayer_evenOnOwnFloor(Function function, Operation operation) {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long floor = AccountFixtures.nextId();
        long resident = AccountFixtures.nextId();
        bedAllocations.allocate(
                resident,
                new BedAllocationView(
                        AccountFixtures.nextId(), AccountFixtures.nextId(), AccountFixtures.nextId(), floor));
        long account = fixtures.staffAccount("AC-02");
        assignments.assign(
                fixtures.staffId(account),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
        ActiveSession manager = fixtures.user(account);

        assertThat(permissions
                        .hasPermission(
                                manager,
                                new PermissionKey(function, operation),
                                new Target("M01.RESIDENT", String.valueOf(resident), resident, null))
                        .failedLayer())
                .contains(FailedLayer.ROLE);
    }
}
