package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.dto.Warning;
import com.cambae.backend.support.fake.m15.FakeFamilyRelationships;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.node.JsonNodeFactory;

/**
 * 004 T013 (US1; FR-001, FR-004a, Q-356): ý kiến của người cao tuổi về việc vào ở. Ý kiến mới thay ý kiến cũ, ý kiến cũ
 * vẫn tra cứu được và không sửa được ở cơ sở dữ liệu; không phải điều kiện nhưng Hoàn tất tiếp nhận kèm cảnh báo khi chưa
 * có ý kiến hoặc ý kiến là Không đồng ý, và ghi ý kiến hiện hành vào lượt.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_004a_AdmissionOpinionIT extends M02TestBase {

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeFamilyRelationships family;

    private long application(long residentId) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/admission-applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody(residentId)))
                .andExpect(status().isCreated());
        return applicationIdOf(residentId);
    }

    private static String opinion(String opinion, String sourceType, Long familyMemberId, String note) {
        return """
                {"opinion":"%s","source":{"type":"%s","familyMemberId":%s},"note":"%s"}""".formatted(opinion, sourceType, familyMemberId, note);
    }

    private void record(long applicationId, String body) throws Exception {
        mvc.perform(post("/api/v1/admission-applications/" + applicationId + "/admission-opinion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void FR_001_recordOpinion_isActiveWithSourceRecorderAndTime() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);

        mvc.perform(post("/api/v1/admission-applications/" + id + "/admission-opinion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opinion("AGREE", "RESIDENT", null, "Cụ tự nói đồng ý")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.opinion").value("AGREE"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.source.type").value("RESIDENT"))
                .andExpect(jsonPath("$.note").value("Cụ tự nói đồng ý"))
                .andExpect(jsonPath("$.recordedBy").isNotEmpty())
                .andExpect(jsonPath("$.at").isNotEmpty());
    }

    @Test
    void FR_001_newOpinionSupersedesOld_andOldStaysInHistory() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        record(id, opinion("DISAGREE", "RESIDENT", null, "Lần đầu"));
        record(id, opinion("AGREE", "RESIDENT", null, "Sau khi nghe giải thích"));

        mvc.perform(get("/api/v1/admission-applications/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionOpinion", hasSize(2)))
                .andExpect(jsonPath("$.admissionOpinion[0].opinion").value("AGREE"))
                .andExpect(jsonPath("$.admissionOpinion[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.admissionOpinion[1].opinion").value("DISAGREE"))
                .andExpect(jsonPath("$.admissionOpinion[1].status").value("SUPERSEDED"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM admission_opinion WHERE application_id = ? AND status = 'ACTIVE'",
                        Integer.class,
                        id))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM admission_opinion WHERE application_id = ? AND status = 'SUPERSEDED'"
                                + " AND superseded_by_id IS NOT NULL",
                        Integer.class,
                        id))
                .isEqualTo(1);
    }

    @Test
    void FR_001_familyMemberSourceNeedsActiveRelationship_else422() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);

        mvc.perform(post("/api/v1/admission-applications/" + id + "/admission-opinion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opinion("AGREE", "FAMILY_MEMBER", 424242L, "Con gái nói")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("INVALID_OPINION_SOURCE"));

        family.relate(424242L, residentId);
        record(id, opinion("AGREE", "FAMILY_MEMBER", 424242L, "Con gái nói"));
    }

    @Test
    void FR_001_missingSource_is400_andClosedApplicationIs409() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);

        mvc.perform(post("/api/v1/admission-applications/" + id + "/admission-opinion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"opinion\":\"AGREE\"}"))
                .andExpect(status().isBadRequest());

        jdbc.update("UPDATE admission_application SET status = 'CANCELLED' WHERE id = ?", id);
        mvc.perform(post("/api/v1/admission-applications/" + id + "/admission-opinion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opinion("AGREE", "RESIDENT", null, "Muộn")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("APPLICATION_CLOSED"));
    }

    @Test
    void FR_001_opinionContentCannotBeEditedOrDeleted_inDatabase() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        record(id, opinion("AGREE", "RESIDENT", null, "Đồng ý"));

        // Tài khoản ứng dụng không có quyền sửa cột nội dung, không có quyền xóa
        assertThatThrownBy(() ->
                        jdbc.update("UPDATE admission_opinion SET opinion = 'DISAGREE' WHERE application_id = ?", id))
                .hasRootCauseMessage(
                        "UPDATE command denied to user 'eldercare_app'@'localhost' for column 'opinion' in table"
                                + " 'admission_opinion'");
        assertThatThrownBy(() -> jdbc.update("DELETE FROM admission_opinion WHERE application_id = ?", id))
                .isInstanceOf(Exception.class);
        // Dù có quyền (tài khoản quản trị), trigger vẫn chặn sửa nội dung và xóa
        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE admission_opinion SET note = 'Sửa' WHERE application_id = " + id))
                    .hasMessageContaining("immutable");
            assertThatThrownBy(
                            () -> statement.executeUpdate("DELETE FROM admission_opinion WHERE application_id = " + id))
                    .hasMessageContaining("retention job");
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM admission_opinion WHERE application_id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void FR_001_supersededOpinionCannotBeReactivated_inDatabase() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        record(id, opinion("DISAGREE", "RESIDENT", null, "Một"));
        record(id, opinion("AGREE", "RESIDENT", null, "Hai"));

        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE admission_opinion SET status = 'ACTIVE' WHERE application_id = " + id
                                    + " AND status = 'SUPERSEDED'"))
                    .hasMessageContaining("cannot change");
        }
    }

    @Test
    void FR_051_retentionAccountCanDeleteOpinionsOldestFirst() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        record(id, opinion("DISAGREE", "RESIDENT", null, "Một"));
        record(id, opinion("AGREE", "RESIDENT", null, "Hai"));

        try (Connection retention = retentionConnection();
                Statement statement = retention.createStatement()) {
            statement.executeUpdate("DELETE FROM admission_opinion WHERE application_id = " + id + " ORDER BY id");
            statement.executeUpdate("DELETE FROM admission_application WHERE id = " + id);
        }
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM admission_opinion WHERE application_id = ?", Integer.class, id))
                .isZero();
    }

    /** Cảnh báo ý kiến của 004; 001 còn cảnh báo thiếu bản đồng ý, nguyện vọng (không thuộc 004) nên lọc theo mã. */
    private List<Warning> complete(long residentId) {
        return statusService
                .completeAdmission(
                        target(residentId),
                        new Reason("Gia đình đã nhận đủ thông tin"),
                        JsonNodeFactory.instance.objectNode())
                .warnings()
                .stream()
                .filter(warning -> warning.code().equals("ADMISSION_OPINION_MISSING")
                        || warning.code().equals("RESIDENT_DISAGREES_WITH_ADMISSION"))
                .toList();
    }

    @Test
    void FR_004a_completeWithoutOpinion_warnsButSucceeds_andLeavesOpinionRefEmpty() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        readyToAdmit(residentId);
        user.set(office.user());

        List<Warning> warnings = complete(residentId);

        assertThat(warnings).extracting(Warning::code).containsExactly("ADMISSION_OPINION_MISSING");
        assertThat(jdbc.queryForObject("SELECT status FROM admission_application WHERE id = ?", String.class, id))
                .isEqualTo("ADMITTED");
        assertThat(jdbc.queryForObject(
                        "SELECT opinion_at_admission_id FROM admission_application WHERE id = ?", Long.class, id))
                .isNull();
    }

    @Test
    void FR_004a_completeWithDisagree_warnsWithSource_andRecordsOpinionAtAdmission() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        record(id, opinion("DISAGREE", "RESIDENT", null, "Cụ muốn ở nhà"));
        readyToAdmit(residentId);
        user.set(office.user());

        List<Warning> warnings = complete(residentId);

        assertThat(warnings).extracting(Warning::code).containsExactly("RESIDENT_DISAGREES_WITH_ADMISSION");
        Long activeOpinion = jdbc.queryForObject(
                "SELECT id FROM admission_opinion WHERE application_id = ? AND status = 'ACTIVE'", Long.class, id);
        assertThat(jdbc.queryForObject(
                        "SELECT opinion_at_admission_id FROM admission_application WHERE id = ?", Long.class, id))
                .isEqualTo(activeOpinion);
        assertThat(jdbc.queryForObject(
                        "SELECT reason FROM resident_status_history WHERE resident_id = ? AND command ="
                                + " 'COMPLETE_ADMISSION'",
                        String.class,
                        residentId))
                .isEqualTo("Gia đình đã nhận đủ thông tin");
    }

    @Test
    void FR_004a_completeWithAgree_hasNoWarning_andOpinionDoesNotBlockConditions() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        record(id, opinion("AGREE", "RESIDENT", null, "Đồng ý"));
        readyToAdmit(residentId);
        user.set(office.user());

        assertThat(complete(residentId)).isEmpty();
    }

    @Test
    void FR_004a_conditionListShowsCurrentOpinion_separateFromConditions() throws Exception {
        long residentId = admissionResident();
        long id = application(residentId);
        record(id, opinion("UNABLE_TO_EXPRESS", "OBSERVING_STAFF", null, "Cụ không trả lời được"));

        mvc.perform(get("/api/v1/admission-applications/" + id + "/admission-conditions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.admissionOpinion.opinion").value("UNABLE_TO_EXPRESS"))
                .andExpect(jsonPath("$.admissionOpinion.source.type").value("OBSERVING_STAFF"))
                .andExpect(jsonPath("$.conditions[?(@.code == 'ADMISSION_OPINION')]")
                        .doesNotExist());
    }
}
