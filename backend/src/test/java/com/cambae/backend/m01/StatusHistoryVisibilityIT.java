package com.cambae.backend.m01;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.Role;
import com.cambae.backend.common.file.FileApi;
import com.cambae.backend.common.file.FileTypes;
import com.cambae.backend.m01.api.dto.ConsenterType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m01.internal.consent.service.ConsentService;
import com.cambae.backend.m01.internal.consent.service.ConsentService.RecordConsent;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.support.TestUsers;
import com.cambae.backend.support.fake.file.FakeFileConfig;
import com.cambae.backend.support.fake.m15.FakeFamilyRelationships;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 001 FR-046, BR-M01-08, Opus #6: lý do, căn cứ, miễn điều kiện của lịch sử trạng thái có thể là nội dung y tế (chẩn
 * đoán khi chuyển viện, nguyên nhân qua đời), nên thuộc nhóm {@code HEALTH}. Người không xem được nhóm này vẫn thấy
 * trạng thái từ và đến, lệnh, thời điểm, người thực hiện; ba trường nằm trong {@code hiddenFields}. Lý do đã đính chính
 * cũng bị che như bản gốc. Quyền xem lịch sử nói chung ({@code RESIDENT_PROFILE VIEW}) không đổi.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeFileConfig.class)
class StatusHistoryVisibilityIT extends StatusTestBase {

    private static final String URL = "/api/v1/residents/%d/status-history";
    private static final String SECRET = "Chuyển viện vì đột quỵ";
    private static final String[] SENSITIVE = {"reason", "basis", "conditionWaiver"};

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeFamilyRelationships families;

    @Autowired
    ConsentService consents;

    @Autowired
    FileApi files;

    @Autowired
    CorrectionApi corrections;

    /** Hồ sơ đang ở viện, có một dòng lịch sử mang đủ lý do, căn cứ và miễn điều kiện. */
    private long residentWithSensitiveRow() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        jdbc.update(
                "INSERT INTO resident_status_history (resident_id, from_status, to_status, command, occurred_at,"
                        + " performed_by, by_system, reason, basis_type, basis_id, condition_waiver)"
                        + " VALUES (?, 'IN_RESIDENCE', 'HOSPITALIZED', 'TRANSFER_OUT', ?, ?, FALSE, ?, 'INCIDENT', 77,"
                        + " '{\"approvalRequestId\": 5, \"conditionCodes\": [\"MEDICINE_NOT_RETURNED\"]}')",
                id,
                Timestamp.from(time.now().plusSeconds(1)),
                office.accountId(),
                SECRET);
        jdbc.update("UPDATE resident SET status = 'HOSPITALIZED' WHERE id = ?", id);
        return id;
    }

    private String last(String field) {
        return "$[-1:]." + field;
    }

    private void assertHidden(long id) throws Exception {
        mvc.perform(get(URL.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[-1:].command").value("TRANSFER_OUT"))
                .andExpect(jsonPath("$[-1:].from").value("IN_RESIDENCE"))
                .andExpect(jsonPath("$[-1:].to").value("HOSPITALIZED"))
                .andExpect(jsonPath("$[-1:].at").exists())
                .andExpect(jsonPath("$[-1:].performedBy").exists())
                .andExpect(jsonPath(last("reason")).doesNotExist())
                .andExpect(jsonPath(last("basis")).doesNotExist())
                .andExpect(jsonPath(last("conditionWaiver")).doesNotExist())
                .andExpect(jsonPath("$[-1:].hiddenFields[*]", containsInAnyOrder(SENSITIVE)));
    }

    private void assertVisible(long id) throws Exception {
        mvc.perform(get(URL.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(last("reason")).value(contains(SECRET)))
                .andExpect(jsonPath(last("basis.type")).value(contains("INCIDENT")))
                .andExpect(jsonPath(last("conditionWaiver.approvalRequestId")).value(contains(5)))
                .andExpect(jsonPath("$[-1:].hiddenFields").value(contains(empty())));
    }

    @Test
    void FR_046_office_seesTheTransitionButNotReasonBasisOrWaiver() throws Exception {
        long id = residentWithSensitiveRow();
        user.set(office.user());

        assertHidden(id);
    }

    @Test
    void FR_046_accountant_seesTheTransitionButNotReasonBasisOrWaiver() throws Exception {
        long id = residentWithSensitiveRow();
        user.set(users.staff(Role.ACCOUNTANT).user());

        assertHidden(id);
    }

    @Test
    void FR_046_doctor_seesEverything() throws Exception {
        long id = residentWithSensitiveRow();
        user.set(doctor.user());

        assertVisible(id);
    }

    @Test
    void FR_046_assignedNurse_seesEverything_andUnassignedNurseGets404() throws Exception {
        long id = residentWithSensitiveRow();
        assignNurse(nurse, id);
        user.set(nurse.user());
        assertVisible(id);

        user.set(users.staff(Role.NURSING_STAFF).user());
        mvc.perform(get(URL.formatted(id))).andExpect(status().isNotFound());
    }

    @Test
    void BR_M01_08_familyMember_seesTheHealthPartsOnlyWhileAnActiveConsentIncludesThem() throws Exception {
        long id = residentWithSensitiveRow();
        long covered = TestUsers.nextId();
        long notCovered = TestUsers.nextId();
        families.relate(covered, id);
        families.relate(notCovered, id);
        user.set(office.user());
        consents.record(
                target(id),
                new RecordConsent(
                        ConsenterType.RESIDENT,
                        null,
                        null,
                        Set.of(covered),
                        true,
                        true,
                        Instant.now(),
                        files.upload(
                                "ban-ky.pdf",
                                FileTypes.PDF,
                                "%PDF-1.4".getBytes(StandardCharsets.UTF_8),
                                office.accountId())));

        user.set(users.family(covered).user());
        assertVisible(id);

        user.set(users.family(notCovered).user());
        assertHidden(id);
    }

    @Test
    void FR_049a_correctedReason_isHiddenLikeTheOriginal_andShownToTheDoctor() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        statusService.grantTemporaryAbsence(target(id), new Reason("Lý do cũ"), noDetails());
        long historyId = jdbc.queryForObject(
                "SELECT id FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                id);
        corrections.create(
                "M01.RESIDENT_STATUS_HISTORY",
                historyId,
                CorrectionType.AMEND_CONTENT,
                Map.of("reason", "Đã sửa: tiểu đường"),
                new Reason("Ghi sai"));

        mvc.perform(get(URL.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[-1:].command").value("GRANT_TEMPORARY_ABSENCE"))
                .andExpect(jsonPath(last("reason")).doesNotExist());

        user.set(doctor.user());
        mvc.perform(get(URL.formatted(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(last("reason")).value(contains("Đã sửa: tiểu đường")));
    }

    @Test
    void FR_046_commandResult_followsTheSameRule() throws Exception {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/v1/residents/" + id + "/grant-temporary-absence")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"" + SECRET + "\",\"details\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history.command").value("GRANT_TEMPORARY_ABSENCE"))
                .andExpect(jsonPath("$.history.reason").doesNotExist())
                .andExpect(jsonPath("$.history.hiddenFields[*]", containsInAnyOrder(SENSITIVE)));
    }
}
