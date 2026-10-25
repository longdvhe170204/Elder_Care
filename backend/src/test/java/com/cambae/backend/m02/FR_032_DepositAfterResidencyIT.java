package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.dto.DepositStatus;
import com.cambae.backend.m02.api.events.DepositNotSatisfiedAfterAdmission;
import com.cambae.backend.m02.internal.deposit.service.DepositRecheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 004 T039 (US4; FR-032): khi 017 báo đặt cọc về Chưa đáp ứng mà người cao tuổi đã Đang lưu trú thì báo Quản lý viện
 * và không đổi trạng thái lưu trú; chưa lưu trú hoặc đặt cọc đạt thì không báo. Listener của sự kiện 017 chờ {@code
 * m11.api}; test gọi thẳng hàm mà listener sẽ gọi.
 */
class FR_032_DepositAfterResidencyIT extends M02TestBase {

    @Autowired
    private DepositRecheckService recheck;

    private long contractOf(long residentId) {
        confirmedAssessment(residentId, CareLevel.BASIC);
        return contracts.active(residentId, CareLevel.BASIC, today());
    }

    private long reported(long residentId) {
        return events.stream(DepositNotSatisfiedAfterAdmission.class)
                .filter(event -> event.residentId() == residentId)
                .count();
    }

    @Test
    void FR_032_notSatisfiedWhileInResidence_notifiesAndKeepsTheStatus() {
        long residentId = admissionResident();
        long contractId = contractOf(residentId);
        setStatus(residentId, "IN_RESIDENCE");
        deposits.status(contractId, DepositStatus.NOT_SATISFIED);

        recheck.recheck(contractId);

        assertThat(reported(residentId)).isEqualTo(1);
        assertThat(currentStatus(residentId)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_032_notSatisfiedBeforeAdmission_isOnlyAnUnmetCondition() {
        long residentId = admissionResident();
        long contractId = contractOf(residentId);
        deposits.status(contractId, DepositStatus.NOT_SATISFIED);

        recheck.recheck(contractId);

        assertThat(reported(residentId)).isZero();
    }

    @Test
    void FR_032_satisfiedOrNotRequired_doesNotNotify() {
        long residentId = admissionResident();
        long contractId = contractOf(residentId);
        setStatus(residentId, "IN_RESIDENCE");

        deposits.status(contractId, DepositStatus.SATISFIED);
        recheck.recheck(contractId);
        deposits.status(contractId, DepositStatus.NOT_REQUIRED);
        recheck.recheck(contractId);

        assertThat(reported(residentId)).isZero();
    }

    @Test
    void FR_032_aCancelledContract_isIgnored() {
        long residentId = admissionResident();
        long contractId = contractOf(residentId);
        setStatus(residentId, "IN_RESIDENCE");
        jdbc.update("UPDATE contract SET status = 'CANCELLED' WHERE id = ?", contractId);
        deposits.status(contractId, DepositStatus.NOT_SATISFIED);

        recheck.recheck(contractId);

        assertThat(reported(residentId)).isZero();
    }
}
