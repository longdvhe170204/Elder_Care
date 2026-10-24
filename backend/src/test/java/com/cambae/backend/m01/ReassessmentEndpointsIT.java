package com.cambae.backend.m01;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.time.TestClockControl;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.ReassessmentNoticeService;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 T066 (US6; contracts/api-001.yaml, tag {@code assessments}): {@code GET /residents/{id}/reassessment} trả trạng thái
 * cần hoặc quá hạn đánh giá lại kèm các căn cứ đã báo; {@code GET /reassessments/due} liệt kê người cần đánh giá lại sắp theo
 * hạn, hồ sơ trạng thái cuối không có trong danh sách. Nhân viên chăm sóc không có quyền xem đánh giá nên nhận 403.
 */
@AutoConfigureMockMvc
@WithMockUser
class ReassessmentEndpointsIT extends StatusTestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    TestClockControl clock;

    @Autowired
    ReassessmentNoticeService notices;

    @BeforeEach
    @AfterEach
    void realClock() {
        clock.reset();
    }

    private long residentAssessed() {
        long id = newResident();
        setStatus(id, ResidentStatus.ADMISSION_IN_PROGRESS.name());
        confirmed(id, AssessmentType.INITIAL, CareLevel.REGULAR, "Đầu vào", null);
        moveTo(id, ResidentStatus.IN_RESIDENCE);
        return id;
    }

    @Test
    void US6_detail_showsStateDueDateAndBasis() throws Exception {
        long id = residentAssessed();
        notices.notifyAfterIncident(id, 501L);
        user.set(doctor.user());

        mvc.perform(get("/api/v1/residents/" + id + "/reassessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentId").value(id))
                .andExpect(jsonPath("$.status").value("DUE"))
                .andExpect(jsonPath("$.dueAt").isNotEmpty())
                .andExpect(jsonPath("$.overdueSince").value(nullValue()))
                .andExpect(jsonPath("$.basis", hasSize(1)))
                .andExpect(jsonPath("$.basis[0].basis").value("AFTER_INCIDENT"))
                .andExpect(jsonPath("$.basis[0].source").value("INCIDENT:501"))
                .andExpect(jsonPath("$.basis[0].notifiedAt").isNotEmpty());
    }

    @Test
    void US6_detail_noticeFreeResident_isNone_andFinalStatusIsNotApplicable() throws Exception {
        long id = residentAssessed();
        user.set(doctor.user());
        mvc.perform(get("/api/v1/residents/" + id + "/reassessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NONE"))
                .andExpect(jsonPath("$.basis", hasSize(0)));

        moveTo(id, ResidentStatus.RESIDENCY_ENDED);
        mvc.perform(get("/api/v1/residents/" + id + "/reassessment"))
                .andExpect(jsonPath("$.status").value("NOT_APPLICABLE"));
    }

    @Test
    void US6_detail_overdueCarriesTheOverdueSince() throws Exception {
        long id = residentAssessed();
        notices.notifyAfterIncident(id, 502L);
        clock.advanceBy(Duration.ofHours(49));
        user.set(doctor.user());

        mvc.perform(get("/api/v1/residents/" + id + "/reassessment"))
                .andExpect(jsonPath("$.status").value("OVERDUE"))
                .andExpect(jsonPath("$.overdueSince").isNotEmpty());
    }

    @Test
    void US6_dueList_hasTheResidentsInNeed_notThoseWithoutNoticeOrInFinalStatus() throws Exception {
        long needing = residentAssessed();
        notices.notifyAfterIncident(needing, 503L);
        long quiet = residentAssessed();
        long ended = residentAssessed();
        notices.notifyAfterIncident(ended, 504L);
        moveTo(ended, ResidentStatus.RESIDENCY_ENDED);
        user.set(doctor.user());

        mvc.perform(get("/api/v1/reassessments/due"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].residentId", hasItem((int) needing)))
                .andExpect(jsonPath("$[*].residentId", not(hasItem((int) quiet))))
                .andExpect(jsonPath("$[*].residentId", not(hasItem((int) ended))));
    }

    @Test
    void US6_caregiverWithoutViewPermission_gets403() throws Exception {
        long id = residentAssessed();
        user.set(users.staff(Role.CAREGIVER).user());

        mvc.perform(get("/api/v1/residents/" + id + "/reassessment")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reassessments/due")).andExpect(status().isForbidden());
    }
}
