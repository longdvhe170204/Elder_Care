package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.roombedallocation.api.VehicleScheduleApi;
import com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import com.eldercare.backend.roombedallocation.internal.vehicle.service.VehicleScheduleReminders;
import com.eldercare.backend.support.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 019 T021 (US6 kịch bản 3, 4, 6; FR-018, FR-019, Q-220, Q-236, Q-239, Q-240; chủ dự án chốt 2026-11-06: có lịch khi
 * Đã đặt hoặc Đang dùng bao trùm giờ rời; tối đa một lịch chưa hủy mỗi chuyến): lịch xe theo lệnh chuyến của 014 trong
 * cùng transaction; job nhắc lịch Đang dùng quá giờ về.
 */
class FR_018_FollowTripIT extends VehicleTestBase {

    @Autowired
    VehicleScheduleApi schedules;

    @Autowired
    VehicleScheduleCommandApi commands;

    @Autowired
    VehicleScheduleReminders reminders;

    long trip;

    long tripSchedule() throws Exception {
        trip = TestUsers.nextId();
        return booked(booking(vehicle, SEVEN, THIRTEEN, "TRIP", trip));
    }

    @Test
    void US6_4_firstDepartureThenFinishedReturnAndUndoOfTheLastDeparture() throws Exception {
        long id = tripSchedule();
        commands.followTrip(trip, TripVehicleChangeType.FIRST_DEPARTURE, SEVEN, THIRTEEN, Optional.of(SEVEN));
        commands.followTrip(trip, TripVehicleChangeType.LAST_DEPARTURE_UNDONE, SEVEN, THIRTEEN, Optional.empty());
        assertThat(scheduleStatus(id)).isEqualTo("BOOKED");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_EXTRACT(content,'$.departedAt') FROM simple_record WHERE id=?", String.class, id))
                .isIn(null, "null");
        commands.followTrip(trip, TripVehicleChangeType.FIRST_DEPARTURE, SEVEN, THIRTEEN, Optional.of(SEVEN));
        commands.followTrip(trip, TripVehicleChangeType.RETURN_FINISHED, SEVEN, THIRTEEN, Optional.of(THIRTEEN));
        assertThat(scheduleStatus(id)).isEqualTo("COMPLETED");
    }

    /**
     * Job nhắc quét mọi lịch Đang dùng của schema test; IT khác commit lịch chuyến (BR_M04_16_RoiVienIT) có giờ về trước
     * đồng hồ giả lập 2099 của ca này. Đưa các lịch Đang dùng có sẵn sang Đã hoàn thành trong transaction của test
     * (tự rollback) trước khi dựng lịch của ca, theo mẫu CallReminderTestBase.isolateFromOtherTests.
     */
    private void isolateFromOtherTests() {
        jdbc.update(
                "UPDATE simple_record SET status = 'COMPLETED' WHERE kind = 'M03.VEHICLE_SCHEDULE' AND status = 'IN_USE'");
    }
}
