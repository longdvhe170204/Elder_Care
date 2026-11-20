package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.internal.change.service.ResidencyChangeClosing;
import com.eldercare.backend.resident.api.spi.CareLevelChangeRequestApi;
import com.eldercare.backend.support.fake.resident.FakeCareLevelChangeRequests;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-065, FR-070 (lệnh Kết thúc lưu trú, Ghi nhận qua đời không bị chặn): khi người cao tuổi sang trạng thái cuối, mọi yêu
 * cầu thay đổi lưu trú đã ghi nhận được đóng; yêu cầu đã ở trạng thái cuối, kể cả Được thay thế (trạng thái mở rộng của
 * BR-M01-03), giữ nguyên chứ không làm hỏng lệnh.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_070_FinalStateClosingIT extends ChangeTestBase {

    @Autowired
    ResidencyChangeClosing closing;

    /** Nền test của 001 đăng ký bản giả {@code @Primary}; ở đây cần bản thật của 004. */
    @Autowired
    List<CareLevelChangeRequestApi> careLevelRequestApis;

    @Autowired
    TransactionTemplate transactions;

    private CareLevelChangeRequestApi realCareLevelRequests() {
        return careLevelRequestApis.stream()
                .filter(api -> !(api instanceof FakeCareLevelChangeRequests))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void FR_070_closingWithASupersededRequest_closesTheOthers() throws Exception {
        Setup s = activeSetup();
        long superseded = createAndApprove(
                request(s.contractId(), "[\"CARE_LEVEL\"]", "{\"careLevel\": \"REGULAR\"}", today().plusDays(3)));
        long pending = createChange(request(
                        s.contractId(),
                        "[\"DURATION\"]",
                        "{\"endDate\": \"%s\"}".formatted(today().plusDays(400)),
                        today().plusDays(3)))
                .get("approvalRequestId")
                .asLong();
        submit(pending);
        transactions.executeWithoutResult(t -> realCareLevelRequests().supersedeByAssessment(s.residentId(), 1L));
        assertThat(requestStatus(superseded)).isEqualTo("SUPERSEDED");

        transactions.executeWithoutResult(t -> closing.closeAll(s.residentId(), "FR-070"));

        assertThat(requestStatus(superseded)).isEqualTo("SUPERSEDED");
        assertThat(requestStatus(pending)).isEqualTo("CANCELLED");
    }
}
