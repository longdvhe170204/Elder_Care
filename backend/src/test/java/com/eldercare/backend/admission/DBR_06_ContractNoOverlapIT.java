package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.internal.contract.service.ContractService;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
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

    private long signedRow(long residentId, LocalDate start, LocalDate end) {
        long id = contracts.contract(
                residentId, ContractStatus.DRAFT, ResidenceType.LONG_TERM_RESIDENTIAL, CareLevel.BASIC, start);
        jdbc.update("UPDATE contract SET end_date = ? WHERE id = ?", end, id);
        return id;
    }

    private int activate(long id, LocalDate currentEnd) {
        return jdbc.update(
                "UPDATE contract SET status = 'ACTIVE', current_end_date = ?, signed_date = start_date,"
                        + " signing_representative_id = 1, scan_file_id = 1 WHERE id = ?",
                currentEnd,
                id);
    }

    @Test
    void DBR_06_trigger_refusesAnOverlappingActiveContract_includingTheBoundaryDay() {
        long residentId = admissionResident();
        long first = signedRow(residentId, today(), today().plusDays(10));
        activate(first, today().plusDays(10));

        long touching = signedRow(residentId, today().plusDays(10), today().plusDays(20));
        assertThatThrownBy(() -> activate(touching, today().plusDays(20)))
                .as("trùng đúng ngày kết thúc của hợp đồng trước")
                .isInstanceOf(DataAccessException.class);

        long after = signedRow(residentId, today().plusDays(11), today().plusDays(20));
        assertThat(activate(after, today().plusDays(20))).isEqualTo(1);
    }

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
