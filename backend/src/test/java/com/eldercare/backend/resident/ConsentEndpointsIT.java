package com.eldercare.backend.resident;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * 001 T057 (contracts/api-001.yaml, tag {@code consents}): đường HTTP của bản đồng ý. Ghi nhận trả 201 kèm bản Hiệu
 * lực, thiếu bản ký 422, người ký thay thiếu quyết định của Tòa án 422; rút lại trả 200, rút lại lần hai 409; hồ sơ
 * trả {@code hasActiveConsent} và lệnh được phép theo quyền (chỉ Hành chính ghi, AC-09).
 */
@AutoConfigureMockMvc
@WithMockUser
class ConsentEndpointsIT extends ConsentTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    JsonMapper json;

    private String recordBody(long familyMemberId, Long evidence) {
        return """
                {"consenterType":"RESIDENT","familyMemberIdsAllowedHealthView":[%d],"photoConsent":true,
                 "notificationConsent":false,"consentedAt":"2026-10-20T08:00:00Z"%s}""".formatted(familyMemberId, evidence == null ? "" : ",\"evidenceFileId\":" + evidence);
    }

    private long recordViaHttp(long residentId, long familyMemberId) throws Exception {
        user.set(office.user());
        MvcResult created = mvc.perform(post("/api/v1/residents/" + residentId + "/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody(familyMemberId, evidence())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.consenterType").value("RESIDENT"))
                .andExpect(jsonPath("$.familyMemberIdsAllowedHealthView[0]").value(familyMemberId))
                .andExpect(jsonPath("$.photoConsent").value(true))
                .andExpect(jsonPath("$.notificationConsent").value(false))
                .andExpect(jsonPath("$.evidenceFileId").isNumber())
                .andReturn();
        return json.readTree(created.getResponse().getContentAsString())
                .get("id")
                .asLong();
    }

    @Test
    void US5_record_returns201_andListShowsIt() throws Exception {
        long residentId = newResident();
        long x = relatedFamilyMember(residentId);

        long id = recordViaHttp(residentId, x);

        mvc.perform(get("/api/v1/residents/" + residentId + "/consents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void US5_missingEvidence_is422() throws Exception {
        long residentId = newResident();
        user.set(office.user());

        mvc.perform(post("/api/v1/residents/" + residentId + "/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody(1, null)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("EVIDENCE_REQUIRED"));
    }

    @Test
    void FR_011_representativeWithoutCourtDecision_is422() throws Exception {
        long residentId = newResident();
        long signer = relatedFamilyMember(residentId);
        user.set(office.user());
        String body = """
                {"consenterType":"LEGAL_REPRESENTATIVE","consenterFamilyMemberId":%d,
                 "familyMemberIdsAllowedHealthView":[],"photoConsent":true,"notificationConsent":true,
                 "consentedAt":"2026-10-20T08:00:00Z","evidenceFileId":%d}""".formatted(signer, evidence());

        mvc.perform(post("/api/v1/residents/" + residentId + "/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("COURT_DECISION_REQUIRED"));
    }

    @Test
    void FR_014_withdraw_returns200_thenSecondWithdrawIs409() throws Exception {
        long residentId = newResident();
        long id = recordViaHttp(residentId, relatedFamilyMember(residentId));
        String body = "{\"reason\":\"Người đại diện yêu cầu rút\",\"evidenceFileId\":" + evidence() + "}";

        mvc.perform(post("/api/v1/consents/" + id + "/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"))
                .andExpect(jsonPath("$.withdrawal.reason").value("Người đại diện yêu cầu rút"));

        String again = "{\"reason\":\"Rút lần hai\",\"evidenceFileId\":" + evidence() + "}";
        mvc.perform(post("/api/v1/consents/" + id + "/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(again))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONSENT_NOT_ACTIVE"));
    }

    @Test
    void FR_014_blankReason_is422() throws Exception {
        long residentId = newResident();
        long id = recordViaHttp(residentId, relatedFamilyMember(residentId));

        mvc.perform(post("/api/v1/consents/" + id + "/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \",\"evidenceFileId\":" + evidence() + "}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }

    @Test
    void AC09_doctorCannotRecord_is403() throws Exception {
        long residentId = newResident();
        user.set(doctor.user());

        mvc.perform(post("/api/v1/residents/" + residentId + "/consents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recordBody(1, evidence())))
                .andExpect(status().isForbidden());
    }

    @Test
    void US5_profileShowsHasActiveConsent_andConsentCommandsFollowPermission() throws Exception {
        long residentId = newResident();
        user.set(office.user());
        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasActiveConsent").value(false))
                .andExpect(jsonPath("$.allowedCommands", hasItem("RECORD_CONSENT")));

        recordViaHttp(residentId, relatedFamilyMember(residentId));

        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(jsonPath("$.hasActiveConsent").value(true))
                .andExpect(jsonPath("$.allowedCommands", hasItem("WITHDRAW_CONSENT")));
        user.set(doctor.user());
        mvc.perform(get("/api/v1/residents/" + residentId))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("RECORD_CONSENT"))))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("WITHDRAW_CONSENT"))));
    }
}
