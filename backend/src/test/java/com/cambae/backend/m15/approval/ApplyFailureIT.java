package com.cambae.backend.m15.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m15.approval.api.RequestApplyFailed;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * US5 #8, #10; FR-037b, FR-038 (000 T086; quickstart K11, K12): điều kiện không thỏa thì Áp dụng không thành, không tác
 * động, báo người duyệt; lỗi khi áp dụng thì bỏ mọi tác động, giữ Đã duyệt, nhật ký lỗi ghi ở transaction riêng, lần sau
 * áp dụng được.
 */
class ApplyFailureIT extends ApprovalTestBase {

    @Test
    void US5_8_conditionNoLongerMet_endsApplyFailed_withoutEffect() throws Exception {
        LocalDate due = time.today().plusDays(2);
        long id = approved(due);
        control.conditionUnmet = true;

        moveTo(due);
        runApplyJob();

        assertThat(read(id).get("status").asString()).isEqualTo("APPLY_FAILED");
        assertThat(read(id).get("closingReason").asString()).contains("Điều kiện mẫu không còn thỏa");
        assertThat(results(id)).isZero();
        assertThat(effects(id)).isZero();
        assertThat(events.stream(RequestApplyFailed.class)
                        .filter(e -> e.request().id() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.notificationRequests()).isNotEmpty());
        assertThat(lastAudit(id, "REQUEST_APPLY_FAILED", "action")).isNotNull();
    }

    @Test
    void US5_10_errorWhileApplying_rollsBackAll_staysApproved_logsError_thenAppliesNextRun() throws Exception {
        LocalDate due = time.today().plusDays(2);
        long id = approved(due);
        control.failOnApply = true;

        moveTo(due);
        runApplyJob();

        assertThat(read(id).get("status").asString()).isEqualTo("APPROVED");
        assertThat(effects(id)).isZero();
        assertThat(results(id)).isZero();
        assertThat(lastAudit(id, "REQUEST_APPLY_ERROR", "JSON_UNQUOTE(JSON_EXTRACT(details, '$.cause'))"))
                .contains("Lỗi mẫu khi áp dụng");

        control.failOnApply = false;
        runApplyJob();

        assertThat(read(id).get("status").asString()).isEqualTo("APPLIED");
        assertThat(effects(id)).isEqualTo(1);
    }
}
