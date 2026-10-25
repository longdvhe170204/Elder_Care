package com.cambae.backend.m01;

import com.cambae.backend.common.Role;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.internal.profile.domain.IdDocumentType;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import com.cambae.backend.m01.internal.profile.service.ResidentProfileService;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.PermissionResult;
import com.cambae.backend.m15.account.api.dto.ScopeFilter;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.support.FakePermissions;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Nền chung cho test hồ sơ người cao tuổi của 001 US1 (T014 → T016): người dùng đổi được giữa các bước, quyền theo
 * dòng "Hồ sơ người cao tuổi" của Phụ lục 27 (Hành chính T, Bác sĩ T, Nhân viên chăm sóc P, Nhân viên bếp không có) và
 * phạm vi theo phân công do test đặt qua {@link ScopedPermissions#assign}. Mỗi test dùng CCCD, họ tên riêng vì cơ sở dữ
 * liệu không dọn giữa các test.
 */
@Import(ResidentProfileTestBase.Fixtures.class)
@RecordApplicationEvents
public abstract class ResidentProfileTestBase extends MySqlIntegrationTestBase {

    protected static final CurrentUser MANAGER = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);
    protected static final CurrentUser OFFICE = FakePermissions.user(2, Role.OFFICE_STAFF);
    protected static final CurrentUser DOCTOR = FakePermissions.user(3, Role.DOCTOR);
    protected static final CurrentUser CAREGIVER = FakePermissions.user(4, Role.CAREGIVER);
    protected static final CurrentUser KITCHEN = FakePermissions.user(5, Role.KITCHEN_STAFF);

    protected static final LocalDate BORN = LocalDate.of(1948, 3, 14);

    private static final AtomicLong SEQUENCE = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    /** Bản giả quyền có phạm vi theo phân công: quyền P chỉ đạt với hồ sơ được {@link #assign}. */
    public static class ScopedPermissions extends FakePermissions {

        private final Set<Long> assigned = new HashSet<>();

        public void assign(long residentId) {
            assigned.add(residentId);
        }

        private boolean facilityWide(CurrentUser user, PermissionKey key) {
            return super.hasPermission(user, key, Target.UNSPECIFIED).allowed();
        }

        @Override
        public ScopeFilter filter(CurrentUser user, PermissionKey key) {
            if (facilityWide(user, key)) {
                return ScopeFilter.FACILITY;
            }
            return new ScopeFilter(false, Set.of(), Set.of(), Set.copyOf(assigned));
        }

        @Override
        public PermissionResult hasPermission(CurrentUser user, PermissionKey key, Target target) {
            PermissionResult result = super.hasPermission(user, key, target);
            boolean facilityWide = facilityWide(user, key);
            if (result.allowed()
                    && !facilityWide
                    && target.residentId() != null
                    && !assigned.contains(target.residentId())) {
                return PermissionResult.denied(FailedLayer.SCOPE);
            }
            return result;
        }
    }

    @TestConfiguration
    static class Fixtures {

        @Bean
        SwitchableUser currentUser() {
            return new SwitchableUser();
        }

        @Bean
        ScopedPermissions permissions() {
            ScopedPermissions permissions = new ScopedPermissions();
            for (CurrentUser user : new CurrentUser[] {MANAGER, OFFICE, DOCTOR, CAREGIVER, KITCHEN}) {
                permissions.account(user);
            }
            for (Operation operation : new Operation[] {Operation.VIEW, Operation.CREATE, Operation.UPDATE}) {
                permissions.grant(Role.OFFICE_STAFF, new PermissionKey(Function.RESIDENT_PROFILE, operation));
                permissions.grant(Role.NURSING_HOME_MANAGER, new PermissionKey(Function.RESIDENT_PROFILE, operation));
            }
            permissions.grant(Role.DOCTOR, new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW));
            permissions.grantAssigned(Role.CAREGIVER, new PermissionKey(Function.RESIDENT_PROFILE, Operation.VIEW));
            return permissions;
        }
    }

    @Autowired
    protected ResidentProfileService profiles;

    @Autowired
    protected SwitchableUser user;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ScopedPermissions permissions;

    /** CCCD 12 chữ số chưa dùng. */
    protected static String uniqueCitizenId() {
        return String.format("%012d", SEQUENCE.incrementAndGet());
    }

    /** Họ tên chưa dùng, vẫn là họ tên tiếng Việt có dấu. */
    protected static String uniqueName() {
        return "Nguyễn Văn Thử " + SEQUENCE.incrementAndGet();
    }

    /** Thông tin tối thiểu (FR-001): họ tên, ngày sinh, giới tính, không giấy tờ. */
    protected static PersonalInfo noDocument(String fullName) {
        return personal(fullName, BORN, Gender.MALE, null, null);
    }

    protected static PersonalInfo withCitizenId(String fullName, String citizenId) {
        return personal(fullName, BORN, Gender.MALE, IdDocumentType.CITIZEN_ID, citizenId);
    }

    protected static PersonalInfo personal(
            String fullName, LocalDate dateOfBirth, Gender gender, IdDocumentType type, String number) {
        return new PersonalInfo(fullName, dateOfBirth, gender, type, number, null, null, null, null, null);
    }

    /** Đặt trạng thái trực tiếp ở cơ sở dữ liệu: lệnh chuyển trạng thái thuộc 004. */
    protected void setStatus(long residentId, String status) {
        jdbc.update("UPDATE resident SET status = ? WHERE id = ?", status, residentId);
    }

    /** Tạo hồ sơ với tư cách Hành chính. */
    protected ResidentProfileService.CreateResult createAsOffice(PersonalInfo info) {
        user.set(OFFICE);
        return profiles.create(info, false);
    }
}
