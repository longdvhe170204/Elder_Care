package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.roombedallocation.api.VehicleScheduleApi;
import com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi;
import com.eldercare.backend.roombedallocation.api.dto.TripVehicleChangeType;
import com.eldercare.backend.roombedallocation.internal.vehicle.service.VehicleScheduleReminders;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
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
    void FR_019_tripHasAScheduleWhenBookedOrInUseCoversThePlannedDeparture() throws Exception {
        assertThat(schedules.hasBookedSchedule(TestUsers.nextId(), SEVEN)).isFalse();
        long id = tripSchedule();
        assertThat(schedules.hasBookedSchedule(trip, SEVEN)).isTrue();
        assertThat(schedules.hasBookedSchedule(trip, THIRTEEN.plusSeconds(60))).isFalse();
        commands.followTrip(trip, TripVehicleChangeType.FIRST_DEPARTURE, SEVEN, THIRTEEN, Optional.of(SEVEN));
        assertThat(scheduleStatus(id)).isEqualTo("IN_USE");
        assertThat(schedules.hasBookedSchedule(trip, SEVEN))
                .as("các lượt rời viện sau")
                .isTrue();
        book(booking(vehicle(null, null), SEVEN, THIRTEEN, "TRIP", trip))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
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

    @Test
    void Q_239_tripRescheduleIsCheckedAndFollowsOrIsRefused() throws Exception {
        long id = tripSchedule();
        booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
        var refused = schedules.checkChange(trip, TEN, FOURTEEN);
        assertThat(refused.allowed()).isFalse();
        assertThat(refused.unmetConditions()).isNotEmpty();
        assertThatThrownBy(
                        () -> commands.followTrip(trip, TripVehicleChangeType.CHANGED, TEN, FOURTEEN, Optional.empty()))
                .isInstanceOf(BusinessException.class);
        assertThat(schedules.checkChange(trip, TEN, THIRTEEN).allowed())
                .as("không tính chính lịch của chuyến")
                .isTrue();
        assertThat(schedules.checkChange(TestUsers.nextId(), TEN, FOURTEEN).allowed())
                .as("chuyến chưa có lịch")
                .isTrue();
        commands.followTrip(trip, TripVehicleChangeType.CHANGED, TEN, THIRTEEN, Optional.empty());
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.plannedDepartureAt')) FROM simple_record WHERE id=?",
                        String.class,
                        id))
                .startsWith("2099-01-05T03:00");
        assertThat(staffNotified(office.staffId(), "VEHICLE_SCHEDULE_CHANGED")).isTrue();
    }

    @Test
    void Q_236_extensionIsNotBlockedAndNotifiesManagerAndNextBooker() throws Exception {
        tripSchedule();
        as(otherOffice);
        booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
        commands.followTrip(trip, TripVehicleChangeType.FIRST_DEPARTURE, SEVEN, THIRTEEN, Optional.of(SEVEN));
        commands.followTrip(trip, TripVehicleChangeType.EXTENDED, SEVEN, FOURTEEN, Optional.empty());
        assertThat(noticesTo(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))
                .isNotEmpty();
        assertThat(staffNotified(otherOffice.staffId(), "VEHICLE_SCHEDULE_EXTENDED_OVERLAP"))
                .isTrue();
    }

    @Test
    void US6_6_cancelledTripCancelsItsScheduleAndTheTripMayBookAgain() throws Exception {
        long id = tripSchedule();
        commands.followTrip(trip, TripVehicleChangeType.CANCELLED, SEVEN, THIRTEEN, Optional.empty());
        assertThat(scheduleStatus(id)).isEqualTo("CANCELLED");
        assertThat(staffNotified(driver.staffId(), "VEHICLE_SCHEDULE_CANCELLED"))
                .isTrue();
        assertThat(schedules.hasBookedSchedule(trip, SEVEN)).isFalse();
        booked(booking(vehicle, SEVEN, THIRTEEN, "TRIP", trip));
    }

    @Test
    void FR_018_inUseScheduleOverdueRemindsTheBookerOnceAndTheManagerOnOverlap() throws Exception {
        tripSchedule();
        as(otherOffice);
        booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
        commands.followTrip(trip, TripVehicleChangeType.FIRST_DEPARTURE, SEVEN, THIRTEEN, Optional.of(SEVEN));
        reminders.remind(THIRTEEN.minus(Duration.ofMinutes(1)));
        assertThat(staffNotified(office.staffId(), "VEHICLE_RETURN_OVERDUE")).isFalse();
        reminders.remind(THIRTEEN.plus(Duration.ofMinutes(1)));
        reminders.remind(THIRTEEN.plus(Duration.ofMinutes(2)));
        long overdue = events.stream(com.eldercare.backend.common.notification.HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.eventType().equals("VEHICLE_RETURN_OVERDUE"))
                .count();
        assertThat(overdue).as("một lần").isEqualTo(1);
        assertThat(staffNotified(office.staffId(), "VEHICLE_RETURN_OVERDUE")).isTrue();
        assertThat(noticesTo(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)))
                .as("chồng lịch kế tiếp")
                .isNotEmpty();
    }
}
