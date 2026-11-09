package com.eldercare.backend.incident.internal.infection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.care.api.ActivityRegistrationAutoCancelApi;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.internal.alert.persistence.AlertStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentContent;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentPeriod;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.incident.internal.infection.persistence.ContainmentStore;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 014 FR-015 (c), (g), FR-018, FR-021 (chủ dự án duyệt 2026-11-05): 007 cài cổng chỉ đọc cho điều kiện đăng ký hoạt động
 * và gọi lệnh tự hủy đăng ký trong cùng transaction của lệnh tạo khoanh vùng.
 */
class FR_021_ActivityHealthGateTest {
    private static final Instant AT = Instant.parse("2026-11-08T03:00:00Z");

    private final DefaultIsolationApi isolation = mock(DefaultIsolationApi.class);
    private final IncidentStore incidents = mock(IncidentStore.class);
    private final AlertStore alerts = mock(AlertStore.class);
    private final IncidentActivityHealthGate gate = new IncidentActivityHealthGate(isolation, incidents, alerts);

    private static IsolationZoneView zone(Set<Long> floors, Set<Long> areas) {
        return new IsolationZoneView(1, floors, areas, AT.minusSeconds(60), null);
    }

    @Test
    void FR_015_gateReadsIsolationSuspicionAndOpenMediumItems() {
        when(isolation.activeByResident(7L, AT)).thenReturn(List.of(zone(Set.of(2L), Set.of())));
        when(isolation.activeByRoom(20L, AT)).thenReturn(List.of());
        when(isolation.activeByFloorArea(2L, AT)).thenReturn(List.of(zone(Set.of(2L), Set.of())));
        when(isolation.activeZones()).thenReturn(List.of(zone(Set.of(), Set.of(9L))));
        when(incidents.suspectedAt(7L, AT)).thenReturn(true);
        when(alerts.hasOpenAtLeastMedium(7L)).thenReturn(false);
        when(incidents.hasOpenAtLeastMedium(7L)).thenReturn(true);

        assertThat(gate.residentIsolated(7L, AT)).isTrue();
        assertThat(gate.residentIsolated(8L, AT)).isFalse();
        assertThat(gate.locationIsolated(20L, null, null, AT)).isFalse();
        assertThat(gate.locationIsolated(null, null, 2L, AT)).isTrue();
        assertThat(gate.locationIsolated(null, 9L, null, AT)).isTrue();
        assertThat(gate.locationIsolated(null, null, null, AT)).isFalse();
        assertThat(gate.suspectedInfection(7L, AT)).isTrue();
        assertThat(gate.hasOpenMediumOrHigher(7L)).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void FR_021_creatingContainmentCancelsRegistrationsInTheSameCommand() {
        var store = mock(ContainmentStore.class);
        var locations = mock(LocationStructureApi.class);
        var time = mock(BusinessTime.class);
        ObjectProvider<CurrentUser> users = mock(ObjectProvider.class);
        var user = mock(CurrentUser.class);
        var autoCancel = mock(ActivityRegistrationAutoCancelApi.class);
        ObjectProvider<ActivityRegistrationAutoCancelApi> cancels = mock(ObjectProvider.class);
        when(cancels.getIfAvailable()).thenReturn(autoCancel);
        when(users.getObject()).thenReturn(user);
        when(time.now()).thenReturn(AT);
        when(locations.active(any())).thenReturn(true);
        when(incidents.lockOpenSuspected(Set.of(4L))).thenReturn(Set.of(4L));
        var scope = new ContainmentScope(Set.of(2L), Set.of());
        var content = new ContainmentContent(new ContainmentPeriod(scope, AT, null), "Cúm", Set.of(4L), null, null);
        when(store.create(any(), any(), any(Long.class), any(), any()))
                .thenReturn(new SimpleRecord<>(55, null, null, "ACTIVE", content, null, null, null, null, AT, 1, 0));
        var service = new ContainmentService(
                store,
                incidents,
                locations,
                mock(AuditLogApi.class),
                mock(ApplicationEventPublisher.class),
                time,
                users,
                cancels);

        service.create(scope, Set.of(4L), new Reason("Cúm A"));

        verify(autoCancel).cancelForIsolation(55L, AT);
    }
}
