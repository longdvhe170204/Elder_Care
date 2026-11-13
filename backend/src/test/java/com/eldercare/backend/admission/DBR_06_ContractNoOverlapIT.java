package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.internal.contract.service.ContractService;
import com.eldercare.backend.common.error.BusinessException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T029 (US3; DBR-06, FR-022, BR-M02-08): một người cao tuổi không có hai hợp đồng Hiệu lực có khoảng ngày chồng
 * nhau (gồm cả hai đầu). Cơ chế chính là khóa {@code resident_residency}; trigger là lưới an toàn khi mã có lỗi.
 */
@AutoConfigureMockMvc
@WithMockUser
class DBR_06_ContractNoOverlapIT extends ContractTestBase {

    @Autowired
    ContractService contractService;

    @Test
    void FR_022_twoConcurrentRecordSigned_exactlyOneBecomesActive() throws Exception {
        long residentId = residentWithRepresentative();
        long first = createDraft(residentId);
        long second = createDraft(residentId);
        send(first);
        send(second);
        bedForSigning(residentId);
        long fileOne = scan();
        long fileTwo = scan();

        CompletableFuture<Boolean> one = CompletableFuture.supplyAsync(() -> signs(first, residentId, fileOne));
        CompletableFuture<Boolean> two = CompletableFuture.supplyAsync(() -> signs(second, residentId, fileTwo));

        List<Boolean> outcomes = List.of(one.get(30, TimeUnit.SECONDS), two.get(30, TimeUnit.SECONDS));
        assertThat(outcomes).containsExactlyInAnyOrder(true, false);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM contract WHERE resident_id = ? AND status = 'ACTIVE'",
                        Integer.class,
                        residentId))
                .isEqualTo(1);
    }

    /** true khi lệnh thành công, false khi bị chặn vì chồng thời gian (CONTRACT_OVERLAP). */
    private boolean signs(long contractId, long residentId, long fileId) {
        try {
            contractService.recordSigned(
                    contractService.targetOf(contractId), contractId, today(), representativeOf(residentId), fileId);
            return true;
        } catch (BusinessException e) {
            assertThat(e.errorCode()).isEqualTo("CONTRACT_OVERLAP");
            return false;
        }
    }
}
