package com.eldercare.backend.administration.approval;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.approval.api.ApprovalRequestApi;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
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

    @Autowired
    ApprovalRequestApi requests;

    @Autowired
    TransactionTemplate transaction;
}
