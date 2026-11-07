package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import com.eldercare.backend.care.api.events.TripChanged;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 014 T019 (FR-031a, Q-170, Q-240): đính chính điểm danh chuyến chỉ do Trưởng tầng của chuyến, theo khung 000. Lượt này
 * có Hủy ghi nhận rời viện (b); đổi thời điểm (a) và hủy ghi nhận về (c) chờ 001 nên bị từ chối rõ ràng.
 */
@AutoConfigureMockMvc
@WithMockUser
@RecordApplicationEvents
@Import(FakeM04Config.class)
class FR_031a_DinhChinhChuyenIT extends StatusTestBase {
    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripStaffSchedule tripShifts;

    @Autowired
    com.eldercare.backend.support.fake.care.FakeTripMedicationPreparation tripMedication;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    ApplicationEvents recorded;

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
                        + ",\"maxParticipants\":10,\"charged\":true,\"serviceId\":" + TestUsers.nextId()
                        + ",\"groupActivity\":false}")));
        departure = time.now().plusSeconds(1800).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        plannedReturn = departure.plusSeconds(21600);
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

    /** Chuyến Đang đi, trưởng đoàn là Trưởng tầng của fixture, các người đã rời viện. */
    long departedTrip(long... residents) throws Exception {
        long s = id(mvc.perform(write(
                post("/api/v1/activity-sessions"),
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + departure + "\",\"endsAt\":\"" + plannedReturn
                        + "\",\"trip\":{\"destination\":\"Chùa\",\"plannedDepartureAt\":\"" + departure
                        + "\",\"plannedReturnAt\":\"" + plannedReturn
                        + "\",\"transport\":\"Xe thuê\",\"facilityVehicle\":false}}")));
        for (long r : residents) {
            jdbc.update(
                    "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status,participant_status)"
                            + " VALUES(?,?,'MANUAL',?,'REGISTERED','ELIGIBLE')",
                    s,
                    r,
                    LocalDateTime.ofInstant(time.now(), BusinessTime.ZONE));
            tripMedication.prepared(r, s);
        }
        tripShifts.shift(manager.staffId(), departure.minusSeconds(3600), departure.plusSeconds(28800));
        mvc.perform(write(
                        post("/api/v1/trips/" + s + "/assignment"),
                        "{\"leaderId\":" + manager.staffId() + ",\"companionIds\":[]}"))
                .andExpect(status().isOk());
        command(s, "depart", "{\"residentIds\":" + Arrays.toString(residents) + "}")
                .andExpect(status().isOk());
        return s;
    }

    ResultActions command(long s, String command, String body) throws Exception {
        return mvc.perform(write(post("/api/v1/trips/" + s + "/" + command), body));
    }

    long resident() {
        long r = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(manager.user());
        return r;
    }

    long attendance(long s, long resident, String type) {
        return jdbc.queryForObject(
                "SELECT id FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(content,'$.tripId'))=?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(content,'$.type'))=?",
                Long.class,
                resident,
                Long.toString(s),
                type);
    }

    void voidRecord(long record) {
        tx.executeWithoutResult(t -> corrections.create(
                "M04.TRIP_ATTENDANCE", record, CorrectionType.VOID, null, new Reason("Ghi đi nhầm, người ở lại viện")));
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

    @Test
    void FR_031a_voidingADepartureBringsTheResidentBackAndCancelsTheBillingSourceWhileTheTripContinues()
            throws Exception {
        long a = resident(), b = resident();
        long s = departedTrip(a, b);
        long record = attendance(s, a, "DEPARTURE");
        voidRecord(record);
        assertThat(currentStatus(a)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(currentStatus(b)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(participant(s, a)).isEqualTo("ELIGIBLE");
        assertThat(tripStatus(s)).isEqualTo("IN_PROGRESS");
        assertThat(recorded.stream(CostSourceChanged.class)
                        .filter(e -> e.sourceId().equals(Long.toString(record))))
                .singleElement()
                .satisfies(e -> assertThat(e.cancelled()).isTrue());
        assertThat(tripMedication.interval(a, s)).hasValueSatisfying(i -> {
            assertThat(i.from()).isEqualTo(departure);
            assertThat(i.to()).isEqualTo(plannedReturn);
        });
        // Bản gốc nhóm 3 không đổi.
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.type')) FROM record_event WHERE id=?",
                        String.class,
                        record))
                .isEqualTo("DEPARTURE");
    }

    @Test
    void FR_031a_voidingTheLastDepartureReturnsTheTripToScheduled() throws Exception {
        long a = resident();
        long s = departedTrip(a);
        voidRecord(attendance(s, a, "DEPARTURE"));
        assertThat(tripStatus(s)).isEqualTo("SCHEDULED");
        assertThat(participant(s, a)).isEqualTo("ELIGIBLE");
        assertThat(recorded.stream(TripChanged.class)
                        .filter(e -> e.tripId() == s && e.type().equals("LAST_DEPARTURE_UNDONE")))
                .hasSize(1);
        // Rời viện lại được sau khi chuyến quay về Đã lên lịch.
        command(s, "depart", "{\"residentIds\":[" + a + "]}").andExpect(status().isOk());
        assertThat(tripStatus(s)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void FR_031a_departureCannotBeVoidedAfterAReturnRecordOrOnceTheTripHasReturned() throws Exception {
        long a = resident(), c = resident();
        long s = departedTrip(a, c);
        command(s, "return", "{\"residentIds\":[" + a + "]}").andExpect(status().isOk());
        assertThat(failure(() -> voidRecord(attendance(s, a, "DEPARTURE"))).errorCode())
                .isEqualTo("STATE_CHANGED");
        command(s, "finish-return", "{}").andExpect(status().isOk());
        assertThat(failure(() -> voidRecord(attendance(s, c, "DEPARTURE"))).errorCode())
                .isEqualTo("STATE_CHANGED");
        assertThat(currentStatus(c)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
    }

    @Test
    void FR_031a_timeAmendmentAndReturnVoidingWaitFor001() throws Exception {
        long a = resident();
        long s = departedTrip(a);
        long record = attendance(s, a, "DEPARTURE");
        assertThat(failure(() -> tx.executeWithoutResult(t -> corrections.create(
                        "M04.TRIP_ATTENDANCE",
                        record,
                        CorrectionType.AMEND_CONTENT,
                        Map.of("at", time.now().toString()),
                        new Reason("Sai giờ")))))
                .extracting(BusinessException::errorCode)
                .isEqualTo("CORRECTION_NOT_SUPPORTED");
        command(s, "return", "{\"residentIds\":[" + a + "]}").andExpect(status().isOk());
        assertThat(failure(() -> voidRecord(attendance(s, a, "RETURN"))).errorCode())
                .isEqualTo("CARE_SOURCE_UNAVAILABLE");
        assertThat(currentStatus(a)).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }
}
