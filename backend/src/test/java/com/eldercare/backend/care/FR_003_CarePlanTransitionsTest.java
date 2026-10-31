package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.common.error.BusinessException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Bảng chuyển FR-003: kiểm đủ cặp trạng thái/lệnh, gồm mốc ngày nghiệp vụ. */
class FR_003_CarePlanTransitionsTest {
    private CarePlanStatus next(CarePlanStatus before, String command, LocalDate from, LocalDate today)
            throws Exception {
        try {
            return (CarePlanStatus)
                    Class.forName("com.eldercare.backend.care.internal.careplan.domain.CarePlanTransitions")
                            .getMethod("next", CarePlanStatus.class, String.class, LocalDate.class, LocalDate.class)
                            .invoke(null, before, command, from, today);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }

    @Test
    void FR_003_allCommandStatePairsFollowTheApprovedTable() throws Exception {
        var allowed = Map.of(
                "SUBMIT", Map.of(CarePlanStatus.DRAFT, CarePlanStatus.PENDING_APPROVAL),
                "APPROVE", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.PENDING_EFFECT),
                "RETURN", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.DRAFT),
                "CANCEL",
                        Map.of(
                                CarePlanStatus.DRAFT,
                                CarePlanStatus.CANCELLED,
                                CarePlanStatus.PENDING_APPROVAL,
                                CarePlanStatus.CANCELLED),
                "WITHDRAW", Map.of(CarePlanStatus.PENDING_EFFECT, CarePlanStatus.CANCELLED));
        var today = LocalDate.of(2026, 10, 31);
        for (var command : allowed.entrySet()) {
            for (var status : CarePlanStatus.values()) {
                if (command.getValue().containsKey(status)) {
                    assertThat(next(status, command.getKey(), today.plusDays(1), today))
                            .as("%s từ %s", command.getKey(), status)
                            .isEqualTo(command.getValue().get(status));
                } else {
                    assertThatThrownBy(() -> next(status, command.getKey(), today.plusDays(1), today))
                            .as("%s từ %s", command.getKey(), status)
                            .isInstanceOf(BusinessException.class)
                            .extracting("errorCode")
                            .isEqualTo("REVISION_LOCKED");
                }
            }
        }
    }

    @Test
    void FR_004_approvalCannotChangeOrAcceptAnExpiredProposedDate() {
        var today = LocalDate.of(2026, 11, 1);
        for (var date : new LocalDate[] {null, today, today.minusDays(1)}) {
            assertThatThrownBy(() -> next(CarePlanStatus.PENDING_APPROVAL, "APPROVE", date, today))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo("INVALID_EFFECTIVE_DATE");
        }
    }

    @Test
    void FR_003_withdrawalIsTooLateOnTheEffectiveDay() {
        var today = LocalDate.of(2026, 11, 1);
        assertThatThrownBy(() -> next(CarePlanStatus.PENDING_EFFECT, "WITHDRAW", today, today))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo("INVALID_EFFECTIVE_DATE");
    }

    @Test
    void FR_006_activationRechecksTheEffectiveDateAfterLocking() {
        var store =
                org.mockito.Mockito.mock(com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore.class);
        var today = LocalDate.of(2026, 10, 31);
        var at = today.atStartOfDay(com.eldercare.backend.common.time.BusinessTime.ZONE)
                .toInstant();
        var candidate = new com.eldercare.backend.care.internal.careplan.domain.CarePlan(
                1,
                2,
                1,
                CarePlanStatus.PENDING_EFFECT,
                today,
                null,
                null,
                null,
                3,
                at,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                1);
        var changed = new com.eldercare.backend.care.internal.careplan.domain.CarePlan(
                1,
                2,
                1,
                CarePlanStatus.PENDING_EFFECT,
                today.plusDays(1),
                null,
                null,
                null,
                3,
                at,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                3);
        org.mockito.Mockito.when(store.pendingOn(today)).thenReturn(java.util.List.of(candidate));
        org.mockito.Mockito.when(store.list(2, true)).thenReturn(java.util.List.of(changed));
        var step = new com.eldercare.backend.care.internal.careplan.service.CarePlanActivation(
                store,
                org.mockito.Mockito.mock(com.eldercare.backend.care.internal.status.persistence.CareLockStore.class),
                org.mockito.Mockito.mock(com.eldercare.backend.administration.auditlog.api.AuditLogApi.class),
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class),
                org.mockito.Mockito.mock(com.eldercare.backend.administration.account.api.spi.BedAllocationApi.class),
                org.mockito.Mockito.mock(com.eldercare.backend.resident.api.ResidentApi.class));
        assertThat(step.run(today, at)).isZero();
        org.mockito.Mockito.verify(store, org.mockito.Mockito.never()).activate(org.mockito.ArgumentMatchers.anyLong());
    }
}
