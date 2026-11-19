package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentStatus;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.assessment.domain.FamilyDeclaredTopic;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.AssessmentView;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.FamilyDeclaredInput;
import com.eldercare.backend.resident.internal.assessment.service.AssessmentService.FamilyDeclaredView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T112 (Q-412, BR-M01-18, FR-025a; US3 #9, #10): Hành chính ghi phần "Gia đình khai" của lần đánh giá đầu vào khi
 * người cao tuổi còn Đang tiếp nhận, vào chính bản Nháp đang mở (kể cả do Bác sĩ, Điều dưỡng lập); chỉ thấy phần đó, không
 * sửa nhận xét, mức, không xác nhận, không hủy; phần này giữ nguyên khi Bác sĩ xác nhận và không đổi phiên bản của Nháp.
 */
class BR_M01_18_FamilyDeclaredIT extends AssessmentTestBase {

    @Autowired
    FakeFamilyRelationships families;

    private long admittingResident() {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        return id;
    }

    private List<FamilyDeclaredInput> fallsReportedBySon(long residentId) {
        long son = TestUsers.nextId();
        families.relate(son, residentId);
        return List.of(new FamilyDeclaredInput(
                FamilyDeclaredTopic.KNOWN_RISKS,
                "Từng ngã hai lần năm nay",
                new OpinionSource(OpinionSourceType.FAMILY_MEMBER, son)));
    }

    @Test
    void US3_9_10_officeWritesIntoDoctorsDraft_keptOnConfirm() {
        long residentId = admittingResident();
        AssessmentView draft =
                draftWithContent(residentId, AssessmentType.INITIAL, CareLevel.REGULAR, "Nhận xét của Bác sĩ", null);

        user.set(office.user());
        FamilyDeclaredView declared =
                assessments.recordFamilyDeclared(target(residentId), fallsReportedBySon(residentId));

        assertThat(declared.assessmentId()).isEqualTo(draft.id());
        assertThat(declared.created()).isFalse();
        assertThat(declared.entries()).singleElement().satisfies(e -> {
            assertThat(e.topic()).isEqualTo(FamilyDeclaredTopic.KNOWN_RISKS);
            assertThat(e.content()).isEqualTo("Từng ngã hai lần năm nay");
        });
        // Hành chính không đọc, sửa, hủy bản Nháp của Bác sĩ
        assertThatThrownBy(() -> assessments.get(target(residentId), draft.id()))
                .isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> assessments.saveDraft(
                        target(residentId),
                        draft.id(),
                        etag(draft),
                        full(CareLevel.BASIC, "x", null, java.util.Set.of())))
                .isInstanceOf(PermissionDeniedException.class);
        assertThatThrownBy(() -> assessments.discard(target(residentId), draft.id(), new Reason("nhầm")))
                .isInstanceOf(PermissionDeniedException.class);

        // Không đổi phiên bản: Bác sĩ xác nhận bằng ETag cũ, phần "Gia đình khai" giữ nguyên
        user.set(doctor.user());
        AssessmentView confirmed =
                assessments.confirm(target(residentId), draft.id(), etag(draft)).assessment();
        assertThat(confirmed.status()).isEqualTo(AssessmentStatus.CONFIRMED);
        assertThat(confirmed.familyDeclared())
                .singleElement()
                .satisfies(e -> assertThat(e.content()).isEqualTo("Từng ngã hai lần năm nay"));

        user.set(office.user());
        assertThatThrownBy(() -> assessments.familyDeclared(target(residentId)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("NO_DRAFT");
    }

    @Test
    void FR_025a_noDraft_officeCreatesInitialDraft_onlyWhileAdmitting() {
        long residentId = admittingResident();
        user.set(office.user());

        FamilyDeclaredView declared =
                assessments.recordFamilyDeclared(target(residentId), fallsReportedBySon(residentId));

        assertThat(declared.created()).isTrue();
        assertThat(assessments.familyDeclared(target(residentId)).assessmentId())
                .isEqualTo(declared.assessmentId());
        assertThatThrownBy(() -> assessments.recordFamilyDeclared(
                        target(residentId),
                        List.of(new FamilyDeclaredInput(
                                FamilyDeclaredTopic.MOBILITY,
                                "Đi lại được",
                                new OpinionSource(OpinionSourceType.OBSERVING_STAFF, null)))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo("INVALID_OPINION_SOURCE");
        // Bác sĩ, Điều dưỡng không ghi phần này
        user.set(doctor.user());
        assertThat(assessments.get(target(residentId), declared.assessmentId()).familyDeclared())
                .hasSize(1);
        assertThatThrownBy(() -> assessments.recordFamilyDeclared(target(residentId), List.of()))
                .isInstanceOf(PermissionDeniedException.class);
        // Rời Đang tiếp nhận thì Hành chính mất quyền ở dòng này (chú thích ⁵²)
        setStatus(residentId, "IN_RESIDENCE");
        user.set(office.user());
        assertThatThrownBy(() -> assessments.familyDeclared(target(residentId)))
                .isInstanceOf(PermissionDeniedException.class);
    }
}
