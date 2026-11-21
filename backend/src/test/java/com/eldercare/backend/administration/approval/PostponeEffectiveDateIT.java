package com.eldercare.backend.administration.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dời ngày hiệu lực thực tế của yêu cầu Đã duyệt (000 FR-041a (3); Q-419): chỉ sang ngày muộn hơn, ghi nhật ký; ngày
 * sớm hơn hoặc bằng thì bỏ qua; yêu cầu không ở Đã duyệt thì 409; tới ngày cũ không áp dụng, tới ngày mới thì áp dụng.
 */
class PostponeEffectiveDateIT extends ApprovalTestBase {

    @Autowired
    ApprovalRequestApi approvals;

    @Autowired
    TransactionTemplate tx;

    @Test
    void FR_041a_postpone_laterDateOnly_auditLogged_appliedOnNewDate() throws Exception {
        LocalDate effective = time.today().plusDays(2);
        long id = approved(effective);

        tx.executeWithoutResult(
                s -> approvals.postponeEffectiveDate(id, effective.minusDays(1), new Reason("Sớm hơn")));
        assertThat(read(id).get("actualEffectiveDate").asString()).isEqualTo(effective.toString());

        LocalDate later = effective.plusDays(3);
        tx.executeWithoutResult(s -> approvals.postponeEffectiveDate(id, later, new Reason("Ký phụ lục muộn")));
        assertThat(read(id).get("actualEffectiveDate").asString()).isEqualTo(later.toString());
        assertThat(lastAudit(id, "REQUEST_EFFECTIVE_DATE_POSTPONED", "reason")).isEqualTo("Ký phụ lục muộn");

        moveTo(effective);
        runApplyJob();
        assertThat(read(id).get("status").asString()).isEqualTo(ApprovalStatus.APPROVED);

        moveTo(later);
        runApplyJob();
        assertThat(read(id).get("status").asString()).isEqualTo(ApprovalStatus.APPLIED);
    }

    @Test
    void FR_041a_postpone_notApproved_409() throws Exception {
        long id = pending(time.today().plusDays(2));
        assertThatThrownBy(() -> tx.executeWithoutResult(
                        s -> approvals.postponeEffectiveDate(id, time.today().plusDays(9), new Reason("Dời"))))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("INVALID_STATE"));
    }
}
