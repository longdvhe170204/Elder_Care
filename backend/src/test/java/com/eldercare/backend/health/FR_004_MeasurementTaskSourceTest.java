package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceView;
import com.eldercare.backend.health.api.dto.MeasurementFrequency;
import com.eldercare.backend.health.internal.schedule.domain.MeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.NewMeasurementSchedule;
import com.eldercare.backend.health.internal.schedule.domain.ScheduleFrequency;
import com.eldercare.backend.health.internal.schedule.persistence.MeasurementScheduleStore;
import com.eldercare.backend.health.internal.schedule.service.MeasurementTaskSource;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 007 FR-004, T023 (research S-05; 005 FR-017, FR-023, Q-306): bean nguồn công việc {@code MEASUREMENT} của {@code health}.
 * Căn cứ lịch đo hợp lệ khi lịch còn là căn cứ tại mốc, đúng người, mốc thuộc chuỗi mốc đo; lịch mức Bắt buộc trả "theo chỉ
 * định" để 005 buộc công việc mức Bắt buộc. Chỉ đọc: không tạo công việc.
 */
class FR_004_MeasurementTaskSourceTest {

    private static final long RESIDENT = 101;
    /** 2026-11-04 00:00 giờ viện; lịch đo 06:00 và 18:00 mỗi ngày. */
    private static final Instant START = Instant.parse("2026-11-03T17:00:00Z");

    private static final Instant SIX = Instant.parse("2026-11-03T23:00:00Z");
    private static final Instant EIGHTEEN = Instant.parse("2026-11-04T11:00:00Z");

    private final MeasurementScheduleStore store = mock(MeasurementScheduleStore.class);
    private final MeasurementTaskSource source = new MeasurementTaskSource(store);

    private static MeasurementSchedule schedule(long id, String priority, Instant stoppedAt) {
        var frequency = new ScheduleFrequency(
                MeasurementFrequency.CLOCK_TIMES,
                List.of(LocalTime.parse("06:00"), LocalTime.parse("18:00")),
                null,
                null,
                Set.of());
        return new MeasurementSchedule(
                id,
                RESIDENT,
                new NewMeasurementSchedule(Set.of(3L), frequency, START, null, priority, null),
                stoppedAt == null ? "EFFECTIVE" : "STOPPED",
                null,
                stoppedAt,
                null,
                1);
    }

    @Test
    void FR_004_scheduleIsAValidBasisOnlyForItsResidentAtItsOccurrences() {
        when(store.find(5)).thenReturn(Optional.of(schedule(5, "MANDATORY", null)));
        when(store.find(6)).thenReturn(Optional.of(schedule(6, "NORMAL", EIGHTEEN)));
        when(store.find(9)).thenReturn(Optional.empty());

        var atOccurrence = source.validate(SourceType.MEASUREMENT, 5, RESIDENT, SIX);
        assertThat(atOccurrence.valid()).isTrue();
        assertThat(atOccurrence.residentMatches()).isTrue();
        assertThat(atOccurrence.timeMatches()).isTrue();
        assertThat(atOccurrence.prescribed()).as("lịch mức Bắt buộc").isTrue();

        var offSeries = source.validate(SourceType.MEASUREMENT, 5, RESIDENT, SIX.plusSeconds(1800));
        assertThat(offSeries.valid()).isFalse();
        assertThat(offSeries.timeMatches()).isFalse();

        var otherResident = source.validate(SourceType.MEASUREMENT, 5, RESIDENT + 1, SIX);
        assertThat(otherResident.valid()).isFalse();
        assertThat(otherResident.residentMatches()).isFalse();

        assertThat(source.validate(SourceType.MEASUREMENT, 6, RESIDENT, SIX).prescribed())
                .as("lịch mức Thường")
                .isFalse();
        assertThat(source.validate(SourceType.MEASUREMENT, 6, RESIDENT, EIGHTEEN)
                        .valid())
                .as("từ thời điểm ngừng lịch không còn là căn cứ")
                .isFalse();
        assertThat(source.validate(SourceType.MEASUREMENT, 9, RESIDENT, SIX).valid())
                .isFalse();
        assertThat(source.managedTypes()).containsExactly(SourceType.MEASUREMENT);
    }

    @Test
    void FR_017_sourcesListTheOccurrencesInsideTheShiftWindow() {
        when(store.byResident(RESIDENT))
                .thenReturn(List.of(schedule(5, "MANDATORY", null), schedule(6, "NORMAL", EIGHTEEN)));

        List<SourceView> views = source.sources(RESIDENT, SIX, EIGHTEEN.plusSeconds(3600));

        assertThat(views)
                .extracting(SourceView::type, SourceView::id, SourceView::at)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(SourceType.MEASUREMENT, 5L, SIX),
                        org.assertj.core.groups.Tuple.tuple(SourceType.MEASUREMENT, 5L, EIGHTEEN),
                        org.assertj.core.groups.Tuple.tuple(SourceType.MEASUREMENT, 6L, SIX));
    }
}
