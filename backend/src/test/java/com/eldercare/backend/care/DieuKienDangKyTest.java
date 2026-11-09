package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.internal.session.domain.RegistrationConditions;
import com.eldercare.backend.care.internal.session.domain.RegistrationConditions.DayCare;
import com.eldercare.backend.care.internal.session.domain.RegistrationConditions.Facts;
import com.eldercare.backend.common.error.UnmetCondition;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** 014 T010 (FR-015 → FR-018, BR-M04-15; research L-05 {@code DieuKienDangKy}): điều kiện chặn và cảnh báo. */
class DieuKienDangKyTest {
    static Facts ok() {
        return new Facts(
                "SCHEDULED", false, true, 1, 20, false, false, false, false, false, false, null, Optional.empty());
    }

    static java.util.List<String> blockers(Facts f) {
        return RegistrationConditions.check(f).blockers().stream()
                .map(UnmetCondition::code)
                .toList();
    }

    static java.util.List<String> warnings(Facts f) {
        return RegistrationConditions.check(f).warnings().stream()
                .map(UnmetCondition::code)
                .toList();
    }

    @Test
    void FR_015_noConditionMeansNoBlockerNoWarning() {
        assertThat(blockers(ok())).isEmpty();
        assertThat(warnings(ok())).isEmpty();
    }

    @Test
    void FR_015a_capacityFull() {
        var f = new Facts(
                "SCHEDULED", false, true, 20, 20, false, false, false, false, false, false, null, Optional.empty());
        assertThat(blockers(f)).containsExactly("CAPACITY_FULL");
    }

    @Test
    void FR_015c_isolationOfResidentOrLocation() {
        var resident = new Facts(
                "SCHEDULED", false, true, 0, 20, false, true, false, false, false, false, null, Optional.empty());
        var location = new Facts(
                "SCHEDULED", false, true, 0, 20, false, false, true, false, false, false, null, Optional.empty());
        assertThat(blockers(resident)).containsExactly("ISOLATION_ACTIVE");
        assertThat(blockers(location)).containsExactly("ISOLATION_ACTIVE");
    }

    @Test
    void FR_015d_residentStatus() {
        var f = new Facts(
                "SCHEDULED", false, true, 0, 20, true, false, false, false, false, false, null, Optional.empty());
        assertThat(blockers(f)).containsExactly("RESIDENT_STATUS_INVALID");
    }

    @Test
    void FR_015e_sessionNotScheduledOrStarted() {
        var started = new Facts(
                "SCHEDULED", true, true, 0, 20, false, false, false, false, false, false, null, Optional.empty());
        var cancelled = new Facts(
                "CANCELLED", false, true, 0, 20, false, false, false, false, false, false, null, Optional.empty());
        assertThat(blockers(started)).containsExactly("SESSION_ALREADY_STARTED");
        assertThat(blockers(cancelled)).containsExactly("STATE_CHANGED");
    }

    @Test
    void FR_015g_infectionBlocksGroupActivityAndWarnsForIndividual() {
        var group = new Facts(
                "SCHEDULED", false, true, 0, 20, false, false, false, true, false, false, null, Optional.empty());
        var individual = new Facts(
                "SCHEDULED", false, false, 0, 20, false, false, false, true, false, false, null, Optional.empty());
        assertThat(blockers(group)).containsExactly("INFECTION_SUSPECTED");
        assertThat(blockers(individual)).isEmpty();
        assertThat(warnings(individual)).containsExactly("INFECTION_SUSPECTED");
    }

    @Test
    void FR_016_overlap() {
        var f = new Facts(
                "SCHEDULED", false, true, 0, 20, false, false, false, false, false, true, null, Optional.empty());
        assertThat(blockers(f)).containsExactly("TIME_OVERLAP");
    }

    @Test
    void FR_016_dayCareScheduleAndFR_016a_lateReturnNeedsConsent() {
        var noSchedule = new DayCare(LocalTime.of(9, 0), LocalTime.of(10, 0), null, null, false);
        var beforeArrival =
                new DayCare(LocalTime.of(7, 0), LocalTime.of(8, 0), LocalTime.of(7, 30), LocalTime.of(16, 0), false);
        var lateNoConsent =
                new DayCare(LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(7, 30), LocalTime.of(16, 0), false);
        var lateConsent =
                new DayCare(LocalTime.of(15, 0), LocalTime.of(17, 0), LocalTime.of(7, 30), LocalTime.of(16, 0), true);
        assertThat(blockers(withDayCare(noSchedule))).containsExactly("OUTSIDE_DAY_CARE_SCHEDULE");
        assertThat(blockers(withDayCare(beforeArrival))).containsExactly("OUTSIDE_DAY_CARE_SCHEDULE");
        assertThat(blockers(withDayCare(lateNoConsent))).containsExactly("LATE_RETURN_CONSENT_REQUIRED");
        assertThat(blockers(withDayCare(lateConsent))).isEmpty();
    }

    @Test
    void FR_018_careNoteAndOpenMediumItemAreWarningsOnly() {
        var f = new Facts(
                "SCHEDULED",
                false,
                true,
                0,
                20,
                false,
                false,
                false,
                false,
                true,
                false,
                null,
                Optional.of("Hạn chế vận động mạnh"));
        assertThat(blockers(f)).isEmpty();
        assertThat(warnings(f)).containsExactly("CARE_NOTE", "OPEN_ALERT_OR_INCIDENT");
        assertThat(RegistrationConditions.check(f).warnings().getFirst().description())
                .contains("Hạn chế vận động mạnh");
    }

    static Facts withDayCare(DayCare d) {
        return new Facts(
                "SCHEDULED", false, true, 0, 20, false, false, false, false, false, false, d, Optional.empty());
    }
}
