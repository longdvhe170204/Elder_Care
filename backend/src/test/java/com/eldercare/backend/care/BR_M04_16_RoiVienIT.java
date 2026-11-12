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
@Import(FakeM04Config.class)
@org.springframework.test.context.event.RecordApplicationEvents
class BR_M04_16_RoiVienIT extends StatusTestBase {
    @Autowired
    org.springframework.test.context.event.ApplicationEvents recorded;

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
    com.eldercare.backend.care.api.spi.AttendanceTracePort trace;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripStaffSchedule tripShifts;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripMedicationPreparation tripMedication;

    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi vehicles;

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

    private void FR_034_offsiteSessionRequiresTripDetails() throws Exception {
        mvc.perform(write(
                        post("/api/v1/activity-sessions"),
                        "{\"activityId\":" + activity + ",\"startsAt\":\"" + departure + "\",\"endsAt\":\""
                                + departure.plusSeconds(21600) + "\"}"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void FR_034_templateAppliesOneTripPlanOnEveryCreatedDateAtomically() throws Exception {
        FR_034_offsiteSessionRequiresTripDetails();
        var day = time.today().plusDays(1);
        mvc.perform(write(
                        post("/api/v1/activities/" + activity + "/change-template"),
                        "{\"reason\":\"Lịch chuyến\",\"effectiveDate\":\"" + day
                                + "\",\"template\":{\"daysOfWeek\":[1,2,3,4,5,6,7],\"startTime\":\"08:00\",\"endTime\":\"16:00\",\"startDate\":\""
                                + day + "\"}}"))
                .andExpect(status().isOk());
        String body = "{\"fromDate\":\"" + day + "\",\"toDate\":\"" + day.plusDays(1)
                + "\",\"trip\":{\"destination\":\"Bảo tàng\",\"plannedDepartureTime\":\"08:00\",\"plannedReturnTime\":\"16:00\",\"transport\":\"Xe thuê\",\"facilityVehicle\":false}}";
        mvc.perform(write(post("/api/v1/activities/" + activity + "/create-sessions-from-template"), body))
                .andExpect(status().isOk());
        var rows = jdbc.queryForList(
                "SELECT trip_destination,trip_planned_departure_at,trip_planned_return_at FROM activity_session WHERE activity_id=? ORDER BY starts_at",
                activity);
        assertThat(rows).hasSize(2);
        for (int i = 0; i < 2; i++) {
            assertThat(rows.get(i).get("trip_destination")).isEqualTo("Bảo tàng");
            assertThat(rows.get(i).get("trip_planned_departure_at"))
                    .isEqualTo(day.plusDays(i).atTime(8, 0));
            assertThat(rows.get(i).get("trip_planned_return_at"))
                    .isEqualTo(day.plusDays(i).atTime(16, 0));
        }
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

    ResultActions assign(long trip, long leader) throws Exception {
        return mvc.perform(write(post("/api/v1/trips/" + trip + "/assignment"), "{\"leaderId\":" + leader + "}"));
    }

    String participantStatus(long s, long r) {
        return jdbc.queryForObject(
                "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?",
                String.class,
                s,
                r);
    }

    int departures(long r) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?", Integer.class, r);
    }

    /**
     * T015, T010 Q12; gộp một ca theo ngân sách IT (AGENTS.md mục 4). Mỗi dòng dựng lại fixture (tầng, Trưởng tầng,
     * hoạt động mới) vì buổi cùng hoạt động và giờ bắt đầu là duy nhất.
     */
    @Test
    void BR_M04_16_departureConditionsTwoDevicesAndSuspectedInfection() throws Exception {
        com.eldercare.backend.support.TableRows.each(List.of("CONDITIONS", "TWO_DEVICES", "INFECTION"), scenario -> {
            tripFixture();
            switch (scenario) {
                case "CONDITIONS" ->
                    BR_M04_16_departureChecksLeaderVehicleAndMedicationThenChangesEveryModuleInOneCommand();
                case "TWO_DEVICES" -> BR_M04_16_twoDevicesCanOnlyDepartTheResidentOnce();
                case "INFECTION" ->
                    FR_021a_suspectedInfectionMarksTheParticipantNotGoingCancelsGroupRegistrationsAndNotifiesTheTrip();
            }
        });
    }

    /**
     * T015 Q18, Q19, Q21 → Q23 (BR-M04-16, FR-035, FR-038, FR-040, FR-041): rời viện bị chặn khi thiếu trưởng đoàn, lịch
     * xe, thuốc; lỗi lệnh lịch xe của 019 hoàn tác cả 001 và điểm danh; đủ điều kiện thì 001, 019 đổi trong cùng lệnh
     * và 006, 011 nhận {@code TripChanged}.
     */
    private void BR_M04_16_departureChecksLeaderVehicleAndMedicationThenChangesEveryModuleInOneCommand()
            throws Exception {
        long s = trip(true), r = participant(s, "ELIGIBLE");
        depart(s, r)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TRIP_LEADER_REQUIRED"));
        var office = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF);
        assign(s, office.staffId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROLE_NOT_ELIGIBLE"));
        assign(s, manager.staffId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_ON_SHIFT"));
        leader(s);
        depart(s, r)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_SCHEDULE_REQUIRED"));
        long schedule = bookVehicle(s);
        depart(s, r)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("MEDICATION_NOT_HANDLED"));
        tripMedication.prepared(r, s);

        var vehicleTarget = org.springframework.test.util.AopTestUtils
                .<com.eldercare.backend.roombedallocation.api.VehicleScheduleCommandApi>getUltimateTargetObject(
                        vehicles);
        org.mockito.Mockito.doThrow(new com.eldercare.backend.common.error.BusinessException(
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                        "SOURCE_UNAVAILABLE",
                        "Lỗi nguồn lịch xe"))
                .when(vehicleTarget)
                .followTrip(
                        org.mockito.ArgumentMatchers.eq(s),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
        depart(s, r).andExpect(status().isServiceUnavailable());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(participantStatus(s, r)).isEqualTo("ELIGIBLE");
        assertThat(jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, s))
                .isEqualTo("SCHEDULED");
        assertThat(departures(r)).isZero();

        org.mockito.Mockito.reset(vehicleTarget);
        depart(s, r).andExpect(status().isOk());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(participantStatus(s, r)).isEqualTo("DEPARTED");
        assertThat(departures(r)).isEqualTo(1);
        // 002 T049 (FR-031): người đã rời viện của chuyến Đang diễn ra thuộc phạm vi trưởng đoàn
        assertThat(dutyReads.getObject().tripResidentsOf(manager.staffId())).contains(r);
        assertThat(jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, s))
                .isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM simple_record WHERE id=? AND kind='M03.VEHICLE_SCHEDULE'",
                        String.class,
                        schedule))
                .isEqualTo("IN_USE");
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.TripChanged.class)
                        .filter(e -> e.tripId() == s && e.type().equals("FIRST_DEPARTURE")))
                .hasSize(1);

        // T014 (007 FR-060 (b)): chuyến là buổi cùng tham gia theo điểm danh rời viện; người Được đi chưa rời viện
        // không
        // có trong danh sách.
        long r2 = participant(s, "ELIGIBLE"), stayed = participant(s, "ELIGIBLE");
        tripMedication.prepared(r2, s);
        depart(s, r2).andExpect(status().isOk());
        var window = new Instant[] {departure.minusSeconds(3600), departure.plusSeconds(3600)};
        assertThat(trace.coAttendance(r, window[0], window[1])).singleElement().satisfies(v -> {
            assertThat(v.sessionId()).isEqualTo(s);
            assertThat(v.residentIds()).containsExactly(r2);
        });
        assertThat(trace.coAttendance(r2, window[0], window[1]))
                .singleElement()
                .satisfies(v -> assertThat(v.residentIds()).containsExactly(r));
        assertThat(trace.coAttendance(stayed, window[0], window[1])).isEmpty();
    }

    /** T015 (BR-M04-16): hai thiết bị điểm danh rời viện cùng người, chỉ một lần được ghi. */
    private void BR_M04_16_twoDevicesCanOnlyDepartTheResidentOnce() throws Exception {
        long s = trip(false), r = participant(s, "ELIGIBLE");
        leader(s);
        tripMedication.prepared(r, s);
        var security = org.springframework.security.test.context.TestSecurityContextHolder.getContext();
        var start = new java.util.concurrent.CountDownLatch(1);
        var statuses = new java.util.concurrent.ConcurrentLinkedQueue<Integer>();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
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
        assertThat(departures(r)).isEqualTo(1);
    }

    /**
     * T010 Q12 (FR-021a): người Được đi chưa rời viện bị gắn nghi nhiễm thì Không đi, đăng ký hoạt động nhóm khác bị hủy,
     * hoạt động cá nhân giữ; trưởng đoàn và Trưởng tầng của chuyến được báo.
     */
    private void FR_021a_suspectedInfectionMarksTheParticipantNotGoingCancelsGroupRegistrationsAndNotifiesTheTrip()
            throws Exception {
        long s = trip(false), b = participant(s, "ELIGIBLE");
        leader(s);
        long group = indoorSession(true), individual = indoorSession(false);
        long g = indoorRegistration(group, b), i = indoorRegistration(individual, b);
        gate.suspectInfection(b);

        tx.executeWithoutResult(t -> cancels.cancelForSuspectedInfection(b, time.now()));

        assertThat(jdbc.queryForMap(
                        "SELECT status,participant_status,participant_reason FROM activity_registration"
                                + " WHERE session_id=? AND resident_id=?",
                        s,
                        b))
                .containsEntry("status", "CANCELLED")
                .containsEntry("participant_status", "NOT_GOING")
                .containsEntry("participant_reason", "Nghi nhiễm");
        assertThat(jdbc.queryForObject("SELECT auto_cancel FROM activity_registration WHERE id=?", String.class, g))
                .isEqualTo("INFECTION_SUSPECTED");
        assertThat(jdbc.queryForObject("SELECT status FROM activity_registration WHERE id=?", String.class, i))
                .isEqualTo("REGISTERED");
        assertThat(currentStatus(b)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(recorded.stream(com.eldercare.backend.care.api.events.ActivityRegistrationsCancelled.class)
                        .filter(e -> e.sessionId() == s))
                .singleElement()
                .satisfies(e -> assertThat(e.context().recipients())
                        .contains(
                                new com.eldercare.backend.common.notification.RecipientGroup.TripLeader(
                                        s, new com.eldercare.backend.common.notification.Substitution.Default()),
                                new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(
                                        floor, new com.eldercare.backend.common.notification.Substitution.Default())));
    }

    /**
     * 019 T022 → 014 (chủ dự án chốt 2026-11-09): đính chính giờ dự kiến của lịch xe Đã hoàn thành đổi giờ rời, về dự
     * kiến của chuyến Đã về; tác động đã xảy ra giữ nguyên, không phát {@code TripChanged}. Nhận lặp cùng giá trị không
     * ghi; chuyến không ở Đã về thì lỗi và lệnh đính chính của 019 rollback.
     */
    @Test
    void VehicleScheduleCorrected_syncsReturnedTripPlannedTimesInTheCorrectionCommand() throws Exception {
        long s = trip(true), r = participant(s, "ELIGIBLE");
        leader(s);
        long schedule = bookVehicle(s);
        tripMedication.prepared(r, s);
        depart(s, r).andExpect(status().isOk());
        mvc.perform(write(post("/api/v1/trips/" + s + "/return"), "{\"residentIds\":[" + r + "]}"))
                .andExpect(status().isOk());
        mvc.perform(write(post("/api/v1/trips/" + s + "/finish-return"), "{}")).andExpect(status().isOk());
        var before = jdbc.queryForMap(
                "SELECT trip_status,trip_return_finished_at,version FROM activity_session WHERE id=?", s);
        long tripEvents = tripChanges(s);
        int attendance = departures(r);
        Instant from = departure.minusSeconds(600), to = departure.plusSeconds(25200);

        correctSchedule(schedule, from, to).andExpect(status().isCreated());
        var after = jdbc.queryForMap("SELECT * FROM activity_session WHERE id=?", s);
        assertThat(after.get("trip_planned_departure_at")).isEqualTo(LocalDateTime.ofInstant(from, BusinessTime.ZONE));
        assertThat(after.get("trip_planned_return_at")).isEqualTo(LocalDateTime.ofInstant(to, BusinessTime.ZONE));
        assertThat(after.get("trip_status")).isEqualTo(before.get("trip_status"));
        assertThat(after.get("trip_return_finished_at")).isEqualTo(before.get("trip_return_finished_at"));
        assertThat(((Number) after.get("version")).intValue())
                .isEqualTo(((Number) before.get("version")).intValue() + 1);
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(participantStatus(s, r)).isEqualTo("RETURNED");
        assertThat(departures(r)).isEqualTo(attendance);
        assertThat(tripChanges(s)).isEqualTo(tripEvents);

        // Nhận lặp cùng giá trị: không ghi thêm.
        correctSchedule(schedule, from, to).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT version FROM activity_session WHERE id=?", Integer.class, s))
                .isEqualTo(((Number) after.get("version")).intValue());
        assertThat(tripChanges(s)).isEqualTo(tripEvents);

        // Chuyến không ở Đã về: listener lỗi, bản đính chính thứ ba không được ghi.
        jdbc.update("UPDATE activity_session SET trip_status='IN_PROGRESS' WHERE id=?", s);
        correctSchedule(schedule, from.plusSeconds(60), to)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        mvc.perform(get("/api/v1/corrections/M03.VEHICLE_SCHEDULE/" + schedule))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corrections.length()").value(2));
        assertThat(jdbc.queryForObject(
                        "SELECT trip_planned_departure_at FROM activity_session WHERE id=?", LocalDateTime.class, s))
                .isEqualTo(LocalDateTime.ofInstant(from, BusinessTime.ZONE));
        jdbc.update("UPDATE activity_session SET trip_status='RETURNED' WHERE id=?", s);
    }

    private ResultActions correctSchedule(long schedule, Instant from, Instant to) throws Exception {
        return mvc.perform(post("/api/v1/corrections/M03.VEHICLE_SCHEDULE/" + schedule)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"plannedDepartureAt\":\"" + from
                        + "\",\"plannedReturnAt\":\"" + to + "\"},\"reason\":\"Ghi nhầm giờ đặt xe\"}"));
    }

    private long tripChanges(long s) {
        return recorded.stream(com.eldercare.backend.care.api.events.TripChanged.class)
                .filter(e -> e.tripId() == s)
                .count();
    }

    long indoorSession(boolean group) {
        long a = TestUsers.nextId(), s = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_at,created_by) VALUES(?,'M04.ACTIVITY','M04','ACTIVE',?,NOW(6),1)",
                a,
                "{\"name\":\"Hát\",\"typeId\":1,\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":20,\"charged\":false,\"groupActivity\":" + group
                        + ",\"createdByStaffId\":1}");
        var day = time.today().plusDays(2);
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,floor_area_id,status,created_at,created_by)"
                        + " VALUES(?,?,?,?,'SINGLE',?,'SCHEDULED',NOW(6),1)",
                s,
                a,
                day.atTime(9, 0),
                day.atTime(10, 0),
                floor);
        return s;
    }

    long indoorRegistration(long session, long resident) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO activity_registration(id,session_id,resident_id,source,registered_by_staff_id,registered_at,status)"
                        + " VALUES(?,?,?,'MANUAL',?,NOW(6),'REGISTERED')",
                id,
                session,
                resident,
                manager.staffId());
        return id;
    }

    private long bookVehicle(long trip) throws Exception {
        long vehicle = TestUsers.nextId(), department = TestUsers.nextId();
        long type = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M03.ASSET_TYPE' AND code='VEHICLE'", Long.class);
        jdbc.update(
                "INSERT INTO asset(id,code,type_id,name,department_id,source,source_description,status,location_type,created_at)"
                        + " VALUES(?,?,?,?,?,'DONATED','Tặng','READY','PARKING_LOT',?)",
                vehicle,
                "XE-" + vehicle,
                type,
                "Xe đưa đón",
                department,
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        return id(mvc.perform(write(post("/api/v1/vehicle-schedules"), """
                {"vehicleId":%d,"plannedDepartureAt":"%s","plannedReturnAt":"%s",
                 "purpose":"TRIP","tripId":%d,"driverStaffId":%d}
                """.formatted(
                        vehicle, departure, departure.plusSeconds(21600), trip, manager.staffId()))));
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
}
