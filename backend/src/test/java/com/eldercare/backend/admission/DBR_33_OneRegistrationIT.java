package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.admission.internal.registration.domain.RegistrationType;
import com.eldercare.backend.admission.internal.registration.persistence.ResidenceRegistrationStore;
import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * 018 T006 ({@code DBR_33_MotKhaiBaoIT}; FR-004, DBR-33, Q-306): mỗi người tối đa một khai báo đang xử lý; lệnh lập thứ
 * hai bị chặn, hai lệnh lập cùng lúc chỉ một thành công (khóa {@code technical_lock} theo người); khóa duy nhất {@code
 * unique_key} của {@code simple_record} giữ cả khi bỏ qua service.
 */
class DBR_33_OneRegistrationIT extends RegistrationTestBase {

    @Autowired
    ResidenceRegistrationStore store;

    private long admittedWithObligation() {
        long residentId = readyWith(ResidenceType.LONG_TERM_RESIDENTIAL, null);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        return residentId;
    }

    private BusinessException create(long residentId) {
        return failure(() -> registrations.createRegistration(
                ResidenceRegistrationService.targetForResident(residentId), residentId));
    }

    @Test
    void DBR_33_aSecondCreateWhileOneIsInProgress_isBlocked() {
        long residentId = admittedWithObligation();
        assertThat(create(residentId)).isNull();

        BusinessException second = create(residentId);

        assertThat(second).isNotNull();
        assertThat(second.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.errorCode()).isEqualTo("REGISTRATION_IN_PROGRESS_EXISTS");
        assertThat(registrationCount(residentId)).isEqualTo(1);
    }

    @Test
    void DBR_33_twoSimultaneousCreates_exactlyOneSucceeds() throws Exception {
        long residentId = admittedWithObligation();
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<BusinessException> errors = new ArrayList<>();
        try {
            List<Future<BusinessException>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Callable<BusinessException> call = () -> {
                    start.await();
                    return create(residentId);
                };
                results.add(pool.submit(call));
            }
            for (Future<BusinessException> result : results) {
                errors.add(result.get());
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(errors).filteredOn(e -> e == null).hasSize(1);
        assertThat(errors).filteredOn(e -> e != null).singleElement().satisfies(e -> {
            assertThat(e.status()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(e.errorCode()).isEqualTo("REGISTRATION_IN_PROGRESS_EXISTS");
        });
        assertThat(registrationCount(residentId)).isEqualTo(1);
    }

    @Test
    void DBR_33_theUniqueKeyHolds_evenWhenTheServiceIsBypassed() {
        long residentId = admittedWithObligation();
        Instant due = time.now();
        transaction.executeWithoutResult(status -> store.insertToSubmit(
                residentId, RegistrationType.STAY_NOTIFICATION, false, null, due, false, null, null, 7L));

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> store.insertToSubmit(
                        residentId, RegistrationType.STAY_NOTIFICATION, false, null, due, false, null, null, 7L)))
                .isInstanceOf(ConditionNotMetException.class);
        assertThat(registrationCount(residentId)).isEqualTo(1);
    }
}
