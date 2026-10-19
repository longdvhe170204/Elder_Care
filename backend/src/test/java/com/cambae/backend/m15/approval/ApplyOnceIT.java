package com.cambae.backend.m15.approval;

import static com.cambae.backend.m15.approval.ApprovalFixtures.MANAGER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

/**
 * US5 #6, #7, #11; FR-037, FR-039, SC-003 (000 T084, T093; quickstart K9): yêu cầu đến hạn được áp dụng đúng một lần dù
 * job chạy lại; duyệt khi ngày hiệu lực đã tới thì áp dụng ngay, ngày thực tế là ngày duyệt.
 */
class ApplyOnceIT extends ApprovalTestBase {

    @Test
    void US5_7_11_jobRunThreeTimes_appliesExactlyOnce_withResultReference() throws Exception {
        LocalDate due = time.today().plusDays(3);
        long id = approved(due);
        assertThat(read(id).get("status").asString()).isEqualTo("APPROVED");

        moveTo(due);
        runApplyJob();
        runApplyJob();
        runApplyJob();

        JsonNode request = read(id);
        assertThat(request.get("status").asString()).isEqualTo("APPLIED");
        assertThat(request.get("appliedAt").isNull()).isFalse();
        assertThat(request.get("catchUpApplied").asBoolean()).isFalse();
        assertThat(request.get("applicationResult").get("type").asString()).isEqualTo("M15.SAMPLE_CATALOG");
        assertThat(results(id)).isEqualTo(1);
        assertThat(effects(id)).isEqualTo(1);
        assertThat(lastAudit(id, "REQUEST_APPLIED", "actor_type")).isEqualTo("SCHEDULER");
        assertThat(lastAudit(id, "REQUEST_APPLIED", "JSON_EXTRACT(basis, '$.approvalRequestId')"))
                .isEqualTo(String.valueOf(id));
    }

    @Test
    void US5_7_beforeEffectiveDate_jobDoesNothing() throws Exception {
        LocalDate due = time.today().plusDays(5);
        long id = approved(due);

        moveTo(due.minusDays(1));
        runApplyJob();

        assertThat(read(id).get("status").asString()).isEqualTo("APPROVED");
        assertThat(results(id)).isZero();
    }

    @Test
    void US5_6_approvedOnOrAfterRequestedDate_appliesImmediately_actualDateIsApprovalDate() throws Exception {
        LocalDate today = time.today();
        long id = pending(today.minusDays(2));
        user.set(MANAGER);

        api.approve(id, "Đồng ý")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.requestedEffectiveDate")
                        .value(today.minusDays(2).toString()))
                .andExpect(jsonPath("$.actualEffectiveDate").value(today.toString()));

        assertThat(results(id)).isEqualTo(1);
        assertThat(effects(id)).isEqualTo(1);
    }
}
