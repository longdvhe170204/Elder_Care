package com.eldercare.backend.incident.internal.infection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.incident.internal.infection.domain.ContainmentScope;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** FR-085 (Q-311; T065): thông báo khoanh vùng mức Trung bình. */
class FR_085_ContainmentNoticeLevelTest {
    @Test
    void FR_085_isolationCreatedIsMedium() {
        var requests = ContainmentNotices.created(
                9L,
                new ContainmentScope(Set.of(3L), Set.of()),
                mock(LocationStructureApi.class),
                77L,
                Instant.parse("2026-11-06T03:00:00Z"));

        assertThat(requests)
                .isNotEmpty()
                .extracting(NotificationRequest::level)
                .containsOnly(Optional.of(NotificationLevel.MEDIUM));
    }
}
