package com.eldercare.backend.medication;

import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.account.api.spi.LicenseValidity;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeBedAllocations;
import com.eldercare.backend.support.fake.administration.FakeFacilityLicenses;
import com.eldercare.backend.support.fake.administration.FakeLicenses;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Nền cho test tích hợp gọi thẳng service của 006: lệnh có {@code @RequiresPermission} cần người dùng hiện tại, nên
 * dùng người dùng thật của 002 ({@link TestUsers}) như {@code HealthTestBase} của 001. Mặc định là Bác sĩ; test của
 * điều dưỡng gọi {@code user.set(nurse.user())}.
 */
@Import({FakeM15Config.class, SwitchableUserConfig.class})
abstract class M07IntegrationTestBase extends MySqlIntegrationTestBase {

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

    @Autowired
    protected FakeLicenses licenses;

    @Autowired
    protected FakeFacilityLicenses facilityLicenses;

    /** Cùng mã với {@code LegalConditionIT} của 002, để hai lớp test không ghi đè nhau trên schema test. */
    static final String PRESCRIBING_LICENSE = "PRACTICE_LICENSE";

    protected TestUser doctor;
    protected TestUser nurse;

    @BeforeEach
    void m07Users() {
        TestUsers users = new TestUsers(jdbc, time);
        doctor = users.staff(Role.DOCTOR);
        nurse = users.staff(Role.NURSING_STAFF);
        user.set(doctor.user());
        licensedToPrescribe(doctor);
    }

    /**
     * Bác sĩ có quyền kê đơn hợp lệ (FR-005, BR-M06-05): Quản lý viện đã cấu hình loại giấy phép của "chẩn đoán và kê
     * đơn nội bộ", giấy phép cơ sở có phạm vi đó và giấy phép của bác sĩ còn hiệu lực.
     */
    protected void licensedToPrescribe(TestUser who) {
        jdbc.update(
                "UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.requiredLicenseType', ?)"
                        + " WHERE catalog_type = 'M15.PROFESSIONAL_ACTIVITY' AND code = ?",
                PRESCRIBING_LICENSE,
                ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING);
        facilityLicenses.scope(
                ProfessionalActivityApi.INTERNAL_DIAGNOSIS_PRESCRIBING,
                time.today().plusYears(1));
        licenses.license(
                who.staffId(),
                PRESCRIBING_LICENSE,
                new LicenseValidity(
                        TestUsers.nextId(), "Giấy phép hành nghề", time.today().plusYears(1), Set.of()));
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
