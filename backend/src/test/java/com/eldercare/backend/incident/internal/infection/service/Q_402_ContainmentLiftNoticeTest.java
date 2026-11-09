package com.eldercare.backend.incident.internal.infection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Q-402, FR-085: gỡ khoanh vùng báo cùng người nhận như lúc đặt (Hành chính, Trưởng tầng, Điều dưỡng trong ca), mức Nhẹ. */
class Q_402_ContainmentLiftNoticeTest {
    @Test
    void Q_402_liftNotifiesSameRecipientsAsCreationAtLowLevel() {
        var locations = mock(LocationStructureApi.class);
        when(locations.floorsInArea(9L)).thenReturn(Set.of(3L));
        var scope = new ContainmentScope(Set.of(), Set.of(9L));
        var at = Instant.parse("2026-11-08T03:00:00Z");

        var created = ContainmentNotices.created(5, scope, locations, 1, at).get(0);
        var lifted = ContainmentNotices.lifted(5, scope, locations, 1, at).get(0);

        assertThat(lifted.recipientGroups()).containsExactlyInAnyOrderElementsOf(created.recipientGroups());
        assertThat(lifted.recipientGroups())
                .contains(
                        new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                        new RecipientGroup.FloorManager(3L, new Substitution.Default()));
        assertThat(lifted.level()).contains(NotificationLevel.LOW);
        assertThat(lifted.eventKey()).isEqualTo("M05.CONTAINMENT:5:LIFTED");
        assertThat(lifted.eventType()).isEqualTo("ISOLATION_LIFTED");
    }
}
