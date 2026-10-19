package com.cambae.backend.m15.correction;

import static com.cambae.backend.m15.sample.CorrectionFixtures.FLOOR_MANAGER;
import static com.cambae.backend.m15.sample.CorrectionFixtures.MANAGER;
import static com.cambae.backend.m15.sample.CorrectionFixtures.OFFICE_A;
import static com.cambae.backend.m15.sample.CorrectionFixtures.OFFICE_B;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.ApprovalStatus;
import com.cambae.backend.m15.approval.internal.ApprovalRequestService;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionResult;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.sample.CorrectionFixtures;
import com.cambae.backend.m15.sample.SampleOfficeRecordProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Cách C (000 T057 phần C, T058; US2 #4, FR-026 c; quickstart K16): bản ghi không gắn tầng, nhân viên cùng vai trò lập
 * yêu cầu {@code M15.CORRECTION} ở Chờ duyệt; Quản lý viện duyệt thì có bản đính chính {@code creationMethod = C} trỏ
 * về yêu cầu; chưa duyệt, bị từ chối hoặc bản gốc đã Hủy ghi nhận thì không có.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(CorrectionFixtures.class)
class CorrectionApprovalIT extends MySqlIntegrationTestBase {

    private static final String OFFICE_RECORD = SampleOfficeRecordProvider.TYPE;
    private static final Reason REASON = new Reason("ghi nhầm số lượng");

    @Autowired
    CorrectionApi corrections;

    @Autowired
    ApprovalRequestService approvals;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @AfterEach
    void resetUser() {
        user.set(CorrectionFixtures.NURSE_A);
    }

    private long officeRecord(String note) {
        jdbc.update(
                "INSERT INTO sample_office_record (occurred_at, actor_id, note) VALUES (UTC_TIMESTAMP(6), ?, ?)",
                OFFICE_A.accountId(),
                note);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private ApprovalRequestView requestByColleague(long id, CorrectionType type, Object content) {
        user.set(OFFICE_B);
        CorrectionResult result = corrections.create(OFFICE_RECORD, id, type, content, REASON);
        assertThat(result).isInstanceOf(CorrectionResult.PendingApproval.class);
        return ((CorrectionResult.PendingApproval) result).request();
    }

    private ApprovalRequestView approveAsManager(ApprovalRequestView request) {
        user.set(MANAGER);
        approvals.approve(request.id(), String.valueOf(request.version()), null);
        return approvals.find(request.id()).orElseThrow();
    }

    private int corrections(long id) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM correction WHERE record_type = ? AND original_record_id = ?",
                Integer.class,
                OFFICE_RECORD,
                id);
    }

    @Test
    void US2_4_colleagueRequest_isPendingApproval_withoutCorrection() {
        long id = officeRecord("giao 2 túi đồ");

        ApprovalRequestView request =
                requestByColleague(id, CorrectionType.AMEND_CONTENT, Map.of("note", "giao 3 túi"));

        assertThat(request.type()).isEqualTo("M15.CORRECTION");
        assertThat(request.status()).isEqualTo(ApprovalStatus.PENDING_APPROVAL);
        assertThat(request.requesterId()).isEqualTo(OFFICE_B.accountId());
        assertThat(request.reason()).isEqualTo(REASON.value());
        assertThat(corrections(id)).isZero();
    }

    @Test
    void US2_4_managerApproves_correctionAddedWithMethodC() {
        long id = officeRecord("giao 2 túi đồ");
        ApprovalRequestView request =
                requestByColleague(id, CorrectionType.AMEND_CONTENT, Map.of("note", "giao 3 túi"));

        ApprovalRequestView applied = approveAsManager(request);

        assertThat(applied.status()).isEqualTo(ApprovalStatus.APPLIED);
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT id, creation_method, approval_request_id, actor_id, sequence_no, reason FROM correction"
                        + " WHERE record_type = ? AND original_record_id = ?",
                OFFICE_RECORD,
                id);
        assertThat(row.get("creation_method")).isEqualTo("C");
        assertThat(((Number) row.get("approval_request_id")).longValue()).isEqualTo(request.id());
        assertThat(((Number) row.get("actor_id")).longValue()).isEqualTo(OFFICE_B.accountId());
        assertThat(((Number) row.get("sequence_no")).intValue()).isEqualTo(1);
        assertThat(row.get("reason")).isEqualTo(REASON.value());
        assertThat(applied.applicationResult().type()).isEqualTo("M15.CORRECTION");
        assertThat(applied.applicationResult().id()).isEqualTo(String.valueOf(row.get("id")));
        assertThat(corrections
                        .current(OFFICE_RECORD, java.util.List.of(id))
                        .get(id)
                        .content())
                .isEqualTo(Map.of("note", "giao 3 túi"));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'CORRECTION_ADDED' AND entity_type = ?"
                                + " AND entity_id = ? AND JSON_EXTRACT(basis, '$.approvalRequestId') = ?",
                        Integer.class,
                        OFFICE_RECORD,
                        String.valueOf(id),
                        request.id()))
                .isEqualTo(1);
    }

    @Test
    void US2_4_onlyManagerApproves() {
        long id = officeRecord("x");
        ApprovalRequestView request = requestByColleague(id, CorrectionType.AMEND_CONTENT, Map.of("note", "y"));

        user.set(OFFICE_A);
        assertThatThrownBy(() -> approvals.approve(request.id(), String.valueOf(request.version()), null))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(corrections(id)).isZero();
    }

    @Test
    void FR_036_rejected_noCorrection() {
        long id = officeRecord("x");
        ApprovalRequestView request = requestByColleague(id, CorrectionType.AMEND_CONTENT, Map.of("note", "y"));

        user.set(MANAGER);
        approvals.reject(request.id(), String.valueOf(request.version()), new Reason("đúng như bản gốc"));

        assertThat(corrections(id)).isZero();
    }

    @Test
    void FR_029_originalVoidedBeforeApproval_applyFails() {
        long id = officeRecord("x");
        ApprovalRequestView request = requestByColleague(id, CorrectionType.AMEND_CONTENT, Map.of("note", "y"));
        user.set(OFFICE_A);
        corrections.create(OFFICE_RECORD, id, CorrectionType.VOID, null, REASON);

        ApprovalRequestView after = approveAsManager(request);

        assertThat(after.status()).isEqualTo(ApprovalStatus.APPLY_FAILED);
        assertThat(after.closingReason()).contains("FR-029");
        assertThat(corrections(id)).isEqualTo(1);
    }

    @Test
    void US2_4_floorManager_stillRefused() {
        long id = officeRecord("x");
        user.set(FLOOR_MANAGER);

        assertThatThrownBy(() -> corrections.create(
                        OFFICE_RECORD, id, CorrectionType.AMEND_CONTENT, Map.of("note", "y"), REASON))
                .hasFieldOrPropertyWithValue("errorCode", "CORRECTION_NOT_ALLOWED");
    }

    @Test
    void T059_httpWayC_returns202WithApprovalRequest() throws Exception {
        long id = officeRecord("giao 2 túi đồ");
        user.set(OFFICE_B);

        mvc.perform(
                        post("/api/v1/corrections/" + OFFICE_RECORD + "/" + id)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"note\":\"giao 3 túi\"},\"reason\":\"ghi nhầm\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type").value("M15.CORRECTION"))
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andExpect(jsonPath("$.content.originalRecordId").value(id))
                .andExpect(jsonPath("$.requester.id").value((int) OFFICE_B.accountId()));
    }
}
