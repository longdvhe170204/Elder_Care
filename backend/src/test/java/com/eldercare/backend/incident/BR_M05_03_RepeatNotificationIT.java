package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.api.events.AlertRepeated;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * Báo lặp lại (BR-M05-03, FR-038a, Q-307): đạt CFG-M05-02 (mặc định 3 lần trong 24 giờ trượt) phát sinh mức Trung bình
 * thì báo người phụ trách và Bác sĩ trực số lần; gửi lại ở mỗi lần tiếp theo khi còn đúng; không đề xuất nâng mức.
 */
class BR_M05_03_RepeatNotificationIT extends AlertRequestTestBase {
    private static final String KEY = "VUOT_NGUONG:31";

    @Test
    void BR_M05_03_thirdMediumOccurrenceWithin24HoursNotifiesAndRepeatsAfter() {
        request(KEY, IncidentLevel.MEDIUM, T0, 1);
        request(KEY, IncidentLevel.MEDIUM, T0.plus(Duration.ofHours(5)), 2);
        assertThat(published).filteredOn(AlertRepeated.class::isInstance).isEmpty();

        request(KEY, IncidentLevel.MEDIUM, T0.plus(Duration.ofHours(10)), 3);
        request(KEY, IncidentLevel.MEDIUM, T0.plus(Duration.ofHours(11)), 4);

        assertThat(published)
                .filteredOn(AlertRepeated.class::isInstance)
                .extracting(event -> ((AlertRepeated) event).count())
                .containsExactly(3, 4);
        assertThat(published)
                .filteredOn(AlertRepeated.class::isInstance)
                .allSatisfy(event -> assertThat(((AlertRepeated) event).notificationRequests())
                        .hasSize(1));
    }

    @Test
    void FR_038a_occurrencesOlderThan24HoursAreNotCounted() {
        request(KEY, IncidentLevel.MEDIUM, T0, 1);
        request(KEY, IncidentLevel.MEDIUM, T0.plus(Duration.ofHours(23)), 2);
        request(KEY, IncidentLevel.MEDIUM, T0.plus(Duration.ofHours(25)), 3);

        assertThat(published).filteredOn(AlertRepeated.class::isInstance).isEmpty();
    }
}
