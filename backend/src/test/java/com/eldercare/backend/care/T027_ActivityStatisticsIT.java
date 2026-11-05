package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.dto.ScopeFilter;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.care.api.ActivityStatisticsApi;
import com.eldercare.backend.care.internal.qualitycheck.domain.QualityCheckKinds;
import com.eldercare.backend.care.internal.trip.domain.TripKinds;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeAssignments;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import com.eldercare.backend.support.fake.resident.FakeM01Config;
import com.eldercare.backend.support.fake.resident.FakeResidents;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

/**
 * 014 T027 (contracts mục 1.2 HoatDongThongKeApi; FR-065, FR-070): số liệu chỉ đọc cho 016 theo phạm vi và khoảng ngày; điểm
 * danh theo giá trị hiện hành sau đính chính. Nguy cơ cô lập chờ chuỗi cô lập (plan Phase 8).
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class T027_ActivityStatisticsIT extends MySqlIntegrationTestBase {
    @Autowired
    org.springframework.test.web.servlet.MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    FakeAssignments assignments;

    @Autowired
    FakeResidents residents;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    RecordEventApi records;

    @Autowired
    ActivityStatisticsApi statistics;

    SessionAttendanceFixture data;
    ScopeFilter floorOnly;

    @BeforeEach
    void fixture() {
        data = new SessionAttendanceFixture(jdbc, time, assignments, user, residents);
        floorOnly = new ScopeFilter(false, Set.of(data.floor), Set.of(), Set.of());
    }

    long record(long session, long resident, String result, String extra) throws Exception {
        mvc.perform(post("/api/v1/activity-sessions/" + session + "/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "attendance-" + TestUsers.nextId())
                        .content("{\"results\":[{\"residentId\":" + resident + ",\"result\":\"" + result + "\"" + extra
                                + "}]}"))
                .andExpect(status().isOk());
        return jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.SESSION_ATTENDANCE' AND JSON_EXTRACT(content,'$.sessionId')=?"
                        + " AND JSON_EXTRACT(content,'$.residentId')=?",
                Long.class,
                session,
                resident);
    }

    LocalDateTime local(Instant at) {
        return LocalDateTime.ofInstant(at, BusinessTime.ZONE);
    }

    @Test
    void FR_070_sessionCountsUseCurrentAttendanceValuesAndRespectTheScope() throws Exception {
        long session = data.session(data.activity(false), Duration.ofMinutes(-30));
        var people = data.residents(session, 4);
        long present = record(session, people.get(0), "PRESENT", ",\"participationLevel\":\"NORMAL\"");
        record(session, people.get(1), "PRESENT", ",\"participationLevel\":\"NORMAL\"");
        record(session, people.get(2), "ABSENT", ",\"absenceReason\":\"HEALTH\"");
        // "Không ghi nhận" do bước hết ngày ghi (FR-014a), không qua lệnh điểm danh.
        long registration = jdbc.queryForObject(
                "SELECT id FROM activity_registration WHERE session_id=? AND resident_id=?",
                Long.class,
                session,
                people.get(3));
        records.add(
                com.eldercare.backend.care.internal.session.domain.SessionKinds.ATTENDANCE,
                new EntityRef("M01.RESIDENT", people.get(3)),
                new NewRecordEvent<>(
                        new com.eldercare.backend.care.internal.session.domain.SessionKinds.AttendanceResult(
                                registration,
                                session,
                                people.get(3),
                                "NOT_RECORDED",
                                null,
                                null,
                                null,
                                null,
                                null,
                                false,
                                null),
                        null,
                        null,
                        true,
                        time.now(),
                        null,
                        time.now(),
                        data.floor,
                        com.eldercare.backend.care.internal.session.domain.SessionKinds.key(registration)));
        corrections.create("M04.SESSION_ATTENDANCE", present, CorrectionType.VOID, null, new Reason("Ghi nhầm người"));
        var today = time.today();
        var view = statistics
                .sessions(floorOnly, today, today)
                .filter(v -> v.sessionId() == session)
                .findFirst()
                .orElseThrow();
        assertThat(view.trip()).isFalse();
        assertThat(view.registered()).isEqualTo(4);
        assertThat(view.present()).isEqualTo(1);
        assertThat(view.absentByReason()).containsExactlyEntriesOf(Map.of("HEALTH", 1));
        assertThat(view.notRecorded()).isEqualTo(1);
        var otherFloor = new ScopeFilter(false, Set.of(TestUsers.nextId()), Set.of(), Set.of());
        assertThat(statistics.sessions(otherFloor, today, today).filter(v -> v.sessionId() == session))
                .isEmpty();
        assertThat(statistics
                        .sessions(floorOnly, today.plusDays(1), today.plusDays(2))
                        .filter(v -> v.sessionId() == session))
                .isEmpty();
    }

    @Test
    void tripsReportTheOriginalReturnTimeExtensionsActualReturnAndMissingResidents() {
        long activity = data.activity(false);
        long trip = TestUsers.nextId();
        Instant departure = time.now().minusSeconds(7200).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        Instant original = departure.plusSeconds(3600);
        Instant extended = original.plusSeconds(1800);
        Instant finished = time.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,floor_area_id,status,created_at,created_by,"
                        + "trip_destination,trip_planned_departure_at,trip_planned_return_at,trip_transport,trip_facility_vehicle,"
                        + "trip_status,trip_return_finished_at)"
                        + " VALUES(?,?,?,?,'SINGLE',?,'SCHEDULED',NOW(6),1,'Chùa',?,?,'Xe thuê',FALSE,'RETURNED',?)",
                trip,
                activity,
                local(departure),
                local(extended),
                data.floor,
                local(departure),
                local(extended),
                local(finished));
        for (String state : List.of("RETURNED", "MISSING_ON_RETURN", "RETURNED"))
            jdbc.update(
                    "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status,participant_status,"
                            + "missing_alert_requested) VALUES(?,?,'MANUAL',?,'REGISTERED',?,?)",
                    trip,
                    data.resident(),
                    local(departure.minusSeconds(86400)),
                    state,
                    !"RETURNED".equals(state));
        records.add(
                TripKinds.EXTENSIONS,
                new EntityRef("M04.ACTIVITY_SESSION", trip),
                new NewRecordEvent<>(
                        new TripKinds.Extension(trip, original, extended, "Kẹt xe"),
                        null,
                        user.accountId(),
                        false,
                        original.minusSeconds(600),
                        null,
                        original.minusSeconds(600),
                        data.floor,
                        null));
        var today = LocalDate.ofInstant(departure, BusinessTime.ZONE);
        var view = statistics
                .trips(floorOnly, today, today)
                .filter(v -> v.tripId() == trip)
                .findFirst()
                .orElseThrow();
        assertThat(view.plannedReturnAt()).isEqualTo(original);
        assertThat(view.extendedReturnAt()).containsExactly(extended);
        assertThat(view.actualReturnAt()).contains(finished);
        assertThat(view.missingOnReturn()).isEqualTo(1);
        // Buổi của chuyến cũng có trong số liệu buổi, đánh dấu là chuyến.
        assertThat(statistics.sessions(floorOnly, today, today).filter(v -> v.sessionId() == trip))
                .singleElement()
                .satisfies(v -> assertThat(v.trip()).isTrue());
    }

    @Test
    void FR_065_qualityChecksReportOnlyClosedItemsWithTheCheckerRole() {
        long shift = TestUsers.nextId();
        long checklist = TestUsers.nextId();
        Instant now = time.now();
        long passed = item(shift, checklist, QualityCheckKinds.PASS, QualityCheckKinds.FLOOR_MANAGER, now, true);
        long overdue = item(shift, checklist, QualityCheckKinds.OVERDUE, null, now, true);
        long pending = item(shift, checklist, QualityCheckKinds.PENDING, null, null, false);
        var today = time.today();
        var views = statistics
                .qualityChecks(floorOnly, today, today)
                .filter(v -> v.shiftId() == shift)
                .toList();
        assertThat(views).extracting(v -> v.itemId()).containsExactlyInAnyOrder(passed, overdue);
        assertThat(views).filteredOn(v -> v.itemId() == passed).singleElement().satisfies(v -> {
            assertThat(v.result()).isEqualTo("PASS");
            assertThat(v.checkerRole()).isEqualTo("FLOOR_MANAGER");
        });
        assertThat(views).noneMatch(v -> v.itemId() == pending);
        assertThat(statistics
                        .qualityChecks(
                                new ScopeFilter(false, Set.of(TestUsers.nextId()), Set.of(), Set.of()), today, today)
                        .filter(v -> v.shiftId() == shift))
                .isEmpty();
    }

    @Test
    void FR_058_isolationRisksAreEmptyUntilTheIsolationChainExists() {
        assertThat(statistics.isolationRisks()).isEmpty();
    }

    long item(long shift, long checklist, String status, String role, Instant at, boolean confirmed) {
        long task = TestUsers.nextId();
        return records.add(
                        QualityCheckKinds.ITEMS,
                        new EntityRef("M04.QUALITY_CHECKLIST", checklist),
                        new NewRecordEvent<>(
                                new QualityCheckKinds.Item(
                                        checklist,
                                        shift,
                                        data.floor,
                                        task,
                                        null,
                                        null,
                                        status,
                                        null,
                                        List.of(),
                                        role == null ? null : user.accountId(),
                                        role,
                                        at),
                                null,
                                user.accountId(),
                                false,
                                time.now(),
                                null,
                                confirmed ? time.now() : null,
                                data.floor,
                                "QC_TASK:" + task))
                .id();
    }
}
