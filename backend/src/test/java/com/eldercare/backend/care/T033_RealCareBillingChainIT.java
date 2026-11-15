package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.RealCareFixture;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/**
 * 005 T033, T036 (FR-051; quyết định 05/11 {@code specs/010-…/care-quantity-decision-2026-11-05.md}): nghiệm thu toàn chuỗi
 * 005 → 010 với bean thật, không có bản giả nguồn. Người được giao ghi vật phẩm 2 cái ở công việc có tính phí, sau commit
 * 010 ghi nguồn {@code CARE_TASK_RESULT} vào chỉ mục; Hành chính lập khoản theo sự kiện, thành tiền theo đơn giá của dịch vụ
 * trong danh mục 004; đính chính nội dung (tổng mới 3) tính lại khoản chưa chốt; Hủy ghi nhận hủy nguồn và khoản. Hồ sơ,
 * giường, hợp đồng, ca, phân công thật ({@link RealCareFixture}); mẫu {@code billing.T033_RealSourceEventsIT}.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(SwitchableUserConfig.class)
class T033_RealCareBillingChainIT extends MySqlIntegrationTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    SwitchableUser user;

    @Autowired
    JsonMapper json;

    @Autowired
    com.eldercare.backend.care.internal.task.service.CareTaskClockProcessor clockProcessor;

    @AfterEach
    void restore() {
        clock.reset();
        user.reset();
    }

    private ResultActions send(String path, String body) throws Exception {
        return mvc.perform(post(path)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Map<String, Object> source(long task) {
        return jdbc.queryForMap(
                "SELECT current_quantity, cancelled, service_id FROM cost_source_index"
                        + " WHERE source_type='CARE_TASK_RESULT' AND source_id=?",
                Long.toString(task));
    }

    private Map<String, Object> item(long resident) {
        return jdbc.queryForMap(
                "SELECT status, quantity, amount, source_id FROM cost_item WHERE resident_id=?"
                        + " AND source_type='CARE_TASK_RESULT'",
                resident);
    }

    @Test
    void FR_051_itemResultFromReal005_isIndexedPricedAmendedAndVoidedIn010() throws Exception {
        clock.setTo(time.today().atTime(8, 0).atZone(BusinessTime.ZONE).toInstant());
        Instant now = time.now();
        var fixture = new RealCareFixture(jdbc, time);
        var place = fixture.place();
        long resident = fixture.residentInBed(place, now.minus(Duration.ofDays(10)));
        TestUser manager = fixture.staff(Role.FLOOR_MANAGER);
        TestUser caregiver = fixture.staff(Role.CAREGIVER);
        TestUser office = fixture.users().staff(Role.OFFICE_STAFF);
        fixture.floorManager(manager.staffId(), place.floor(), now.minus(Duration.ofDays(30)));
        long shift = fixture.shift(place.floor(), now.minus(Duration.ofHours(1)), 8);
        fixture.member(shift, caregiver.staffId());
        fixture.lead(shift, manager.staffId(), now.minus(Duration.ofHours(1)));
        fixture.assignResident(
                shift,
                caregiver.staffId(),
                Role.CAREGIVER,
                resident,
                now.minus(Duration.ofHours(1)),
                now.plusSeconds(25200));

        String serviceName = "Tã người lớn " + UUID.randomUUID();
        jdbc.update(
                "INSERT INTO catalog_item (catalog_type, module, name, status, attributes) VALUES ('M02.SERVICE', 'M02', ?,"
                        + " 'ACTIVE', ?)",
                serviceName,
                "{\"type\":\"CONSUMABLE\",\"unit\":\"cái\",\"billingPeriod\":\"UNIT\",\"careLevelCode\":null,"
                        + "\"roomTypeId\":null,\"medicationId\":null}");
        long service = jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M02.SERVICE' AND name=?", Long.class, serviceName);
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from, effective_to, referenced)"
                        + " VALUES (?, 15000, '2000-01-01', NULL, FALSE)",
                service);
        long type = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                type,
                "Thay tã có phí " + type,
                "{\"group\":\"DIAPER_ITEM_CHANGE\",\"professional\":false,\"resultType\":\"ITEM_QUANTITY\",\"defaultRole\":\"CAREGIVER\","
                        + "\"defaultImportance\":\"NORMAL\",\"billable\":true,\"serviceId\":" + service
                        + ",\"certificates\":[]}");

        Instant planned = now.plus(Duration.ofMinutes(30));
        user.set(manager.user());
        String body = send(
                        "/api/v1/shifts/" + shift + "/care-tasks/plan",
                        "[{\"residentId\":" + resident + ",\"taskTypeId\":" + type
                                + ",\"basis\":{\"type\":\"AD_HOC\"},\"content\":\"Thay tã\",\"plannedAt\":\"" + planned
                                + "\",\"windowBeforeMinutes\":15,\"windowAfterMinutes\":15,\"importance\":\"NORMAL\","
                                + "\"performerId\":" + caregiver.staffId() + ",\"shared\":false}]")
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long task = json.readTree(body).get(0).get("id").asLong();
        clock.setTo(planned);
        clockProcessor.advance(task, planned);
        user.set(caregiver.user());
        send("/api/v1/care-tasks/" + task + "/record", "{\"numericValue\":2}").andExpect(status().isOk());

        // Listener 010 chạy sau commit của lệnh 005
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(
                        () -> jdbc.queryForObject(
                                "SELECT COUNT(*) FROM cost_source_index WHERE source_type='CARE_TASK_RESULT' AND source_id=?",
                                Integer.class,
                                Long.toString(task)),
                        n -> n == 1);
        assertThat((BigDecimal) source(task).get("current_quantity")).isEqualByComparingTo("2");
        assertThat(((Number) source(task).get("service_id")).longValue()).isEqualTo(service);

        // Chỉ lập khoản cho ngày đã kết thúc: sang hôm sau, Hành chính lập bằng lệnh
        clock.setTo(
                time.today().plusDays(1).atTime(8, 0).atZone(BusinessTime.ZONE).toInstant());
        user.set(office.user());
        send(
                        "/api/v1/costs/items/from-events",
                        "{\"residentId\":" + resident + ",\"sourceType\":\"CARE_TASK_RESULT\",\"sourceIds\":[\"" + task
                                + "\"]}")
                .andExpect(status().isOk());
        assertThat(item(resident).get("source_id")).isEqualTo(Long.toString(task));
        assertThat(((Number) item(resident).get("amount")).longValue())
                .as("2 cái × đơn giá 15.000 của dịch vụ 004")
                .isEqualTo(30_000);

        // Đính chính nội dung: tổng mới 3, khoản chưa chốt tính lại
        user.set(manager.user());
        send(
                        "/api/v1/care-results/" + task + "/correct",
                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"numericValue\":3},\"reason\":\"Ghi nhầm số tã\"}")
                .andExpect(status().isCreated());
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(() -> ((Number) item(resident).get("amount")).longValue(), amount -> amount == 45_000);
        assertThat((BigDecimal) source(task).get("current_quantity")).isEqualByComparingTo("3");

        // Hủy ghi nhận: nguồn bị hủy, khoản chưa chốt hủy theo nguồn
        send("/api/v1/care-results/" + task + "/correct", "{\"type\":\"VOID\",\"reason\":\"Ghi nhầm người\"}")
                .andExpect(status().isCreated());
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .until(() -> item(resident).get("status"), "CANCELLED"::equals);
        assertThat(source(task).get("cancelled")).isEqualTo(Boolean.TRUE);
    }
}
