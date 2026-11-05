package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.StatusTestBase;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/** 014 T015: HTTP thật, trạng thái 001 thật; dữ liệu từng test có id riêng. */
@AutoConfigureMockMvc
@WithMockUser
@Import({FakeM04Config.class, com.eldercare.backend.support.fake.roombedallocation.FakeVehicleSchedules.Config.class})
class BR_M04_16_RoiVienIT extends StatusTestBase {
    @Autowired
    com.eldercare.backend.care.internal.trip.service.TripDepartureService departures;

    @Autowired
    com.eldercare.backend.administration.shared.api.SharedRowRetention sharedRows;

    @Autowired
    org.springframework.beans.factory.ObjectProvider<com.eldercare.backend.care.api.TripDutyCommandApi> dutyCommands;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeShiftStaff shifts;

    @Autowired
    org.springframework.beans.factory.ObjectProvider<com.eldercare.backend.care.api.TripMedicationReadApi> tripReads;

    @Autowired
    org.springframework.beans.factory.ObjectProvider<com.eldercare.backend.care.api.TripDutyReadApi> dutyReads;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeActivityHealthGate gate;

    @Autowired
    com.eldercare.backend.care.api.ActivityRegistrationAutoCancelApi cancels;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripStaffSchedule tripShifts;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripMedicationPreparation tripMedication;

    @Autowired
    com.eldercare.backend.support.fake.roombedallocation.FakeVehicleSchedules vehicles;

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    TransactionTemplate tx;

    long floor;
    long activity;
    Instant departure;
    TestUsers.TestUser manager;

