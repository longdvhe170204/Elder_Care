package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.dto.RepresentativeSelection;
import com.eldercare.backend.resident.api.dto.WishSignerType;
import com.eldercare.backend.resident.internal.wish.domain.EndOfLifeWish;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 001 T067 (FR-023a, FR-023c, FR-023g, FR-023h, FR-011; US7 #2, #3, #7): người ký theo nhận định của Bác sĩ cho chính
 * phiếu. Tự quyết được thì người cao tuổi ký; ngược lại người đại diện của người bệnh (kèm cách được chọn) hoặc người đại
 * diện theo pháp luật (kèm quyết định của Tòa án) ký, và phải là người thân có quan hệ Hiệu lực. Nhận định thiếu cách hỗ
 * trợ hiểu hoặc ngày xem lại thì từ chối. Ý kiến khác lưu từng dòng có nguồn hợp lệ.
 */
class EndOfLifeWishSignerIT extends WishTestBase {

    @Test
    void FR_023c_residentWhoCanSelfDetermine_signsHimself_andAssessmentIsStoredOnTheWish() {
        long residentId = newResident();
        LocalDate review = LocalDate.now().plusDays(45);

        EndOfLifeWish wish = record(command(residentId)
                .assessment(true, "Dùng tranh minh họa, hỏi lại từng ý", review)
                .build());

        assertThat(wish.canSelfDetermine()).isTrue();
        assertThat(wish.signerType()).isEqualTo(WishSignerType.RESIDENT);
        assertThat(wish.signerFamilyMemberId()).isNull();
        assertThat(wish.supportMethod()).isEqualTo("Dùng tranh minh họa, hỏi lại từng ý");
        assertThat(wish.reviewDate()).isEqualTo(review);
        assertThat(wish.assessedAt()).isNotNull();
    }

    @Test
    void US7_2_residentWhoCannotSelfDetermine_cannotSignHimself() {
        long residentId = newResident();

        var error = failure(() -> record(command(residentId)
                .assessment(false, "Giải thích cho người nhà", LocalDate.now().plusDays(30))
                .build()));

        assertThat(error.errorCode()).isEqualTo("INVALID_SIGNER");
        assertThat(activeWishCount(residentId)).isZero();
    }

    @Test
    void FR_023c_representativeCannotSignWhenTheResidentCanSelfDetermine() {
        long residentId = newResident();
        long family = relatedFamilyMember(residentId);

        var error = failure(() -> record(command(residentId)
                .signer(WishSignerType.PATIENT_REPRESENTATIVE, family)
                .selection(RepresentativeSelection.CHOSEN_BY_FAMILY)
                .build()));

        assertThat(error.errorCode()).isEqualTo("INVALID_SIGNER");
    }

    @Test
    void FR_023c_patientRepresentative_storesHowTheyWereChosen() {
        long residentId = newResident();
        long family = relatedFamilyMember(residentId);

        EndOfLifeWish wish = record(command(residentId)
                .representativeSigns(family, RepresentativeSelection.CHOSEN_BY_FAMILY)
                .build());

        assertThat(wish.canSelfDetermine()).isFalse();
        assertThat(wish.signerType()).isEqualTo(WishSignerType.PATIENT_REPRESENTATIVE);
        assertThat(wish.signerFamilyMemberId()).isEqualTo(family);
        assertThat(wish.representativeSelection()).isEqualTo(RepresentativeSelection.CHOSEN_BY_FAMILY);
        assertThat(wish.respondentFamilyMemberId()).isEqualTo(family);
        assertThat(wish.relationship()).isEqualTo("Con gái");
    }

    @Test
    void FR_023c_patientRepresentativeWithoutHowTheyWereChosen_is422() {
        long residentId = newResident();
        long family = relatedFamilyMember(residentId);

        var error = failure(() ->
                record(command(residentId).representativeSigns(family, null).build()));

        assertThat(error.errorCode()).isEqualTo("REPRESENTATIVE_SELECTION_REQUIRED");
        assertThat(error.status().value()).isEqualTo(422);
    }

    @Test
    void FR_011_legalRepresentativeWithoutCourtDecision_is422() {
        long residentId = newResident();
        long family = relatedFamilyMember(residentId);

        var error = failure(() -> record(
                command(residentId).legalRepresentativeSigns(family, null).build()));

        assertThat(error.errorCode()).isEqualTo("COURT_DECISION_REQUIRED");
        assertThat(error.status().value()).isEqualTo(422);
    }

    @Test
    void FR_011_legalRepresentativeWithCourtDecision_isAccepted_andFilesAreAttached() {
        long residentId = newResident();
        long family = relatedFamilyMember(residentId);
        var decision = courtDecision();

        EndOfLifeWish wish = record(
                command(residentId).legalRepresentativeSigns(family, decision).build());

        assertThat(wish.signerType()).isEqualTo(WishSignerType.LEGAL_REPRESENTATIVE);
        assertThat(wish.courtDecision().number()).isEqualTo(decision.number());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM file WHERE id IN (?, ?) AND owner_type = ? AND owner_id = ?",
                        Integer.class,
                        wish.signedCopyFileId(),
                        decision.fileId(),
                        ENTITY,
                        wish.id()))
                .as("bản ký và quyết định của Tòa án gắn vào phiếu")
                .isEqualTo(2);
    }

    @Test
    void FR_023c_signerWithoutAnActiveRelationship_is409() {
        long residentId = newResident();
        long stranger = newFamilyMember();

        var error = failure(() -> record(command(residentId)
                .representativeSigns(stranger, RepresentativeSelection.CHOSEN_BY_RESIDENT)
                .build()));

        assertThat(error.errorCode()).isEqualTo("INVALID_SIGNER");
        assertThat(error.status().value()).isEqualTo(409);
    }

    @Test
    void FR_023c_respondentWhoIsNotTheResident_needsFamilyMemberAndRelationship() {
        long residentId = newResident();

        var error = failure(() -> record(command(residentId)
                .respondent(WishSignerType.PATIENT_REPRESENTATIVE, null, null)
                .build()));

        assertThat(error.errorCode()).isEqualTo("RESPONDENT_REQUIRED");
    }

    @Test
    void FR_023h_assessmentWithoutSupportMethodOrReviewDate_is422() {
        long residentId = newResident();

        assertThat(failure(() -> record(command(residentId).noAssessment().build()))
                        .errorCode())
                .isEqualTo("SELF_DETERMINATION_ASSESSMENT_REQUIRED");
        assertThat(failure(() -> record(command(residentId)
                                .assessment(true, "  ", LocalDate.now().plusDays(1))
                                .build()))
                        .errorCode())
                .as("thiếu cách hỗ trợ hiểu")
                .isEqualTo("SELF_DETERMINATION_ASSESSMENT_REQUIRED");
        assertThat(failure(() -> record(command(residentId)
                                .assessment(true, "Giải thích bằng lời", null)
                                .build()))
                        .errorCode())
                .as("thiếu ngày xem lại")
                .isEqualTo("SELF_DETERMINATION_ASSESSMENT_REQUIRED");
        assertThat(failure(() -> record(command(residentId)
                                .assessment(
                                        true,
                                        "Giải thích bằng lời",
                                        LocalDate.now().minusDays(1))
                                .build()))
                        .errorCode())
                .as("ngày xem lại không trước ngày nhận định")
                .isEqualTo("INVALID_REVIEW_DATE");
        assertThat(activeWishCount(residentId)).isZero();
    }

    @Test
    void FR_023a_assessingDoctorMustHaveTheDoctorRole() {
        long residentId = newResident();

        var error = failure(() ->
                record(command(residentId).assessingDoctor(nurse.accountId()).build()));

        assertThat(error.errorCode()).isEqualTo("INVALID_ASSESSING_DOCTOR");
    }

    @Test
    void FR_023a_signedCopyIsRequired() {
        long residentId = newResident();

        var error = failure(() -> record(command(residentId).signedCopy(null).build()));

        assertThat(error.errorCode()).isEqualTo("EVIDENCE_REQUIRED");
        assertThat(error.status().value()).isEqualTo(422);
    }

    @Test
    void US7_3_otherOpinions_areStoredOnePerRowWithTheirSource() {
        long residentId = newResident();
        long son = relatedFamilyMember(residentId);
        long daughter = relatedFamilyMember(residentId);

        EndOfLifeWish wish = record(command(residentId)
                .opinion(OpinionSourceType.FAMILY_MEMBER, son, "Muốn chuyển bệnh viện")
                .opinion(OpinionSourceType.FAMILY_MEMBER, daughter, "Ủng hộ ở lại viện")
                .opinion(OpinionSourceType.OBSERVING_STAFF, null, "Cụ hay nhắc nhớ nhà")
                .build());

        assertThat(wish.otherOpinions()).hasSize(3);
        assertThat(wish.otherOpinions().get(0).source().familyMemberId()).isEqualTo(son);
        assertThat(wish.otherOpinions().get(0).recordedBy()).isEqualTo(nurse.accountId());
        assertThat(wish.otherOpinions().get(2).source().type()).isEqualTo(OpinionSourceType.OBSERVING_STAFF);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM end_of_life_wish_opinion WHERE wish_id = ?", Integer.class, wish.id()))
                .isEqualTo(3);
        assertThat(wish.choice())
                .as("lựa chọn ghi theo ý người cao tuổi, không bị ý kiến khác đổi")
                .isNotNull();
    }

    @Test
    void FR_023g_opinionWithAnInvalidSource_is422_andNothingIsSaved() {
        long residentId = newResident();
        long stranger = newFamilyMember();

        var error = failure(() -> record(command(residentId)
                .opinion(OpinionSourceType.FAMILY_MEMBER, stranger, "Ý kiến của người không phải người thân")
                .build()));

        assertThat(error.errorCode()).isEqualTo("INVALID_OPINION_SOURCE");
        assertThat(error.status().value()).isEqualTo(422);
        assertThat(activeWishCount(residentId)).isZero();
        assertThat(List.of(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM end_of_life_wish_opinion o JOIN end_of_life_wish w"
                                + " ON w.id = o.wish_id WHERE w.resident_id = ?",
                        Integer.class,
                        residentId)))
                .containsExactly(0);
    }

    @Test
    void FR_023h_noColumnOrFieldNamedAfterCapacity_existsOnTheWish() {
        long residentId = newResident();
        record(command(residentId).build());

        var columns = jdbc.queryForList(
                "SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()"
                        + " AND TABLE_NAME LIKE 'end_of_life_wish%' AND (COLUMN_NAME LIKE '%capacity%'"
                        + " OR COLUMN_NAME LIKE '%nang_luc%' OR COLUMN_NAME LIKE '%competen%')",
                String.class);
        assertThat(columns)
                .as("nhận định gắn với phiếu là can_self_determine, không có dấu năng lực chung")
                .isEmpty();
    }
}
