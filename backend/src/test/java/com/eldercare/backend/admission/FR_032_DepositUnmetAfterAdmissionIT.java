package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.DepositStatus;
import com.eldercare.backend.admission.api.events.DepositNotSatisfiedAfterAdmission;
import com.eldercare.backend.admission.internal.deposit.service.DepositRecheckService;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 004 T040 (FR-030 → FR-032; 017 FR-006): trạng thái đặt cọc về Chưa đáp ứng khi người cao tuổi đã Đang lưu trú thì báo
 * Quản lý viện (một lần mỗi lần kiểm), không đổi trạng thái lưu trú; Đã đáp ứng thì không báo. Việc nghe
 * {@code DepositStatusChanged} của 017 kiểm ở {@code DepositStatusListenerTest}.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_032_DepositUnmetAfterAdmissionIT extends AbsenceTestBase {

    @Autowired
    DepositRecheckService recheck;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void FR_032_unmetDepositWhileInResidence_notifiesManager_andKeepsResidencyStatus() throws Exception {
        long residentId = inResidence();
        long contractId =
                jdbc.queryForObject("SELECT MAX(id) FROM contract WHERE resident_id = ?", Long.class, residentId);

        deposits.status(contractId, DepositStatus.SATISFIED);
        transactions.executeWithoutResult(status -> recheck.recheck(contractId));
        assertThat(events.stream(DepositNotSatisfiedAfterAdmission.class).filter(e -> e.contractId() == contractId))
                .as("Đã đáp ứng thì không báo")
                .isEmpty();

        deposits.status(contractId, DepositStatus.NOT_SATISFIED);
        transactions.executeWithoutResult(status -> recheck.recheck(contractId));
        assertThat(events.stream(DepositNotSatisfiedAfterAdmission.class).filter(e -> e.contractId() == contractId))
                .as("Chưa đáp ứng khi đã lưu trú thì báo Quản lý viện")
                .hasSize(1);
        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_032_unmetDepositBeforeResidence_doesNotNotify() throws Exception {
        long residentId = inResidence();
        long contractId =
                jdbc.queryForObject("SELECT MAX(id) FROM contract WHERE resident_id = ?", Long.class, residentId);
        setStatus(residentId, ResidentStatus.ADMISSION_IN_PROGRESS.name());

        deposits.status(contractId, DepositStatus.NOT_SATISFIED);
        transactions.executeWithoutResult(status -> recheck.recheck(contractId));

        assertThat(events.stream(DepositNotSatisfiedAfterAdmission.class).filter(e -> e.contractId() == contractId))
                .as("Chưa lưu trú thì điều kiện tiếp nhận chặn, không báo FR-032")
                .isEmpty();
    }
}
