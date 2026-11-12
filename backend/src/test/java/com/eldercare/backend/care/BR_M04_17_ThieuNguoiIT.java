package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.api.events.AlertProgressRequested;
import com.eldercare.backend.care.api.events.AlertRequested;
import com.eldercare.backend.care.api.events.TripChanged;
import com.eldercare.backend.care.api.events.TripSafetyNotice;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.care.internal.trip.service.TripReturnOverdueStep;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 014 T019 (US5, FR-043 → FR-050, BR-M04-17): HTTP thật, trạng thái 001 thật; nguồn sự cố dùng bản giả chung,
 * kiểm đồng thời, trực tuyến và rollback. Đính chính FR-031a còn chờ nguồn 001.
 */
@AutoConfigureMockMvc
@WithMockUser
@RecordApplicationEvents
@Import(FakeM04Config.class)
class BR_M04_17_ThieuNguoiIT extends StatusTestBase {
    @Autowired
    org.springframework.context.ApplicationContext context;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripIncidents incidents;

    @Autowired
    com.eldercare.backend.support.fake.administration.FakeDuties duties;

    @Test
    void BR_M04_17_concurrentMissingCommandsCreateOneIncidentAndPortErrorsRollBack() throws Exception {
        incidents.reset();
        long s = trip(), r = eligible(s);
        var companion = new TestUsers(jdbc, time).staff(Role.KITCHEN_STAFF);
        assign(s, companion.staffId());
        departed(s, r);
        duties.trip(s, manager.staffId(), List.of(companion.staffId()));
        user.set(companion.user());
        String body = "{\"residentIds\":[" + r + "],\"lastSeenPlace\":\"Cổng công viên\",\"lastSeenAt\":\"" + time.now()
                + "\",\"description\":\"Không thấy người trong đoàn\"}";
        var security = org.springframework.security.test.context.TestSecurityContextHolder.getContext();
        var start = new java.util.concurrent.CountDownLatch(1);
        var statuses = new java.util.concurrent.ConcurrentLinkedQueue<Integer>();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures = new ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 2; i++)
                futures.add(pool.submit(() -> {
                    org.springframework.security.test.context.TestSecurityContextHolder.setContext(security);
                    try {
                        start.await();
                        statuses.add(command(s, "report-missing", body)
                                .andReturn()
                                .getResponse()
                                .getStatus());
                    } finally {
                        org.springframework.security.test.context.TestSecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            start.countDown();
            for (var f : futures) f.get(30, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        user.set(manager.user());
        assertThat(incidents.missing()).singleElement().satisfies(m -> {
            assertThat(m.residentId()).isEqualTo(r);
            assertThat(m.tripId()).isEqualTo(s);
            assertThat(m.leaderId()).isEqualTo(manager.staffId());
            assertThat(m.place()).isEqualTo("Cổng công viên");
        });
        assertThat(participant(s, r)).isEqualTo("MISSING_ON_RETURN");
        incidents.failing(true);
        try {
            assertThatThrownBy(() -> command(s, "return", "{\"residentIds\":[" + r + "]}"))
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            assertThat(participant(s, r)).isEqualTo("MISSING_ON_RETURN");
            assertThat(currentStatus(r)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        } finally {
            incidents.failing(false);
        }
        command(s, "return", "{\"residentIds\":[" + r + "]}").andExpect(status().isOk());
        assertThat(incidents.found())
                .singleElement()
                .satisfies(f -> assertThat(f.residentId()).isEqualTo(r));

        tripFixture();
        long second = trip(), other = eligible(second);
        assign(second);
        departed(second, other);
        String secondBody = body.replace("\"residentIds\":[" + r + "]", "\"residentIds\":[" + other + "]");
        incidents.failing(true);
        try {
            assertThatThrownBy(() -> command(second, "report-missing", secondBody))
                    .hasRootCauseInstanceOf(IllegalStateException.class);
            assertThat(participant(second, other)).isEqualTo("DEPARTED");
        } finally {
            incidents.failing(false);
        }
        command(
                        second,
                        "report-missing",
                        secondBody.substring(0, secondBody.length() - 1) + ",\"deviceAt\":\"" + time.now() + "\"}")
                .andExpect(status().isConflict());
        command(second, "finish-return", "{}").andExpect(status().isOk());
        command(second, "record-missing-incident", secondBody).andExpect(status().isOk());
        assertThat(incidents.missing()).hasSize(2);
        var online = context
                .getBeansOfType(com.eldercare.backend.administration.account.api.spi.OfflineHandler.class)
                .values()
                .stream()
                .filter(h -> h.recordType().equals("M04.TRIP_MISSING"))
                .toList();
        assertThat(online).singleElement().satisfies(h -> {
            assertThat(h.requiresOnline()).isTrue();
            assertThatThrownBy(() -> h.apply(null, null, time.now()))
                    .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class);
        });
    }

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripStaffSchedule tripShifts;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripMedicationPreparation tripMedication;

    @Autowired
    TripReturnOverdueStep overdue;

    @Autowired
    ApplicationEvents recorded;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    TransactionTemplate tx;

    long floor;
    long activity;
    Instant departure;
    Instant plannedReturn;
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
        plannedReturn = departure.plusSeconds(21600);
    }

    /**
     * Q24, FR-031a (c) (đổi api 001 duyệt 2026-11-11): điểm danh về ghi ngoại tuyến mang thời điểm thiết bị vào lịch sử 001;
     * Hủy ghi nhận bản ghi về đưa người về Hoạt động bên ngoài và Đã rời viện.
     */
    @Test
    void Q24_FR_031a_offlineReturnThenVoidingTheReturnRecord() throws Exception {
        tripFixture();
        long s = trip();
        long r = eligible(s);
        assign(s);
        departed(s, r);
        var deviceAt = time.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        command(s, "return", "{\"residentIds\":[" + r + "],\"deviceAt\":\"" + deviceAt + "\"}")
                .andExpect(status().isOk());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(participant(s, r)).isEqualTo("RETURNED");
        assertThat(context.getBean(
                                com.eldercare.backend.medication.internal.handover.persistence
                                        .MedicationAbsenceIntervalRepository.class)
                        .findByTripId(s)
                        .orElseThrow()
                        .isOpen())
                .as("006 đóng khoảng mang thuốc khi về")
                .isFalse();

        long returnRecord = jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(content, '$.type'))='RETURN'",
                Long.class,
                r);
        mvc.perform(post("/api/v1/corrections/M04.TRIP_ATTENDANCE/" + returnRecord)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"VOID\",\"reason\":\"Ghi về nhầm người chưa về\"}"))
                .andExpect(status().is2xxSuccessful());
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(participant(s, r)).isEqualTo("DEPARTED");
        assertThat(context.getBean(
                                com.eldercare.backend.medication.internal.handover.persistence
                                        .MedicationAbsenceIntervalRepository.class)
                        .findByTripId(s)
                        .orElseThrow()
                        .isOpen())
                .as("006 mở lại khoảng mang thuốc")
                .isTrue();
    }

