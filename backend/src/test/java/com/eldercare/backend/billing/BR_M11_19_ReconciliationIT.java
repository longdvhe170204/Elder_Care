package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import org.junit.jupiter.api.Test;

/**
 * 019 T005 (US7 kịch bản 4, 5; FR-008b, BR-M11-19, Q-293, Q-294; quickstart Q5 → Q7): Kế toán đối chiếu, người ghi đã mua
 * không tự đối chiếu; lệch thì Quản lý viện xử lý; trạng thái cuối khóa dòng.
 */
class BR_M11_19_ReconciliationIT extends ProcurementTestBase {

    @Test
    void Q5_accountantConfirmsMatchAndTheRowLocks() throws Exception {
        long id = purchased(1_800_000, 1_900_000);
        as(accountant);
        command(id, "confirm-match").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("RECONCILED");
        assertThat(jdbc.queryForObject(
                        "SELECT reconciled_by_staff_id FROM procurement_request WHERE id=?", Long.class, id))
                .isEqualTo(accountant.staffId());
        assertThatThrownBy(() -> jdbc.update("UPDATE procurement_request SET supplier='B' WHERE id=?", id))
                .hasMessageContaining("procurement request row is locked");
    }

    @Test
    void Q6_purchaseRecorderCannotReconcileAndOnlyAccountantsReconcile() throws Exception {
        long id = purchased(1_800_000, 1_900_000);
        as(kitchenLead);
        command(id, "confirm-match")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PURCHASE_RECORDER_CANNOT_RECONCILE"));
        as(kitchenStaff);
        command(id, "confirm-match").andExpect(status().isForbidden());
        as(manager);
        command(id, "confirm-match").andExpect(status().isForbidden());
        assertThat(stateOf(id)).isEqualTo("PENDING_RECONCILIATION");
    }

    @Test
    void US7_5_returnForMissingReceiptGoesBackToApprovedForANewPurchaseRecord() throws Exception {
        long id = purchased(1_800_000, 1_900_000);
        as(accountant);
        command(id, "return-for-reconciliation")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        command(id, "return-for-reconciliation", "{\"reason\":\"Thiếu hóa đơn thịt\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("APPROVED");
        assertThat(recipients(id, "APPROVED"))
                .contains(new RecipientGroup.Staff(kitchenLead.staffId(), "PROCUREMENT_REQUEST_CREATOR"));
        as(kitchenLead);
        command(id, "record-purchased", purchase(1_950_000, kitchenLead)).andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("PENDING_RECONCILIATION");
    }

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
