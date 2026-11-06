package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import org.junit.jupiter.api.Test;

/**
 * 019 T005 (US7 kịch bản 3; FR-008a, BR-M11-18, CFG-M11-07; quickstart Q4): ghi đã mua cần chứng từ, vượt số được duyệt
 * quá CFG-M11-07 (mặc định 10%) thì chặn và hướng dẫn lập yêu cầu bổ sung tham chiếu yêu cầu gốc.
 */
class BR_M11_18_OverrunIT extends ProcurementTestBase {

    @Test
    void Q4_overrunAboveTenPercentIsBlockedWithGuidanceThenWithinLimitMovesToReconciliation() throws Exception {
        long id = approved(1_800_000);
        as(kitchenLead);
        command(id, "record-purchased", purchase(2_100_000, kitchenLead))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPROVED_AMOUNT_EXCEEDED"))
                .andExpect(jsonPath("$.unmetConditions[0].code").value("APPROVED_AMOUNT_EXCEEDED"));
        assertThat(stateOf(id)).isEqualTo("APPROVED");

        command(id, "record-purchased", purchase(1_980_000, kitchenLead)).andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("PENDING_RECONCILIATION");
        assertThat(jdbc.queryForMap(
                        "SELECT actual_amount, supplier, purchase_recorded_by_staff_id FROM procurement_request WHERE id=?",
                        id))
                .containsEntry("supplier", "Chợ Bến Thành")
                .containsEntry("purchase_recorded_by_staff_id", kitchenLead.staffId());
        assertThat(recipients(id, "PENDING_RECONCILIATION"))
                .containsExactly(new RecipientGroup.FacilityRole(Role.ACCOUNTANT));
    }

    @Test
    void FR_008a_receiptIsRequiredAndOnlyTheDepartmentLeadRecordsPurchase() throws Exception {
        long id = approved(1_800_000);
        as(kitchenLead);
        command(id, "record-purchased", "{\"actualAmount\":1900000,\"purchasedOn\":\"2026-11-05\",\"supplier\":\"A\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECEIPT_REQUIRED"));
        as(kitchenStaff);
        command(id, "record-purchased", purchase(1_900_000, kitchenStaff))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_DEPARTMENT_LEAD"));
        assertThat(stateOf(id)).isEqualTo("APPROVED");
    }

    @Test
    void FR_008a_supplementaryRequestReferencesTheOriginal() throws Exception {
        long original = approved(1_800_000);
        as(kitchenLead);
        long supplement = created("{\"departmentId\":" + kitchen + ",\"type\":\"INGREDIENT\",\"reason\":\"Phần vượt\","
                + "\"originalRequestId\":" + original + ",\"lines\":[{\"name\":\"Gạo\",\"quantity\":10,"
                + "\"estimatedUnitPrice\":30000}]}");
        assertThat(jdbc.queryForObject(
                        "SELECT original_request_id FROM procurement_request WHERE id=?", Long.class, supplement))
                .isEqualTo(original);
        create(
                        "{\"departmentId\":" + kitchen + ",\"type\":\"INGREDIENT\",\"reason\":\"Phần vượt\","
                                + "\"originalRequestId\":-1,\"lines\":[{\"name\":\"Gạo\",\"quantity\":1,\"estimatedUnitPrice\":1}]}")
                .andExpect(status().isUnprocessableEntity());
    }
}
