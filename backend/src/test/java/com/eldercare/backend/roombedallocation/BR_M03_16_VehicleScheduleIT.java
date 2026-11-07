package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.support.TestUsers;
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

    @org.springframework.beans.factory.annotation.Autowired
    List<com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider> scopes;

    @org.springframework.beans.factory.annotation.Autowired
    List<com.eldercare.backend.administration.retention.api.spi.RetentionProvider> retentions;

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
