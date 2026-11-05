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
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.StatusTestBase;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
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
 * 014 T019 (US5, FR-043 → FR-050, BR-M04-17): HTTP thật, trạng thái 001 thật. Hai lệnh ghi sự cố chờ port 007 nên chỉ kiểm
 * 503; đính chính FR-031a làm ở lượt sau.
 */
@AutoConfigureMockMvc
@WithMockUser
@RecordApplicationEvents
@Import({FakeM04Config.class, com.eldercare.backend.support.fake.roombedallocation.FakeVehicleSchedules.Config.class})
class BR_M04_17_ThieuNguoiIT extends StatusTestBase {
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

    @Test
    void FR_044_returnMovesTheResidentBackAndRecalculatesCarriedMedicationWhileTheTripContinues() throws Exception {
        long s = trip(), a = eligible(s), b = eligible(s);
        assign(s);
        departed(s, a, b);
        command(s, "return", "{\"residentIds\":[" + a + "]}").andExpect(status().isOk());
        assertThat(currentStatus(a)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(currentStatus(b)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
        assertThat(participant(s, a)).isEqualTo("RETURNED");
        assertThat(tripStatus(s)).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_ATTENDANCE' AND entity_id=?"
                                + " AND JSON_UNQUOTE(JSON_EXTRACT(content,'$.type'))='RETURN'",
                        Integer.class,
                        a))
                .isEqualTo(1);
        // Khoảng mang thuốc kết thúc ở giờ về thực tế, sớm hơn giờ về dự kiến.
        assertThat(tripMedication.interval(a, s)).hasValueSatisfying(i -> {
            assertThat(i.to()).isAfterOrEqualTo(i.from()).isBeforeOrEqualTo(time.now());
            // Đầu khoảng là giờ rời thực tế (record_event lưu UTC), không lệch múi giờ.
            assertThat(i.from()).isBetween(time.now().minusSeconds(60), time.now());
            assertThat(i.to()).isBefore(plannedReturn);
        });
        command(s, "return", "{\"residentIds\":[" + a + "]}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
    }

    @Test
    void FR_046a_returnFromAnOfflineDeviceIsBlockedUntil001SupportsDeviceTime() throws Exception {
        long s = trip(), a = eligible(s);
        assign(s);
        departed(s, a);
        command(s, "return", "{\"residentIds\":[" + a + "],\"deviceAt\":\"" + time.now() + "\"}")
                .andExpect(status().isServiceUnavailable());
        assertThat(currentStatus(a)).isEqualTo(ResidentStatus.OFF_SITE_ACTIVITY);
    }

    @Test
    void FR_043_extensionNeedsAReasonMovesTheMilestoneAndNotifiesMedicationAndMeals() throws Exception {
        long s = trip(), a = eligible(s);
        assign(s);
        departed(s, a);
        Instant later = plannedReturn.plusSeconds(3600);
        command(s, "extend", "{\"newPlannedReturnAt\":\"" + later + "\"}").andExpect(status().isUnprocessableEntity());
        command(
                        s,
                        "extend",
                        "{\"newPlannedReturnAt\":\"" + plannedReturn.minusSeconds(60) + "\",\"reason\":\"Kẹt xe\"}")
                .andExpect(status().isUnprocessableEntity());
        command(s, "extend", "{\"newPlannedReturnAt\":\"" + later + "\",\"reason\":\"Kẹt xe\"}")
                .andExpect(status().isOk());
        // FR-069, DBR-23: lệnh ghi nhật ký với người thực hiện, thời điểm, lý do.
        assertThat(jdbc.queryForMap(
                        "SELECT actor_id,reason,occurred_at FROM audit_log WHERE action=? AND entity_type=? AND entity_id=?"
                                + " ORDER BY id DESC LIMIT 1",
                        "TRIP_RETURN_EXTENDED",
                        "M04.TRIP",
                        Long.toString(s)))
                .containsEntry("actor_id", manager.accountId())
                .containsEntry("reason", "Kẹt xe")
                .containsKey("occurred_at");
        assertThat(jdbc.queryForObject(
                        "SELECT trip_planned_return_at FROM activity_session WHERE id=?", LocalDateTime.class, s))
                .isEqualTo(LocalDateTime.ofInstant(later, BusinessTime.ZONE));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind='M04.TRIP_EXTENSION'"
                                + " AND JSON_UNQUOTE(JSON_EXTRACT(content,'$.tripId'))=?",
                        Integer.class,
                        Long.toString(s)))
                .isEqualTo(1);
        assertThat(tripMedication.interval(a, s))
                .hasValueSatisfying(i -> assertThat(i.to()).isEqualTo(later));
        assertThat(recorded.stream(TripChanged.class)
                        .filter(e -> e.tripId() == s && e.type().equals("EXTENDED")))
                .singleElement()
                .satisfies(e -> assertThat(e.plannedReturnAt()).isEqualTo(later));
        // Mốc cũ không còn báo; mốc mới báo một lần.
        Instant cfg = later.plusSeconds(1860);
        overdue.run(plannedReturn.plusSeconds(1860));
        assertThat(notices(s, "RETURN_OVERDUE")).isEmpty();
        overdue.run(cfg);
        overdue.run(cfg.plusSeconds(60));
        assertThat(notices(s, "RETURN_OVERDUE")).hasSize(1);
    }

    @Test
    void FR_049_overdueReturnNotifiesLeaderAndFloorOnceThenTheManagerAtTwiceTheThreshold() throws Exception {
        long s = trip(), a = eligible(s);
        assign(s);
        departed(s, a);
        overdue.run(plannedReturn.plusSeconds(1740));
        assertThat(notices(s, "RETURN_OVERDUE")).isEmpty();
        overdue.run(plannedReturn.plusSeconds(1860));
        overdue.run(plannedReturn.plusSeconds(1920));
        assertThat(notices(s, "RETURN_OVERDUE"))
                .singleElement()
                .satisfies(n ->
                        assertThat(n.context().recipients()).noneMatch(r -> r instanceof RecipientGroup.FacilityRole));
        assertThat(jdbc.queryForObject("SELECT trip_overdue_return FROM activity_session WHERE id=?", Boolean.class, s))
                .isTrue();
        assertThat(participant(s, a)).isEqualTo("DEPARTED");
        assertThat(notices(s, "RETURN_OVERDUE_MANAGER")).isEmpty();
        overdue.run(plannedReturn.plusSeconds(3660));
        overdue.run(plannedReturn.plusSeconds(3720));
        assertThat(notices(s, "RETURN_OVERDUE_MANAGER"))
                .singleElement()
                .satisfies(n -> assertThat(n.context().recipients())
                        .contains(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));
    }

    @Test
    void FR_047_finishingReturnMarksAbsentResidentsMissingAndRequestsAnUrgentAlertWithoutAnIncident() throws Exception {
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

    @Test
    void FR_045_residentTransferredToHospitalDuringTheTripLeavesTheGroupAndIsNotCountedMissing() throws Exception {
        long s = trip(), b = eligible(s);
        assign(s);
        departed(s, b);
        tx.executeWithoutResult(t -> publisher.publishEvent(new ResidentStatusChanged(
                b,
                ResidentStatus.OFF_SITE_ACTIVITY,
                ResidentStatus.HOSPITALIZED,
                StatusCommand.TRANSFER_OUT,
                TestUsers.nextId(),
                time.now())));
        assertThat(participant(s, b)).isEqualTo("LEFT_GROUP");
        command(s, "finish-return", "{}").andExpect(status().isOk());
        assertThat(participant(s, b)).isEqualTo("LEFT_GROUP");
        assertThat(recorded.stream(AlertRequested.class).filter(e -> e.residentId() == b))
                .isEmpty();
    }

    @Test
    void FR_035a_transferLeaderOnlyToACompanionWithALeaderRoleAndKeepsTheHistory() throws Exception {
        var nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF);
        var office = new TestUsers(jdbc, time).staff(Role.OFFICE_STAFF);
        user.set(manager.user());
        long s = trip(), a = eligible(s);
        assign(s, nurse.staffId(), office.staffId());
        departed(s, a);
        command(s, "transfer-leader", "{\"newLeaderId\":" + nurse.staffId() + "}")
                .andExpect(status().isUnprocessableEntity());
        command(s, "transfer-leader", "{\"newLeaderId\":" + office.staffId() + ",\"reason\":\"Đưa B đi viện\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ROLE_NOT_ELIGIBLE"));
        command(s, "transfer-leader", "{\"newLeaderId\":" + TestUsers.nextId() + ",\"reason\":\"Đưa B đi viện\"}")
                .andExpect(status().isConflict());
        command(s, "transfer-leader", "{\"newLeaderId\":" + nurse.staffId() + ",\"reason\":\"Đưa B đi viện\"}")
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/trips/" + s))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leaderId").value(nurse.staffId()))
                .andExpect(jsonPath("$.companionIds.length()").value(2));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM status_history WHERE entity_type='M04.ACTIVITY_SESSION' AND entity_id=?"
                                + " AND attribute='TRIP_DUTIES'",
                        Integer.class,
                        s))
                .isEqualTo(2);
    }

    @Test
    void FR_046_incidentCommandsWaitForThe007PortAndChangeNothing() throws Exception {
        long s = trip(), c = eligible(s);
        assign(s);
        departed(s, c);
        command(
                        s,
                        "report-missing",
                        "{\"residentIds\":[" + c + "],\"lastSeenPlace\":\"Cổng chùa\",\"lastSeenAt\":\"" + time.now()
                                + "\",\"description\":\"Không thấy\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("CARE_SOURCE_UNAVAILABLE"));
        command(s, "record-missing-incident", "{\"residentIds\":[" + c + "]}")
                .andExpect(status().isServiceUnavailable());
        assertThat(participant(s, c)).isEqualTo("DEPARTED");
    }
}
