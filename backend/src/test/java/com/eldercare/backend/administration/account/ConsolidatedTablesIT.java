package com.eldercare.backend.administration.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 002 T098 (data-model 002 mục 0.1, "Bảo đảm"): ràng buộc của {@code permission_assignment}, {@code device_credential}
 * thay ràng buộc của năm bảng cũ; chạy bằng tài khoản {@code eldercare_app}. MySQL báo vi phạm {@code CHECK} bằng lỗi
 * 3819 (không phải lỗi toàn vẹn của Spring) nên test so tên ràng buộc.
 */
class ConsolidatedTablesIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Test
    void permissionAssignment_oneOpenRolePerAccountAndRole() {
        long account = new TestUsers(jdbc, time).staff().accountId();
        jdbc.update(
                "INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, reason)"
                        + " VALUES ('ROLE_ASSIGNMENT', 'ACCOUNT', ?, 'AC-05', UTC_TIMESTAMP(6), 'Gán')",
                account);

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, reason)"
                                + " VALUES ('ROLE_ASSIGNMENT', 'ACCOUNT', ?, 'AC-05', UTC_TIMESTAMP(6), 'Gán lại')",
                        account))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void permissionAssignment_checksByKind() {
        long account = new TestUsers(jdbc, time).staff().accountId();
        // Gán thêm chỉ Duyệt kế hoạch chăm sóc (FR-024)
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO permission_assignment (kind, applies_to, account_id, function_code, operation,"
                                + " effective_from, actor_id, reason) VALUES ('ADDITIONAL_GRANT', 'ACCOUNT', ?,"
                                + " 'INCIDENT', 'APPROVE', UTC_TIMESTAMP(6), 1, 'Gán thêm')",
                        account))
                .hasMessageContaining("chk_permission_assignment_kind");
        // Thu hẹp theo vai trò phải có vai trò, không có tài khoản
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO permission_assignment (kind, applies_to, account_id, function_code, operation,"
                                + " restriction_kind, effective_from, actor_id, reason) VALUES ('RESTRICTION', 'ROLE', ?,"
                                + " 'INCIDENT', 'VIEW', 'REMOVE_OPERATION', UTC_TIMESTAMP(6), 1, 'Thu hẹp')",
                        account))
                .hasMessageContaining("chk_permission_assignment_kind");
        // metric_group, channel chỉ với DASHBOARD_REPORT
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO permission_assignment (kind, applies_to, role, function_code, operation,"
                                + " restriction_kind, metric_group, effective_from, actor_id, reason) VALUES ('RESTRICTION',"
                                + " 'ROLE', 'AC-04', 'INCIDENT', 'VIEW', 'REMOVE_OPERATION', 'CARE', UTC_TIMESTAMP(6), 1,"
                                + " 'Thu hẹp')"))
                .hasMessageContaining("chk_permission_assignment_dashboard");
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO permission_assignment (kind, applies_to, account_id, role, effective_from, reason)"
                                + " VALUES ('ROLE_ASSIGNMENT', 'ACCOUNT', ?, 'AC-05', UTC_TIMESTAMP(6), ' ')",
                        account))
                .hasMessageContaining("chk_permission_assignment_reason");
    }

    @Test
    void deviceCredential_oneActiveDevicePerAccountAndPerDevice_uniqueHash() {
        long first = new TestUsers(jdbc, time).staff().accountId();
        long second = new TestUsers(jdbc, time).staff().accountId();
        String device = "dc-" + TestUsers.nextId();
        bind(first, device, "tok-a-" + device);

        assertThatThrownBy(() -> bind(first, "other-" + device, "tok-b-" + device))
                .isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> bind(second, device, "tok-c-" + device)).isInstanceOf(DuplicateKeyException.class);
        assertThatThrownBy(() -> bind(second, "third-" + device, "tok-a-" + device))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void deviceCredential_checksByKind_andRowsAreNeverDeleted() throws SQLException {
        long account = new TestUsers(jdbc, time).staff().accountId();
        String code = "qr-" + TestUsers.nextId();
        // Mã QR đã dùng phải có thiết bị
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO device_credential (kind, account_id, secret_hash, created_by, created_at,"
                                + " expires_at, used_at) VALUES ('LOGIN_QR_CODE', ?, UNHEX(SHA2(?, 256)), 1,"
                                + " UTC_TIMESTAMP(6), UTC_TIMESTAMP(6) + INTERVAL 1 HOUR, UTC_TIMESTAMP(6))",
                        account,
                        code))
                .hasMessageContaining("chk_device_credential_kind");
        // Gỡ thủ công phải có ghi chú
        String device = "dc-" + TestUsers.nextId();
        bind(account, device, "tok-" + device);
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE device_credential SET ended_at = UTC_TIMESTAMP(6), end_reason = 'MANUAL'"
                                + " WHERE device_id = ?",
                        device))
                .hasMessageContaining("chk_device_credential_note");

        assertThatThrownBy(() -> jdbc.update("DELETE FROM device_credential WHERE device_id = ?", device))
                .isInstanceOf(DataAccessException.class);
        try (Connection c = adminConnection();
                Statement s = c.createStatement()) {
            assertThatThrownBy(
                            () -> s.executeUpdate("DELETE FROM device_credential WHERE device_id = '" + device + "'"))
                    .isInstanceOf(SQLException.class)
                    .hasFieldOrPropertyWithValue("SQLState", "45000");
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM device_credential WHERE device_id = ?", Integer.class, device))
                .isEqualTo(1);
    }

    private void bind(long account, String device, String token) {
        jdbc.update(
                "INSERT INTO device_credential (kind, account_id, secret_hash, device_id, created_at)"
                        + " VALUES ('BOUND_DEVICE', ?, UNHEX(SHA2(?, 256)), ?, UTC_TIMESTAMP(6))",
                account,
                token,
                device);
    }
}
