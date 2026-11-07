package com.eldercare.backend.administration.correction;

import static com.eldercare.backend.administration.sample.CorrectionFixtures.MANAGER;
import static com.eldercare.backend.administration.sample.CorrectionFixtures.OFFICE_A;
import static com.eldercare.backend.administration.sample.CorrectionFixtures.OFFICE_B;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.internal.service.ApprovalRequestService;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionResult;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.sample.CorrectionFixtures;
import com.eldercare.backend.administration.sample.SampleOfficeRecordProvider;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.sql.Statement;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
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
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO sample_office_record (occurred_at, actor_id, note) VALUES (UTC_TIMESTAMP(6), ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, OFFICE_A.accountId());
                    statement.setString(2, note);
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
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

    private ApprovalRequestView requestByColleagueOn(String recordType, long id) {
        user.set(OFFICE_B);
        CorrectionResult result =
                corrections.create(recordType, id, CorrectionType.AMEND_CONTENT, Map.of("note", "sửa"), REASON);
        assertThat(result).isInstanceOf(CorrectionResult.PendingApproval.class);
        return ((CorrectionResult.PendingApproval) result).request();
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
}
