package com.cambae.backend.m02.internal.waitinglist.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.internal.waitinglist.domain.ScoreBreakdown;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 004 T020 (US2; FR-009, BR-M02-10): điểm = điểm mức + chu kỳ chờ trọn vẹn × điểm mỗi chu kỳ + tình huống đã xác nhận +
 * điều chỉnh đã duyệt. Hàm thuần theo bảng CFG-M02-08.
 */
class BR_M02_10_PriorityScoreTest {

    private static final LocalDate REGISTERED = LocalDate.of(2026, 10, 1);

    private static final PriorityScoreTable TABLE = new PriorityScoreTable(
            7,
            1,
            Map.of(CareLevel.BASIC, 0, CareLevel.REGULAR, 2, CareLevel.SPECIAL, 5),
            Map.of("LIVING_ALONE", 2, "JUST_DISCHARGED", 3));

    private static ScoreBreakdown score(
            PriorityScoreTable table, CareLevel level, int waitedDays, Set<String> circumstances, int adjustment) {
        return PriorityScoreCalculator.compute(
                table, level, REGISTERED, REGISTERED.plusDays(waitedDays), circumstances, adjustment);
    }

    @Test
    void FR_009_specialLivingAlone_waited15Days_is5Plus2Plus2() {
        ScoreBreakdown breakdown = score(TABLE, CareLevel.SPECIAL, 15, Set.of("LIVING_ALONE"), 0);

        assertThat(breakdown).isEqualTo(new ScoreBreakdown(5, 2, 2, 0));
        assertThat(breakdown.total()).isEqualTo(9);
    }

    @Test
    void FR_009_waitingPoints_areWholeCyclesOnly() {
        assertThat(score(TABLE, CareLevel.BASIC, 0, Set.of(), 0).waitingTime()).isZero();
        assertThat(score(TABLE, CareLevel.BASIC, 6, Set.of(), 0).waitingTime()).isZero();
        assertThat(score(TABLE, CareLevel.BASIC, 7, Set.of(), 0).waitingTime()).isEqualTo(1);
        assertThat(score(TABLE, CareLevel.BASIC, 13, Set.of(), 0).waitingTime()).isEqualTo(1);
        assertThat(score(TABLE, CareLevel.BASIC, 14, Set.of(), 0).waitingTime()).isEqualTo(2);
    }

    @Test
    void BR_M02_10_oneMoreCycle_addsOnePoint() {
        int day15 =
                score(TABLE, CareLevel.SPECIAL, 15, Set.of("LIVING_ALONE"), 0).total();
        int day22 =
                score(TABLE, CareLevel.SPECIAL, 22, Set.of("LIVING_ALONE"), 0).total();

        assertThat(day22 - day15).isEqualTo(1);
    }

    @Test
    void FR_011_approvedAdjustments_areAddedAndMayBeNegative() {
        assertThat(score(TABLE, CareLevel.REGULAR, 0, Set.of(), 3).total()).isEqualTo(2 + 3);
        assertThat(score(TABLE, CareLevel.REGULAR, 0, Set.of(), -2).total()).isEqualTo(2 - 2);
    }

    @Test
    void FR_009_circumstancePoints_areSummed_andUnknownCodeScoresZero() {
        ScoreBreakdown breakdown = score(TABLE, CareLevel.BASIC, 0, Set.of("LIVING_ALONE", "JUST_DISCHARGED"), 0);
        assertThat(breakdown.circumstances()).isEqualTo(5);

        assertThat(score(TABLE, CareLevel.BASIC, 0, Set.of("NOT_IN_TABLE"), 0).circumstances())
                .isZero();
    }

    @Test
    void FR_009_dayBeforeRegistration_waitsZeroCycles() {
        assertThat(PriorityScoreCalculator.compute(
                                TABLE, CareLevel.BASIC, REGISTERED, REGISTERED.minusDays(3), Set.of(), 0)
                        .waitingTime())
                .isZero();
    }

    @Test
    void CFG_M02_08_unsetTable_scoresOnlyWaitingTimeAndAdjustment() {
        PriorityScoreTable unset = new PriorityScoreTable(7, 1, null, null);

        assertThat(score(unset, CareLevel.SPECIAL, 15, Set.of("LIVING_ALONE"), 4))
                .isEqualTo(new ScoreBreakdown(0, 2, 0, 4));
    }

    @Test
    void CFG_M02_08_noWaitingPeriod_givesNoWaitingPoints() {
        PriorityScoreTable noPeriod = new PriorityScoreTable(0, 1, null, null);

        assertThat(score(noPeriod, CareLevel.BASIC, 100, Set.of(), 0).waitingTime())
                .isZero();
    }
}
