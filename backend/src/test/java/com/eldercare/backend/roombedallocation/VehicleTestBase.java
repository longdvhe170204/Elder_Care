package com.eldercare.backend.roombedallocation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 019 Phase 9 (US6): nền test lịch xe. Xe XE thuộc bộ phận quản lý xe {@code fleet} (Phụ trách {@code fleetLead}); Hành
 * chính {@code office}, {@code otherOffice}; người lái {@code driver}. Giờ ở ngày 2099-01-05 để không chạm dữ liệu khác.
 */
abstract class VehicleTestBase extends AssetTestBase {
    static final String SCHEDULES = "/api/v1/vehicle-schedules";
    static final Instant SEVEN = Instant.parse("2099-01-05T00:00:00Z");
    static final Instant TEN = Instant.parse("2099-01-05T03:00:00Z");
    static final Instant ELEVEN = Instant.parse("2099-01-05T04:00:00Z");
    static final Instant THIRTEEN = Instant.parse("2099-01-05T06:00:00Z");
    static final Instant FOURTEEN = Instant.parse("2099-01-05T07:00:00Z");

    long fleet, vehicle, vehicleType;
    TestUsers.TestUser fleetLead, office, otherOffice, driver;

    @BeforeEach
    void vehicleFixture() {
        var users = new TestUsers(jdbc, time);
        fleet = TestUsers.nextId();
        fleetLead = users.staff(Role.ACCOUNTANT);
        office = users.staff(Role.OFFICE_STAFF);
        otherOffice = users.staff(Role.OFFICE_STAFF);
        driver = users.staff(Role.CAREGIVER);
        duties.departmentLead(fleet, fleetLead.staffId());
        vehicleType = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M03.ASSET_TYPE' AND code='VEHICLE'", Long.class);
        vehicle = vehicle(null, null);
        as(office);
    }

    long vehicle(LocalDate inspectionDue, String status) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO asset(id,code,type_id,name,department_id,source,source_description,status,location_type,"
                        + "vehicle_inspection_due_on,insurance_due_on,created_at)"
                        + " VALUES(?,?,?,?,?,'DONATED','Tặng',?,'PARKING_LOT',?,?,?)",
                id,
                "XE-" + id,
                vehicleType,
                "Xe 16 chỗ",
                fleet,
                status == null ? "READY" : status,
                inspectionDue,
                null,
                LocalDateTime.now());
        return id;
    }

    String booking(long vehicleId, Instant from, Instant to, String purpose, Long tripId) {
        return "{\"vehicleId\":" + vehicleId + ",\"plannedDepartureAt\":\"" + from + "\",\"plannedReturnAt\":\"" + to
                + "\",\"purpose\":\"" + purpose + "\""
                + (tripId == null ? ",\"residentId\":" + TestUsers.nextId() : ",\"tripId\":" + tripId)
                + ",\"driverStaffId\":" + driver.staffId() + "}";
    }

    ResultActions book(String body) throws Exception {
        return mvc.perform(post(SCHEDULES)
                .header("Idempotency-Key", "vs-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    long booked(String body) throws Exception {
        String location = book(body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    ResultActions schedule(long id, String command, String body) throws Exception {
        return mvc.perform(post(SCHEDULES + "/" + id + "/" + command)
                .header("Idempotency-Key", "vc-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    String scheduleStatus(long id) {
        return jdbc.queryForObject(
                "SELECT status FROM simple_record WHERE kind='M03.VEHICLE_SCHEDULE' AND id=?", String.class, id);
    }

    boolean staffNotified(long staffId, String event) {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.eventType().equals(event))
                .anyMatch(n -> n.recipientGroups().stream()
                        .anyMatch(g -> g instanceof RecipientGroup.Staff s && s.staffId() == staffId));
    }

    List<NotificationRequest> noticesTo(RecipientGroup group) {
        return events.stream(HasNotificationRequests.class)
                .flatMap(e -> e.notificationRequests().stream())
                .filter(n -> n.recipientGroups().contains(group))
                .toList();
    }
}
