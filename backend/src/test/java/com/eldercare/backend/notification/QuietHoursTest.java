package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.notification.internal.quiethours.domain.QuietHours;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** FR-024, BR-M13-03 (009 T029, tên logic {@code GioYenTinhTest}): khoảng qua nửa đêm, mốc bắt đầu thuộc, kết thúc không. */
class QuietHoursTest {

    private static final QuietHours NIGHT = new QuietHours(LocalTime.of(21, 0), LocalTime.of(7, 0));

    private static Instant at(String local) {
        return OffsetDateTime.parse(local + "+07:00").toInstant();
    }

    @Test
    void FR_024_acrossMidnight_endIsNextMorning() {
        assertThat(NIGHT.endIfWithin(at("2030-03-04T22:30:00"))).contains(at("2030-03-05T07:00:00"));
        assertThat(NIGHT.endIfWithin(at("2030-03-05T05:30:00"))).contains(at("2030-03-05T07:00:00"));
    }

    @Test
    void FR_024_startBelongs_endDoesNot() {
        assertThat(NIGHT.endIfWithin(at("2030-03-04T21:00:00"))).contains(at("2030-03-05T07:00:00"));
        assertThat(NIGHT.endIfWithin(at("2030-03-05T07:00:00"))).isEmpty();
        assertThat(NIGHT.endIfWithin(at("2030-03-04T20:59:59"))).isEmpty();
        assertThat(NIGHT.endIfWithin(at("2030-03-05T10:00:00"))).isEmpty();
    }

    @Test
    void FR_024_sameDayWindow() {
        QuietHours noon = new QuietHours(LocalTime.of(12, 0), LocalTime.of(13, 30));

        assertThat(noon.endIfWithin(at("2030-03-04T12:15:00"))).contains(at("2030-03-04T13:30:00"));
        assertThat(noon.endIfWithin(at("2030-03-04T13:30:00"))).isEmpty();
        assertThat(noon.endIfWithin(at("2030-03-04T11:00:00"))).isEmpty();
    }

    @Test
    void FR_024_emptyWindow_neverQuiet() {
        QuietHours none = new QuietHours(LocalTime.of(21, 0), LocalTime.of(21, 0));

        assertThat(none.endIfWithin(at("2030-03-04T21:00:00"))).isEqualTo(Optional.empty());
    }
}
