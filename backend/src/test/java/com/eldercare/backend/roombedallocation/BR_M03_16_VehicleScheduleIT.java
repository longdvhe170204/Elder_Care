package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T021 (US6 kịch bản 1, 2, 5, 7; FR-017, FR-018, FR-020, BR-M03-16, DBR-32, SC-005, Q-298): đặt lịch xe không chồng
 * giờ dưới khóa xe; xe Hỏng, Đang bảo trì, Đã thanh lý hoặc quá hạn đăng kiểm, bảo hiểm tại ngày đi bị chặn; người đặt,
 * Quản lý viện ghi xuất phát, trả xe, dời, hủy; dời, hủy báo người đặt, Phụ trách bộ phận quản lý xe, người lái.
 */
class BR_M03_16_VehicleScheduleIT extends VehicleTestBase {

    @Test
    void US6_1_overlappingScheduleOfTheSameVehicleIsBlocked() throws Exception {
        booked(booking(vehicle, SEVEN, THIRTEEN, "TRIP", TestUsers.nextId()));
        book(booking(vehicle, TEN, ELEVEN, "MEDICAL_VISIT", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_SCHEDULE_OVERLAP"));
        booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
        book(booking(vehicle(null, null), TEN, ELEVEN, "MEDICAL_VISIT", null)).andExpect(status().isCreated());
        book(booking(vehicle, ELEVEN, TEN, "OTHER", null)).andExpect(status().isUnprocessableEntity());
        // chuyến đi cần tripId
        book(booking(vehicle, TEN, ELEVEN, "TRIP", null)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void US6_2_unavailableOrOverdueVehicleIsBlocked() throws Exception {
        book(booking(vehicle(null, "UNDER_MAINTENANCE"), TEN, ELEVEN, "OTHER", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_NOT_AVAILABLE"));
        book(booking(vehicle(LocalDate.of(2099, 1, 4), null), TEN, ELEVEN, "OTHER", null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_INSPECTION_OR_INSURANCE_OVERDUE"));
        as(medicalLead);
        long notVehicle = created(donated("Tặng"));
        as(office);
        book(booking(notVehicle, TEN, ELEVEN, "OTHER", null)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_020_floorManagerOfficeAndManagerBookOthersCannot() throws Exception {
        as(floorManager);
        booked(booking(vehicle, SEVEN, TEN, "OTHER", null));
        as(manager);
        booked(booking(vehicle, TEN, ELEVEN, "OTHER", null));
        as(medicalLead);
        book(booking(vehicle, ELEVEN, THIRTEEN, "OTHER", null)).andExpect(status().isForbidden());
    }

    @Test
    void US6_5_bookerRecordsDepartureAndReturnWithActualTimes() throws Exception {
        long id = booked(booking(vehicle, TEN, ELEVEN, "MEDICAL_VISIT", null));
        as(otherOffice);
        schedule(id, "record-departure", "{}").andExpect(status().isForbidden());
        as(office);
        schedule(id, "record-return", "{}").andExpect(status().isConflict());
        schedule(id, "record-departure", "{\"occurredAt\":\"2099-01-05T03:05:00Z\"}")
                .andExpect(status().isOk());
        assertThat(scheduleStatus(id)).isEqualTo("IN_USE");
        as(manager);
        schedule(id, "record-return", "{}").andExpect(status().isOk());
        assertThat(scheduleStatus(id)).isEqualTo("COMPLETED");
        as(office);
        mvc.perform(get(SCHEDULES).param("vehicleId", Long.toString(vehicle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].departedAt").value("2099-01-05T03:05:00Z"))
                .andExpect(jsonPath("$[0].returnedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].bookedByStaffId").value(office.staffId()))
                .andExpect(jsonPath("$[0].driverStaffId").value(driver.staffId()));
        mvc.perform(get(SCHEDULES)
                        .param("vehicleId", Long.toString(vehicle))
                        .param("from", THIRTEEN.toString())
                        .param("to", FOURTEEN.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void US6_7_rescheduleAndCancelNotifyBookerVehicleDepartmentLeadAndDriver() throws Exception {
        long id = booked(booking(vehicle, TEN, ELEVEN, "OTHER", null));
        long other = booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
        as(manager);
        schedule(
                        id,
                        "reschedule",
                        "{\"plannedDepartureAt\":\"" + ELEVEN + "\",\"plannedReturnAt\":\"" + FOURTEEN + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("VEHICLE_SCHEDULE_OVERLAP"));
        schedule(id, "reschedule", "{\"plannedDepartureAt\":\"" + SEVEN + "\",\"plannedReturnAt\":\"" + TEN + "\"}")
                .andExpect(status().isOk());
        assertThat(staffNotified(office.staffId(), "VEHICLE_SCHEDULE_CHANGED")).isTrue();
        assertThat(staffNotified(driver.staffId(), "VEHICLE_SCHEDULE_CHANGED")).isTrue();
        assertThat(noticesTo(new RecipientGroup.DepartmentLead(fleet, new Substitution.Default())))
                .isNotEmpty();
        as(office);
        schedule(other, "cancel", "{}").andExpect(status().isUnprocessableEntity());
        schedule(other, "cancel", "{\"reason\":\"Đổi kế hoạch\"}").andExpect(status().isOk());
        assertThat(scheduleStatus(other)).isEqualTo("CANCELLED");
        assertThat(staffNotified(driver.staffId(), "VEHICLE_SCHEDULE_CANCELLED"))
                .isTrue();
        schedule(other, "cancel", "{\"reason\":\"Lần nữa\"}").andExpect(status().isConflict());
        booked(booking(vehicle, THIRTEEN, FOURTEEN, "OTHER", null));
    }

    @org.springframework.beans.factory.annotation.Autowired
    List<com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider> scopes;

    @org.springframework.beans.factory.annotation.Autowired
    List<com.eldercare.backend.administration.retention.api.spi.RetentionProvider> retentions;

    @Test
    void T004_vehicleScheduleIsFacilityWideAndHasARetentionProvider() throws Exception {
        long id = booked(booking(vehicle, TEN, ELEVEN, "OTHER", null));
        var scope = scopes.stream()
                .filter(p -> p.entityType().equals("M03.VEHICLE_SCHEDULE"))
                .findFirst()
                .orElseThrow();
        assertThat(scope.assignScope(id).floorAreaId()).isNull();
        assertThat(retentions).anyMatch(p -> p.dataType().equals("M03.VEHICLE_SCHEDULE"));
    }

    /** SC-005: hai lịch cùng giờ đặt đồng thời, đúng một lịch được ghi (khóa xe). Dữ liệu commit thật, id riêng. */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void SC_005_twoConcurrentBookingsOfTheSameSlotLeaveExactlyOne() throws Exception {
        as(manager);
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++)
                results.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(
                                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(SCHEDULES)
                                            .with(org.springframework.security.test.web.servlet.request
                                                    .SecurityMockMvcRequestPostProcessors.user("sc005"))
                                            .header("Idempotency-Key", "vs-" + TestUsers.nextId())
                                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                            .content(booking(vehicle, TEN, ELEVEN, "OTHER", null)))
                            .andReturn()
                            .getResponse()
                            .getStatus();
                }));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (var r : results) statuses.add(r.get());
            Collections.sort(statuses);
            assertThat(statuses).containsExactly(201, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind='M03.VEHICLE_SCHEDULE' AND entity_type='M03.ASSET' AND entity_id=?",
                        Integer.class,
                        vehicle))
                .isEqualTo(1);
    }
}
