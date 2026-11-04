package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.ResidentStatisticsApi;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidentChangeView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.ResidentStatusAtDateView;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bản thật {@link ResidentStatisticsApi} cho 016 (module-interfaces 001 mục 1.8a; 016 FR-022, FR-023): trạng thái và mức
 * chăm sóc hiệu lực tại ngày, biến động trong khoảng, lọc theo phạm vi (toàn viện hoặc tập hồ sơ do 002 tính).
 */
class ResidentStatisticsApiIT extends StatusTestBase {

    @Autowired
    ResidentStatisticsApi statistics;

    @Autowired
    TestClockControl clock;

    /** Sau test về giờ thật, để tài khoản của test sau không nhận vai trò từ ngày giả lập. */
    @BeforeEach
    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private void careLevel(long residentId, CareLevel level, Instant from) {
        jdbc.update(
                "INSERT INTO care_level_history (resident_id, care_level, effective_from, basis_type, basis_id,"
                        + " recorded_at, recorded_by) VALUES (?, ?, ?, 'ASSESSMENT', 1, ?, ?)",
                residentId,
                level.name(),
                java.sql.Timestamp.from(from),
                java.sql.Timestamp.from(from),
                office.accountId());
        jdbc.update("UPDATE resident SET current_care_level = ? WHERE id = ?", level.name(), residentId);
    }

    private List<ResidentStatusAtDateView> on(LocalDate date, long residentId) {
        return statistics
                .statusAtDate(new ScopeFilter(false, Set.of(), Set.of(), Set.of(residentId)), date)
                .toList();
    }

    @Test
    void statusAtDate_isTheLatestStatusAndCareLevelEffectiveByTheEndOfThatDay() {
        LocalDate day1 = time.today();
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        careLevel(id, CareLevel.BASIC, time.now());

        assertThat(on(day1.minusDays(1), id)).as("chưa có hồ sơ trước ngày tạo").isEmpty();
        assertThat(on(day1, id))
                .containsExactly(
                        new ResidentStatusAtDateView(id, ResidentStatus.IN_RESIDENCE, Optional.of(CareLevel.BASIC)));

        clock.advanceBy(Duration.ofDays(2));
        moveTo(id, ResidentStatus.HOSPITALIZED);
        careLevel(id, CareLevel.SPECIAL, time.now());

        assertThat(on(day1, id))
                .as("ngày cũ giữ giá trị cũ")
                .containsExactly(
                        new ResidentStatusAtDateView(id, ResidentStatus.IN_RESIDENCE, Optional.of(CareLevel.BASIC)));
        assertThat(on(time.today(), id))
                .containsExactly(
                        new ResidentStatusAtDateView(id, ResidentStatus.HOSPITALIZED, Optional.of(CareLevel.SPECIAL)));
    }

    @Test
    void statusAtDate_withoutAssessedCareLevel_hasEmptyCareLevel_andScopeExcludesOthers() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        long other = residentIn(ResidentStatus.IN_RESIDENCE);

        assertThat(on(time.today(), id))
                .containsExactly(new ResidentStatusAtDateView(id, ResidentStatus.IN_RESIDENCE, Optional.empty()));
        assertThat(on(time.today(), id))
                .extracting(ResidentStatusAtDateView::residentId)
                .doesNotContain(other);
        assertThat(statistics
                        .statusAtDate(ScopeFilter.FACILITY, time.today())
                        .map(ResidentStatusAtDateView::residentId))
                .contains(id, other);
    }

    @Test
    void changes_listStatusCommandsAndCareLevelChangesInsideTheHalfOpenRange() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        clock.advanceBy(Duration.ofHours(1));
        Instant from = time.now();
        moveTo(id, ResidentStatus.HOSPITALIZED);
        careLevel(id, CareLevel.REGULAR, time.now());
        clock.advanceBy(Duration.ofHours(1));
        Instant to = time.now();
        moveTo(id, ResidentStatus.IN_RESIDENCE);

        List<ResidentChangeView> changes = statistics
                .changes(new ScopeFilter(false, Set.of(), Set.of(), Set.of(id)), from, to)
                .toList();

        assertThat(changes).hasSize(2);
        assertThat(changes)
                .anySatisfy(c -> {
                    assertThat(c.fromStatus()).contains(ResidentStatus.IN_RESIDENCE);
                    assertThat(c.toStatus()).contains(ResidentStatus.HOSPITALIZED);
                    assertThat(c.careLevelAfter()).isEmpty();
                })
                .anySatisfy(c -> {
                    assertThat(c.careLevelAfter()).contains(CareLevel.REGULAR);
                    assertThat(c.toStatus()).isEmpty();
                });
        assertThat(changes).allSatisfy(c -> assertThat(c.at()).isBetween(from, to));
    }
}
