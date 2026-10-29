package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import java.time.Period;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** T007: bản cài thật của M09; không import FakeM15Config để bản giả không che bản thật. */
@Transactional
class StaffSpiIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    StaffProfileApi profiles;

    @Autowired
    LicenseApi licenses;

    @Autowired
    PracticeScopeCatalogApi scopes;

    private long staff(String position) {
        jdbc.update("INSERT INTO staff_profile(full_name,position) VALUES ('[Nhân viên SPI]',?)", position);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void FR_004a_positionRoleIsPresentOnlyForSystemStaffPositions() {
        long nurse = staff("Điều dưỡng");
        assertThat(profiles.positionRole(nurse)).contains(Role.NURSING_STAFF);
        assertThat(profiles.status(nurse, time.today())).isEqualTo(StaffStatus.WORKING);
        assertThat(profiles.fullName(nurse)).contains("[Nhân viên SPI]");
        assertThat(profiles.positionRole(staff("Nhân viên vệ sinh"))).isEmpty();
    }

    @Test
    void FR_016_009_staffContactPhoneIsReadFromProfileAndAbsentWithoutContact() {
        long id = staff("NURSING_STAFF");
        assertThat(profiles.phoneNumber(id)).isEmpty();
        jdbc.update("UPDATE staff_profile SET phone_number='0900000001' WHERE id=?", id);
        assertThat(profiles.phoneNumber(id)).contains("0900000001");
        assertThat(profiles.phoneNumber(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void BR_M15_22_minimalBootstrapProfileIsCreatedInCallerTransactionWithAudit() {
        long id = profiles.createMinimal("[Quản lý viện kiểm thử]", Role.NURSING_HOME_MANAGER);
        assertThat(profiles.positionRole(id)).contains(Role.NURSING_HOME_MANAGER);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type='M09.STAFF' AND entity_id=? AND actor_type='BOOTSTRAP'",
                        Integer.class,
                        Long.toString(id)))
                .isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void BR_M15_22_minimalProfileRequiresExistingTransaction() {
        assertThatThrownBy(() -> profiles.createMinimal("[Quản lý viện kiểm thử]", Role.NURSING_HOME_MANAGER))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void FR_007_licenseIncludesExpiryDateAndLatestStillReturnsExpiredRecord() {
        long staff = staff("DOCTOR");
        var expiry = time.today();
        jdbc.update(
                "INSERT INTO credential(staff_id,credential_type_id,issue_date,expiry_date) VALUES (?,1,?,?)",
                staff,
                expiry.minusYears(1),
                expiry);
        assertThat(licenses.valid(staff, "DOCTOR_LICENSE", expiry)).isPresent();
        assertThat(licenses.valid(staff, "DOCTOR_LICENSE", expiry.plusDays(1))).isEmpty();
        assertThat(licenses.latest(staff, "DOCTOR_LICENSE")).isPresent();
        assertThat(licenses.expiringWithin(Period.ofDays(1)))
                .anyMatch(l -> l.staffId() == staff && l.licenseType().equals("DOCTOR_LICENSE"));
    }

    @Test
    void FR_005_practiceScopeKeepsTechnicalIdAndActiveState() {
        jdbc.update(
                "INSERT INTO practice_scope(code,name,status) VALUES ('TEST-SPI-SCOPE','[Phạm vi kiểm thử]','INACTIVE')");
        long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        assertThat(scopes.find(id)).hasValueSatisfying(v -> {
            assertThat(v.name()).isEqualTo("[Phạm vi kiểm thử]");
            assertThat(v.active()).isFalse();
        });
    }
}
