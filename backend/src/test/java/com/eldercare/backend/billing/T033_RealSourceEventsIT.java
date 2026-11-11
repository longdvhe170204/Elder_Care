package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 010 T033 (chạy lại US1 với bản thật của 014, 2026-11-11): không có bản giả nguồn; Trưởng tầng điểm danh Có mặt ở hoạt
 * động có thu phí qua lệnh thật của 014, sau khi commit 010 ghi nguồn {@code ACTIVITY_ATTENDANCE} vào chỉ mục (listener sau
 * commit), Hành chính lập khoản nháp theo sự kiện, khoản trỏ về đúng nguồn, thành tiền theo phiên bản đơn giá của dịch vụ
 * trong danh mục 004. Người cao tuổi, giường, hợp đồng, phân công thật ({@link RealCareFixture}).
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(SwitchableUserConfig.class)
class T033_RealSourceEventsIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    com.eldercare.backend.common.time.TestClockControl clock;

    @AfterEach
    void restore() {
        user.reset();
        clock.reset();
    }

    @Test
    void US1_chargedActivityAttendanceFromReal014_isIndexed_thenDraftItemPointsToTheSource() throws Exception {
        var fixture = new RealCareFixture(jdbc, time);
        var place = fixture.place();
        long resident = fixture.residentInBed(place, time.now().minus(Duration.ofDays(20)));
        TestUser manager = fixture.staff(Role.FLOOR_MANAGER);
        TestUser office = fixture.users().staff(Role.OFFICE_STAFF);
        fixture.floorManager(manager.staffId(), place.floor(), time.now().minus(Duration.ofDays(30)));

        String serviceName = "Hoạt động có phí " + java.util.UUID.randomUUID();
        jdbc.update(
                "INSERT INTO catalog_item (catalog_type, module, name, status, attributes) VALUES ('M02.SERVICE', 'M02', ?,"
                        + " 'ACTIVE', ?)",
                serviceName,
                "{\"type\":\"ACTIVITY\",\"unit\":\"lần\",\"billingPeriod\":\"OCCURRENCE\",\"careLevelCode\":null,"
                        + "\"roomTypeId\":null,\"medicationId\":null}");
        long service = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M02.SERVICE' AND name=?", Long.class, serviceName);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from, effective_to, referenced)"
                        + " VALUES (?, 80000, '2000-01-01', NULL, FALSE)",
                service);
        long type = TestUsers.nextId(), activity = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.ACTIVITY_TYPE','M04',?,'ACTIVE',?)",
                type,
                "Nhóm " + type,
                "{\"groupActivity\":true,\"offSite\":false,\"interestGroupIds\":[]}");
        jdbc.update(
                "INSERT INTO simple_record(id,kind,module,status,content,created_at,created_by) VALUES(?,'M04.ACTIVITY','M04','ACTIVE',?,NOW(6),1)",
                activity,
                "{\"name\":\"Làm bánh có phí\",\"typeId\":" + type + ",\"scope\":\"FLOOR\",\"floorAreaId\":"
                        + place.floor()
                        + ",\"maxParticipants\":20,\"charged\":true,\"serviceId\":" + service
                        + ",\"groupActivity\":true,\"createdByStaffId\":" + manager.staffId() + "}");
        long session = TestUsers.nextId();
        var start = LocalDateTime.ofInstant(time.now().minus(Duration.ofMinutes(30)), BusinessTime.ZONE);
        jdbc.update(
                "INSERT INTO activity_session(id,activity_id,starts_at,ends_at,source,floor_area_id,responsible_staff_id,status,created_at,created_by)"
                        + " VALUES(?,?,?,?,'SINGLE',?,?,'SCHEDULED',NOW(6),1)",
                session,
                activity,
                start,
                start.plusHours(1),
                place.floor(),
                manager.staffId());
        jdbc.update(
                "INSERT INTO activity_registration(session_id,resident_id,source,registered_at,status) VALUES(?,?,'MANUAL',NOW(6),'REGISTERED')",
                session,
                resident);

        user.set(manager.user());
        mvc.perform(post("/api/v1/activity-sessions/" + session + "/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "attendance-" + TestUsers.nextId())
                        .content("{\"results\":[{\"residentId\":" + resident
                                + ",\"result\":\"PRESENT\",\"participationLevel\":\"ACTIVE\"}]}"))
                .andExpect(status().isOk());

        Map<String, Object> source = Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(
                        () -> jdbc.queryForList(
                                "SELECT source_id, service_id FROM cost_source_index"
                                        + " WHERE source_type='ACTIVITY_ATTENDANCE' AND resident_id=?",
                                resident),
                        rows -> rows.size() == 1)
                .getFirst();
        assertThat(((Number) source.get("service_id")).longValue()).isEqualTo(service);

        // Chỉ lập khoản cho ngày đã kết thúc: sang hôm sau.
        clock.setTo(
                time.today().plusDays(1).atTime(8, 0).atZone(BusinessTime.ZONE).toInstant());
        user.set(office.user());
        mvc.perform(post("/api/v1/costs/items/from-events")
                        .header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"residentId\":" + resident
                                + ",\"sourceType\":\"ACTIVITY_ATTENDANCE\",\"sourceIds\":[\"" + source.get("source_id")
                                + "\"]}"))
                .andExpect(status().isOk());
        Map<String, Object> item = jdbc.queryForMap(
                "SELECT origin, source_id, amount FROM cost_item WHERE resident_id=? AND source_type='ACTIVITY_ATTENDANCE'",
                resident);
        assertThat(item.get("origin")).isEqualTo("EVENT_BASED");
        assertThat(item.get("source_id")).isEqualTo(source.get("source_id"));
        assertThat(((Number) item.get("amount")).longValue())
                .as("đơn giá theo phiên bản của dịch vụ 004")
                .isEqualTo(80_000);
    }
}
