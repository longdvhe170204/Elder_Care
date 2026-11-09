package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.AssessmentView;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.ConfirmResult;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.DraftContent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T034 (US3; BR-M01-09, FR-024 → FR-029, FR-031, DBR-05, Q-306): điều dưỡng lập nháp, chỉ Bác sĩ xác nhận; thiếu
 * nhận xét hoặc mức thì không xác nhận được và {@code CHECK} ở MySQL chặn một lớp nữa; xác nhận đặt mức hiện hành và dòng
 * lịch sử cùng transaction; không tạo bản ghi kế hoạch chăm sóc.
 */
class BR_M01_09_AssessmentIT extends AssessmentTestBase {

    private long admissionResident() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        return id;
    }

    private AssessmentView nurseDraft(long residentId) {
        assignNurse(nurse, residentId);
        user.set(nurse.user());
        return assessments.create(target(residentId), AssessmentType.INITIAL, "Tiếp nhận mới");
    }

    private static List<String> missing(ConditionNotMetException e) {
        return e.unmetConditions().stream().map(c -> c.code()).toList();
    }

    @Test
    void FR_033_confirmInitial_setsCurrentLevelAndHistoryInOneTransaction_noCarePlanCreated() {
        long resident = admissionResident();
        long need = careNeedId();
        AssessmentView draft = nurseDraft(resident);
        user.set(doctor.user());
        AssessmentView ready = assessments.saveDraft(
                target(resident),
                draft.id(),
                etag(draft),
                new DraftContent(null, "Đi lại khó, tỉnh táo", "Dễ ngã", CareLevel.SPECIAL, Set.of(need), null));

        ConfirmResult result = assessments.confirm(target(resident), draft.id(), etag(ready));

        assertThat(result.assessment().status()).isEqualTo(AssessmentStatus.CONFIRMED);
        assertThat(result.assessment().confirmedBy()).isEqualTo("#" + doctor.accountId());
        assertThat(result.effects().careLevelChanged()).isTrue();
        assertThat(currentCareLevel(resident)).isEqualTo("SPECIAL");
        assertThat(jdbc.queryForList(
                        "SELECT care_level, basis_type, basis_id FROM "
                                + com.eldercare.backend.support.ResidentHistoryRows.CARE_LEVEL
                                + " WHERE resident_id = ?",
                        resident))
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.get("care_level")).isEqualTo("SPECIAL");
                    assertThat(row.get("basis_type")).isEqualTo("ASSESSMENT");
                    assertThat(row.get("basis_id")).isEqualTo(draft.id());
                });
        assertThat(result.effects().carePlanReviewNotified()).isFalse();
        assertThat(carePlanRows()).isZero();
        assertThat(audit("ASSESSMENT_CONFIRMED", draft.id())).isEqualTo(1);
    }

    private int carePlanRows() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE()"
                        + " AND table_name LIKE 'care\\_plan%'",
                String.class);
        int rows = 0;
        for (String table : tables) {
            rows += jdbc.queryForObject("SELECT COUNT(*) FROM `" + table + "`", Integer.class);
        }
        return rows;
    }

    private int audit(String action, long assessmentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.ASSESSMENT' AND entity_id = ? AND action = ?",
                Integer.class,
                String.valueOf(assessmentId),
                action);
    }
}
