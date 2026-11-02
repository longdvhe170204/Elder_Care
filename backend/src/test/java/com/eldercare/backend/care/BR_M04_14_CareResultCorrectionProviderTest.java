package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.correction.api.*;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.result.service.*;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.error.BusinessException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Bảo vệ callback chung, không thay thế test lifecycle/MySQL. */
class BR_M04_14_CareResultCorrectionProviderTest {
    CareResultCorrectionProvider provider;

    @BeforeEach
    void fixture() {
        var tasks = mock(CareTaskStore.class);
        var task = mock(CareTask.class);
        when(tasks.get(anyLong(), anyBoolean())).thenReturn(task);
        when(task.data())
                .thenReturn(new CareTaskData(1, "Kiểm thử", "Kiểm thử", "ML", false, null, Set.of(), null, null, true));
        provider = new CareResultCorrectionProvider(
                mock(CareResultStore.class),
                tasks,
                mock(CareLockStore.class),
                mock(PermissionApi.class),
                mock(CareResultValidation.class),
                mock(ApplicationEventPublisher.class),
                mock(CareResultQueries.class),
                mock(com.eldercare.backend.administration.account.api.CurrentUser.class),
                mock(com.eldercare.backend.administration.account.api.FieldVisibilityApi.class));
    }

    CorrectionView correction(CreationMethod method) {
        return new CorrectionView(
                1,
                "M04.CARE_RESULT",
                1,
                1,
                CorrectionType.AMEND_CONTENT,
                Map.of("numericValue", 100),
                "Đối chiếu",
                2,
                method,
                null,
                Instant.parse("2026-11-02T00:00:00Z"));
    }

    @AfterEach
    void clear() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void BR_M04_14_callbackCannotApplyMethodCThroughTheGenericApprovalPath() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(Isolation.READ_COMMITTED.value());
        assertThatThrownBy(() -> provider.afterCorrection(correction(CreationMethod.C)))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CORRECTION_NOT_ALLOWED"));
    }

    @Test
    void BR_M04_14_callbackCannotRecalculateFromAnUnsafeRepeatableReadSnapshot() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(Isolation.REPEATABLE_READ.value());
        assertThatThrownBy(() -> provider.afterCorrection(correction(CreationMethod.A)))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("CARE_SOURCE_UNAVAILABLE"));
    }
}
