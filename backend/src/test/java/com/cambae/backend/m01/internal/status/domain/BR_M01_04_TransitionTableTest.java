package com.cambae.backend.m01.internal.status.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.api.dto.StatusCommand;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 001 T043 (BR-M01-04, FR-044): mọi cặp (trạng thái, lệnh) của bảng chuyển ở data-model mục 2; cặp ngoài bảng không có
 * trạng thái đến (service trả 409 {@code INVALID_TRANSITION}).
 */
class BR_M01_04_TransitionTableTest {

    private static final ResidentStatus ADMISSION = ResidentStatus.ADMISSION_IN_PROGRESS;
    private static final ResidentStatus IN = ResidentStatus.IN_RESIDENCE;
    private static final ResidentStatus ABSENT = ResidentStatus.TEMPORARILY_ABSENT;
    private static final ResidentStatus OFF_SITE = ResidentStatus.OFF_SITE_ACTIVITY;
    private static final ResidentStatus HOSPITAL = ResidentStatus.HOSPITALIZED;

    /** Bảng kỳ vọng viết lại độc lập với mã: từ trạng thái → (lệnh → trạng thái đến). */
    private static Map<ResidentStatus, Map<StatusCommand, ResidentStatus>> expected() {
        Map<ResidentStatus, Map<StatusCommand, ResidentStatus>> table = new EnumMap<>(ResidentStatus.class);
        table.put(
                ADMISSION,
                Map.of(
                        StatusCommand.COMPLETE_ADMISSION,
                        IN,
                        StatusCommand.CANCEL_ADMISSION,
                        ResidentStatus.ADMISSION_CANCELLED));
        table.put(
                IN,
                Map.of(
                        StatusCommand.GRANT_TEMPORARY_ABSENCE, ABSENT,
                        StatusCommand.ROLL_CALL_DEPARTURE, OFF_SITE,
                        StatusCommand.TRANSFER_OUT, HOSPITAL,
                        StatusCommand.END_RESIDENCY, ResidentStatus.RESIDENCY_ENDED,
                        StatusCommand.RECORD_DEATH, ResidentStatus.DECEASED));
        table.put(
                ABSENT,
                Map.of(
                        StatusCommand.RECORD_RETURN,
                        IN,
                        StatusCommand.TRANSFER_OUT,
                        HOSPITAL,
                        StatusCommand.END_RESIDENCY,
                        ResidentStatus.RESIDENCY_ENDED,
                        StatusCommand.RECORD_DEATH,
                        ResidentStatus.DECEASED));
        table.put(
                OFF_SITE,
                Map.of(
                        StatusCommand.ROLL_CALL_RETURN, IN,
                        StatusCommand.CANCEL_DEPARTURE_RECORD, IN,
                        StatusCommand.TRANSFER_OUT, HOSPITAL,
                        StatusCommand.RECORD_DEATH, ResidentStatus.DECEASED));
        table.put(
                HOSPITAL,
                Map.of(
                        StatusCommand.RECORD_RETURN, IN,
                        StatusCommand.END_RESIDENCY, ResidentStatus.RESIDENCY_ENDED,
                        StatusCommand.RECORD_DEATH, ResidentStatus.DECEASED));
        return table;
    }

    @Test
    void BR_M01_04_everyPairMatchesTheTable() {
        var expected = expected();
        for (ResidentStatus from : ResidentStatus.values()) {
            for (StatusCommand command : StatusCommand.values()) {
                ResidentStatus want = expected.getOrDefault(from, Map.of()).get(command);
                assertThat(StatusTransitionTable.next(from, command))
                        .as("%s + %s", from, command)
                        .isEqualTo(java.util.Optional.ofNullable(want));
            }
        }
    }

    @Test
    void BR_M01_05_finalStatusesHaveNoCommand() {
        for (ResidentStatus finalStatus : StatusTransitionTable.FINAL_STATUSES) {
            for (StatusCommand command : StatusCommand.values()) {
                assertThat(StatusTransitionTable.next(finalStatus, command)).isEmpty();
            }
        }
        assertThat(StatusTransitionTable.FINAL_STATUSES)
                .containsExactlyInAnyOrder(
                        ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED);
    }

    @Test
    void FR_044_createProfileIsNeverATransition() {
        for (ResidentStatus from : ResidentStatus.values()) {
            assertThat(StatusTransitionTable.next(from, StatusCommand.CREATE_PROFILE))
                    .isEmpty();
        }
    }

    @Test
    void FR_046_systemCommandsAreTheThreeFromAttendance() {
        assertThat(StatusTransitionTable.SYSTEM_COMMANDS)
                .containsExactlyInAnyOrder(
                        StatusCommand.ROLL_CALL_DEPARTURE,
                        StatusCommand.ROLL_CALL_RETURN,
                        StatusCommand.CANCEL_DEPARTURE_RECORD);
    }
}
