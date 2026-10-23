package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.OpenAbsence;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.events.TemporaryAbsenceOverdue;
import com.cambae.backend.m01.internal.status.service.TemporaryAbsenceOverdueAlert;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T046 (BR-M01-01, FR-050; đồng hồ giả lập): Tạm vắng quá thời điểm dự kiến trở lại cộng CFG-M01-01 (2 giờ) mà chưa
 * Ghi nhận trở về thì cảnh báo đúng một lần cho mỗi mốc dự kiến; gia hạn tạo mốc mới thì cảnh báo lại.
 */
class BR_M01_01_TemporaryAbsenceOverdueIT extends StatusTestBase {

    private static long absenceSequence = System.nanoTime() % 1_000_000L;

    @Autowired
    TestClockControl clock;

    @Autowired
    TemporaryAbsenceOverdueAlert alert;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    private long overdueEvents(long residentId) {
        return events.stream(TemporaryAbsenceOverdue.class)
                .filter(e -> e.residentId() == residentId)
                .count();
    }

    private long absentUntil(long residentId, Duration fromNow) {
        long absenceId = ++absenceSequence;
        absences.open(residentId, new OpenAbsence(absenceId, time.now().plus(fromNow)));
        return absenceId;
    }

    @Test
    void BR_M01_01_notYetPastExpectedReturnPlusTwoHours_noAlert() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        absentUntil(id, Duration.ofHours(1));

        clock.advanceBy(Duration.ofHours(1).plusMinutes(119));
        alert.run();

        assertThat(overdueEvents(id)).isZero();
    }

    @Test
    void BR_M01_01_pastTheMilestone_alertsExactlyOnce_evenWhenTheJobRunsAgain() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        long absence = absentUntil(id, Duration.ofHours(1));
        var expected = time.now().plus(Duration.ofHours(1));

        clock.advanceBy(Duration.ofHours(3).plusMinutes(1));
        alert.run();
        alert.run();
        clock.advanceBy(Duration.ofHours(5));
        alert.run();

        assertThat(overdueEvents(id)).isEqualTo(1);
        assertThat(events.stream(TemporaryAbsenceOverdue.class).filter(e -> e.residentId() == id))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.absenceId()).isEqualTo(absence);
                    assertThat(e.expectedReturnAt()).isEqualTo(expected);
                });
    }

    @Test
    void BR_M01_01_extendingCreatesANewMilestone_andAlertsAgain() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        long absence = absentUntil(id, Duration.ofHours(1));
        clock.advanceBy(Duration.ofHours(4));
        alert.run();
        assertThat(overdueEvents(id)).isEqualTo(1);

        absences.open(id, new OpenAbsence(absence, time.now().plus(Duration.ofHours(1))));
        clock.advanceBy(Duration.ofHours(2));
        alert.run();
        assertThat(overdueEvents(id)).as("mốc mới chưa quá hạn").isEqualTo(1);

        clock.advanceBy(Duration.ofHours(2));
        alert.run();
        assertThat(overdueEvents(id)).isEqualTo(2);
    }

    @Test
    void BR_M01_01_afterRecordReturn_noAlert() {
        long id = residentIn(ResidentStatus.TEMPORARILY_ABSENT);
        absentUntil(id, Duration.ofHours(1));
        user.set(office.user());
        statusService.recordReturn(target(id), reason(), noDetails());
        absences.close(id);

        clock.advanceBy(Duration.ofHours(10));
        alert.run();

        assertThat(overdueEvents(id)).isZero();
    }

    @Test
    void BR_M01_01_onlyResidentsWhoAreTemporarilyAbsentAreChecked() {
        long inResidence = residentIn(ResidentStatus.IN_RESIDENCE);
        absentUntil(inResidence, Duration.ofHours(1));

        clock.advanceBy(Duration.ofHours(10));
        alert.run();

        assertThat(overdueEvents(inResidence)).isZero();
    }
}
