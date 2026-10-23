package com.cambae.backend.m01;

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

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T041 (US3; contracts/api-001.yaml, tag {@code assessments}): đường HTTP của lần đánh giá và mức chăm sóc. Điều
 * dưỡng lập và sửa nháp, chỉ Bác sĩ xác nhận (403 với Điều dưỡng, 409 {@code CONDITION_NOT_MET} khi thiếu nhận xét hoặc
 * mức); dữ liệu tham khảo kèm chi tiết; mức hiện hành và lịch sử sau xác nhận.
 */
@AutoConfigureMockMvc
@WithMockUser
class AssessmentEndpointsIT extends AssessmentTestBase {

    @Autowired
    MockMvc mvc;

    private static final String FULL =
            "{\"comment\":\"Đi lại cần gậy, tỉnh táo\",\"careNote\":\"Dễ ngã\",\"careLevel\":\"REGULAR\",\"careNeedIds\":[]}";

    private long admitting() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        assignNurse(nurse, id);
        return id;
    }

    @Test
    void US3_allowedCommands_followRole_onlyDoctorConfirms() throws Exception {
        long resident = admitting();

        user.set(nurse.user());
        mvc.perform(get("/api/v1/residents/" + resident))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowedCommands", hasItem("CREATE_ASSESSMENT")))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("CONFIRM_ASSESSMENT"))));

        user.set(doctor.user());
        mvc.perform(get("/api/v1/residents/" + resident))
                .andExpect(jsonPath("$.allowedCommands", hasItems("CREATE_ASSESSMENT", "CONFIRM_ASSESSMENT")));

        user.set(office.user());
        mvc.perform(get("/api/v1/residents/" + resident))
                .andExpect(jsonPath("$.allowedCommands", not(hasItem("CREATE_ASSESSMENT"))));
    }

    @Test
    void FR_026_nurseDraftsDoctorConfirms_levelAndHistoryAppearAfterwards() throws Exception {
        long resident = admitting();
        String base = "/api/v1/residents/" + resident;

        user.set(nurse.user());
        String etag = mvc.perform(post(base + "/assessments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"INITIAL\",\"basis\":\"Tiếp nhận mới\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.type").value("INITIAL"))
                .andExpect(jsonPath("$.reference.activeHealthItems", hasSize(0)))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        long id = assessments.list(target(resident)).getFirst().id();

        String edited = mvc.perform(put("/api/v1/assessments/" + id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(FULL)
                        .header("If-Match", etag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careLevel").value("REGULAR"))
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        mvc.perform(post("/api/v1/assessments/" + id + "/confirm").with(csrf()).header("If-Match", edited))
                .andExpect(status().isForbidden());

        user.set(doctor.user());
        mvc.perform(get("/api/v1/assessments/" + id)).andExpect(status().isOk()).andExpect(header().exists("ETag"));
        mvc.perform(post("/api/v1/assessments/" + id + "/confirm").with(csrf()).header("If-Match", edited))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assessment.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.effects.currentCareLevelChanged").value(true))
                .andExpect(jsonPath("$.effects.carePlanReviewNotified").value(false));

        mvc.perform(get(base + "/care-level"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careLevel").value("REGULAR"))
                .andExpect(jsonPath("$.basisType").value("ASSESSMENT"))
                .andExpect(jsonPath("$.basisId").value(id));
        mvc.perform(get(base + "/care-level-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get(base)).andExpect(jsonPath("$.currentCareLevel").value("REGULAR"));
    }

    @Test
    void FR_028_confirmWithoutComment_returns409_listingMissingFields() throws Exception {
        long resident = admitting();
        user.set(doctor.user());
        String etag = mvc.perform(post("/api/v1/residents/" + resident + "/assessments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"INITIAL\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("ETag");
        long id = assessments.list(target(resident)).getFirst().id();

        mvc.perform(post("/api/v1/assessments/" + id + "/confirm").with(csrf()).header("If-Match", etag))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CONDITION_NOT_MET"))
                .andExpect(jsonPath("$.unmetConditions[*].code", hasItems("comment", "careLevel")));
    }

    @Test
    void BR_M01_13_secondDraftReturns409_discardWithReasonThenCreateAgain() throws Exception {
        long resident = admitting();
        String base = "/api/v1/residents/" + resident + "/assessments";
        user.set(nurse.user());
        mvc.perform(post(base)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"INITIAL\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post(base)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"INITIAL\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DRAFT_ALREADY_EXISTS"));

        long id = assessments.list(target(resident)).getFirst().id();
        mvc.perform(post("/api/v1/assessments/" + id + "/discard-draft")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Nhập nhầm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISCARDED"));
        mvc.perform(post(base)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"INITIAL\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get(base)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void FR_052_careLevelBeforeAnyAssessment_returns404() throws Exception {
        long resident = admitting();
        user.set(doctor.user());

        mvc.perform(get("/api/v1/residents/" + resident + "/care-level")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/residents/" + resident + "/care-level-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void FR_032_correctAssessmentThroughHttp_changesDisplayedNote() throws Exception {
        long resident = admitting();
        long id = confirmed(resident, AssessmentType.INITIAL, CareLevel.REGULAR, "Khỏe", "Lưu ý cũ")
                .id();

        user.set(doctor.user());
        mvc.perform(post("/api/v1/assessments/" + id + "/correct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"EDIT_CONTENT\",\"correctedContent\":{\"careNote\":\"Lưu ý đúng\"},"
                                + "\"reason\":\"Ghi sai\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creationMethod").value("A_ORIGINAL_AUTHOR"));
        mvc.perform(get("/api/v1/assessments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.careNote").value("Lưu ý đúng"))
                .andExpect(jsonPath("$.corrected").value(true));
    }
}
