package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionResult;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CreationMethod;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T050 (FR-049a; module-interfaces mục 1.9): lịch sử trạng thái là dữ liệu nhóm 3 nên chỉ sửa bằng đính chính của 000
 * (loại {@code M01.RESIDENT_STATUS_HISTORY}). Chỉ lý do của dòng lịch sử đính chính được; đính chính không đổi trạng
 * thái, không thêm dòng lịch sử, không báo đánh giá lại. Trên hồ sơ trạng thái cuối chỉ có cách C (yêu cầu phê duyệt,
 * Quản lý viện duyệt), bản gốc và lịch sử hiển thị chưa đổi tới khi được duyệt.
 */
class StatusHistoryCorrectionIT extends StatusTestBase {

    private static final String TYPE = "M01.RESIDENT_STATUS_HISTORY";

    @Autowired
    CorrectionApi corrections;

    private long lastHistoryId(long residentId) {
        return jdbc.queryForObject(
                "SELECT id FROM resident_status_history WHERE resident_id = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                residentId);
    }

    private CorrectionResult correct(long historyId, Object content) {
        return corrections.create(TYPE, historyId, CorrectionType.AMEND_CONTENT, content, new Reason("Ghi sai lý do"));
    }

    private String shownReason(long residentId) {
        return statusService.history(target(residentId)).stream()
                .filter(h -> "GRANT_TEMPORARY_ABSENCE".equals(h.command()))
                .findFirst()
                .orElseThrow()
                .reason();
    }

    private long absentResident() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        statusService.grantTemporaryAbsence(target(id), new Reason("Lý do cũ"), noDetails());
        return id;
    }

    @Test
    void FR_049a_recorderCorrectsTheReason_historyShowsTheCorrectedReason_andNothingElseChanges() {
        long id = absentResident();
        long historyId = lastHistoryId(id);
        int rows = historyCount(id);

        CorrectionResult result = correct(historyId, Map.of("reason", "Lý do đúng"));

        assertThat(result)
                .isInstanceOfSatisfying(
                        CorrectionResult.Saved.class,
                        saved -> assertThat(saved.correction().creationMethod()).isEqualTo(CreationMethod.A));
        assertThat(shownReason(id)).isEqualTo("Lý do đúng");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.TEMPORARILY_ABSENT);
        assertThat(historyCount(id)).isEqualTo(rows);
        assertThat(jdbc.queryForObject(
                        "SELECT reason FROM resident_status_history WHERE id = ?", String.class, historyId))
                .as("bản gốc giữ nguyên")
                .isEqualTo("Lý do cũ");
        assertThat(jdbc.queryForList(
                        "SELECT action FROM audit_log WHERE entity_type = ? AND entity_id = ? AND resident_id = ?",
                        String.class,
                        TYPE,
                        String.valueOf(historyId),
                        id))
                .as("một dòng đính chính của 000, gắn người cao tuổi (L98)")
                .containsExactly("CORRECTION_ADDED");
    }

    @Test
    void FR_049a_onlyTheReasonCanBeCorrected_andItMustNotBeBlank() {
        long id = absentResident();
        long historyId = lastHistoryId(id);

        for (Object bad : List.of(
                Map.of("reason", " "),
                Map.of("command", "RECORD_DEATH"),
                Map.of("to", "DECEASED"),
                Map.of("reason", "ok", "basis", "x"),
                Map.of())) {
            assertThatThrownBy(() -> correct(historyId, bad)).isInstanceOf(InvalidValueException.class);
        }
        assertThat(shownReason(id)).isEqualTo("Lý do cũ");
    }

    @Test
    void FR_049a_unknownHistoryRow_isRefused() {
        absentResident();

        assertThatThrownBy(() -> correct(987_654_321L, Map.of("reason", "x"))).hasMessageContaining("không tồn tại");
    }

    @Test
    void FR_049a_onAFinalStatusResident_onlyTheApprovalRoute_leavesTheShownReasonUntilApproved() {
        long id = residentIn(ResidentStatus.IN_RESIDENCE);
        user.set(office.user());
        statusService.endResidency(target(id), new Reason("Lý do cũ"), noDetails());
        long historyId = lastHistoryId(id);

        // người ghi gốc không tự đính chính trên hồ sơ trạng thái cuối (cách A bị chặn, như lần đánh giá)
        assertThatThrownBy(() -> correct(historyId, Map.of("reason", "Lý do đúng")))
                .hasMessageContaining("Không được đính chính");

        // đồng nghiệp cùng vai trò lập yêu cầu phê duyệt (cách C)
        user.set(users.staff(com.cambae.backend.common.Role.OFFICE_STAFF).user());
        CorrectionResult result = correct(historyId, Map.of("reason", "Lý do đúng"));

        assertThat(result).isInstanceOf(CorrectionResult.PendingApproval.class);
        assertThat(statusService.history(target(id)).getLast().reason()).isEqualTo("Lý do cũ");
        assertThat(currentStatus(id)).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
    }
}
