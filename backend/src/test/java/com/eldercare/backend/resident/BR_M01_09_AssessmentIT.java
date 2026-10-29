package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.AssessmentView;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.ConfirmResult;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.DraftContent;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

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
    void FR_028_confirmWithoutComment_isRejectedAndNothingChanges() {
        long resident = admissionResident();
        AssessmentView draft = nurseDraft(resident);
        user.set(doctor.user());
        AssessmentView withLevelOnly = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(CareLevel.REGULAR, null, null, Set.of()));

        assertThatThrownBy(() -> assessments.confirm(target(resident), draft.id(), etag(withLevelOnly)))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        e -> assertThat(missing(e)).containsExactly("comment"));
        assertThat(currentCareLevel(resident)).isNull();
        assertThat(historyRows(resident)).isZero();
        assertThat(assessments.get(target(resident), draft.id()).status()).isEqualTo(AssessmentStatus.DRAFT);
    }

    @Test
    void FR_028_blankCommentOrMissingLevel_isRejected_listingBothMissingFields() {
        long resident = admissionResident();
        AssessmentView draft = nurseDraft(resident);
        user.set(doctor.user());
        AssessmentView blank = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(null, "   ", "Cẩn thận", Set.of()));

        assertThatThrownBy(() -> assessments.confirm(target(resident), draft.id(), etag(blank)))
                .isInstanceOfSatisfying(
                        ConditionNotMetException.class,
                        e -> assertThat(missing(e)).containsExactlyInAnyOrder("comment", "careLevel"));
    }

    @Test
    void DBR_05_checkConstraint_blocksConfirmingWithoutCommentOrLevelAtDatabase() {
        long resident = admissionResident();
        AssessmentView draft = nurseDraft(resident);

        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE assessment SET status = 'CONFIRMED', confirmed_by = ?, confirmed_at = NOW(6)"
                                + " WHERE id = ?",
                        doctor.accountId(),
                        draft.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_assessment_confirm");
        assertThatThrownBy(() -> jdbc.update(
                        "UPDATE assessment SET status = 'CONFIRMED', comment = '   ', care_level = 'BASIC',"
                                + " confirmed_by = ?, confirmed_at = NOW(6) WHERE id = ?",
                        doctor.accountId(),
                        draft.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("chk_assessment_confirm");
    }

    @Test
    void BR_M01_09_nurseCannotConfirm_onlyDoctorCan() {
        long resident = admissionResident();
        AssessmentView draft = nurseDraft(resident);
        user.set(doctor.user());
        AssessmentView ready = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(CareLevel.BASIC, "Tỉnh táo", null, Set.of()));

        user.set(nurse.user());
        assertThatThrownBy(() -> assessments.confirm(target(resident), draft.id(), etag(ready)))
                .isInstanceOf(PermissionDeniedException.class);
        assertThat(currentCareLevel(resident)).isNull();
    }

    @Test
    void BR_M01_09_nurseCanCreateAndEditDraft_caregiverCannotSeeAssessments() {
        long resident = admissionResident();
        AssessmentView draft = nurseDraft(resident);
        AssessmentView edited = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(CareLevel.SPECIAL, "Cần theo dõi", null, Set.of()));
        assertThat(edited.careLevel()).isEqualTo(CareLevel.SPECIAL);
        assertThat(edited.status()).isEqualTo(AssessmentStatus.DRAFT);

        var caregiver = users.staff(Role.CAREGIVER);
        assignNurse(caregiver, resident);
        user.set(caregiver.user());
        assertThatThrownBy(() -> assessments.list(target(resident))).isInstanceOf(PermissionDeniedException.class);
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
                        "SELECT care_level, basis_type, basis_id FROM care_level_history WHERE resident_id = ?",
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

    @Test
    void FR_032_confirmedAssessment_isBlockedByTriggerAgainstDirectUpdate() {
        long resident = admissionResident();
        AssessmentView done = confirmed(resident, AssessmentType.INITIAL, CareLevel.BASIC, "Khỏe", null);

        assertThatThrownBy(() -> jdbc.update("UPDATE assessment SET comment = 'Đổi lén' WHERE id = ?", done.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("assessment");
    }

    @Test
    void FR_035_initialOnlyWhileAdmissionInProgress_reassessmentNeedsConfirmedInitialWhenAdmitting() {
        long residing = newResident();
        assignNurse(nurse, residing);
        user.set(nurse.user());
        assertThatThrownBy(() -> assessments.create(target(residing), AssessmentType.INITIAL, null))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("STATUS_NOT_ASSESSABLE"));

        long admitting = admissionResident();
        assignNurse(nurse, admitting);
        user.set(nurse.user());
        assertThatThrownBy(() -> assessments.create(target(admitting), AssessmentType.REASSESSMENT, "Sớm"))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("STATUS_NOT_ASSESSABLE"));

        long hospital = newResident();
        setStatus(hospital, "HOSPITALIZED");
        assignNurse(nurse, hospital);
        user.set(nurse.user());
        assertThatThrownBy(() -> assessments.create(target(hospital), AssessmentType.REASSESSMENT, "Về viện"))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("STATUS_NOT_ASSESSABLE"));
    }

    @Test
    void BR_M01_05_closedProfile_blocksNewAssessment() {
        long resident = newResident();
        setStatus(resident, "RESIDENCY_ENDED");
        assignNurse(nurse, resident);
        user.set(nurse.user());

        assertThatThrownBy(() -> assessments.create(target(resident), AssessmentType.REASSESSMENT, null))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("PROFILE_CLOSED"));
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
