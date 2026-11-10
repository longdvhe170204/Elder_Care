package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.care.internal.wellbeing.domain.IsolationStreak;
import com.eldercare.backend.support.TableRows;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CapNhatChuoiTest {
    @Test
    void FR_054_056_skipsWithoutBreakingResetsOnPresenceAndAlertsOncePerStreak() throws Exception {
        var day = LocalDate.of(2026, 11, 9);
        TableRows.each(List.of("COUNT", "SKIP", "PRESENT", "PRESENT_SKIP", "ALREADY_ALERTED"), row -> {
            var old = new IsolationStreak(6, day.minusDays(1), row.equals("ALREADY_ALERTED"));
            var next = old.advance(day, row.startsWith("PRESENT"), !row.contains("SKIP"), 7);
            assertThat(next.days()).as(row).isEqualTo(row.startsWith("PRESENT") ? 0 : row.equals("SKIP") ? 6 : 7);
            assertThat(next.alerted()).as(row).isEqualTo(row.equals("COUNT") || row.equals("ALREADY_ALERTED"));
            assertThat(next.lastDate()).isEqualTo(day);
            assertThat(next.advance(day, false, true, 7)).isEqualTo(next);
        });
    }
}
