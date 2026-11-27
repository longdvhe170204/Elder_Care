package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.common.error.BusinessException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Bảng chuyển FR-003 (sửa Q-447): kiểm đủ cặp trạng thái/lệnh và ngày hiệu lực khi duyệt. */
class FR_003_CarePlanTransitionsTest {
    private static final String TRANSITIONS = "com.eldercare.backend.care.internal.careplan.domain.CarePlanTransitions";

    private static Object call(String method, Class<?>[] types, Object... args) throws Exception {
        try {
            return Class.forName(TRANSITIONS).getMethod(method, types).invoke(null, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw e;
        }
    }

    private CarePlanStatus next(CarePlanStatus before, String command) throws Exception {
        return (CarePlanStatus) call("next", new Class<?>[] {CarePlanStatus.class, String.class}, before, command);
    }

    @Test
    void FR_003_allCommandStatePairsFollowTheApprovedTable() throws Exception {
        var allowed = Map.of(
                "SUBMIT", Map.of(CarePlanStatus.DRAFT, CarePlanStatus.PENDING_APPROVAL),
                "RETRACT", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.DRAFT),
                "APPROVE", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.PENDING_EFFECT),
                "RETURN", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.DRAFT),
                "CANCEL",
                        Map.of(
                                CarePlanStatus.DRAFT,
                                CarePlanStatus.CANCELLED,
                                CarePlanStatus.PENDING_APPROVAL,
                                CarePlanStatus.CANCELLED),
                "WITHDRAW", Map.<CarePlanStatus, CarePlanStatus>of());
        for (var command : allowed.entrySet()) {
            for (var status : CarePlanStatus.values()) {
                if (command.getValue().containsKey(status)) {
                    assertThat(next(status, command.getKey()))
                            .as("%s từ %s", command.getKey(), status)
                            .isEqualTo(command.getValue().get(status));
                } else {
                    assertThatThrownBy(() -> next(status, command.getKey()))
                            .as("%s từ %s", command.getKey(), status)
                            .isInstanceOf(BusinessException.class)
                            .extracting("errorCode")
                            .isEqualTo("REVISION_LOCKED");
                }
            }
        }
    }

    @Test
    void Q447_approvalDateFollowsWhetherAnEffectivePlanExists() throws Exception {
        var today = LocalDate.of(2026, 11, 25);
        assertThat(call("approvedStatus", new Class<?>[] {boolean.class}, false))
                .isEqualTo(CarePlanStatus.EFFECTIVE);
        assertThat(call("approvedStatus", new Class<?>[] {boolean.class}, true))
                .isEqualTo(CarePlanStatus.PENDING_EFFECT);
        assertThat(call("effectiveFromOnApproval", new Class<?>[] {boolean.class, LocalDate.class}, false, today))
                .isEqualTo(today);
        assertThat(call("effectiveFromOnApproval", new Class<?>[] {boolean.class, LocalDate.class}, true, today))
                .isEqualTo(LocalDate.of(2026, 11, 26));
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
