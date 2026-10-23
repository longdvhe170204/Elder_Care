package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.FieldVisibilityApi;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.api.dto.Visibility;
import com.cambae.backend.m15.account.api.spi.AssignmentKind;
import com.cambae.backend.m15.account.api.spi.AssignmentView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.m15.FakeAssignments;
import com.cambae.backend.support.fake.m15.FakeDuties;
import com.cambae.backend.support.fake.m15.FakeM15Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** Phần mở rộng A đã được người dùng duyệt: không dùng nhóm CONTACT chung cho dữ liệu nhân viên. */
@Import(FakeM15Config.class)
class StaffFieldVisibilityIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    FieldVisibilityApi visibility;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeDuties duties;

    @Test
    void FR_052_staffPrivateFieldsAreManagerOnlyEvenForStaffViewingThemself() {
        var users = new TestUsers(jdbc, time);
        var nurse = users.staff(Role.NURSING_STAFF).user();
        var target = new Target("M09.STAFF", nurse.staffId().orElseThrow().toString(), null, null);
        assertThat(visibility.canView(nurse, InfoGroup.STAFF_RESTRICTED, target))
                .isEqualTo(Visibility.NONE);
        assertThat(visibility.canView(
                        users.staff(Role.NURSING_HOME_MANAGER).user(), InfoGroup.STAFF_RESTRICTED, target))
                .isEqualTo(Visibility.FULL);
    }

    @Test
    void FR_052_floorManagerContactRequiresStaffShiftInTheirFloorDuringCurrentMonth() {
        var users = new TestUsers(jdbc, time);
        var floor = users.staff(Role.FLOOR_MANAGER).user();
        long staff = TestUsers.nextId();
        long floorId = TestUsers.nextId();
        var target = new Target("M09.STAFF", Long.toString(staff), null, null);
        assertThat(visibility.canView(floor, InfoGroup.STAFF_CONTACT, target)).isEqualTo(Visibility.NONE);
        assignments.assign(
                staff,
                new AssignmentView(
                        AssignmentKind.SHIFT_MEMBER,
                        floorId,
                        null,
                        null,
                        1L,
                        time.now(),
                        time.now().plusSeconds(3600)));
        duties.floorManager(floorId, floor.staffId().orElseThrow());
        assertThat(visibility.canView(floor, InfoGroup.STAFF_CONTACT, target)).isEqualTo(Visibility.FULL);
        assertThat(visibility.canView(floor, InfoGroup.STAFF_RESTRICTED, target))
                .isEqualTo(Visibility.NONE);
    }
}
