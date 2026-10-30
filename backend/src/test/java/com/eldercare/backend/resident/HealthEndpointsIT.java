package com.eldercare.backend.resident;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T032 (US2; contracts/api-001.yaml, tag {@code health}): đường HTTP của hồ sơ sức khỏe ban đầu và mục sức khỏe.
 * Quyền theo Phụ lục 27 qua {@code PermissionApi} thật; nút lệnh trong {@code allowedCommands} của hồ sơ (chỉ Bác sĩ có
 * lệnh xác nhận); lỗi trả problem+json có mã lỗi; mục sức khỏe không có lệnh sửa, xóa.
 */
@AutoConfigureMockMvc
@WithMockUser
class HealthEndpointsIT extends HealthTestBase {

    @Autowired
    MockMvc mvc;

    private static final String NOTE =
            "{\"bloodType\":\"O+\",\"mobility\":\"Đi lại cần gậy\",\"currentMedications\":\"Amlodipine 5mg\"}";

    @Test
    void US2_allowedCommands_followRoleAndAssignment_onlyDoctorConfirms() throws Exception {
        long resident = newResident();
        assignNurse(nurse, resident);

        user.set(doctor.user());
        mvc.perform(get("/api/v1/residents/" + resident))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.allowedCommands",
                        hasItems(
                                "RECORD_HEALTH_ITEM",
                                "EXCLUDE_HEALTH_ITEM",
                                "WRITE_INITIAL_HEALTH_RECORD",
                                "CONFIRM_INITIAL_HEALTH_RECORD")));

        user.set(nurse.user());
        mvc.perform(get("/api/v1/residents/" + resident))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands", hasItems("RECORD_HEALTH_ITEM", "WRITE_INITIAL_HEALTH_RECORD")))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("CONFIRM_INITIAL_HEALTH_RECORD"))));

        user.set(office.user());
        mvc.perform(get("/api/v1/residents/" + resident))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("RECORD_HEALTH_ITEM"))))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("WRITE_INITIAL_HEALTH_RECORD"))));
    }

    @Test
    void FR_017_initialRecord_draftEtagConfirmAndConflict() throws Exception {
        long resident = newResident();
        String path = "/api/v1/residents/" + resident + "/initial-health-record";
        user.set(doctor.user());

        mvc.perform(get(path)).andExpect(status().isNotFound());
        String created = mvc.perform(put(path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOTE))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.confirmed").value(false))
                .andExpect(jsonPath("$.currentMedications").value("Amlodipine 5mg"))
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        mvc.perform(put(path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOTE)
                        .header("If-Match", "\"999\""))
                .andExpect(status().isPreconditionFailed());
        mvc.perform(post(path + "/confirm").with(csrf()).header("If-Match", created))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmed").value(true));
        mvc.perform(put(path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOTE)
                        .header("If-Match", created))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ALREADY_CONFIRMED"));
    }

    @Test
    void FR_017_nurseCannotConfirm_returns403() throws Exception {
        long resident = newResident();
        assignNurse(nurse, resident);
        String path = "/api/v1/residents/" + resident + "/initial-health-record";
        user.set(nurse.user());
        String etag = mvc.perform(put(path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOTE))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader("ETag");

        mvc.perform(post(path + "/confirm").with(csrf()).header("If-Match", etag))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("PERMISSION_DENIED"));
    }

    @Test
    void FR_017_correction_viaResidentPath_returnsCreationMethod() throws Exception {
        long resident = newResident();
        assignNurse(nurse, resident);
        String path = "/api/v1/residents/" + resident + "/initial-health-record";
        user.set(nurse.user());
        String etag = mvc.perform(put(path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NOTE))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        user.set(doctor.user());
        mvc.perform(post(path + "/confirm").with(csrf()).header("If-Match", etag))
                .andExpect(status().isOk());
        long recordId =
                jdbc.queryForObject("SELECT id FROM initial_health_record WHERE resident_id = ?", Long.class, resident);

        user.set(nurse.user());
        mvc.perform(post("/api/v1/initial-health-records/" + recordId + "/correct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EDIT_CONTENT\",\"correctedContent\":{\"currentMedications\":\"Amlodipine"
                                + " 10mg\"},\"reason\":\"Ghi sai liều\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creationMethod").value("A_ORIGINAL_AUTHOR"))
                .andExpect(jsonPath("$.correctionId").exists());
        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corrected").value(true))
                .andExpect(jsonPath("$.currentMedications").value("Amlodipine 10mg"));
    }

    @Test
    void FR_018_healthItems_recordListExclude_overHttp() throws Exception {
        long resident = newResident();
        String base = "/api/v1/residents/" + resident + "/health-items";
        user.set(doctor.user());

        String result = mvc.perform(post(base)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ALLERGY\",\"allergenId\":" + allergenId() + ",\"severityId\":"
                                + severityId() + ",\"sourceId\":" + sourceId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.type").value("ALLERGY"))
                .andExpect(jsonPath("$.items.pendingCheck").value(true))
                .andExpect(jsonPath("$.items.status").value("ACTIVE"))
                .andExpect(jsonPath("$.warnings", hasSize(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long itemId = ((Number) com.jayway.jsonpath.JsonPath.read(result, "$.items.id")).longValue();

        mvc.perform(get(base)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(post("/api/v1/health-items/" + itemId + "/exclude")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Ghi nhầm người\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXCLUDED"))
                .andExpect(jsonPath("$.exclusion.reason").value("Ghi nhầm người"));
        mvc.perform(get(base)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(base + "?status=ALL")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(base + "?status=EXCLUDED")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void FR_021_duplicateItem_returns409WithErrorCodeThenAcceptsConfirmation() throws Exception {
        long resident = newResident();
        String base = "/api/v1/residents/" + resident + "/health-items";
        String body = "{\"type\":\"CHRONIC_CONDITION\",\"freeText\":\"Tăng huyết áp\",\"sourceId\":" + sourceId() + "}";
        user.set(doctor.user());
        mvc.perform(post(base)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(post(base)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_HEALTH_ITEM"))
                .andExpect(jsonPath("$.existingItemId").exists());
        mvc.perform(post(base)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("}", ",\"confirmDuplicate\":true}")))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_018_invalidItem_returns422_andExcludeWithoutReason_returns422() throws Exception {
        long resident = newResident();
        user.set(doctor.user());

        mvc.perform(post("/api/v1/residents/" + resident + "/health-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ALLERGY\",\"allergenId\":" + allergenId() + ",\"sourceId\":" + sourceId()
                                + "}"))
                .andExpect(status().isUnprocessableContent());
        mvc.perform(post("/api/v1/health-items/1/exclude")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
