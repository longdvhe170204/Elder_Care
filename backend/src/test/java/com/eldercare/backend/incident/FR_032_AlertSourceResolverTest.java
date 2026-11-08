package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.events.IncidentNotificationCreated;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.alert.service.AlertService;
import com.eldercare.backend.incident.internal.alert.service.AlertSourceResolver;
import com.eldercare.backend.incident.internal.incident.service.IncidentProgressionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-032: cảnh báo đã Chuyển sự cố giữ nguyên, thêm diễn biến "nguồn đã xử lý" và báo người xử lý (Nhẹ). */
class FR_032_AlertSourceResolverTest {
    private static final Instant AT = Instant.parse("2026-11-08T03:00:00Z");

    @Test
    void FR_032_convertedAlertKeepsStatusAndAddsProgressionAndNotifiesHandler() {
        var store = mock(AlertStore.class);
        var alertService = mock(AlertService.class);
        var progressions = mock(IncidentProgressionService.class);
        var recordEvents = mock(RecordEventApi.class);
        var converted = mock(AlertRecord.class);
        when(converted.status()).thenReturn(AlertStatus.CONVERTED_TO_INCIDENT);
        when(converted.incidentId()).thenReturn(77L);
        when(converted.id()).thenReturn(5L);
        when(store.bySource("M04.TASK", List.of(9L))).thenReturn(List.<long[]>of(new long[] {9, 5}));
        when(store.findById(5L)).thenReturn(Optional.of(converted));
        when(recordEvents.findByUniqueKey(any(), any())).thenReturn(Optional.empty());
        when(recordEvents.list(any(), any())).thenReturn(List.of());
        when(progressions.addBySystem(eq(77L), eq("SOURCE_RESOLVED"), any(), eq(AT)))
                .thenReturn(List.of(mock(NotificationRequest.class)));
        var published = new ArrayList<Object>();
        var resolver = new AlertSourceResolver(
                store, alertService, recordEvents, progressions, published::add, mock(BusinessTime.class));

        resolver.resolve("M04.TASK", 9, "TASK_COMPLETED_LATE", AT);

        verify(progressions).addBySystem(eq(77L), eq("SOURCE_RESOLVED"), any(), eq(AT));
        verify(alertService, never()).closeBySystem(anyLong(), any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(published).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(published.get(0)).isInstanceOf(IncidentNotificationCreated.class);
    }
}
