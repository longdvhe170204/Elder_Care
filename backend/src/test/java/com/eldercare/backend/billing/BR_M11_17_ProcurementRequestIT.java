package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 019 T005 (US7 kịch bản 1, 2, 5, 7; FR-008, FR-020, FR-021; BR-M11-17; Q-290, Q-292, Q-293, Q-299; quickstart Q1, Q2,
 * Q9): vòng đời Nháp → Chờ duyệt → Đã duyệt/Từ chối, chỉ Phụ trách bộ phận của bộ phận lập, nội dung khóa khi rời Nháp.
 */
class BR_M11_17_ProcurementRequestIT extends ProcurementTestBase {

    @Test
    void Q1_departmentLeadDraftsSubmitsWithdrawsAndManagerApprovesWithReduction() throws Exception {
        long id = created(ingredientBody(kitchen));
        assertThat(stateOf(id)).isEqualTo("DRAFT");
        assertThat(jdbc.queryForObject("SELECT estimated_total FROM procurement_request WHERE id=?", Long.class, id))
                .isEqualTo(2_000_000L);

        command(id, "submit").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("PENDING_APPROVAL");
        assertThat(recipients(id, "PENDING_APPROVAL"))
                .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        // Đã gửi: không sửa được nội dung; rút lại về Nháp để sửa (15.10).
        update(id, 1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        command(id, "withdraw").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("DRAFT");
        update(id, 2).andExpect(status().isOk());
        command(id, "submit").andExpect(status().isOk());

        as(manager);
        command(id, "approve", "{\"approvedAmount\":1800000}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        command(id, "approve", "{\"approvedAmount\":2100000,\"reason\":\"Tăng\"}")
                .andExpect(status().isUnprocessableEntity());
        command(id, "approve", "{\"approvedAmount\":1800000,\"reason\":\"Giảm theo giá chợ\"}")
                .andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT approved_amount FROM procurement_request WHERE id=?", Long.class, id))
                .isEqualTo(1_800_000L);
        assertThat(recipients(id, "APPROVED"))
                .containsExactly(new RecipientGroup.Staff(kitchenLead.staffId(), "PROCUREMENT_REQUEST_CREATOR"));

        as(kitchenLead);
        mvc.perform(get(URL).param("departmentId", Long.toString(kitchen)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + id + ")].status").value("APPROVED"))
                .andExpect(jsonPath("$[?(@.id==" + id + ")].approvedAmount").value(1_800_000))
                .andExpect(jsonPath("$[?(@.id==" + id + ")].lines.length()").value(2));
    }

    @Test
    void FR_008_approvalWithoutReductionNeedsNoReasonAndRejectionNeedsOne() throws Exception {
        long id = created(ingredientBody(kitchen));
        command(id, "submit").andExpect(status().isOk());
        as(manager);
        command(id, "reject")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        command(id, "reject", "{\"reason\":\"Chưa cần\"}").andExpect(status().isOk());
        assertThat(stateOf(id)).isEqualTo("REJECTED");
        command(id, "approve", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));

        as(kitchenLead);
        long other = created(ingredientBody(kitchen));
        command(other, "submit").andExpect(status().isOk());
        as(manager);
        command(other, "approve", "{}").andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT approved_amount FROM procurement_request WHERE id=?", Long.class, other))
                .isEqualTo(2_000_000L);
    }

    @Test
    void Q2_Q293_Q299_onlyTheDepartmentLeadCreates() throws Exception {
        as(accountant);
        create(ingredientBody(kitchen)).andExpect(status().isForbidden());
        as(manager);
        create(ingredientBody(kitchen)).andExpect(status().isForbidden());
        as(kitchenStaff);
        create(ingredientBody(kitchen))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_DEPARTMENT_LEAD"));
        // Phụ trách bộ phận Bếp không lập cho bộ phận khác
        long other = TestUsers.nextId();
        duties.departmentLead(
                other, new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF).staffId());
        as(kitchenLead);
        create(ingredientBody(other))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("NOT_DEPARTMENT_LEAD"));
    }

    @Test
    void Q9_Q292_departmentWithoutLeadIsBlockedAndManagerIsReminded() throws Exception {
        as(technicalStaff);
        create(ingredientBody(technical))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DEPARTMENT_LEAD_MISSING"));
        assertThat(events.stream(com.eldercare.backend.billing.internal.procurement.domain.DepartmentLeadMissing.class)
                        .filter(e -> e.departmentId() == technical)
                        .flatMap(e -> e.notificationRequests().stream())
                        .flatMap(n -> n.recipientGroups().stream()))
                .containsExactly(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM procurement_request WHERE department_id=?", Integer.class, technical))
                .isZero();
    }

    @Test
    void FR_008_validationAndCancellation() throws Exception {
        create("{\"departmentId\":" + kitchen + ",\"type\":\"INGREDIENT\",\"reason\":\"x\",\"lines\":[]}")
                .andExpect(status().isUnprocessableEntity());
        create("{\"departmentId\":" + kitchen + ",\"type\":\"INGREDIENT\",\"reason\":\" \",\"lines\":["
                        + "{\"name\":\"Gạo\",\"quantity\":1,\"estimatedUnitPrice\":1}]}")
                .andExpect(status().isUnprocessableEntity());
        create("{\"departmentId\":" + kitchen + ",\"type\":\"INGREDIENT\",\"reason\":\"x\",\"lines\":["
                        + "{\"name\":\"Gạo\",\"quantity\":0,\"estimatedUnitPrice\":1}]}")
                .andExpect(status().isUnprocessableEntity());

        long draft = created(ingredientBody(kitchen));
        as(kitchenStaff);
        command(draft, "cancel").andExpect(status().isForbidden());
        as(kitchenLead);
        command(draft, "cancel").andExpect(status().isOk());
        assertThat(stateOf(draft)).isEqualTo("CANCELLED");

        long approved = approved(1_800_000);
        as(kitchenLead);
        command(approved, "cancel")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
        command(approved, "cancel", "{\"reason\":\"Không mua nữa\"}").andExpect(status().isOk());
        assertThat(stateOf(approved)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT locked_at IS NOT NULL FROM procurement_request WHERE id=?", Boolean.class, approved))
                .isTrue();
    }

    @Test
    void DBR_content_isLockedByTriggerOutsideDraft() throws Exception {
        long id = created(ingredientBody(kitchen));
        command(id, "submit").andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> jdbc.update("UPDATE procurement_request SET reason='sửa lén' WHERE id=?", id))
                .hasMessageContaining("procurement request content is locked");
    }

    private org.springframework.test.web.servlet.ResultActions update(long id, long version) throws Exception {
        return mvc.perform(put(URL + "/" + id)
                .header("Idempotency-Key", "pu-" + TestUsers.nextId())
                .header("If-Match", "\"" + version + "\"")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(kitchen, "INGREDIENT", "Hết gạo, thêm thịt")));
    }
}
