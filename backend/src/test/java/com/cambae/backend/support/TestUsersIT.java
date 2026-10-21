package com.cambae.backend.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
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
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 002 T080: {@link TestUsers} dựng người dùng thật cho test của 001, 003–019: quyền theo Phụ lục 27 qua
 * {@code PermissionApi} thật, phạm vi theo bản giả SPI ({@code FakeM15Config}), phiên dùng được với endpoint.
 */
@AutoConfigureMockMvc
@Import(FakeM15Config.class)
class TestUsersIT extends MySqlIntegrationTestBase {

    private static final PermissionKey VIEW_PROFILE = new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW);

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    PermissionApi permissions;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeBedAllocations bedAllocations;

    @Autowired
    MockMvc mvc;

    TestUsers users;

    @BeforeEach
    void users() {
        users = new TestUsers(jdbc, time);
    }

    @Test
    void staffUser_realMatrixAndScope() {
        TestUsers.TestUser nurse = users.staff(Role.NURSING_STAFF);
        TestUsers.TestUser kitchen = users.staff(Role.KITCHEN_STAFF);
        long floor1 = TestUsers.nextId();
        long onFloor1 = TestUsers.nextId();
        long onFloor2 = TestUsers.nextId();
        bedAllocations.allocate(
                onFloor1, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor1));
        bedAllocations.allocate(
                onFloor2,
                new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId()));
        assignments.assign(
                nurse.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor1,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));

        assertThat(permissions
                        .hasPermission(nurse.user(), VIEW_PROFILE, resident(onFloor1))
                        .allowed())
                .isTrue();
        assertThat(permissions
                        .hasPermission(nurse.user(), VIEW_PROFILE, resident(onFloor2))
                        .failedLayer())
                .contains(FailedLayer.SCOPE);
        assertThat(permissions
                        .hasPermission(kitchen.user(), VIEW_PROFILE, resident(onFloor1))
                        .failedLayer())
                .contains(FailedLayer.ROLE);
    }

    @Test
    void session_worksWithEndpoints() throws Exception {
        TestUsers.TestUser manager = users.staff(Role.NURSING_HOME_MANAGER, Role.DOCTOR);

        mvc.perform(get("/api/v1/me").header("Authorization", TestUsers.bearer(users.session(manager.accountId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(manager.accountId()))
                .andExpect(jsonPath("$.roles.length()").value(2));
    }

    @Test
    void familyUser_hasFamilyMemberId() {
        long familyMemberId = TestUsers.nextId();

        assertThat(users.family(familyMemberId).user().familyMemberId()).contains(familyMemberId);
    }

    private static Target resident(long id) {
        return new Target("M01.RESIDENT", String.valueOf(id), id, null);
    }
}