    /**
     * Q26 → Q28, FR-031a (b), Q-240 (viết lại 2026-11-11, thay {@code FR_031a_DinhChinhChuyenIT} mất trong đợt giảm test):
     * Hủy ghi nhận bản ghi rời viện đưa người về Đang lưu trú, người tham gia về Được đi, 006 có lại khoảng dự kiến; còn
     * người khác đã rời viện thì chuyến vẫn Đang đi; hủy bản rời viện cuối cùng đưa chuyến về Đã lên lịch.
     */
    @Test
    void Q26_FR_031a_voidingDepartureRecords_lastOneUnstartsTheTrip() throws Exception {
        tripFixture();
        long s = trip();
        long first = eligible(s), second = eligible(s);
        assign(s);
        departed(s, first, second);
        assertThat(tripStatus(s)).isEqualTo("IN_PROGRESS");

        voidDeparture(first);
        assertThat(currentStatus(first)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(participant(s, first)).isEqualTo("ELIGIBLE");
        assertThat(tripStatus(s)).as("còn người đã rời viện").isEqualTo("IN_PROGRESS");

        voidDeparture(second);
        assertThat(currentStatus(second)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(tripStatus(s)).as("Q-240: hủy bản rời viện cuối cùng").isEqualTo("SCHEDULED");
        assertThat(recorded.stream(TripChanged.class).filter(e -> e.tripId() == s))
                .extracting(TripChanged::type)
                .containsSubsequence("CHANGED", "LAST_DEPARTURE_UNDONE");
        assertThat(context.getBean(
                                com.eldercare.backend.medication.internal.handover.persistence
                                        .MedicationAbsenceIntervalRepository.class)
                        .findByTripId(s))
                .as("006 vẫn giữ khoảng dự kiến của chuyến")
                .isPresent();
    }

    @Autowired
    com.eldercare.backend.common.time.TestClockControl clock;

    /**
     * FR-031a (a), Q-170 (đổi api 001 chủ dự án duyệt 2026-11-11): Trưởng tầng đính chính thời điểm thực tế rời viện, về;
     * lịch sử 001 tính khoảng Hoạt động bên ngoài theo thời điểm đúng (dòng gốc giữ nguyên), 006 nhận khoảng mang thuốc mới,
     * 011 nhận {@link TripChanged}; thời điểm sai thứ tự với lịch sử hoặc ở tương lai bị 422, nội dung khác {@code at} bị 422.
     */
    @Test
    void FR_031a_correctingDepartureAndReturnTimes_recalculatesHistoryAndMedication() throws Exception {
        tripFixture();
        long s = trip();
        long r = eligible(s);
        assign(s);
        Instant seeded = time.now();
        try {
            clock.advanceBy(Duration.ofHours(1));
            departed(s, r);
            clock.advanceBy(Duration.ofHours(1));
            command(s, "return", "{\"residentIds\":[" + r + "]}").andExpect(status().isOk());
            clock.advanceBy(Duration.ofMinutes(10));
            long departureRecord = attendance(r, "DEPARTURE"), returnRecord = attendance(r, "RETURN");
            Instant departedAt = seeded.plus(Duration.ofMinutes(30)).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
            Instant returnedAt = seeded.plus(Duration.ofMinutes(100)).truncatedTo(java.time.temporal.ChronoUnit.MICROS);

            amendTime(departureRecord, "{\"at\":\"" + departedAt + "\",\"residentId\":1}")
                    .andExpect(status().isUnprocessableContent());
            amendTime(departureRecord, "{\"at\":\"" + time.now().plusSeconds(3600) + "\"}")
                    .andExpect(status().isUnprocessableContent());
            // sau lần về của chính người đó
            amendTime(departureRecord, "{\"at\":\"" + seeded.plus(Duration.ofHours(3)) + "\"}")
                    .andExpect(status().isUnprocessableContent());

            amendTime(departureRecord, "{\"at\":\"" + departedAt + "\"}").andExpect(status().is2xxSuccessful());
            amendTime(returnRecord, "{\"at\":\"" + returnedAt + "\"}").andExpect(status().is2xxSuccessful());

            assertThat(statusApi.statusPeriods(r, Set.of(ResidentStatus.OFF_SITE_ACTIVITY), seeded, time.now()))
                    .singleElement()
                    .satisfies(p -> {
                        assertThat(p.from()).isEqualTo(departedAt);
                        assertThat(p.to()).contains(returnedAt);
                    });
            assertThat(jdbc.queryForObject(
                            "SELECT COUNT(*) FROM status_history WHERE entity_type='M01.RESIDENT' AND attribute='STATUS'"
                                    + " AND entity_id=? AND command IN ('ROLL_CALL_DEPARTURE','ROLL_CALL_RETURN')",
                            Integer.class,
                            r))
                    .as("dòng gốc giữ nguyên, không thêm dòng trạng thái")
                    .isEqualTo(2);
            assertThat(participant(s, r)).isEqualTo("RETURNED");
            assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
            // 006 nhận khoảng mang thuốc theo thời điểm đúng qua cổng (bản giả chung của lớp này)
            assertThat(tripMedication.interval(r, s))
                    .contains(new com.eldercare.backend.care.api.dto.TripInterval(departedAt, returnedAt));
            assertThat(recorded.stream(TripChanged.class).filter(e -> e.tripId() == s))
                    .filteredOn(e -> e.eventKey().startsWith("TRIP_ATTENDANCE_TIME_CORRECTED:"))
                    .hasSize(2)
                    .last()
                    .satisfies(e -> assertThat(e.actualAt()).contains(returnedAt));
        } finally {
            clock.reset();
        }
    }

    private long attendance(long resident, String type) {
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(content, '$.type'))=?",
                Long.class,
                resident,
                type);
    }

