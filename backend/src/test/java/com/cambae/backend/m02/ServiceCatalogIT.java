package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T066 (US9; FR-034, Q-297, BR-M15-06): danh mục dịch vụ. Chỉ Quản lý viện (AC-01) tạo, sửa, ngừng hiệu lực; Hành chính
 * chỉ xem. Mỗi dịch vụ chăm sóc gắn đúng một mức và mỗi mức tối đa một dịch vụ Hiệu lực; tên Hiệu lực không trùng; loại và
 * các liên kết không đổi sau khi tạo.
 */
@AutoConfigureMockMvc
@WithMockUser
class ServiceCatalogIT extends ContractTestBase {

    private ObjectNode input(String type, String name) {
        ObjectNode body = JSON.createObjectNode();
        body.put("type", type);
        body.put("name", name + " " + UUID.randomUUID());
        body.put("unit", "lần");
        body.put("billingPeriod", "OCCURRENCE");
        return body;
    }

    private ResultActions postService(ObjectNode body) throws Exception {
        return mvc.perform(post("/api/v1/services")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()));
    }

    private JsonNode createdService(ObjectNode body) throws Exception {
        user.set(manager.user());
        return JSON.readTree(postService(body)
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    private ResultActions update(long id, String ifMatch, ObjectNode body) throws Exception {
        var request = put("/api/v1/services/" + id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString());
        return mvc.perform(ifMatch == null ? request : request.header("If-Match", ifMatch));
    }

    private ResultActions deactivate(long id, String ifMatch) throws Exception {
        return mvc.perform(post("/api/v1/services/" + id + "/deactivate")
                .with(csrf())
                .header("If-Match", ifMatch)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Không còn cung cấp\"}"));
    }

    @Test
    void FR_034_onlyTheManagerWrites_officeCanOnlyRead() throws Exception {
        user.set(office.user());
        postService(input("ACTIVITY", "Sinh hoạt nhóm")).andExpect(status().isForbidden());

        JsonNode service = createdService(input("ACTIVITY", "Sinh hoạt nhóm"));
        assertThat(service.get("status").asString()).isEqualTo("ACTIVE");

        user.set(office.user());
        mvc.perform(get("/api/v1/services").param("type", "ACTIVITY"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[?(@.id == %d)]".formatted(service.get("id").asLong()))
                                .exists());
        user.set(office.user());
        update(service.get("id").asLong(), "\"0\"", input("ACTIVITY", "Đổi tên"))
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_034_listFiltersByType() throws Exception {
        JsonNode rehab = createdService(input("REHABILITATION", "Vật lý trị liệu"));
        createdService(input("INJECTION", "Tiêm bắp"));

        mvc.perform(get("/api/v1/services").param("type", "REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type != 'REHABILITATION')]").isEmpty())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(rehab.get("id").asLong()))
                        .exists());
    }

    @Test
    void FR_034_typeSpecificLinks_areValidated() throws Exception {
        user.set(manager.user());
        postService(input("CARE", "Chăm sóc thiếu mức")).andExpect(status().isUnprocessableEntity());

        ObjectNode mealWithLevel = input("MEAL", "Suất ăn có mức");
        mealWithLevel.put("careLevelCode", "BASIC");
        postService(mealWithLevel).andExpect(status().isUnprocessableEntity());

        postService(input("RESIDENCY_FEE", "Phí phòng thiếu loại phòng")).andExpect(status().isUnprocessableEntity());
        ObjectNode fee = input("RESIDENCY_FEE", "Phí phòng đôi");
        fee.put("roomTypeId", 7);
        postService(fee)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomTypeId").value(7));

        postService(input("FACILITY_SUPPLIED_MEDICATION", "Thuốc viện cấp thiếu mã thuốc"))
                .andExpect(status().isUnprocessableEntity());
        ObjectNode medication = input("FACILITY_SUPPLIED_MEDICATION", "Paracetamol");
        medication.put("medicationId", 11);
        postService(medication).andExpect(status().isCreated());
    }

    @Test
    void BR_M15_06_activeNamesAreUnique_inactiveNameCanBeReused() throws Exception {
        ObjectNode first = input("ACTIVITY", "Tập dưỡng sinh");
        JsonNode service = createdService(first);

        postService(first)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_NAME_EXISTS"));

        deactivate(service.get("id").asLong(), "\"" + service.get("version").asLong() + "\"")
                .andExpect(status().isOk());
        postService(first).andExpect(status().isCreated());
    }

    @Test
    void Q_297_oneActiveCareServicePerLevel() throws Exception {
        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE type = 'CARE'");
        ObjectNode basic = input("CARE", "Chăm sóc cơ bản");
        basic.put("billingPeriod", "MONTH");
        basic.put("careLevelCode", "BASIC");
        JsonNode first = createdService(basic);

        ObjectNode again = input("CARE", "Chăm sóc cơ bản khác");
        again.put("billingPeriod", "MONTH");
        again.put("careLevelCode", "BASIC");
        postService(again)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CARE_SERVICE_LEVEL_EXISTS"));

        ObjectNode special = input("CARE", "Chăm sóc đặc biệt");
        special.put("billingPeriod", "MONTH");
        special.put("careLevelCode", "SPECIAL");
        postService(special).andExpect(status().isCreated());

        deactivate(first.get("id").asLong(), "\"" + first.get("version").asLong() + "\"")
                .andExpect(status().isOk());
        postService(again).andExpect(status().isCreated());
    }

    @Test
    void FR_034_update_checksIfMatch_andLocksTypeAndLinks() throws Exception {
        ObjectNode body = input("ACTIVITY", "Âm nhạc trị liệu");
        JsonNode service = createdService(body);
        long id = service.get("id").asLong();
        String version = "\"" + service.get("version").asLong() + "\"";

        ObjectNode renamed = body.deepCopy();
        renamed.put("name", "Âm nhạc " + UUID.randomUUID());
        renamed.put("unit", "buổi");
        renamed.put("billingPeriod", "SESSION");
        String next = update(id, version, renamed)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        assertThat(next).isNotEqualTo(version);
        mvc.perform(get("/api/v1/services").param("type", "ACTIVITY"))
                .andExpect(jsonPath("$[?(@.id == %d)].unit".formatted(id)).value("buổi"));

        update(id, version, renamed)
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.errorCode").value("VERSION_MISMATCH"));
        update(id, null, renamed).andExpect(status().isPreconditionFailed());

        ObjectNode otherType = renamed.deepCopy();
        otherType.put("type", "MEAL");
        update(id, next, otherType)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_FIELD_LOCKED"));
        update(99_999_999L, next, renamed).andExpect(status().isNotFound());
    }

    @Test
    void FR_034_billingPeriodIsLockedOnceAPriceVersionIsReferenced() throws Exception {
        ObjectNode body = input("ACTIVITY", "Du ngoạn");
        JsonNode service = createdService(body);
        long id = service.get("id").asLong();
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from, referenced)"
                        + " VALUES (?, 100000, '2020-01-01', TRUE)",
                id);

        ObjectNode changed = body.deepCopy();
        changed.put("billingPeriod", "DAY");
        update(id, "\"" + service.get("version").asLong() + "\"", changed)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_FIELD_LOCKED"));
    }

    @Test
    void FR_034_deactivate_requiresReason_isOnce_andIsAudited() throws Exception {
        JsonNode service = createdService(input("ACTIVITY", "Câu lạc bộ cờ"));
        long id = service.get("id").asLong();
        String version = "\"" + service.get("version").asLong() + "\"";

        mvc.perform(post("/api/v1/services/" + id + "/deactivate")
                        .with(csrf())
                        .header("If-Match", version)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());

        user.set(office.user());
        deactivate(id, version).andExpect(status().isForbidden());

        user.set(manager.user());
        String next = deactivate(id, version)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        assertThat(next).isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM service WHERE id = ?", String.class, id))
                .isEqualTo("INACTIVE");
        deactivate(id, next)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_ALREADY_INACTIVE"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M02.SERVICE' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(id)))
                .isEqualTo(2);
    }

    @Test
    void FR_034_inactiveServiceIsNotEdited() throws Exception {
        ObjectNode body = input("ACTIVITY", "Dã ngoại");
        JsonNode service = createdService(body);
        long id = service.get("id").asLong();
        String next = deactivate(id, "\"" + service.get("version").asLong() + "\"")
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        update(id, next, body)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("SERVICE_INACTIVE"));
    }
}
