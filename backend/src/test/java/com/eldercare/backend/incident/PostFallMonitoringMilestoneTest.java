package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.incident.domain.FallMonitoringSetting;
import com.eldercare.backend.incident.internal.incident.domain.FallMonitoringSetting.Amount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** FR-051 (CHK013, research S-14): mốc theo dõi sau ngã = max(thời điểm xảy ra, thời điểm ghi nhận − khoảng đo). */
class PostFallMonitoringMilestoneTest {
    private final FallMonitoringSetting setting =
            new FallMonitoringSetting(new Amount(4, "HOURS"), new Amount(72, "HOURS"));
    private final Instant recorded = Instant.parse("2026-11-05T10:00:00Z");

    @Test
    void FR_051_noOccurredAtUsesRecordedAt() {
        assertThat(setting.anchor(null, recorded)).isEqualTo(recorded);
    }

    @Test
    void FR_051_recentOccurredAtIsKept() {
        var occurred = Instant.parse("2026-11-05T08:30:00Z");

        assertThat(setting.anchor(occurred, recorded)).isEqualTo(occurred);
    }

    @Test
    void FR_051_oldOccurredAtIsLimitedToOneIntervalBeforeRecording() {
        var occurred = Instant.parse("2026-11-04T10:00:00Z");

        assertThat(setting.anchor(occurred, recorded)).isEqualTo(Instant.parse("2026-11-05T06:00:00Z"));
    }

    @Test
    void FR_052_windowEndsAfterConfiguredDuration() {
        var anchor = setting.anchor(null, recorded);

        assertThat(anchor.plus(setting.duration().toDuration())).isEqualTo(Instant.parse("2026-11-08T10:00:00Z"));
    }
}
