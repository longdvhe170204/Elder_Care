package com.eldercare.backend.administration.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Hoãn áp dụng (000 FR-041a (2); Q-420): yêu cầu Đã duyệt mà loại báo chưa sẵn sàng thì Bộ lập lịch bỏ qua, giữ Đã duyệt,
 * không Áp dụng không thành, không nhật ký lỗi; sẵn sàng thì lần chạy sau áp dụng bù.
 */
class ApplyTransactionNotReadyIT extends ApprovalTestBase {

    @Test
    void FR_041a_notReady_staysApproved_noFailure_thenAppliedWhenReady() throws Exception {
        LocalDate effective = time.today().plusDays(2);
        long id = approved(effective);
        control.notReadyToApply = true;

        moveTo(effective);
        runApplyJob();

        assertThat(read(id).get("status").asString()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(effects(id)).isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M15.APPROVAL_REQUEST' AND entity_id = ?"
                                + " AND action IN ('REQUEST_APPLY_FAILED', 'REQUEST_APPLY_ERROR')",
                        Integer.class,
                        String.valueOf(id)))
                .isZero();

        control.notReadyToApply = false;
        moveTo(effective.plusDays(1));
        runApplyJob();

        assertThat(read(id).get("status").asString()).isEqualTo(ApprovalStatus.APPLIED);
        assertThat(effects(id)).isEqualTo(1);
    }
}
