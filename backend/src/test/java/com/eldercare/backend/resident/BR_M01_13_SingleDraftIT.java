package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.VersionMismatchException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.AssessmentView;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;

/**
 * 001 T035 (US3; BR-M01-13, FR-026, FR-034): mỗi người cao tuổi tối đa một bản Nháp và một đánh giá đầu vào Đã xác nhận;
 * hủy nháp kèm lý do rồi tạo lại được; bản Đã hủy vẫn lưu, không sửa được. Cột sinh có {@code UNIQUE} giữ ràng buộc ở
 * cơ sở dữ liệu.
 */
class BR_M01_13_SingleDraftIT extends AssessmentTestBase {

    private long admissionResident() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        assignNurse(nurse, id);
        user.set(nurse.user());
        return id;
    }

    @Test
    void BR_M01_13_secondDraft_isRejectedWithDraftAlreadyExists() {
        long resident = admissionResident();
        assessments.create(target(resident), AssessmentType.INITIAL, "Lần 1");

        assertThatThrownBy(() -> assessments.create(target(resident), AssessmentType.INITIAL, "Lần 2"))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("DRAFT_ALREADY_EXISTS"));
        user.set(doctor.user());
        assertThatThrownBy(() -> assessments.create(target(resident), AssessmentType.INITIAL, "Bác sĩ"))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("DRAFT_ALREADY_EXISTS"));
        assertThat(assessments.list(target(resident))).hasSize(1);
    }

    @Test
    void BR_M01_13_uniqueIndex_blocksSecondDraftEvenAgainstDirectInsert() {
        long resident = admissionResident();
        assessments.create(target(resident), AssessmentType.INITIAL, "Lần 1");

        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO assessment (resident_id, type, status, performed_by, performed_at)"
                                + " VALUES (?, 'INITIAL', 'DRAFT', ?, NOW(6))",
                        resident,
                        nurse.accountId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_assessment_open_draft");
    }

    @Test
    void FR_026_discardNeedsReason_thenNewDraftIsAllowed_andDiscardedRowStaysImmutable() {
        long resident = admissionResident();
        AssessmentView first = assessments.create(target(resident), AssessmentType.INITIAL, "Lần 1");

        AssessmentView discarded = assessments.discard(target(resident), first.id(), new Reason("Nhập nhầm người"));

        assertThat(discarded.status()).isEqualTo(AssessmentStatus.DISCARDED);
        assertThat(jdbc.queryForMap("SELECT discarded_by, discard_reason FROM assessment WHERE id = ?", first.id()))
                .containsEntry("discard_reason", "Nhập nhầm người");
        AssessmentView second = assessments.create(target(resident), AssessmentType.INITIAL, "Lần 2");
        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(assessments.list(target(resident)))
                .extracting(AssessmentView::id)
                .contains(first.id(), second.id());

        assertThatThrownBy(() -> assessments.saveDraft(
                        target(resident), first.id(), etag(discarded), full(CareLevel.BASIC, "x", null, Set.of())))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("NOT_A_DRAFT"));
        assertThatThrownBy(() -> jdbc.update("UPDATE assessment SET basis = 'Đổi lén' WHERE id = ?", first.id()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("assessment");
    }

    @Test
    void FR_026_discardByOtherNurseIsDenied_butDoctorMay() {
        long resident = admissionResident();
        AssessmentView draft = assessments.create(target(resident), AssessmentType.INITIAL, "Lần 1");

        var otherNurse = users.staff(Role.NURSING_STAFF);
        assignNurse(otherNurse, resident);
        user.set(otherNurse.user());
        assertThatThrownBy(() -> assessments.discard(target(resident), draft.id(), new Reason("Không phải của tôi")))
                .isInstanceOf(PermissionDeniedException.class);

        user.set(doctor.user());
        assertThat(assessments
                        .discard(target(resident), draft.id(), new Reason("Bác sĩ hủy"))
                        .status())
                .isEqualTo(AssessmentStatus.DISCARDED);
    }

    @Test
    void FR_034_secondConfirmedInitial_isRejectedByServiceAndByUniqueIndex() {
        long resident = admissionResident();
        user.set(doctor.user());
        confirmed(resident, AssessmentType.INITIAL, CareLevel.BASIC, "Đầu vào", null);

        user.set(nurse.user());
        assertThatThrownBy(() -> assessments.create(target(resident), AssessmentType.INITIAL, "Thêm"))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("INITIAL_ASSESSMENT_EXISTS"));
        assertThatThrownBy(() -> jdbc.update(
                        "INSERT INTO assessment (resident_id, type, status, comment, care_level, performed_by,"
                                + " performed_at, confirmed_by, confirmed_at)"
                                + " VALUES (?, 'INITIAL', 'CONFIRMED', 'x', 'BASIC', ?, NOW(6), ?, NOW(6))",
                        resident,
                        nurse.accountId(),
                        doctor.accountId()))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("uq_assessment_confirmed_initial");
    }

    @Test
    void BR_M01_13_staleIfMatch_isRejectedOnDraftEdit() {
        long resident = admissionResident();
        AssessmentView draft = assessments.create(target(resident), AssessmentType.INITIAL, "Lần 1");
        assessments.saveDraft(target(resident), draft.id(), etag(draft), full(CareLevel.BASIC, "A", null, Set.of()));

        assertThatThrownBy(() -> assessments.saveDraft(
                        target(resident), draft.id(), etag(draft), full(CareLevel.BASIC, "B", null, Set.of())))
                .isInstanceOf(VersionMismatchException.class);
    }
}
