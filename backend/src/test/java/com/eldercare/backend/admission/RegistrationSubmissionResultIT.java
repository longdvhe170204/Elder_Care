package com.eldercare.backend.admission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.internal.registration.domain.ResidenceRegistration;
import com.eldercare.backend.admission.internal.registration.persistence.RegistrationObligationStore;
import com.eldercare.backend.admission.internal.registration.service.ResidenceRegistrationService;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.file.FakeFileConfig;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/**
 * 018 T009 ({@code KhaiBaoNopKetQuaIT}; US2, quickstart Q7 → Q10; FR-002 → FR-005, FR-009; DBR-33; research Z-04): Hành
 * chính ghi đã nộp (dừng nhắc quá hạn), ghi được chấp nhận (bằng chứng; Đăng ký tạm trú có ngày hết hạn; khai báo Đã xác
 * nhận cũ Được thay thế) hoặc bị từ chối (về Cần khai báo, hạn mới ở khai báo = lúc ghi + CFG-M02-12, hạn gốc của nghĩa vụ
 * giữ nguyên, nhắc quá hạn theo hạn mới, Quản lý viện tối đa một lần mỗi nghĩa vụ); lịch sử lần nộp chỉ thêm; kết quả đặt
 * một lần; đính chính {@code M02.RESIDENCE_REGISTRATION}.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeFileConfig.class)
class RegistrationSubmissionResultIT extends RegistrationTestBase {

    private static final String CORRECTIONS = "/api/v1/corrections/M02.RESIDENCE_REGISTRATION/";

    @Autowired
    MockMvc mvc;

    @Autowired
    TestClockControl clock;

    @Autowired
    FileApi files;

    @Autowired
    RegistrationObligationStore obligationStore;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    // ---- Dựng dữ liệu -------------------------------------------------------------------------------------------

    /** Người nội trú dài hạn đã Hoàn tất tiếp nhận, có nghĩa vụ và khai báo Đăng ký tạm trú ở Cần khai báo. */
    private ResidenceRegistration created() {
        return createdFor(ResidenceType.LONG_TERM_RESIDENTIAL, null);
    }

    private ResidenceRegistration createdFor(ResidenceType type, LocalDate end) {
        long residentId = readyWith(type, end);
        completeAdmission(residentId);
        awaitObligations(residentId, 1);
        return create(residentId);
    }

    private ResidenceRegistration create(long residentId) {
        user.set(office.user());
        return registrations.createRegistration(ResidenceRegistrationService.targetForResident(residentId), residentId);
    }

    private long evidence() {
        return files.upload(
                "ket-qua.pdf",
                FileTypes.PDF,
                "%PDF-1.4 ket qua khai bao".getBytes(StandardCharsets.UTF_8),
                office.accountId());
    }

    private ResultActions command(TestUser who, long id, String command, String body) throws Exception {
        return command(who, id, command, body, UUID.randomUUID());
    }

    private ResultActions command(TestUser who, long id, String command, String body, UUID key) throws Exception {
        user.set(who.user());
        return mvc.perform(post("/api/v1/residence-registrations/" + id + "/" + command)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String submittedBody(LocalDate date) {
        return """
                {"submittedDate":"%s","channel":"PUBLIC_SERVICE_PORTAL","receivingAuthority":"Công an phường X",\
                "dossierCode":"G22.%s"}""".formatted(date, UUID.randomUUID().toString().substring(0, 8));
    }

    private static String acceptedBody(Long evidenceFileId, LocalDate expiryDate) {
        StringBuilder body = new StringBuilder("{");
        if (evidenceFileId != null) {
            body.append("\"evidenceFileId\":").append(evidenceFileId);
        }
        if (expiryDate != null) {
            body.append(evidenceFileId != null ? "," : "")
                    .append("\"expiryDate\":\"")
                    .append(expiryDate)
                    .append('"');
        }
        return body.append('}').toString();
    }

    private void submit(ResidenceRegistration registration) throws Exception {
        command(office, registration.id(), "record-submitted", submittedBody(today()))
                .andExpect(status().isOk());
    }

    private List<Map<String, Object>> submissions(long registrationId) {
        return RegistrationSql.submissions(jdbc, registrationId);
    }

    private Map<String, Object> row(long registrationId) {
        JsonNode content = RegistrationSql.content(jdbc, registrationId);
        Map<String, Object> row = new HashMap<>();
        row.put("current_submission_id", id(content, "currentSubmissionId"));
        row.put("evidence_file_id", id(content, "evidenceFileId"));
        row.put("replaced_registration_id", id(content, "replacedRegistrationId"));
        return row;
    }

    private static Long id(JsonNode content, String field) {
        return content.hasNonNull(field) ? content.get(field).asLong() : null;
    }

    // ---- Q7: ghi đã nộp, ghi được chấp nhận ------------------------------------------------------------------------

    @Test
    void Q7_accepted_exposesEvidenceFileId_onTheRegistration() throws Exception {
        ResidenceRegistration registration = created();
        String path = "/api/v1/residents/" + registration.residentId() + "/residence-registrations";
        user.set(office.user());
        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].evidenceFileId".formatted(registration.id()))
                        .isEmpty());

        submit(registration);
        long evidence = evidence();
        command(office, registration.id(), "record-accepted", acceptedBody(evidence, today().plusYears(1)))
                .andExpect(status().isOk());

        user.set(office.user());
        mvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].evidenceFileId".formatted(registration.id()))
                        .value(org.hamcrest.Matchers.contains((int) evidence)));
    }

    // ---- Q8: chưa ghi nộp -------------------------------------------------------------------------------------

    // ---- Q9: bị từ chối ---------------------------------------------------------------------------------------

    // ---- Thay thế khai báo Đã xác nhận cũ (FR-005, DBR-33) ------------------------------------------------------

    // ---- Đồng thời, chống lặp ---------------------------------------------------------------------------------

    // ---- Q10: quyền -------------------------------------------------------------------------------------------

    // ---- Đính chính M02.RESIDENCE_REGISTRATION (research Z-04, FR-001) --------------------------------------------
}
