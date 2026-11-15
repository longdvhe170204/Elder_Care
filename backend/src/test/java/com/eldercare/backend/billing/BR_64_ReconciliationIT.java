package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import org.junit.jupiter.api.Test;

/**
 * 019 T005 (US7 kịch bản 4, 5; FR-008b, BR-M11-19, Q-293, Q-294; quickstart Q5 → Q7): Kế toán đối chiếu, người ghi đã mua
 * không tự đối chiếu; lệch thì Quản lý viện xử lý; trạng thái cuối khóa dòng.
 */
class BR_64_ReconciliationIT extends ProcurementTestBase {

    @Test
    void Q7_discrepancyIsRecordedThenResolvedByManager() throws Exception {
        long id = purchased(1_800_000, 1_950_000);
        as(accountant);
        command(id, "record-discrepancy", "{\"reason\":\"Chứng từ 1.900.000\"}")
                .andExpect(status().isUnprocessableEntity());
        command(id, "record-discrepancy", "{\"reason\":\"Chứng từ 1.900.000\",\"discrepancyAmount\":50000}")
                .andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("DISCREPANCY");
        assertThat(recipients(id, "DISCREPANCY"))
                .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));

        as(otherAccountant);
        command(id, "resolve-discrepancy", "{\"reason\":\"x\",\"resolution\":\"ACCEPT_DISCREPANCY\"}")
                .andExpect(status().isForbidden());
        as(manager);
        command(id, "resolve-discrepancy", "{\"reason\":\"Chấp nhận\"}").andExpect(status().isUnprocessableEntity());
        command(id, "resolve-discrepancy", "{\"reason\":\"Yêu cầu hoàn 50.000\",\"resolution\":\"REQUEST_REFUND\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("RECONCILED");
        assertThat(recipients(id, "RECONCILED"))
                .containsExactlyInAnyOrder(
                        new RecipientGroup.Staff(kitchenLead.staffId(), "PROCUREMENT_REQUEST_CREATOR"),
                        new RecipientGroup.Staff(accountant.staffId(), "PROCUREMENT_RECONCILER"));
        assertThat(jdbc.queryForObject(
                        "SELECT discrepancy_resolution FROM procurement_request WHERE id=?", String.class, id))
                .isEqualTo("REQUEST_REFUND");
    }
}
