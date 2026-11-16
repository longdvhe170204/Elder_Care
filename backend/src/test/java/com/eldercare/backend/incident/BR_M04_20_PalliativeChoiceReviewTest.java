package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.care.api.CarePlanReviewApi;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.PalliativeCareChoiceRecorded;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertResponsibility;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.critical.domain.ChoiceExecutionContent;
import com.eldercare.backend.incident.internal.critical.domain.ReconfirmationResult;
import com.eldercare.backend.incident.internal.critical.domain.WishReconfirmationContent;
import com.eldercare.backend.incident.internal.critical.persistence.CriticalCareStore;
import com.eldercare.backend.incident.internal.critical.service.CriticalCareService;
import com.eldercare.backend.incident.internal.emergency.service.TransferOutService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import com.eldercare.backend.resident.api.EndOfLifeWishApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.support.fake.care.FakeM04ApiFixture;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * BR-M04-20, 005 FR-011 (005 module-interfaces mục 1.10b): lệnh ghi lựa chọn "ở lại chăm sóc giảm nhẹ" gọi 005 báo xem
 * xét kế hoạch với cùng khóa của {@code PalliativeCareChoiceRecorded}; 005 lỗi thì lệnh 007 lỗi theo (rollback).
 */
class BR_M04_20_PalliativeChoiceReviewTest {
    private static final Instant NOW = Instant.parse("2026-11-16T03:00:00Z");
    private static final long ALERT = 70L;
    private static final long RESIDENT = 101L;

    private final CriticalCareStore store = mock(CriticalCareStore.class);
    private final AlertStore alerts = mock(AlertStore.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final FakeM04ApiFixture care = new FakeM04ApiFixture();
    private final AtomicInteger reviewCalls = new AtomicInteger();
    private final CriticalCareService service = new CriticalCareService(
            store,
            mock(AlertService.class),
            alerts,
            mock(AlertResponsibility.class),
            mock(MonitoringLock.class),
            permissions,
            mock(AuditLogApi.class),
            events,
            time,
            mock(ResidentApi.class),
            mock(EndOfLifeWishApi.class),
            mock(TransferOutService.class),
            mock(IncidentRecordService.class),
            null,
            null,
            care.api(CarePlanReviewApi.class));

    @SuppressWarnings("unchecked")
    @BeforeEach
    void criticalAlertWithKeptWish() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.DOCTOR));
        when(permissions.hasPermission(eq(user), any(), any()))
                .thenReturn(PermissionResult.allowed(PermissionBasis.ROLE));
        var alert = mock(AlertRecord.class);
        when(alert.residentId()).thenReturn(RESIDENT);
        when(alert.floorAreaId()).thenReturn(7L);
        when(alert.criticalFlagId()).thenReturn(5L);
        when(alert.status()).thenReturn(AlertStatus.IN_PROGRESS);
        when(alerts.lock(ALERT)).thenReturn(Optional.of(alert));
        RecordEvent<WishReconfirmationContent> kept = mock(RecordEvent.class);
        when(kept.content())
                .thenReturn(new WishReconfirmationContent(
                        1L, "Người đại diện", null, NOW, ReconfirmationResult.KEPT, null));
        when(store.reconfirmations(ALERT)).thenReturn(List.of(kept));
        when(store.choices(ALERT)).thenReturn(List.of());
        RecordEvent<ChoiceExecutionContent> row = mock(RecordEvent.class);
        when(row.id()).thenReturn(300L);
        when(store.addChoice(eq(ALERT), any(), anyLong(), any(), any())).thenReturn(row);
    }

    @Test
    void BR_M04_20_stayPalliativeAsksCarePlanReviewWithTheEventKey() {
        var published = ArgumentCaptor.forClass(Object.class);
        // fixture chỉ nhận đúng khóa của sự kiện; khóa khác thì bản giả ném lỗi
        care.reply(CarePlanReviewApi.class, "palliativeCareChoiceRecorded", List.of(RESIDENT, key(), NOW), () -> {
            reviewCalls.incrementAndGet();
            return null;
        });

        service.executeChoice(ALERT, "STAY_PALLIATIVE", null, null, null, user);

        verify(events).publishEvent(published.capture());
        assertThat(published.getAllValues())
                .filteredOn(PalliativeCareChoiceRecorded.class::isInstance)
                .singleElement()
                .isEqualTo(new PalliativeCareChoiceRecorded(RESIDENT, key(), NOW));
        assertThat(reviewCalls).hasValue(1);
    }

    @Test
    void BR_M04_20_carePlanReviewFailureFailsTheChoiceCommand() {
        assertThatThrownBy(() -> service.executeChoice(ALERT, "STAY_PALLIATIVE", null, null, null, user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CarePlanReviewApi.palliativeCareChoiceRecorded");
        verify(store).addChoice(eq(ALERT), any(), anyLong(), any(), any());
    }

    @Test
    void BR_M04_20_otherChoicesDoNotAskCarePlanReview() {
        service.executeChoice(ALERT, "RETURN_HOME", null, null, null, user);

        assertThat(reviewCalls).hasValue(0);
    }

    /** Khóa {@code CriticalCareNotices.key(alertId, "PALLIATIVE_CARE_CHOICE")} của lần chọn. */
    private static String key() {
        return "M05.ALERT:" + ALERT + ":PALLIATIVE_CARE_CHOICE";
    }
}