    private ResultActions amendTime(long record, String content) throws Exception {
        return mvc.perform(post("/api/v1/corrections/M04.TRIP_ATTENDANCE/" + record)
                .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":" + content
                        + ",\"reason\":\"Ghi nhầm giờ\"}"));
    }

    /**
     * Q24, Q-01 (T015, T018, T020): điểm danh rời viện, về ghi khi mất mạng đồng bộ qua {@code M04.TRIP_DEPARTURE},
     * {@code M04.TRIP_RETURN} (không bắt buộc trực tuyến); thời điểm thiết bị là thời điểm của bản ghi và lịch sử 001.
     */
    @Test
    void Q24_offlineDepartureAndReturn_syncThroughHandlers() throws Exception {
        tripFixture();
        long s = trip();
        long r = eligible(s);
        assign(s);
        var departedAt = time.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        assertThat(offline("M04.TRIP_DEPARTURE").requiresOnline()).isFalse();
        sync("M04.TRIP_DEPARTURE", "{\"tripId\":" + s + ",\"residentIds\":[" + r + "]}", departedAt);
        assertThat(participant(s, r)).isEqualTo("DEPARTED");
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(tripStatus(s)).isEqualTo("IN_PROGRESS");
        // 007 T063 (US12, FR-047e): SOS lúc đang đi đọc chuyến thật qua cổng của 007 (TripSosSource), có trưởng đoàn.
        assertThat(context.getBean(com.eldercare.backend.incident.api.spi.SosTripPort.class)
                        .ongoing(r))
                .hasValueSatisfying(v -> assertThat(v.tripId()).isEqualTo(s));

        var returnedAt = time.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        sync("M04.TRIP_RETURN", "{\"tripId\":" + s + ",\"residentIds\":[" + r + "]}", returnedAt);
        assertThat(participant(s, r)).isEqualTo("RETURNED");
        assertThat(currentStatus(r)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForList(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content, '$.at')) FROM record_event"
                                + " WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=? ORDER BY id",
                        String.class,
                        r))
                .extracting(java.time.Instant::parse)
                .containsExactly(departedAt, returnedAt);
    }

    @Autowired
    List<com.eldercare.backend.administration.account.api.spi.OfflineHandler<?>> offlineHandlers;

    @Autowired
    tools.jackson.databind.json.JsonMapper json;

    @SuppressWarnings("unchecked")
    private com.eldercare.backend.administration.account.api.spi.OfflineHandler<Object> offline(String type) {
        return (com.eldercare.backend.administration.account.api.spi.OfflineHandler<Object>) offlineHandlers.stream()
                .filter(h -> h.recordType().equals(type))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có bộ xử lý " + type));
    }

    /** Như {@code SyncService} của 002: giải nội dung, kiểm, transaction riêng, thời điểm thiết bị là effectiveAt. */
    private void sync(String type, String body, java.time.Instant deviceAt) {
        var h = offline(type);
        var contentType = org.springframework.core.ResolvableType.forClass(h.getClass())
                .as(com.eldercare.backend.administration.account.api.spi.OfflineHandler.class)
                .getGeneric(0)
                .resolve(Object.class);
        Object value = json.readValue(body, contentType);
        h.validate(value);
        h.target(value);
        tx.execute(t -> h.apply(
                value,
                new com.eldercare.backend.common.offline.SyncOutcome(
                        com.eldercare.backend.common.offline.SyncResult.ACCEPTED, null),
                deviceAt));
    }

    private void voidDeparture(long resident) throws Exception {
        long record = jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(content, '$.type'))='DEPARTURE'",
                Long.class,
                resident);
        mvc.perform(post("/api/v1/corrections/M04.TRIP_ATTENDANCE/" + record)
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"VOID\",\"reason\":\"Điểm danh rời viện nhầm, người ở lại viện\"}"))
                .andExpect(status().is2xxSuccessful());
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

    long trip() throws Exception {
        return id(mvc.perform(write(
                post("/api/v1/activity-sessions"),
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + departure + "\",\"endsAt\":\"" + plannedReturn
                        + "\",\"trip\":{\"destination\":\"Chùa\",\"plannedDepartureAt\":\"" + departure
                        + "\",\"plannedReturnAt\":\"" + plannedReturn
                        + "\",\"transport\":\"Xe thuê\",\"facilityVehicle\":false}}")));
    }

    long eligible(long s) {
        long resident = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(manager.user());
        jdbc.update(
                "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status,participant_status)"
                        + " VALUES(?,?,'MANUAL',?,'REGISTERED','ELIGIBLE')",
                s,
                resident,
                LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
        tripMedication.prepared(resident, s);
        return resident;
    }

    void assign(long s, long... companions) throws Exception {
        tripShifts.shift(manager.staffId(), departure.minusSeconds(3600), departure.plusSeconds(28800));
        for (long c : companions) tripShifts.shift(c, departure.minusSeconds(3600), departure.plusSeconds(28800));
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/assignment"),
                        "{\"leaderId\":" + manager.staffId() + ",\"companionIds\":" + Arrays.toString(companions)
                                + "}"))
                .andExpect(status().isOk());
    }

    /** Chuyến Đang đi với các người đã rời viện; trưởng đoàn là Trưởng tầng của fixture. */
    long departed(long s, long... residents) throws Exception {
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/depart"), "{\"residentIds\":" + Arrays.toString(residents) + "}"))
                .andExpect(status().isOk());
        return s;
    }

    ResultActions command(long s, String command, String body) throws Exception {
        return mvc.perform(write(post("/api/v1/trips/" + s + "/" + command), body));
    }

    String participant(long s, long resident) {
        return jdbc.queryForObject(
                "SELECT participant_status FROM activity_registration WHERE session_id=? AND resident_id=?"
                        + " ORDER BY id DESC LIMIT 1",
                String.class,
                s,
                resident);
    }

    String tripStatus(long s) {
        return jdbc.queryForObject("SELECT trip_status FROM activity_session WHERE id=?", String.class, s);
    }

    List<TripSafetyNotice> notices(long s, String cause) {
        return recorded.stream(TripSafetyNotice.class)
                .filter(n -> n.tripId() == s && n.cause().equals(cause))
                .toList();
    }

    /**
     * T015 Q25 (FR-049, FR-049a, CFG-M04-07): quá giờ về +30 phút báo trưởng đoàn và Trưởng tầng, +60 phút báo Quản lý
     * viện; mức Trung bình, mỗi mốc một lần.
     */
    /** Gộp một ca theo ngân sách IT (AGENTS.md mục 4); mỗi dòng dựng lại fixture (buổi cùng giờ bắt đầu là duy nhất). */
    @Test
    void FR_047_FR_049_overdueMilestonesAndFinishingReturn() throws Exception {
        com.eldercare.backend.support.TableRows.each(List.of("OVERDUE", "FINISH_RETURN"), scenario -> {
            tripFixture();
            switch (scenario) {
                case "OVERDUE" -> FR_049_overdueReturnNotifiesLeaderAndFloorOnceThenTheManagerAtTwiceTheThreshold();
                case "FINISH_RETURN" ->
                    FR_047_finishingReturnMarksAbsentResidentsMissingAndRequestsAnUrgentAlertWithoutAnIncident();
            }
        });
    }

    private void FR_049_overdueReturnNotifiesLeaderAndFloorOnceThenTheManagerAtTwiceTheThreshold() throws Exception {
        long s = trip(), a = eligible(s);
        assign(s);
        departed(s, a);
        overdue.run(plannedReturn.plusSeconds(1740));
        assertThat(notices(s, "RETURN_OVERDUE")).isEmpty();
        overdue.run(plannedReturn.plusSeconds(1860));
        overdue.run(plannedReturn.plusSeconds(1920));
        assertThat(notices(s, "RETURN_OVERDUE")).singleElement().satisfies(n -> {
            assertThat(n.level()).isEqualTo(NotificationLevel.MEDIUM);
            assertThat(n.context().recipients())
                    .contains(
                            new com.eldercare.backend.common.notification.RecipientGroup.TripLeader(
                                    s, new com.eldercare.backend.common.notification.Substitution.Default()),
                            new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(
                                    floor, new com.eldercare.backend.common.notification.Substitution.Default()))
                    .noneMatch(r -> r instanceof com.eldercare.backend.common.notification.RecipientGroup.FacilityRole);
        });
        assertThat(jdbc.queryForObject("SELECT trip_overdue_return FROM activity_session WHERE id=?", Boolean.class, s))
                .isTrue();
        assertThat(participant(s, a)).isEqualTo("DEPARTED");
        assertThat(notices(s, "RETURN_OVERDUE_MANAGER")).isEmpty();
        overdue.run(plannedReturn.plusSeconds(3660));
        overdue.run(plannedReturn.plusSeconds(3720));
        assertThat(notices(s, "RETURN_OVERDUE_MANAGER"))
                .singleElement()
                .satisfies(n -> assertThat(n.context().recipients())
                        .contains(new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(
                                Role.NURSING_HOME_MANAGER)));
    }

    private void FR_047_finishingReturnMarksAbsentResidentsMissingAndRequestsAnUrgentAlertWithoutAnIncident()
            throws Exception {
        long s = trip(), a = eligible(s), c = eligible(s);
        assign(s);
        departed(s, a, c);
        command(s, "return", "{\"residentIds\":[" + a + "]}").andExpect(status().isOk());
        command(s, "finish-return", "{}").andExpect(status().isOk());
        assertThat(tripStatus(s)).isEqualTo("RETURNED");
        assertThat(participant(s, a)).isEqualTo("RETURNED");
        assertThat(participant(s, c)).isEqualTo("MISSING_ON_RETURN");
        assertThat(currentStatus(c)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(recorded.stream(AlertRequested.class).filter(e -> e.residentId() == c || e.residentId() == a))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.residentId()).isEqualTo(c);
                    assertThat(e.type()).isEqualTo("MISSING_ON_RETURN");
                    assertThat(e.level()).isEqualTo(NotificationLevel.URGENT);
                });
        assertThat(jdbc.queryForObject(
                        "SELECT missing_incident_id FROM activity_registration WHERE session_id=? AND resident_id=?",
                        Long.class,
                        s,
                        c))
                .isNull();
        assertThat(recorded.stream(TripChanged.class)
                        .filter(e -> e.tripId() == s && e.type().equals("RETURN_FINISHED")))
                .hasSize(1);
        command(s, "finish-return", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        // FR-050: người Thiếu khi về được điểm danh về muộn sau khi chuyến Đã về; cảnh báo nhận diễn biến.
        command(s, "return", "{\"residentIds\":[" + c + "]}").andExpect(status().isOk());
        assertThat(currentStatus(c)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(participant(s, c)).isEqualTo("RETURNED");
        assertThat(recorded.stream(AlertProgressRequested.class)
                        .filter(e -> e.sourceType().equals("M04.TRIP_PARTICIPANT")))
                .anySatisfy(e -> assertThat(e.progressCode()).isEqualTo("FOUND_RETURNED"));
        overdue.run(plannedReturn.plusSeconds(3660));
        assertThat(notices(s, "RETURN_OVERDUE")).isEmpty();
    }
}
