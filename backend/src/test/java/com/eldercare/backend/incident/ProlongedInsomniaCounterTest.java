package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.incident.internal.trend.domain.ProlongedInsomniaCounter;
import com.eldercare.backend.incident.internal.trend.domain.ProlongedInsomniaCounter.SleepResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * FR-039c, CHK045 (research S-10, hàm thuần "DemDemMatNgu"): đếm đêm mất ngủ liên tiếp lùi từ đêm của kết quả vừa ghi;
 * mỗi ngày lấy kết quả hiện hành mới nhất; đêm không có ghi nhận làm đứt chuỗi; đêm vắng mặt không tính, không làm đứt.
 */
class ProlongedInsomniaCounterTest {
    private static final LocalDate D = LocalDate.of(2026, 11, 5);
    private static final String INSOMNIA = ProlongedInsomniaCounter.INSOMNIA;

    @Test
    void FR_039c_threeConsecutiveInsomniaNightsCountThree() {
        var results = List.of(night(D.minusDays(2), INSOMNIA), night(D.minusDays(1), INSOMNIA), night(D, INSOMNIA));

        assertThat(ProlongedInsomniaCounter.consecutiveNights(D, results, Set.of()))
                .isEqualTo(3);
    }

    @Test
    void FR_039c_nightWithoutRecordBreaksTheChain() {
        var results = List.of(night(D.minusDays(3), INSOMNIA), night(D.minusDays(1), INSOMNIA), night(D, INSOMNIA));

        assertThat(ProlongedInsomniaCounter.consecutiveNights(D, results, Set.of()))
                .isEqualTo(2);
    }

    @Test
    void CHK045_latestCurrentResultOfTheDayWins() {
        var overridden = List.of(
                night(D.minusDays(1), INSOMNIA),
                result(D, "2026-11-05T22:00:00Z", INSOMNIA),
                result(D, "2026-11-05T23:30:00Z", null));
        var confirmed = List.of(
                night(D.minusDays(1), INSOMNIA),
                result(D, "2026-11-05T23:30:00Z", INSOMNIA),
                result(D, "2026-11-05T22:00:00Z", null));

        assertThat(ProlongedInsomniaCounter.consecutiveNights(D, overridden, Set.of()))
                .isZero();
        assertThat(ProlongedInsomniaCounter.consecutiveNights(D, confirmed, Set.of()))
                .isEqualTo(2);
    }

    private static SleepResult night(LocalDate night, String group) {
        return new SleepResult(night, night.atStartOfDay().toInstant(java.time.ZoneOffset.UTC), group);
    }

    private static SleepResult result(LocalDate night, String recordedAt, String group) {
        return new SleepResult(night, Instant.parse(recordedAt), group);
    }
}
