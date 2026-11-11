package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.support.ResidentHistoryRows;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Đổi api 001 (chủ dự án duyệt 2026-11-11; Q-01, 014 FR-031a (c)): lệnh điểm danh rời viện, về ghi ngoại tuyến mang
 * thời điểm thực tế trên thiết bị vào lịch sử trạng thái; thời điểm không ở tương lai, không trước lần đổi trạng thái
 * trước, chỉ cho hai lệnh điểm danh. Lệnh mới Hủy ghi nhận về đưa người cao tuổi từ Đang lưu trú về Hoạt động bên ngoài.
 */
class Q01_RollCallDeviceTimeIT extends StatusTestBase {

    @Autowired
    TestClockControl clock;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private Instant historyAt(long resident) {
        return jdbc.queryForObject(
                        "SELECT occurred_at FROM " + ResidentHistoryRows.STATUS
                                + " WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                        java.time.LocalDateTime.class,
                        resident)
                .toInstant(java.time.ZoneOffset.UTC); // lịch sử chung lưu UTC
    }

    @Test
    void Q01_deviceTimeIsRecorded_futureOrBeforePreviousOrOtherCommandIsRejected() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        Instant seeded = historyAt(resident);
        clock.advanceBy(Duration.ofHours(1));
        Instant device = seeded.plus(Duration.ofMinutes(30));

        assertThat(failure(() -> inTransaction(() -> statusApi.execute(
                        resident,
                        StatusCommand.ROLL_CALL_DEPARTURE,
                        office.user(),
                        Optional.empty(),
                        details("tripId", 9),
                        sourceRecord(9),
                        time.now().plusSeconds(60)))))
                .as("thời điểm ở tương lai")
                .isNotNull();
        assertThat(failure(() -> inTransaction(() -> statusApi.execute(
                        resident,
                        StatusCommand.ROLL_CALL_DEPARTURE,
                        office.user(),
                        Optional.empty(),
                        details("tripId", 9),
                        sourceRecord(9),
                        seeded.minusSeconds(60)))))
                .as("trước lần đổi trạng thái trước")
                .isNotNull();
        assertThat(failure(() -> inTransaction(() -> statusApi.execute(
                        resident,
                        StatusCommand.TRANSFER_OUT,
                        office.user(),
                        Optional.of(reason()),
                        noDetails(),
                        sourceRecord(9),
                        device))))
                .as("chỉ hai lệnh điểm danh nhận thời điểm thiết bị")
                .isNotNull();
        assertThat(currentStatus(resident)).isEqualTo(ResidentStatus.IN_RESIDENCE);

        var result = inTransaction(() -> statusApi.execute(
                resident,
                StatusCommand.ROLL_CALL_DEPARTURE,
                office.user(),
                Optional.empty(),
                details("tripId", 9),
                sourceRecord(9),
                device));
        assertThat(result.at()).isEqualTo(device);
        assertThat(historyAt(resident)).isEqualTo(device);
        assertThat(currentStatus(resident)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
    }

    @Test
    void FR_031a_cancelReturnRecordBringsResidentBackOffSite() {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);

        inTransaction(() -> statusApi.execute(
                resident,
                StatusCommand.CANCEL_RETURN_RECORD,
                office.user(),
                Optional.of(reason()),
                details("tripId", 9),
                sourceRecord(9)));

        assertThat(currentStatus(resident)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(latestHistoryStatus(resident)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
    }
}
