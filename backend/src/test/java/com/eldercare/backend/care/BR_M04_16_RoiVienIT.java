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
