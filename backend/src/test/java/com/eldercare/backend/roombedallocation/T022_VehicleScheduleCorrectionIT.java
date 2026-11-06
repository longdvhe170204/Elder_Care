package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import com.eldercare.backend.roombedallocation.api.events.VehicleScheduleCorrected;
import com.eldercare.backend.support.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 019 T022 phần đính chính {@code M03.VEHICLE_SCHEDULE} (chủ dự án chốt 2026-11-07; 000 FR-026, FR-029): lịch Đã hoàn thành
 * đính chính được giờ đi, giờ về dự kiến, giờ xuất phát, trả thực tế, người lái, ghi chú hoặc Hủy ghi nhận; đổi giờ dự
 * kiến phải kiểm lại chồng giờ (409 {@code VEHICLE_SCHEDULE_OVERLAP}) và lịch gắn chuyến phát sự kiện cho 014.
 */
class T022_VehicleScheduleCorrectionIT extends VehicleTestBase {
    static final String TYPE = "M03.VEHICLE_SCHEDULE";

    @Autowired
    VehicleScheduleCommandApi commands;

    long trip;

    long completedTripSchedule() throws Exception {
        trip = TestUsers.nextId();
        long id = booked(booking(vehicle, SEVEN, THIRTEEN, "TRIP", trip));
        commands.followTrip(trip, TripVehicleChangeType.FIRST_DEPARTURE, SEVEN, THIRTEEN, Optional.of(SEVEN));
        commands.followTrip(trip, TripVehicleChangeType.RETURN_FINISHED, SEVEN, THIRTEEN, Optional.of(THIRTEEN));
        assertThat(scheduleStatus(id)).isEqualTo("COMPLETED");
        return id;
    }

    ResultActions correct(long id, String body) throws Exception {
        return mvc.perform(post("/api/v1/corrections/" + TYPE + "/" + id)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    String amend(String content) {
        return "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":" + content + ",\"reason\":\"Ghi nhầm\"}";
    }

    @Test
    void actualTimesDriverAndNoteAreAmendedWithoutNotifyingTheTrip() throws Exception {
        long id = completedTripSchedule();
        correct(
                        id,
                        amend("{\"returnedAt\":\"" + THIRTEEN.plusSeconds(600) + "\",\"note\":\"Xe về muộn\","
                                + "\"driverStaffId\":" + driver.staffId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creationMethod").value("A"));
        mvc.perform(get("/api/v1/corrections/" + TYPE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corrected").value(true))
                .andExpect(jsonPath("$.currentContent.note").value("Xe về muộn"));
        assertThat(events.stream(VehicleScheduleCorrected.class)).isEmpty();
        assertThat(scheduleStatus(id)).isEqualTo("COMPLETED");
    }

    @Test
    void plannedTimesOverlappingAnotherScheduleAreRefusedAndNothingIsPublished() throws Exception {
        long id = completedTripSchedule();
        booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
        correct(id, amend("{\"plannedReturnAt\":\"" + FOURTEEN + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_SCHEDULE_OVERLAP"));
        // Ở production lệnh rollback cả bản đính chính; transaction test dùng chung nên chỉ kiểm không có sự kiện
        assertThat(events.stream(VehicleScheduleCorrected.class)).isEmpty();
    }

    @Test
    void plannedTimesWithoutOverlapAreSavedAndTheTripIsNotified() throws Exception {
        long id = completedTripSchedule();
        booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
        correct(id, amend("{\"plannedDepartureAt\":\"" + TEN + "\"}")).andExpect(status().isCreated());
        assertThat(events.stream(VehicleScheduleCorrected.class))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.scheduleId()).isEqualTo(id);
                    assertThat(e.tripId()).isEqualTo(trip);
                    assertThat(e.plannedDepartureAt()).isEqualTo(TEN);
                    assertThat(e.plannedReturnAt()).isEqualTo(THIRTEEN);
                });
    }

    @Test
    void invalidContentIsRefused() throws Exception {
        long id = completedTripSchedule();
        correct(id, amend("{\"purpose\":\"OTHER\"}")).andExpect(status().isUnprocessableEntity());
        correct(id, amend("{}")).andExpect(status().isUnprocessableEntity());
        correct(id, amend("{\"plannedReturnAt\":\"" + SEVEN.minusSeconds(60) + "\"}"))
                .andExpect(status().isUnprocessableEntity());
        correct(id, amend("{\"returnedAt\":\"" + SEVEN + "\"}")).andExpect(status().isUnprocessableEntity());
        correct(id, amend("{\"departedAt\":\"khong-phai-gio\"}")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void onlyCompletedSchedulesAndBookingRolesAreCorrectable() throws Exception {
        long booked = booked(booking(vehicle(null, null), SEVEN, THIRTEEN, "OTHER", null));
        correct(booked, amend("{\"note\":\"x\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RECORD_NOT_CONFIRMED"));
        long id = completedTripSchedule();
        as(driver);
        correct(id, amend("{\"note\":\"x\"}")).andExpect(status().isForbidden());
    }

    @Test
    void voidKeepsTheScheduleAsItIs() throws Exception {
        long id = completedTripSchedule();
        correct(id, "{\"type\":\"VOID\",\"reason\":\"Ghi nhầm lịch\"}").andExpect(status().isCreated());
        assertThat(scheduleStatus(id)).isEqualTo("COMPLETED");
        assertThat(events.stream(VehicleScheduleCorrected.class)).isEmpty();
    }
}
