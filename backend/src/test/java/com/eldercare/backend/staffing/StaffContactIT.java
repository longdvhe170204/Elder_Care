package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.spi.StaffProfileApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@code StaffProfileApi.email}, {@code updateContact} (002 module-interfaces, bổ sung 2026-11-09; cho {@code PUT
 * /me/contact}): ghi số điện thoại, email lên hồ sơ nhân viên, khóa tra cứu số, tăng phiên bản, nhật ký STAFF_UPDATED
 * có trước và sau mang tài khoản người sửa. Cùng tổ hợp context với DepartmentCommandsIT.
 */
@Import(SwitchableUserConfig.class)
class StaffContactIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser current;

    @Autowired
    StaffProfileApi profiles;

    @Test
    void updateContact_writesProfileAndAudit_nullClears() {
        var user = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        long staffId = user.staffId();
        jdbc.update(
                "INSERT INTO staff_profile(id,full_name,position,phone_number,email,account_id) VALUES (?,'[Nhân viên liên hệ]','NURSING_STAFF','0912000111','cu@vien.vn',?)",
                staffId,
                user.accountId());
        current.set(user.user());
        int version = jdbc.queryForObject("SELECT version FROM staff_profile WHERE id=?", Integer.class, staffId);

        profiles.updateContact(staffId, "0987654321", "moi@vien.vn");

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT phone_number, normalized_phone, email, version FROM staff_profile WHERE id=?", staffId);
        assertThat(row)
                .containsEntry("phone_number", "0987654321")
                .containsEntry("normalized_phone", "0987654321")
                .containsEntry("email", "moi@vien.vn")
                .containsEntry("version", version + 1);
        assertThat(profiles.email(staffId)).contains("moi@vien.vn");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action='STAFF_UPDATED' AND entity_id=? AND actor_id=?",
                        Integer.class,
                        String.valueOf(staffId),
                        user.accountId()))
                .isOne();

        profiles.updateContact(staffId, null, null);
        assertThat(profiles.phoneNumber(staffId)).isEmpty();
        assertThat(profiles.email(staffId)).isEmpty();
    }
}
