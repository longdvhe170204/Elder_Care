package com.eldercare.backend.resident;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.common.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T016, T020 (US1 #1, #2, #4, #5, #6, #7; FR-007 → FR-009): đường HTTP của hồ sơ. Hành chính tạo hồ sơ, không thấy
 * trường sức khỏe và mức chăm sóc (nằm trong {@code hiddenFields}); nhân viên ngoài phạm vi nhận 404; Nhân viên bếp
 * nhận 403; lỗi trả dạng problem+json có mã lỗi.
 */
@AutoConfigureMockMvc
@WithMockUser
class ProfileFieldLimitsIT extends ResidentProfileTestBase {

    @Autowired
    MockMvc mvc;

    @BeforeEach
    void grantVisibility() {
        // Phụ lục 27 mục 19.3: Hành chính, Bác sĩ, Nhân viên chăm sóc xem đủ nhận dạng và liên hệ; sức khỏe và mức
        // chăm sóc chỉ Bác sĩ (Hành chính không thấy, Nhân viên chăm sóc theo phân công)
        for (Role role : new Role[] {Role.OFFICE_STAFF, Role.DOCTOR, Role.CAREGIVER}) {
            permissions.visibility(role, InfoGroup.IDENTITY, Visibility.FULL);
            permissions.visibility(role, InfoGroup.CONTACT, Visibility.FULL);
        }
        permissions.visibility(Role.DOCTOR, InfoGroup.HEALTH, Visibility.FULL);
        permissions.visibility(Role.DOCTOR, InfoGroup.CARE_LEVEL, Visibility.FULL);
    }

    private String json(String fullName, String extra) {
        return "{\"fullName\":\"" + fullName + "\",\"dateOfBirth\":\"1948-03-14\",\"gender\":\"MALE\"" + extra + "}";
    }

    private long create(String body) throws Exception {
        user.set(OFFICE);
        String response = mvc.perform(post("/api/v1/residents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.profile.id")).longValue();
    }

    @Test
    void US1_1_post_createsProfileAndReturnsEtagAndWarnings() throws Exception {
        user.set(OFFICE);

        mvc.perform(post("/api/v1/residents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(
                                uniqueName(),
                                ",\"idDocumentType\":\"CITIZEN_ID\",\"idDocumentNumber\":\"" + uniqueCitizenId()
                                        + "\",\"permanentAddress\":\"12 Lê Lợi\"")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.profile.profileCode")
                        .value(org.hamcrest.Matchers.matchesPattern("NCT-\\d{4}-\\d{6}")))
                .andExpect(jsonPath("$.profile.status").value("ADMISSION_IN_PROGRESS"))
                .andExpect(jsonPath("$.profile.personalInfo.permanentAddress").value("12 Lê Lợi"))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void US1_1_post_withMissingRequiredField_isRejected() throws Exception {
        user.set(OFFICE);

        mvc.perform(post("/api/v1/residents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Thiếu ngày sinh\",\"gender\":\"MALE\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void US1_2_post_withCitizenIdInUse_returns409WithErrorCode() throws Exception {
        String cccd = uniqueCitizenId();
        String extra = ",\"idDocumentType\":\"CITIZEN_ID\",\"idDocumentNumber\":\"" + cccd + "\"";
        create(json(uniqueName(), extra));

        mvc.perform(post("/api/v1/residents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(uniqueName(), extra)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CITIZEN_ID_IN_USE"))
                .andExpect(jsonPath("$.profileCode").exists());
    }

    @Test
    void US1_4_post_withPossibleDuplicate_returns409WithCandidatesThenAcceptsConfirmation() throws Exception {
        String name = "Mai Thị " + uniqueCitizenId();
        create(json(name, ""));

        mvc.perform(post("/api/v1/residents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name, "")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("POSSIBLE_DUPLICATE_PROFILE"))
                .andExpect(jsonPath("$.candidates", hasSize(1)));
        mvc.perform(post("/api/v1/residents")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(name, ",\"confirmNotDuplicate\":true")))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_008_office_doesNotSeeHealthFieldsOrCareLevel() throws Exception {
        long id = create(json(uniqueName(), ""));
        user.set(OFFICE);

        mvc.perform(get("/api/v1/residents/" + id))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.personalInfo.fullName").exists())
                .andExpect(jsonPath("$.activeHealthItems").doesNotExist())
                .andExpect(jsonPath("$.currentCareLevel").doesNotExist())
                .andExpect(jsonPath("$.hiddenFields", hasItems("activeHealthItems", "currentCareLevel")))
                .andExpect(jsonPath("$.allowedCommands", hasItem("UPDATE_PERSONAL_INFO")));
    }

    @Test
    void FR_008_doctor_seesHealthFieldsAndCannotUpdatePersonalInfo() throws Exception {
        long id = create(json(uniqueName(), ""));
        user.set(DOCTOR);

        mvc.perform(get("/api/v1/residents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeHealthItems").isArray())
                .andExpect(jsonPath("$.hiddenFields", not(hasItem("activeHealthItems"))))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("UPDATE_PERSONAL_INFO"))));
    }

    @Test
    void US1_6_caregiver_seesAssignedProfileOnly() throws Exception {
        long assigned = create(json(uniqueName(), ""));
        long other = create(json(uniqueName(), ""));
        permissions.assign(assigned);
        user.set(CAREGIVER);

        mvc.perform(get("/api/v1/residents/" + assigned)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/residents/" + other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/residents").param("q", "NCT-"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].id", not(hasItem((int) other))));
    }

    @Test
    void US1_7_kitchenStaff_gets403() throws Exception {
        user.set(KITCHEN);

        mvc.perform(get("/api/v1/residents")).andExpect(status().isForbidden());
    }

    @Test
    void US1_5_updatePersonalInfo_needsMatchingEtagAndReason() throws Exception {
        long id = create(json(uniqueName(), ""));
        user.set(OFFICE);
        String etag = mvc.perform(get("/api/v1/residents/" + id))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        String body = json(uniqueName(), ",\"reason\":\"Sửa theo giấy tờ gốc\"");

        mvc.perform(post("/api/v1/residents/" + id + "/update-personal-info")
                        .with(csrf())
                        .header("If-Match", "\"999\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.errorCode").value("VERSION_MISMATCH"));
        mvc.perform(post("/api/v1/residents/" + id + "/update-personal-info")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(uniqueName(), "")))
                .andExpect(status().is4xxClientError());
        mvc.perform(post("/api/v1/residents/" + id + "/update-personal-info")
                        .with(csrf())
                        .header("If-Match", etag)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    void FR_004_linkedProfiles_returnsPreviousProfiles() throws Exception {
        String cccd = uniqueCitizenId();
        String extra = ",\"idDocumentType\":\"CITIZEN_ID\",\"idDocumentNumber\":\"" + cccd + "\"";
        long old = create(json(uniqueName(), extra));
        setStatus(old, "RESIDENCY_ENDED");
        long fresh = create(json(uniqueName(), extra));
        user.set(OFFICE);

        mvc.perform(get("/api/v1/residents/" + fresh + "/linked-profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(old))
                .andExpect(jsonPath("$[0].viewable").value(true));
    }
}
