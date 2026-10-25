package com.cambae.backend.m07;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.m15.account.api.spi.BedAllocationView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.TestUsers.TestUser;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeBedAllocations;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Nền cho test tích hợp gọi thẳng service của 006: lệnh có {@code @RequiresPermission} cần người dùng hiện tại, nên
 * dùng người dùng thật của 002 ({@link TestUsers}) như {@code HealthTestBase} của 001. Mặc định là Bác sĩ; test của
 * điều dưỡng gọi {@code user.set(nurse.user())}.
 */
@Import({FakeM15Config.class, M07IntegrationTestBase.Fixtures.class})
abstract class M07IntegrationTestBase extends MySqlIntegrationTestBase {

    @TestConfiguration
    static class Fixtures {

        @Bean
        SwitchableUser currentUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    protected SwitchableUser user;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected BusinessTime time;

    @Autowired
    protected FakeAssignments assignments;

    @Autowired
    protected FakeBedAllocations bedAllocations;

    protected TestUser doctor;
    protected TestUser nurse;

    @BeforeEach
    void m07Users() {
        TestUsers users = new TestUsers(jdbc, time);
        doctor = users.staff(Role.DOCTOR);
        nurse = users.staff(Role.NURSING_STAFF);
        user.set(doctor.user());
    }

    /** Cho điều dưỡng thuộc ca của tầng chứa người cao tuổi (quyền Đơn thuốc của AC-04 theo phân công). */
    protected void assignNurse(TestUser who, long residentId) {
        long floor = TestUsers.nextId();
        bedAllocations.allocate(
                residentId, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        assignments.assign(
                who.staffId(),
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minus(Duration.ofDays(1)),
                        null));
    }
}
