package com.cambae.backend.m01;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m15.account.api.dto.Target;
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
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Nền chung cho test sức khỏe của 001 US2 (T024 → T026): người dùng thật của 002 ({@link TestUsers}), nên quyền theo
 * Phụ lục 27 qua {@code PermissionApi} thật (Bác sĩ toàn viện, Điều dưỡng theo phân công). Phạm vi lấy từ bản giả SPI của
 * {@link FakeM15Config}; {@link #assignNurse} đặt phân công cho điều dưỡng. Mỗi test tạo hồ sơ người cao tuổi riêng vì cơ
 * sở dữ liệu không dọn giữa các test.
 */
@Import({FakeM15Config.class, HealthTestBase.Fixtures.class})
@RecordApplicationEvents
public abstract class HealthTestBase extends MySqlIntegrationTestBase {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.nanoTime() % 1_000_000_000L);

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
    protected ResidentProfileService profiles;

    @Autowired
    protected FakeAssignments assignments;

    @Autowired
    protected FakeBedAllocations bedAllocations;

    protected TestUsers users;
    protected TestUser doctor;
    protected TestUser nurse;
    protected TestUser office;

    @BeforeEach
    void healthUsers() {
        users = new TestUsers(jdbc, time);
        doctor = users.staff(Role.DOCTOR);
        nurse = users.staff(Role.NURSING_STAFF);
        office = users.staff(Role.OFFICE_STAFF);
    }

    /** Hồ sơ người cao tuổi mới ở Đang lưu trú (lệnh chuyển trạng thái thuộc 004, nên đặt thẳng ở cơ sở dữ liệu). */
    protected long newResident() {
        user.set(office.user());
        long id = profiles.create(
                        new PersonalInfo(
                                "Nguyễn Văn Sức Khỏe " + SEQUENCE.incrementAndGet(),
                                LocalDate.of(1945, 4, 2),
                                Gender.MALE,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        false)
                .profile()
                .id();
        jdbc.update("UPDATE resident SET status = 'IN_RESIDENCE' WHERE id = ?", id);
        return id;
    }

    protected void setStatus(long residentId, String status) {
        jdbc.update("UPDATE resident SET status = ? WHERE id = ?", status, residentId);
    }

    /** Cho điều dưỡng thuộc ca của tầng chứa người cao tuổi (quyền theo phân công, AC-04). */
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

    protected static Target target(long residentId) {
        return ResidentProfileService.target(residentId);
    }

    protected long severityId() {
        return jdbc.queryForObject("SELECT id FROM allergy_severity WHERE name = 'Nhẹ'", Long.class);
    }

    protected long sourceId() {
        return jdbc.queryForObject("SELECT id FROM information_source WHERE name = 'Người thân'", Long.class);
    }

    /** Dị nguyên chọn từ danh mục, tên riêng cho mỗi lần gọi. */
    protected long allergenId() {
        jdbc.update(
                "INSERT INTO allergen (name, allergen_group) VALUES (?, 'ACTIVE_INGREDIENT')",
                "Hoạt chất " + UUID.randomUUID());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
