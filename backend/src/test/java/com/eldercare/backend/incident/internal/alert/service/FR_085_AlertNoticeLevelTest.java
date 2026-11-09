package com.eldercare.backend.incident.internal.alert.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.incident.internal.alert.domain.AlertRecord;
import com.eldercare.backend.incident.internal.alert.domain.AlertStatus;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * FR-085 (Q-311; T065): mức của thông báo cảnh báo mà spec không nêu mức: báo lặp lại Trung bình; nguồn đã được đính
 * chính và người phụ trách đổi (lệnh Giao người phụ trách) Nhẹ.
 */
class FR_085_AlertNoticeLevelTest {
    private static final Instant NOW = Instant.parse("2026-11-06T03:00:00Z");

    @Test
    void FR_085_repeatedIsMedium() {
        assertThat(levels(AlertNotices.repeated(alert(), 3, "k", NOW)))
                .containsOnly(Optional.of(NotificationLevel.MEDIUM));
    }

    @Test
    void FR_085_sourceCorrectedIsLow() {
        assertThat(levels(AlertNotices.sourceCorrected(alert(), "k", NOW)))
                .containsOnly(Optional.of(NotificationLevel.LOW));
    }

    private static List<Optional<NotificationLevel>> levels(List<NotificationRequest> requests) {
        assertThat(requests).isNotEmpty();
        return requests.stream().map(NotificationRequest::level).toList();
    }

    private static AlertRecord alert() {
        return new AlertRecord(
                40,
                101L,
                7L,
                "VITAL_SIGN",
                "CHI_SO:1",
                IncidentLevel.MEDIUM,
                null,
                1,
                NOW,
                NOW,
                AlertStatus.RECEIVED,
                0,
                null,
                null,
                0,
                800L,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                false,
                false,
                900L,
                null,
                null,
                null,
                null,
                false,
                1);
    }
}