    @BeforeEach
    void tripFixture() throws Exception {
        floor = TestUsers.nextId();
        manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        user.set(manager.user());
        long type = tx.execute(s -> catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Ngoài viện " + TestUsers.nextId(),
                                null,
                                0,
                                new ActivityTypeAttributes(false, true, List.of())))
                .id());
        activity = id(mvc.perform(write(
                post("/api/v1/activities"),
                "{\"name\":\"Tham quan\",\"typeId\":" + type + ",\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":10,\"groupActivity\":false}")));
        departure = time.now().plusSeconds(1800).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        return request.header("Idempotency-Key", "trip-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    long id(ResultActions result) throws Exception {
        String location =
                result.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    long trip(boolean vehicle) throws Exception {
        return id(mvc.perform(write(
                post("/api/v1/activity-sessions"),
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + departure + "\",\"endsAt\":\""
                        + departure.plusSeconds(21600)
                        + "\",\"trip\":{\"destination\":\"Chùa\",\"plannedDepartureAt\":\""
                        + departure + "\",\"plannedReturnAt\":\"" + departure.plusSeconds(21600)
                        + "\",\"transport\":\"Xe thuê\",\"facilityVehicle\":" + vehicle + "}}")));
    }

    long participant(long s, String eligibility) {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(manager.user());
        jdbc.update(
                "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status,participant_status)"
                        + " VALUES(?,?,'MANUAL',?,'REGISTERED',?)",
                s,
                resident,
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE),
                eligibility);
        return resident;
    }

    @Test
    void FR_034_readSourcesProvideApprovedIntervalLeaderAndDepartedResidents() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        assertThat(tripReads.getObject().approvedTripInterval(r, s))
                .contains(new com.eldercare.backend.care.api.dto.TripInterval(departure, departure.plusSeconds(21600)));
        assertThat(dutyReads.getObject().groupLeader(s)).contains(manager.staffId());
        depart(s, r).andExpect(status().isOk());
        var views = dutyReads.getObject().inShift(floor, time.now().minusSeconds(3600), departure.plusSeconds(30000));
        assertThat(views).filteredOn(v -> v.tripId() == s).singleElement().satisfies(v -> {
            assertThat(v.residentIds()).containsExactly(r);
            assertThat(v.departedAt()).isNotNull();
        });
    }

    @Test
    void FR_015_tripRegistrationRemainsOpenAfterPlannedStartUntilFirstDeparture() throws Exception {
        long s = trip(false), r = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(manager.user());
        jdbc.update(
                "UPDATE activity_session SET starts_at=? WHERE id=?",
                LocalDateTime.ofInstant(time.now().minusSeconds(60), BusinessTime.ZONE),
                s);
        mvc.perform(write(post("/api/v1/activity-sessions/" + s + "/registrations"), "{\"residentId\":" + r + "}"))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .isEqualTo("REGISTERED");
    }

    @Test
    void FR_021a_suspectedInfectionCancelsIndividualTripAndMarksTheParticipantNotGoing() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        gate.suspectInfection(r);
        tx.executeWithoutResult(t -> cancels.cancelForSuspectedInfection(r, time.now()));
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .isEqualTo("NOT_GOING");
    }

    @Test
    void FR_021a_partialDepartureStillCancelsThePendingParticipantButPreservesTheDepartedOne() throws Exception {
        long s = trip(false), departed = participant(s, "ELIGIBLE"), pending = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(departed, s);
        depart(s, departed).andExpect(status().isOk());
        gate.suspectInfection(departed);
        gate.suspectInfection(pending);
        tx.executeWithoutResult(t -> {
            cancels.cancelForSuspectedInfection(departed, time.now());
            cancels.cancelForSuspectedInfection(pending, time.now());
        });
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        pending))
                .isEqualTo("NOT_GOING");
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        pending))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT participant_reason FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        pending))
                .isEqualTo("Nghi nhiễm");
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        departed))
                .isEqualTo("DEPARTED");
        assertThat(currentStatus(departed)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(currentStatus(pending)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_038a_reschedulingToAnotherDateResetsEligibilityAndMovesTheTripInterval() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        mvc.perform(write(
                        post("/api/v1/activity-sessions/" + s + "/reschedule"),
                        "{\"startsAt\":\"" + departure.plusSeconds(86400) + "\",\"endsAt\":\""
                                + departure.plusSeconds(108000) + "\",\"reason\":\"Dời ngày tham quan\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .isEqualTo("REGISTERED");
        assertThat(tripReads.getObject().tripInterval(s).orElseThrow().from()).isEqualTo(departure.plusSeconds(86400));
    }

    @Test
    void FR_013_tripCannotBeCancelledAfterFirstDeparture() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        depart(s, r).andExpect(status().isOk());
        mvc.perform(write(
                        post("/api/v1/activity-sessions/" + s + "/cancel"), "{\"reason\":\"Không thể hủy khi đã đi\"}"))
                .andExpect(status().isConflict());
        long registration = jdbc.queryForObject(
                "SELECT id FROM activity_registration WHERE session_id=? AND resident_id=?", Long.class, s, r);
        mvc.perform(write(
                        post("/api/v1/activity-registrations/" + registration + "/cancel"),
                        "{\"reason\":\"Không thể hủy khi đã đi\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SESSION_ALREADY_STARTED"));
    }

    @Test
    void FR_038a_cancellingSessionAlsoCancelsTheTrip() throws Exception {
        long s = trip(false);
        participant(s, "ELIGIBLE");
        mvc.perform(write(post("/api/v1/activity-sessions/" + s + "/cancel"), "{\"reason\":\"Hoãn chuyến\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, s))
                .isEqualTo("CANCELLED");
    }

    @Test
    void FR_046a_offlineDepartureIsBlockedUntil001SupportsDeviceTime() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/depart"),
                        "{\"residentIds\":[" + r + "],\"deviceAt\":\""
                                + time.now().minusSeconds(60) + "\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_035_removingDutyInTheOverlappingShiftRemovesLeaderWithoutTransferringIt() throws Exception {
        long s = trip(false);
        leader(s);
        long shift = TestUsers.nextId();
        shifts.shift(new com.eldercare.backend.care.api.spi.ShiftStaffPort.ShiftView(
                shift, floor, departure.minusSeconds(3600), departure.plusSeconds(28800), true));
        tx.executeWithoutResult(t -> dutyCommands
                .getObject()
                .removeDuty(
                        manager.staffId(),
                        shift,
                        new com.eldercare.backend.administration.auditlog.api.dto.Basis(null, null, shift, null)));
        assertThat(dutyReads.getObject().groupLeader(s)).isEmpty();
    }

    @Test
    void FR_039_overdueTripRemainsVisibleInTheNextShift() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        depart(s, r).andExpect(status().isOk());
        assertThat(dutyReads.getObject().inShift(floor, departure.plusSeconds(25200), departure.plusSeconds(54000)))
                .extracting(v -> v.tripId())
                .contains(s);
    }

    @Test
    void FR_051_tripDutyHistoryIsRemovedWithTheSessionRetentionRoot() throws Exception {
        long s = trip(false);
        leader(s);
        tx.executeWithoutResult(t -> sharedRows.deleteFor(
                List.of(new com.eldercare.backend.administration.shared.api.dto.EntityRef("M04.ACTIVITY_SESSION", s))));
        assertThat(dutyReads.getObject().groupLeader(s)).isEmpty();
    }

    @Test
    void FR_034_indoorActivityCannotBeCreatedAsTrip() throws Exception {
        long indoor = tx.execute(t -> catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Trong viện " + TestUsers.nextId(),
                                null,
                                0,
                                new ActivityTypeAttributes(false, false, List.of())))
                .id());
        long a = id(mvc.perform(write(
                post("/api/v1/activities"),
                "{\"name\":\"Đọc sách\",\"typeId\":" + indoor + ",\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":10}")));
        mvc.perform(write(
                        post("/api/v1/activity-sessions"),
                        "{\"activityId\":" + a + ",\"startsAt\":\"" + departure + "\",\"endsAt\":\""
                                + departure.plusSeconds(21600)
                                + "\",\"trip\":{\"destination\":\"Chùa\",\"plannedDepartureAt\":\"" + departure
                                + "\",\"plannedReturnAt\":\"" + departure.plusSeconds(21600)
                                + "\",\"transport\":\"Xe thuê\",\"facilityVehicle\":false}}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_034_offsiteSessionRequiresTripDetails() throws Exception {
        mvc.perform(write(
                        post("/api/v1/activity-sessions"),
                        "{\"activityId\":" + activity + ",\"startsAt\":\"" + departure + "\",\"endsAt\":\""
                                + departure.plusSeconds(21600) + "\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_036_eligibleParticipantCanBeReassessedAsNotGoing() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        bedAllocations.allocate(
                r,
                new com.eldercare.backend.administration.account.api.spi.BedAllocationView(
                        TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/eligibility-assessments"),
                        "{\"residentId\":" + r + ",\"result\":\"FAIL\",\"reason\":\"Sức khỏe thay đổi\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .isEqualTo("NOT_GOING");
    }

    @Test
    void FR_035_cancellingTripEndsDutyScope() throws Exception {
        long s = trip(false);
        leader(s);
        mvc.perform(write(post("/api/v1/activity-sessions/" + s + "/cancel"), "{\"reason\":\"Hoãn chuyến\"}"))
                .andExpect(status().isOk());
        assertThat(dutyReads.getObject().groupLeader(s)).isEmpty();
        assertThat(dutyReads.getObject().companions(s)).isEmpty();
    }

    @Test
    void FR_034_offsiteTemplateCannotCreateASessionWithoutTripMetadata() throws Exception {
        mvc.perform(write(
                        post("/api/v1/activities/" + activity + "/create-sessions-from-template"),
                        "{\"fromDate\":\"" + time.today() + "\",\"toDate\":\""
                                + time.today().plusDays(1) + "\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void FR_042_laterDepartureIsBlockedAfterTheExpectedReturnTime() throws Exception {
        long s = trip(false), a = participant(s, "ELIGIBLE"), b = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(a, s);
        tripMedication.prepared(b, s);
        depart(s, a).andExpect(status().isOk());
        jdbc.update(
                "UPDATE activity_session SET trip_planned_departure_at=?,trip_planned_return_at=? WHERE id=?",
                LocalDateTime.ofInstant(time.now().minusSeconds(7200), BusinessTime.ZONE),
                LocalDateTime.ofInstant(time.now().minusSeconds(60), BusinessTime.ZONE),
                s);
        depart(s, b)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        assertThat(currentStatus(b)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_027_tripCannotUseInstituteSessionAttendance() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        jdbc.update(
                "UPDATE activity_session SET starts_at=? WHERE id=?",
                LocalDateTime.ofInstant(time.now().minusSeconds(60), BusinessTime.ZONE),
                s);
        mvc.perform(write(
                        post("/api/v1/activity-sessions/" + s + "/attendance"),
                        "{\"results\":[{\"residentId\":" + r
                                + ",\"result\":\"PRESENT\",\"participationLevel\":\"NORMAL\"}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.SESSION_ATTENDANCE' AND entity_id=?",
                        Integer.class,
                        r))
                .isZero();
    }

    @Test
    void FR_029_tripCannotCompleteInstituteAttendanceEvenWhenEmpty() throws Exception {
        long s = trip(false);
        jdbc.update(
                "UPDATE activity_session SET starts_at=? WHERE id=?",
                LocalDateTime.ofInstant(time.now().minusSeconds(60), BusinessTime.ZONE),
                s);
        mvc.perform(write(post("/api/v1/activity-sessions/" + s + "/complete-attendance"), "{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void FR_021_isolationAfterEarlyDepartureDoesNotCancelTheTripOrFailTheSourceCommand() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE"), room = TestUsers.nextId();
        leader(s);
        tripMedication.prepared(r, s);
        jdbc.update("UPDATE activity_session SET location_room_id=? WHERE id=?", room, s);
        depart(s, r).andExpect(status().isOk());
        gate.isolateRoom(room);
        tx.executeWithoutResult(t -> cancels.cancelForIsolation(TestUsers.nextId(), time.now()));
        assertThat(jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, s))
                .isEqualTo("IN_PROGRESS");
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
    }

    @Test
    void FR_035_assignmentCannotReplaceLeaderAfterDeparture() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        depart(s, r).andExpect(status().isOk());
        var other = new TestUsers(jdbc, time).staff(Role.CAREGIVER);
        tripShifts.shift(other.staffId(), departure.minusSeconds(3600), departure.plusSeconds(28800));
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/assignment"),
                        "{\"leaderId\":" + other.staffId() + ",\"companionIds\":[]}"))
                .andExpect(status().isConflict());
        assertThat(dutyReads.getObject().groupLeader(s)).contains(manager.staffId());
    }

    @Test
    void FR_012_scheduledTripCanBeRescheduledAfterThePlannedStart() throws Exception {
        long s = trip(false);
        jdbc.update(
                "UPDATE activity_session SET starts_at=? WHERE id=?",
                LocalDateTime.ofInstant(time.now().minusSeconds(60), BusinessTime.ZONE),
                s);
        mvc.perform(write(
                        post("/api/v1/activity-sessions/" + s + "/reschedule"),
                        "{\"startsAt\":\"" + departure.plusSeconds(3600) + "\",\"endsAt\":\""
                                + departure.plusSeconds(25200) + "\",\"reason\":\"Dời giờ xuất phát\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void FR_039_tripDetailContainsParticipantsAndTheirStatuses() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        mvc.perform(get("/api/v1/trips/" + s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("Chùa"))
                .andExpect(jsonPath("$.leaderId").value(manager.staffId()))
                .andExpect(jsonPath("$.participants[0].residentId").value(r))
                .andExpect(jsonPath("$.participants[0].status").value("ELIGIBLE"));
    }

    @Test
    void DBR_04_tripRecordsKeepTheAccountAsActorAndTheStaffAsLeader() throws Exception {
        assertThat(manager.accountId()).isNotEqualTo(manager.staffId());
        long s = trip(false), r = participant(s, "REGISTERED");
        bedAllocations.allocate(
                r,
                new com.eldercare.backend.administration.account.api.spi.BedAllocationView(
                        TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        leader(s);
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/eligibility-assessments"),
                        "{\"residentId\":" + r + ",\"result\":\"PASS\"}"))
                .andExpect(status().isOk());
        tripMedication.prepared(r, s);
        depart(s, r).andExpect(status().isOk());
        assertThat(dutyReads.getObject().groupLeader(s)).contains(manager.staffId());
        org.assertj.core.api.SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(jdbc.queryForObject(
                            "SELECT actor_id FROM status_history WHERE entity_type='M04.ACTIVITY_SESSION' AND entity_id=? AND attribute='TRIP_DUTIES'",
                            Long.class,
                            s))
                    .isEqualTo(manager.accountId());
            soft.assertThat(jdbc.queryForObject(
                            "SELECT actor_id FROM record_event WHERE kind='M04.TRIP_ELIGIBILITY' AND entity_id=?",
                            Long.class,
                            r))
                    .isEqualTo(manager.accountId());
            soft.assertThat(jdbc.queryForObject(
                            "SELECT actor_id FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?",
                            Long.class,
                            r))
                    .isEqualTo(manager.accountId());
        });
    }

    void leader(long trip) throws Exception {
        tripShifts.shift(manager.staffId(), departure.minusSeconds(3600), departure.plusSeconds(28800));
        mvc.perform(write(
                        post("/api/v1/trips/" + trip + "/assignment"),
                        "{\"leaderId\":" + manager.staffId() + ",\"companionIds\":[]}"))
                .andExpect(status().isOk());
    }

    ResultActions depart(long trip, long resident) throws Exception {
        return mvc.perform(write(post("/api/v1/trips/" + trip + "/depart"), "{\"residentIds\":[" + resident + "]}"));
    }

    @Test
    void BR_M04_16_preparedMedicationAllowsDepartureAndChangesReal001Status() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        depart(s, r)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_NOT_HANDLED"));
        tripMedication.prepared(r, s);
        depart(s, r).andExpect(status().isOk());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .isEqualTo("DEPARTED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?",
                        Integer.class,
                        r))
                .isEqualTo(1);
    }

    @Test
    void BR_M04_16_facilityVehicleRequiresBookedScheduleAndRollsBackWhenVehicleCommandFails() throws Exception {
        long s = trip(true), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        depart(s, r)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_SCHEDULE_REQUIRED"));
        vehicles.book(s, departure);
        vehicles.failCommand(s);
        depart(s, r).andExpect(status().isServiceUnavailable());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, s))
                .isEqualTo("SCHEDULED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?",
                        Integer.class,
                        r))
                .isZero();
    }

    @Test
    void BR_M04_16_twoDevicesCanOnlyDepartTheResidentOnce() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        var security = org.springframework.security.test.context.TestSecurityContextHolder.getContext();
        var start = new java.util.concurrent.CountDownLatch(1);
        var statuses = new java.util.concurrent.ConcurrentLinkedQueue<Integer>();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 2; i++)
                futures.add(pool.submit(() -> {
                    org.springframework.security.test.context.TestSecurityContextHolder.setContext(security);
                    start.await();
                    statuses.add(depart(s, r).andReturn().getResponse().getStatus());
                    return null;
                }));
            start.countDown();
            for (var future : futures) future.get(30, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?",
                        Integer.class,
                        r))
                .isEqualTo(1);
    }

    @Test
    void FR_035_departureCannotUseAnAssignmentReplacedAfterTheInitialAuthorityCheck() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        tripShifts.shift(nurse.staffId(), departure.minusSeconds(3600), departure.plusSeconds(28800));
        var failure = departureDuring(s, r, () -> {
            mvc.perform(write(
                            post("/api/v1/trips/" + s + "/assignment"),
                            "{\"leaderId\":" + nurse.staffId() + ",\"companionIds\":[]}"))
                    .andExpect(status().isOk());
            return null;
        });
        assertThat(failure).isNotNull();
        assertThat(failure.status()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
        assertThat(failure.errorCode()).isEqualTo("STATE_CHANGED");
        assertThat(dutyReads.getObject().groupLeader(s)).contains(nurse.staffId());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .isEqualTo("ELIGIBLE");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?",
                        Integer.class,
                        r))
                .isZero();
    }

    @Test
    void FR_019_departureCannotOverwriteARegistrationCancelledAfterTheInitialCheck() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        long registration = jdbc.queryForObject(
                "SELECT id FROM activity_registration WHERE session_id=? AND resident_id=?", Long.class, s, r);
        var failure = departureDuring(s, r, () -> {
            mvc.perform(write(
                            post("/api/v1/activity-registrations/" + registration + "/cancel"),
                            "{\"reason\":\"Người cao tuổi đổi ý\"}"))
                    .andExpect(status().isOk());
            return null;
        });
        assertThat(failure).isNotNull();
        assertThat(failure.status()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT);
        assertThat(failure.errorCode()).isEqualTo("STATE_CHANGED");
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE id=?", String.class, registration))
                .isEqualTo("NOT_GOING");
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM activity_registration WHERE id=?", String.class, registration))
                .isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?",
                        Integer.class,
                        r))
                .isZero();
    }

    private com.eldercare.backend.common.error.BusinessException departureDuring(
            long s, long r, java.util.concurrent.Callable<Void> concurrentChange) throws Exception {
        var checked = new java.util.concurrent.CountDownLatch(1);
        var resume = new java.util.concurrent.CountDownLatch(1);
        tripMedication.onPreparedCheck(r, s, () -> {
            checked.countDown();
            try {
                if (!resume.await(30, java.util.concurrent.TimeUnit.SECONDS))
                    throw new IllegalStateException("Chưa hoàn tất thay đổi đồng thời trong test");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
        var security = org.springframework.security.test.context.TestSecurityContextHolder.getContext();
        try (var pool = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var future = pool.submit(() -> {
                org.springframework.security.test.context.TestSecurityContextHolder.setContext(security);
                var repeatable = new TransactionTemplate(Objects.requireNonNull(tx.getTransactionManager()));
                repeatable.setIsolationLevel(
                        org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
                try {
                    repeatable.executeWithoutResult(st -> departures.depart(
                            new com.eldercare.backend.administration.account.api.dto.Target(
                                    "M04.TRIP", Long.toString(s), null, floor),
                            s,
                            List.of(r),
                            null,
                            null));
                    return null;
                } catch (com.eldercare.backend.common.error.BusinessException e) {
                    return e;
                } finally {
                    org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
                }
            });
            try {
                assertThat(checked.await(30, java.util.concurrent.TimeUnit.SECONDS))
                        .isTrue();
                concurrentChange.call();
            } finally {
                resume.countDown();
            }
            return future.get(30, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @Test
    void FR_035_officeStaffCannotLeadAndStaffWithoutOverlappingShiftCannotBeAssigned() throws Exception {
        long s = trip(false);
        var officeStaff = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF);
        mvc.perform(write(post("/api/v1/trips/" + s + "/assignment"), "{\"leaderId\":" + officeStaff.staffId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROLE_NOT_ELIGIBLE"));
        mvc.perform(write(post("/api/v1/trips/" + s + "/assignment"), "{\"leaderId\":" + manager.staffId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_ON_SHIFT"));
    }

    @Test
    void FR_034_singleSessionWithTripCreatesTheTripOnTheSameRow() throws Exception {
        long s = id(mvc.perform(write(
                post("/api/v1/activity-sessions"),
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + departure + "\",\"endsAt\":\""
                        + departure.plusSeconds(21600)
                        + "\",\"trip\":{\"destination\":\"Chùa\",\"plannedDepartureAt\":\""
                        + departure + "\",\"plannedReturnAt\":\"" + departure.plusSeconds(21600)
                        + "\",\"transport\":\"Xe thuê\",\"facilityVehicle\":false}}")));
        assertThat(jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, s))
                .isEqualTo("SCHEDULED");
    }

    @Test
    void FR_035_departureRequiresALeaderAndLeavesTheResidentInResidence() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/depart"),
                        "{\"residentIds\":[" + r + "],\"reason\":\"Trưởng tầng làm thay\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TRIP_LEADER_REQUIRED"));
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_036_failedAssessmentClosesRegistrationWithoutChangingResidentStatus() throws Exception {
        long s = trip(false), r = participant(s, "REGISTERED");
        bedAllocations.allocate(
                r,
                new com.eldercare.backend.administration.account.api.spi.BedAllocationView(
                        TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/eligibility-assessments"),
                        "{\"residentId\":" + r + ",\"result\":\"FAIL\",\"reason\":\"Không đủ sức khỏe\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                        String.class,
                        s,
                        r))
                .isEqualTo("NOT_GOING");
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_036_nurseCannotAssessEvenWhenInTheTripFloor() throws Exception {
        long s = trip(false), r = participant(s, "REGISTERED");
        assignNurse(nurse, r);
        user.set(nurse.user());
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/eligibility-assessments"),
                        "{\"residentId\":" + r + ",\"result\":\"PASS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_036_reRegistrationAfterNotGoingUsesTheNewRegistrationForAssessmentAndDeparture() throws Exception {
        long s = trip(false), r = participant(s, "REGISTERED");
        bedAllocations.allocate(
                r,
                new com.eldercare.backend.administration.account.api.spi.BedAllocationView(
                        TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/eligibility-assessments"),
                        "{\"residentId\":" + r + ",\"result\":\"FAIL\",\"reason\":\"Đang mệt\"}"))
                .andExpect(status().isOk());
        mvc.perform(write(post("/api/v1/activity-sessions/" + s + "/registrations"), "{\"residentId\":" + r + "}"))
                .andExpect(status().isCreated());
        long renewed = jdbc.queryForObject(
                "SELECT id FROM activity_registration WHERE session_id=? AND resident_id=? AND status='REGISTERED'",
                Long.class,
                s,
                r);
        mvc.perform(get("/api/v1/trips/" + s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andExpect(jsonPath("$.participants[0].id").value(renewed))
                .andExpect(jsonPath("$.participants[0].status").value("REGISTERED"));
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/eligibility-assessments"),
                        "{\"residentId\":" + r + ",\"result\":\"PASS\"}"))
                .andExpect(status().isOk());
        leader(s);
        tripMedication.prepared(r, s);
        depart(s, r).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT participant_status FROM activity_registration WHERE id=?", String.class, renewed))
                .isEqualTo("DEPARTED");
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
    }

    /**
     * Rời viện khóa buổi rồi mới cập nhật đăng ký; hủy đăng ký phải cùng thứ tự, nếu không hai lệnh chờ nhau thành
     * deadlock (500). Kiểm bằng cách giữ khóa buổi: lệnh hủy đang chờ không được giữ khóa dòng đăng ký.
     */
    @Test
    void FR_017_cancelWaitingForTheTripLockDoesNotHoldTheRegistrationLock() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        long registration = jdbc.queryForObject(
                "SELECT id FROM activity_registration WHERE session_id=? AND resident_id=?", Long.class, s, r);
        var locked = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var security = org.springframework.security.test.context.TestSecurityContextHolder.getContext();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var holder = pool.submit(() -> {
                tx.executeWithoutResult(st -> {
                    jdbc.queryForObject("SELECT id FROM activity_session WHERE id=? FOR UPDATE", Long.class, s);
                    locked.countDown();
                    try {
                        release.await(30, java.util.concurrent.TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                return null;
            });
            java.util.concurrent.Future<Integer> cancel;
            boolean registrationHeld = false;
            try {
                assertThat(locked.await(30, java.util.concurrent.TimeUnit.SECONDS))
                        .isTrue();
                cancel = pool.submit(() -> {
                    org.springframework.security.test.context.TestSecurityContextHolder.setContext(security);
                    try {
                        return mvc.perform(write(
                                        post("/api/v1/activity-registrations/" + registration + "/cancel"),
                                        "{\"reason\":\"Người cao tuổi đổi ý\"}"))
                                .andReturn()
                                .getResponse()
                                .getStatus();
                    } finally {
                        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
                    }
                });
                // Quan sát có giới hạn trong lúc lệnh hủy bị chặn: dòng đăng ký phải luôn khóa được ngay.
                long until = System.nanoTime() + java.time.Duration.ofSeconds(3).toNanos();
                while (!registrationHeld && !cancel.isDone() && System.nanoTime() < until) {
                    try {
                        tx.executeWithoutResult(st -> jdbc.queryForObject(
                                "SELECT id FROM activity_registration WHERE id=? FOR UPDATE NOWAIT",
                                Long.class,
                                registration));
                    } catch (org.springframework.dao.DataAccessException e) {
                        registrationHeld = true;
                    }
                    Thread.sleep(50);
                }
            } finally {
                release.countDown();
            }
            holder.get(30, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(registrationHeld)
                    .as("lệnh hủy giữ khóa đăng ký khi đang chờ khóa buổi")
                    .isFalse();
            assertThat(cancel.get(30, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM activity_registration WHERE id=?", String.class, registration))
                .isEqualTo("CANCELLED");
    }
}
