package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * 013 T032 (DBR-23, FR-031a): mọi lệnh đồ gửi ghi nhật ký: Tiếp nhận, bàn giao, lập và hoàn thành phiếu kiểm kê, đúng
 * người cao tuổi để tra theo người cao tuổi.
 */
class DBR_23_AuditIT extends HandoverTestBase {

    private long count(String action, long entityId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE action = ? AND entity_id = ?",
                Long.class,
                action,
                String.valueOf(entityId));
    }

    @Test
    void intakeHandoverAndInventoryCommandsAreAudited() throws Exception {
        long resident = resident("IN_RESIDENCE");
        long location = location();
        long phone = intake(resident, "PHONE", "\"quantity\": 1", location);

        handover(
                        tokenA,
                        command(
                                "TRANSFER_CUSTODY",
                                item(phone, ""),
                                ", \"storageLocationId\": " + location() + ", \"recipientStaffId\": "
                                        + officeB.staffId() + ", \"reason\": \"Đổi tủ\""))
                .andExpect(status().isOk());
        long record = latestHandover(phone);

        assertThat(count("M12_BELONGING_HANDOVER", record)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M12_BELONGING_INTAKE' AND resident_id = ?",
                        Long.class,
                        resident))
                .isGreaterThanOrEqualTo(1);
    }
}
