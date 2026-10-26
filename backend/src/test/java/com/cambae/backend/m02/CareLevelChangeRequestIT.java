package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.spi.CareLevelChangeRequestApi;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 004 T043, T046 (US5 #10; FR-036, FR-036a của 001; research T-06): xác nhận lần đánh giá làm yêu cầu đổi mức chưa kết thúc
 * (Nháp, Chờ duyệt, Đã duyệt) chuyển sang Được thay thế ({@code SUPERSEDED}, qua chuyển mở rộng của 000), phụ lục của yêu cầu
 * Đã duyệt chuyển Không áp dụng, tới ngày hiệu lực không áp dụng gì; {@code hasOpenRequest} phản ánh đúng.
 */
@AutoConfigureMockMvc
@WithMockUser
class CareLevelChangeRequestIT extends ChangeTestBase {

    private static final String CARE_LEVEL = "[\"CARE_LEVEL\"]";
    private static final String REGULAR = "{\"careLevel\": \"REGULAR\"}";

    /** Bản thật của 004; {@code AssessmentTestBase} đặt bản giả của 001 làm {@code @Primary}. */
    @Autowired
    @Qualifier("careLevelChangeRequestHandler")
    CareLevelChangeRequestApi requests;

    @Autowired
    PlatformTransactionManager transactions;

    private List<Long> supersede(long residentId) {
        return new TransactionTemplate(transactions)
                .execute(status -> requests.supersedeByAssessment(residentId, 4242L));
    }

    @Test
    void FR_036a_noOpenRequest_meansNothingToSupersede() throws Exception {
        Setup s = activeSetup();

        assertThat(requests.hasOpenRequest(s.residentId())).isFalse();
        assertThat(supersede(s.residentId())).isEmpty();
    }

    @Test
    void FR_036a_draftPendingAndApprovedCareLevelRequests_areSuperseded() throws Exception {
        Setup s = activeSetup();
        long draft = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(9)))
                .get("approvalRequestId")
                .asLong();
        assertThat(requests.hasOpenRequest(s.residentId())).isTrue();
        long pending = createChange(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(9)))
                .get("approvalRequestId")
                .asLong();
        submit(pending);
        long approved = createAndApprove(request(s.contractId(), CARE_LEVEL, REGULAR, today().plusDays(9)));

        List<Long> superseded = supersede(s.residentId());

        assertThat(superseded).containsExactlyInAnyOrder(draft, pending, approved);
        assertThat(List.of(draft, pending, approved))
                .allSatisfy(id -> assertThat(requestStatus(id)).isEqualTo("SUPERSEDED"));
        assertThat(requests.hasOpenRequest(s.residentId())).isFalse();
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM contract_addendum WHERE approval_request_id = ?", String.class, approved))
                .isEqualTo("NOT_APPLIED");
    }

    @Test
    void FR_036a_aSupersededApprovedRequest_isNotAppliedOnItsDate() throws Exception {
        Setup s = activeSetup();
        var effective = today().plusDays(2);
        long approved = createAndApprove(request(s.contractId(), CARE_LEVEL, REGULAR, effective));
        supersede(s.residentId());

        applyDueOn(effective);

        assertThat(requestStatus(approved)).isEqualTo("SUPERSEDED");
        assertThat(versionCount(s.contractId())).isEqualTo(1);
    }

    @Test
    void FR_036a_otherAttributesAndFinishedRequests_areLeftAlone() throws Exception {
        Setup s = activeSetup();
        long injection = service("INJECTION", "Tiêm", null, 150_000);
        long serviceRequest = createChange(request(
                        s.contractId(),
                        "[\"SERVICE\"]",
                        "{\"services\": [{\"serviceId\": " + injection + ", \"action\": \"ADD\"}]}",
                        today().plusDays(9)))
                .get("approvalRequestId")
                .asLong();
        long applied = createAndApprove(request(s.contractId(), CARE_LEVEL, REGULAR, today()));

        assertThat(supersede(s.residentId())).isEmpty();

        assertThat(requestStatus(serviceRequest)).isEqualTo("DRAFT");
        assertThat(requestStatus(applied)).isEqualTo("APPLIED");
    }
}
