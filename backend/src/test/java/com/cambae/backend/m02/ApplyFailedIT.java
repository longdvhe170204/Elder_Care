package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T043 (US5 #3; FR-045, FR-042; 000 FR-038): yêu cầu Đã duyệt mà tới ngày hiệu lực không còn áp dụng được thì chuyển
 * Áp dụng không thành; phụ lục chuyển Không áp dụng (nhờ listener sau commit), không có phiên bản nội dung mới, thuộc tính
 * được nhả để yêu cầu khác dùng.
 */
@AutoConfigureMockMvc
@WithMockUser
class ApplyFailedIT extends ChangeTestBase {

    private void awaitAddendum(long requestId, String status) {
        Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "SELECT status FROM contract_addendum WHERE approval_request_id = ?",
                                String.class,
                                requestId))
                        .isEqualTo(status));
    }

    @Test
    void FR_045_residentInAFinalStatusOnTheEffectiveDate_failsTheApplication() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(2);
        long requestId = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));
        setStatus(s.residentId(), "DECEASED");

        applyDueOn(effective);

        assertThat(requestStatus(requestId)).isEqualTo("APPLY_FAILED");
        awaitAddendum(requestId, "NOT_APPLIED");
        assertThat(versionCount(s.contractId())).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM change_request_attribute WHERE approval_request_id = ? AND pending_effective",
                        Integer.class,
                        requestId))
                .isZero();
    }

    @Test
    void FR_045_serviceDeactivatedBeforeTheEffectiveDate_failsTheApplication() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        LocalDate effective = today().plusDays(2);
        long requestId = createAndApprove(request(
                s.contractId(),
                "[\"SERVICE\"]",
                "{\"services\": [{\"serviceId\": " + injection + ", \"action\": \"ADD\"}]}",
                effective));
        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE id = ?", injection);

        applyDueOn(effective);

        assertThat(requestStatus(requestId)).isEqualTo("APPLY_FAILED");
        awaitAddendum(requestId, "NOT_APPLIED");
    }

    @Test
    void FR_042_aFailedApplication_changesNothingOfTheCareLevel() throws Exception {
        Setup s = activeSetup();
        LocalDate effective = today().plusDays(2);
        createAndApprove(request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", effective));
        jdbc.update(
                "UPDATE contract SET status = 'TERMINATED', termination_reason = 'Chấm dứt thử' WHERE id = ?",
                s.contractId());

        applyDueOn(effective);

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM care_level_history WHERE resident_id = ? AND basis_type = 'RESIDENCY_CHANGE_REQUEST'",
                        Integer.class,
                        s.residentId()))
                .isZero();
    }
}
