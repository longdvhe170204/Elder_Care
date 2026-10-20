package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.ApproverApi;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T054 (contracts mục 1.5; 000 BR-M15-09, FR-031a): người duyệt là tài khoản Hoạt động có quyền vai trò hoặc nhiệm
 * vụ và phạm vi bao đối tượng tại lúc hỏi; tài khoản đã khóa, người ngoài phạm vi không có trong danh sách; ngoại lệ ghi
 * sự cố FR-044a không áp.
 */
@Import(FakeM15Config.class)
class ApproverApiIT extends MySqlIntegrationTestBase {

    private static final PermissionKey HANDLE_INCIDENT =
            new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE);
    private static final PermissionKey APPROVE_SWAP = new PermissionKey(Function.SHIFT_SWAP_REQUEST, Operation.APPROVE);

    @Autowired
    ApproverApi approvers;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void BR_M15_09_activeInScopeOnly_lockedAndOutOfScopeExcluded() {
        AccountFixtures fixtures = new AccountFixtures(jdbc, time);
        long floor = AccountFixtures.nextId();
        long resident = AccountFixtures.nextId();
        bedAllocations.allocate(
                resident, new BedAllocationView(AccountFixtures.nextId(), AccountFixtures.nextId(), 1, floor));
        long inScope = fixtures.staffAccount("AC-05");
        long locked = fixtures.staffAccount("AC-05");
        long outOfScope = fixtures.staffAccount("AC-05");
        for (long account : new long[] {inScope, locked}) {
            assignments.assign(
                    fixtures.staffId(account),
                    new AssignmentView(
                            AssignmentKind.SHIFT_MEMBER,
                            floor,
                            null,
                            null,
                            null,
                            time.now().minus(Duration.ofDays(1)),
                            null));
        }
        jdbc.update("UPDATE account SET status = 'LOCKED', lock_cause = 'MANUAL' WHERE id = ?", locked);
        Target target = new Target("M01.RESIDENT", String.valueOf(resident), resident, null);

        assertThat(approvers.approvers(HANDLE_INCIDENT, target))
                .contains(inScope)
                .doesNotContain(locked, outOfScope);
        assertThat(approvers.hasAnyApprover(HANDLE_INCIDENT, target)).isTrue();
    }

    @Test
    void BR_M15_09_noAccountWithPermission_noApprover() {
        Target nowhere = new Target("M09.SHIFT_SWAP", "1", null, AccountFixtures.nextId());

        assertThat(approvers.approvers(APPROVE_SWAP, nowhere))
                .allSatisfy(id -> assertThat(jdbc.queryForObject(
                                "SELECT COUNT(*) FROM role_assignment WHERE account_id = ? AND role = 'AC-01'"
                                        + " AND effective_to IS NULL",
                                Integer.class,
                                id))
                        .isEqualTo(1));
    }
}
