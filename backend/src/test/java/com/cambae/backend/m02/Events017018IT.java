package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m02.api.events.DepositAmountChanged;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T041 (US4; FR-033a, FR-033b): khoản cần đặt cọc gửi 017 khi hợp đồng vào Chờ ký (lúc 017 bắt đầu theo dõi) và mỗi
 * lần gửi ký lại với khoản khác. Sự kiện hợp đồng đổi trạng thái, Hoàn tất tiếp nhận cho 017, 018 đã có test ở {@code
 * ContractLifecycleIT}, {@code FR_004_AdmissionWithRealContractIT}.
 */
@AutoConfigureMockMvc
@WithMockUser
class Events017018IT extends ContractTestBase {

    private List<BigDecimal> amounts(long contractId) {
        return events.stream(DepositAmountChanged.class)
                .filter(event -> event.contractId() == contractId)
                .map(DepositAmountChanged::newAmount)
                .toList();
    }

    @Test
    void FR_033a_sendForSignature_publishesTheRequiredDeposit() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.put("noDepositRequired", false);
        body.put("depositAmount", 15_000_000);
        long id = create(body).get("contract").get("id").asLong();
        assertThat(amounts(id)).as("Nháp chưa được theo dõi").isEmpty();

        send(id);

        assertThat(amounts(id))
                .singleElement()
                .satisfies(amount -> assertThat(amount).isEqualByComparingTo("15000000"));
    }

    @Test
    void FR_033a_noDepositRequired_publishesZero() throws Exception {
        long residentId = residentWithRepresentative();
        long id = createDraft(residentId);

        send(id);

        assertThat(amounts(id))
                .singleElement()
                .satisfies(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO));
    }

    @Test
    void FR_033a_resendWithAChangedDeposit_publishesTheNewAmount() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.put("noDepositRequired", false);
        body.put("depositAmount", 10_000_000);
        long id = create(body).get("contract").get("id").asLong();
        send(id);
        commandWithReason(id, "return-to-draft", "Đổi khoản cọc").andExpect(status().isOk());
        body.put("depositAmount", 12_000_000);
        updateRaw(id, etag(id), body).andExpect(status().isOk());
        send(id);

        assertThat(amounts(id)).extracting(BigDecimal::intValueExact).containsExactly(10_000_000, 12_000_000);
    }
}
